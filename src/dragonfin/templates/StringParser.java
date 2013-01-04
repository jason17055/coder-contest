package dragonfin.templates;

import java.io.*;
import java.util.*;

class StringParser
{
	PushbackReader in;
	int st;
	StringBuilder cur;

	static enum Token
	{
		LITERAL,
		EXPRESSION;
	}

	StringParser(String rawString)
	{
		this.in = new PushbackReader(new StringReader(rawString));
		this.st = 0;
	}

	public Expression parse()
		throws IOException, TemplateSyntaxException
	{
		Expression rv = Expressions.Literal.EMPTY_STRING;
		Token t;

		while ( ( t = nextToken() ) != null )
		{
			if (t == Token.LITERAL)
			{
				Expression e = new Expressions.Literal(cur.toString());
				rv = Concatenate.concat(rv, e);
			}
			else if (t == Token.EXPRESSION)
			{
				String raw = cur.toString();
				Parser p = new Parser(new StringReader(raw));
				p.st = 2;
				Expression e = p.parseExpression();
				rv = Concatenate.concat(rv, e);
			}
			else
				throw new Error("unreachable");
		}

		return rv;
	}

	private Token nextToken()
		throws IOException
	{
		if (st == -1)
			return null;

		cur = new StringBuilder();
		while (st != -1)
		{
			int c = in.read();

			switch(st)
			{
			case 0: //starting state
				if (c == '\\') {
					st = 1;
				}
				else if (c == '$') {
					if (cur.length() != 0)
					{
						st = 0;
						in.unread(c);
						return Token.LITERAL;
					}
					st = 2;
				}
				else if (c == -1) {
					st = -1;
					return Token.LITERAL;
				}
				else {
					cur.append((char)c);
				}
				break;

			case 1: //backslash
				if (c == 'n') {
					cur.append('\n');
					st = 0;
				} else if (c == 'r') {
					cur.append('\r');
					st = 0;
				} else if (c == 't') {
					cur.append('\t');
					st = 0;
				} else if (c == -1) {
					st = -1;
					cur.append('\\');
					return Token.LITERAL;
				} else {
					cur.append((char)c);
					st = 0;
				}
				break;

			case 2:
				if (c == '$') {
					// $$ converted to single $
					cur.append('$');
					st = 0;
				} else if (c == '{') {
					// ${ start of expression
					assert cur.length() == 0;
					st = 4;
				} else if (Character.isJavaIdentifierStart(c)) {
					// $[A-Za-z] start of variable name
					cur.append((char)c);
					st = 3;
				} else if (c == -1) {
					cur.append('$');
					st = -1;
					return Token.LITERAL;
				} else {
					// anything else
					cur.append('$');
					in.unread(c);
					st = 0;
				}
				break;

			case 3: // token begins with "$a"
				if (Character.isJavaIdentifierPart(c)) {
					cur.append((char)c);
				} else if (c == -1) {
					st = -1;
					return Token.EXPRESSION;
				} else {
					st = 0;
					in.unread(c);
					return Token.EXPRESSION;
				}
				break;

			case 4: // token begins with "${a"
				if (c == '}') {
					st = 0;
					return Token.EXPRESSION;
				} else if (c == -1) {
					// incomplete expression;
					// treat the ${ as normal chars
					cur.insert(0, "${");//}}
					st = -1;
					return Token.LITERAL;
				} else {
					cur.append((char)c);
				}
				break;

			default:
				throw new Error("not reached");
			}
		}

		assert false;
		return null;
	}
}

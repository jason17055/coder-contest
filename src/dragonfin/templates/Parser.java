package dragonfin.templates;

import java.io.*;
import java.util.*;

class Parser
{
	TemplateToolkit toolkit;
	PushbackReader in;
	int lineno;
	int colno;
	int st;            //parser state (see nextToken)
	StringBuilder cur; //current token
	Token peekToken;
	boolean peeked;

	Parser(TemplateToolkit toolkit, BufferedReader in)
		throws IOException
	{
		this.toolkit = toolkit;
		this.in = new PushbackReader(in,1);
		this.st = 0;
		this.cur = new StringBuilder();
		this.lineno = 1;
		this.colno = 0;
	}

	static enum TokenType
	{
		EOF,
		LITERAL_STRING,
		SINGLE_QUOTE_STRING,
		FILTER,
		IDENTIFIER,
		PERIOD,
		EQUAL,
		NOT_EQUAL,
		ASSIGN,
		OPEN_PAREN,
		CLOSE_PAREN;
	}

	static class Token
	{
		TokenType token;
		String text;

		Token(TokenType token, String text)
		{
			this.token = token;
			this.text = text;
		}
	}

	static final Token FILTER    = new Token(TokenType.FILTER, "|");
	static final Token PERIOD    = new Token(TokenType.PERIOD, ".");
	static final Token EQUAL     = new Token(TokenType.EQUAL, "==");
	static final Token NOT_EQUAL = new Token(TokenType.NOT_EQUAL, "!=");
	static final Token ASSIGN    = new Token(TokenType.ASSIGN, "=");

	private Token makeIdentifier(String s)
	{
		return new Token(TokenType.IDENTIFIER, s);
	}

	private Token makeSingleQuoteString(String s)
	{
		return new Token(TokenType.SINGLE_QUOTE_STRING, s);
	}

	private TokenType peekToken()
		throws IOException, TemplateSyntaxException
	{
		if (!peeked)
		{
			peeked = true;
			peekToken = nextToken_real();
		}
		return peekToken != null ? peekToken.token : TokenType.EOF;
	}

	private Token eatToken(TokenType expectedType)
		throws IOException, TemplateSyntaxException
	{
		peekToken();
		peeked = false;

		if (peekToken == null)
			throw unexpectedEof();

		if (peekToken.token != expectedType)
		{
			throw unexpectedToken(peekToken.token, expectedType);
		}
		return peekToken;
	}
			
	private Token nextToken_real()
		throws IOException, TemplateSyntaxException
	{
		if (st == -1) //end of file
			return null;

		while (st != -1)
		{
			int c = in.read();
			if (c == '\n')
			{
				lineno++;
				colno = 0;
			}
			else
			{
				colno++;
			}

			switch(st)
			{
			case 0:
				if (c == -1) {
					Token t = new Token(
						TokenType.LITERAL_STRING,
						cur.toString()
						);
					st = -1;
					return t;
				} else if (c == '[') {
					st = 1;
				} else {
					cur.append((char)c);
				}
				break;
			case 1:
				if (c == -1) {
					cur.append('[');
					Token t = new Token(
						TokenType.LITERAL_STRING,
						cur.toString()
						);
					st = -1;
					return t;
				} else if (c == '%') {
					Token t = new Token(
						TokenType.LITERAL_STRING,
						cur.toString()
						);
					cur = new StringBuilder();
					st = 2;
					return t;
				} else {
					cur.append('[');
					cur.append((char)c);
					st = 0;
				}
				break;
			case 2:
				if (c == '%') {
					st = 3;
				} else if (c == '.') {
					return PERIOD;
				} else if (c == '=') {
					st = 5;
				} else if (c == '|') {
					return FILTER;
				} else if (c == '\'') {
					st = 6;
				} else if (Character.isJavaIdentifierStart(c)) {
					cur.append((char)c);
					st = 4;
				} else if (Character.isWhitespace(c)) {
					//do nothing
				} else if (c == '#') {
					st = 7;
				} else {
					throw unexpectedCharacter(c);
				}
				break;
			case 3: // token beginning with "%"
				if (c == -1) {
					throw unexpectedEof();
				} else if (c == ']') {
					assert cur.length() == 0;
					st = 0;
				} else {
					throw unexpectedCharacter(c);
				}
				break;
			case 4: // token beginning with [A-Za-z]
				if (Character.isJavaIdentifierPart(c))
				{
					// continues an identifier
					cur.append((char)c);
				}
				else
				{
					// end of identifier
					Token t = makeIdentifier(cur.toString());
					cur = new StringBuilder();
					st = 2;
					unread(c);
					return t;
				}
				break;

			case 5: // token beginning with "="
				if (c == '=')
				{
					return EQUAL;
				}
				else
				{
					assert cur.length() == 0;
					st = 2;
					unread(c);
					return ASSIGN;
				}

			case 6: // single-quote-delimited string
				if (c == '\'')
				{
					// end of string
					Token t = makeSingleQuoteString(cur.toString());
					cur = new StringBuilder();
					st = 2;
					return t;
				}
				else if (c == -1)
				{
					throw unexpectedEof();
				}
				else
				{
					cur.append((char)c);
				}
				break;

			case 7: // token beginning with # (i.e. a comment)
				if (c == '%')
				{
					// this might end the comment
					st = 8;
				}
				else if (c == '\n')
				{
					// end of comment
					st = 2;
				}
				else if (c == -1)
				{
					st = 2;
					unread(c);
				}
				else
				{
					// ignore
				}
				break;

			case 8: // found a % inside a comment
				if (c == ']')
				{
					// end of comment, also: end of
					// directive
					assert cur.length() == 0;
					st = 0;
				}
				else
				{
					st = 7;
					unread(c);
				}
				break;

			default:
				throw new Error("Should be unreachable");
			}

			assert c != -1;
		}
		assert false;
		return null;
	}

	private void unread(int c)
		throws IOException
	{
		if (c == '\n')
		{
			//note- this does not correctly track the column
			//number, but presumably this is being called to set
			//the finite state machine back to a basic state,
			//which will read the '\n' again without error,
			//and the column number will again be correct.
			lineno--;
		}
		else
		{
			colno--;
		}
		in.unread(c);
	}

	private SyntaxException unexpectedCharacter(int c)
	{
		return new SyntaxException("Unexpected character ("+((char)c)+")");
	}

	private SyntaxException unexpectedEof()
	{
		return new SyntaxException("Unexpected EOF");
	}

	private SyntaxException unexpectedToken(TokenType actual, TokenType expected)
	{
		return new SyntaxException("Found "+actual+" but expected "+expected);
	}

	class SyntaxException extends TemplateSyntaxException
	{
		SyntaxException(String message)
		{
			super(lineno, colno, message);
		}
	}

	public Document parse()
		throws IOException, TemplateSyntaxException
	{
		Document doc = new Document(toolkit);
		TokenType token;
		while ( (token = peekToken()) != TokenType.EOF )
		{
		System.err.println("token "+token);
			if (token == TokenType.LITERAL_STRING)
			{
				doc.parts.add(eatToken(token).text);
			}
			else if (token == TokenType.IDENTIFIER)
			{
				doc.parts.add(parseGetDirective());
			}
			else
			{
				throw new SyntaxException("Unexpected token: "+token);
			}
		}
		return doc;
	}

	private GetDirective parseGetDirective()
		throws IOException, TemplateSyntaxException
	{
		return new GetDirective(parseExpression());
	}

	private Expression parseExpression()
		throws IOException, TemplateSyntaxException
	{
		return parseChain();
	}

	private Expression parseChain()
		throws IOException, TemplateSyntaxException
	{
		TokenType t = peekToken();
		if (t == TokenType.OPEN_PAREN)
		{
			throw new Error("TODO");
		}

		Expression rv = new Variable(parseIdentifier());
		TokenType n = peekToken();
		while (n == TokenType.PERIOD)
		{
			eatToken(TokenType.PERIOD);
			String propName = parseIdentifier();
			rv = new GetProperty(rv, propName);

			n = peekToken();
		}

		return rv;
	}

	private String parseIdentifier()
		throws IOException, TemplateSyntaxException
	{
		Token t = eatToken(TokenType.IDENTIFIER);
		return t.text;
	}

	static abstract class Expression
	{
		abstract Object evaluate(Context ctx)
			throws TemplateRuntimeException;
	}

	static class Variable extends Expression
	{
		String variableName;
		Variable(String variableName)
		{
			this.variableName = variableName;
		}

		@Override
		Object evaluate(Context ctx)
		{
			return ctx.vars.get(variableName);
		}
	}

	static class GetDirective implements Directive
	{
		Expression expr;
		GetDirective(Expression expr)
		{
			this.expr = expr;
		}

		public void execute(Context ctx)
			throws IOException, TemplateRuntimeException
		{
			Object v = expr.evaluate(ctx);
			if (v != null)
			{
				ctx.out.write(v.toString());
			}
		}
	}
}

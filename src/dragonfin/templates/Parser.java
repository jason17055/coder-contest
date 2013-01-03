package dragonfin.templates;

import java.io.*;

class Parser
{
	TemplateToolkit toolkit;
	BufferedReader in;
	int st;            //parser state (see nextToken)
	StringBuilder cur; //current token
	Token peekToken;
	boolean peeked;

	Parser(TemplateToolkit toolkit, BufferedReader in)
		throws IOException
	{
		this.toolkit = toolkit;
		this.in = in;
		this.st = 0;
		this.cur = new StringBuilder();
	}

	static enum TokenType
	{
		LITERAL_STRING,
		IDENTIFIER;
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

	private Token makeIdentifier(String s)
	{
		return new Token(TokenType.IDENTIFIER, s);
	}

	private Token peekToken()
		throws IOException, TemplateSyntaxException
	{
		if (!peeked)
		{
			peeked = true;
			peekToken = nextToken_real();
		}
		return peekToken;
	}

	private Token eatToken()
		throws IOException, TemplateSyntaxException
	{
		Token t = peekToken();
		peeked = false;
		return t;
	}
			
	private Token nextToken_real()
		throws IOException, TemplateSyntaxException
	{
		if (st == -1) //end of file
			return null;

		while (st != -1)
		{
			int c = in.read();
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
				} else if (Character.isJavaIdentifierStart(c)) {
					cur.append((char)c);
					st = 4;
				} else if (Character.isWhitespace(c)) {
					//do nothing
				} else {
					throw new TemplateSyntaxException();
				}
				break;
			case 3:
				if (c == -1) {
					throw new TemplateSyntaxException("unexpected eof");
				} else if (c == ']') {
					assert cur.length() == 0;
					st = 0;
				} else {
					cur.append('%');
					cur.append((char)c);
					st = 2;
				}
				break;
			case 4:
				if (c == -1) {
					throw new TemplateSyntaxException("unexpected eof");
				} else if (c == '%') {
					Token t = makeIdentifier(cur.toString());
					cur = new StringBuilder();
					st = 3;
					return t;
				} else if (Character.isJavaIdentifierPart(c)) {
					cur.append((char)c);
				} else if (Character.isWhitespace(c)) {
					// end of identifier
					Token t = makeIdentifier(cur.toString());
					cur = new StringBuilder();
					st = 2;
					return t;
				} else {
					throw new TemplateSyntaxException();
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

	public Document parse()
		throws IOException, TemplateSyntaxException
	{
		Document doc = new Document(toolkit);
		Token t;
		while ( (t = peekToken()) != null)
		{
		System.err.println("token "+t.token);
			if (t.token == TokenType.LITERAL_STRING)
			{
				doc.parts.add(t.text);
				eatToken();
			}
			else if (t.token == TokenType.IDENTIFIER)
			{
				doc.parts.add(parseExpression());
			}
			else
			{
				throw new TemplateSyntaxException();
			}
		}
		return doc;
	}

	private GetDirective parseExpression()
		throws IOException, TemplateSyntaxException
	{
		Token identifier = eatToken();
		return new GetDirective(identifier.text);
	}

	static class GetDirective implements Directive
	{
		String variableName;
		GetDirective(String variableName)
		{
			this.variableName = variableName;
		}

		public void execute(Context ctx)
			throws IOException
		{
			Object v = ctx.vars.get(variableName);
			if (v != null)
			{
				ctx.out.write(v.toString());
			}
		}
	}
}

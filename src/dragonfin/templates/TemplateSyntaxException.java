package dragonfin.templates;

public class TemplateSyntaxException extends Exception
{
	public TemplateSyntaxException()
	{
		super("Template syntax error");
	}

	public TemplateSyntaxException(String message)
	{
		super(message);
	}
}

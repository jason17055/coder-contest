package dragonfin.templates;

public class TemplateSyntaxException extends Exception
{
	public TemplateSyntaxException(int lineno, int colno, String message)
	{
		super(String.format("%d:%d: %s", lineno, colno, message));
	}
}

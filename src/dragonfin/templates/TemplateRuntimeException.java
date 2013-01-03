package dragonfin.templates;

public class TemplateRuntimeException extends Exception
{
	public TemplateRuntimeException(String message)
	{
		super(message);
	}

	public TemplateRuntimeException(String message, Throwable cause)
	{
		super(message, cause);
	}
}

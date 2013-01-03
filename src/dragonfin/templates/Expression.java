package dragonfin.templates;

abstract class Expression
{
	abstract Object evaluate(Context ctx)
		throws TemplateRuntimeException;
}

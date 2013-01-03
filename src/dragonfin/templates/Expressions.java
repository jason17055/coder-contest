package dragonfin.templates;

public class Expressions
{
	private Expressions() {}

	static class Literal extends Expression
	{
		String literalText;
		public Literal(String literalText)
		{
			this.literalText = literalText;
		}

		@Override
		public Object evaluate(Context ctx)
		{
			return literalText;
		}
	}
}

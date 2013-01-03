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

		static final Literal EMPTY_STRING = new Literal("");
	}
}

class Concatenate extends Expression
{
	Expression lhs;
	Expression rhs;

	Concatenate(Expression lhs, Expression rhs)
	{
		this.lhs = lhs;
		this.rhs = rhs;
	}

	static Expression concat(Expression lhs, Expression rhs)
	{
		assert lhs != null;
		assert rhs != null;

		if ((lhs instanceof Expressions.Literal) &&
			(rhs instanceof Expressions.Literal))
		{
			Expressions.Literal a = (Expressions.Literal) lhs;
			Expressions.Literal b = (Expressions.Literal) rhs;
			return new Expressions.Literal(
				a.literalText + b.literalText
				);
		}
		else
		{
			return new Concatenate(lhs, rhs);
		}
	}

	@Override
	public Object evaluate(Context ctx)
		throws TemplateRuntimeException
	{
		Object a = lhs.evaluate(ctx);
		Object b = rhs.evaluate(ctx);
		if (a == null)
		{
			return b;
		}
		else if (b == null)
		{
			return a;
		}
		else
		{
			assert (a != null && b != null);
			return a.toString() + b.toString();
		}
	}
}

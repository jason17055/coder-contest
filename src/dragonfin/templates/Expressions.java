package dragonfin.templates;

import java.io.*;
import java.util.*;

public class Expressions
{
	private Expressions() {}

	static class Literal extends Expression
	{
		Object value;

		public Literal(Object value)
		{
			if (!((value instanceof String) || (value instanceof Number)))
				throw new Error("invalid Literal "+value);

			this.value = value;
		}

		@Override
		public Object evaluate(Context ctx)
		{
			return value;
		}

		static final Literal EMPTY_STRING = new Literal("");
	}

	static class ArrayLiteral extends Expression
	{
		List<Expression> parts;
		public ArrayLiteral(List<Expression> parts)
		{
			this.parts = parts;
		}

		@Override
		public Object evaluate(Context ctx)
			throws TemplateRuntimeException
		{
			List<Object> result = new ArrayList<Object>();
			for (Expression e : parts)
			{
				result.add(e.evaluate(ctx));
			}
			return result;
		}
	}

	static class CompareExpression extends Expression
	{
		Expression lhs;
		Parser.TokenType op;
		Expression rhs;
		public CompareExpression(Expression lhs, Parser.TokenType op, Expression rhs)
		{
			this.lhs = lhs;
			this.op = op;
			this.rhs = rhs;
		}

		@Override
		public Object evaluate(Context ctx)
			throws TemplateRuntimeException
		{
			Object a = lhs.evaluate(ctx);
			Object b = rhs.evaluate(ctx);

			if (op == Parser.TokenType.EQUAL)
			{
				return new Boolean(
					Value.checkEquality(a, b)
					);
			}
			else if (op == Parser.TokenType.NOT_EQUAL)
			{
				return new Boolean(
					!Value.checkEquality(a,b)
					);
			}
			else
			{
				throw new Error("invalid compare op: "+op);
			}
		}
	}

	static class NotExpression extends Expression
	{
		Expression expr;
		public NotExpression(Expression expr)
		{
			this.expr = expr;
		}

		@Override
		public Object evaluate(Context ctx)
			throws TemplateRuntimeException
		{
			Object v = expr.evaluate(ctx);
			return new Boolean(!Value.asBoolean(v));
		}
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
				Value.asString(a.value) + Value.asString(b.value)
				);
		}
		return new Concatenate(lhs, rhs);
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

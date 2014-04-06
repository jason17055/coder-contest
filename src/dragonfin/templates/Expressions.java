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

	static class HashLiteral extends Expression
	{
		List<SetDirective> parts;
		public HashLiteral(List<SetDirective> parts)
		{
			this.parts = parts;
		}

		@Override
		public Object evaluate(Context ctx)
			throws TemplateRuntimeException
		{
			HashMap<String,Object> hash = new HashMap<String,Object>();
			for (SetDirective sd : parts)
			{
				sd.executeOnHash(ctx, hash);
			}

			return hash;
		}
	}

	static class ArithmeticExpression extends Expression
	{
		Expression lhs;
		Parser.TokenType op;
		Expression rhs;
		public ArithmeticExpression(Expression lhs, Parser.TokenType op, Expression rhs)
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

			int a1 = Value.asInt(a);
			int b1 = Value.asInt(b);

			switch (op) {
			case PLUS:
				return new Integer(a1+b1);

			case MINUS:
				return new Integer(a1-b1);

			case MULTIPLY:
				return new Integer(a1*b1);

			case DIVIDE:
			case DIV:
				return new Integer(a1/b1);

			case PERCENT:
			case MOD:
				return new Integer(a1%b1);

			default:
				throw new Error("invalid arithmetic op: "+op);
			}
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

			switch (op) {
			case EQUAL:
				return new Boolean(
					Value.checkEquality(a, b)
					);
			case NOT_EQUAL:
				return new Boolean(
					!Value.checkEquality(a,b)
					);
			case GT:
				return new Boolean( Value.compare(a,b) > 0 );
			case GE:
				return new Boolean( Value.compare(a,b) >= 0 );
			case LT:
				return new Boolean( Value.compare(a,b) < 0 );
			case LE:
				return new Boolean( Value.compare(a,b) <= 0 );
			default:
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

	static class AndExpression extends Expression
	{
		Expression lhs;
		Expression rhs;
		public AndExpression(Expression lhs, Expression rhs)
		{
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override
		public Object evaluate(Context ctx)
			throws TemplateRuntimeException
		{
			Object v = lhs.evaluate(ctx);
			if (!Value.asBoolean(v)) {
				return v;
			}
			else {
				return rhs.evaluate(ctx);
			}
		}
	}

	static class OrExpression extends Expression
	{
		Expression lhs;
		Expression rhs;
		public OrExpression(Expression lhs, Expression rhs)
		{
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override
		public Object evaluate(Context ctx)
			throws TemplateRuntimeException
		{
			Object v = lhs.evaluate(ctx);
			if (Value.asBoolean(v)) {
				return v;
			}
			else {
				return rhs.evaluate(ctx);
			}
		}
	}
}

class IfExpression extends Expression
{
	Expression condition;
	Expression trueExpr;
	Expression falseExpr;

	public IfExpression(Expression condition, Expression trueExpr, Expression falseExpr)
	{
		this.condition = condition;
		this.trueExpr = trueExpr;
		this.falseExpr = falseExpr;
	}

	@Override
	public Object evaluate(Context ctx)
		throws TemplateRuntimeException
	{
		boolean eval = Value.asBoolean(condition.evaluate(ctx));
		if (eval) {
			return trueExpr.evaluate(ctx);
		}
		else {
			return falseExpr.evaluate(ctx);
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

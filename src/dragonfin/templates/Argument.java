package dragonfin.templates;

abstract class Argument
{
	Expression expr;
}

class SimpleArgument extends Argument
{
	SimpleArgument(Expression expr)
	{
		this.expr = expr;
	}
}

class NamedArgument extends Argument
{
	String name;
	NamedArgument(String name, Expression expr)
	{
		this.name = name;
		this.expr = expr;
	}
}

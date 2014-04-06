package dragonfin.templates;

import java.util.*;

class MethodCall extends Expression
{
	Expression objectExpr;
	String methodName;
	List<Argument> arguments;

	MethodCall(Expression objectExpr, String methodName, List<Argument> arguments)
	{
		this.objectExpr = objectExpr;
		this.methodName = methodName;
		this.arguments = arguments;
	}

	@Override
	Object evaluate(Context ctx)
		throws TemplateRuntimeException
	{
		Object obj = objectExpr.evaluate(ctx);
		Object [] args = new Object[arguments.size()];
		for (int i = 0; i < args.length; i++)
		{
			args[i] = arguments.get(i).expr.evaluate(ctx);
		}
		if (methodName.equals("substr") && args.length == 2)
		{
			String s = Value.asString(obj);
			int a = Value.asInt(args[0]);
			int b = Value.asInt(args[1]);
			return s.substring(a,a+b);
		}
		else
		{
			throw new TemplateRuntimeException("Invalid method call: "+methodName);
		}
	}
}

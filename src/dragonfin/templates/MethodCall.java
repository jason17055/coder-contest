package dragonfin.templates;

import java.lang.reflect.Method;
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
			return doDynamicMethod(obj, args);
		}
	}

	Object doDynamicMethod(Object subject, Object [] args)
		throws TemplateRuntimeException
	{
		try {

		Method m = subject.getClass().getMethod(methodName, args.getClass());
		Object rv = m.invoke(subject, new Object[] { args });
		return rv;

		}
		catch (Exception e) {
			throw new TemplateRuntimeException("Bad method call: "+methodName, e);
		}
	}
}

package dragonfin.templates;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;

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

		while (obj instanceof Callable)
		{
			Callable<?> asCallable = (Callable<?>) obj;
			try
			{
				obj = asCallable.call();
			}
			catch (Exception e)
			{
				throw new TemplateRuntimeException("Exception thrown by "+obj.getClass().getName()+".call() method", e);
			}
		}

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
		else if (methodName.equals("get") && args.length == 1)
		{
			String pname = args[0].toString();
			if (pname == null) {
				throw new TemplateRuntimeException("get vmethod: argument cannot be null");
			}
			return GetProperty.getPropertyValueOf(obj, pname);
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

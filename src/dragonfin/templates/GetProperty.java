package dragonfin.templates;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Callable;

class GetProperty extends Expression
{
	Expression subject;
	String propertyName;

	public GetProperty(Expression subject, String propertyName)
	{
		this.subject = subject;
		this.propertyName = propertyName;
	}

	@Override
	Object evaluate(Context ctx)
		throws TemplateRuntimeException
	{
		Object obj = subject.evaluate(ctx);
		if (obj == null)
			return null;

		return evalHelper(obj);
	}

	private Object evalHelper(Object obj)
		throws TemplateRuntimeException
	{
		if (obj instanceof Map)
		{
			return ((Map<?,?>)obj).get(propertyName);
		}

		if (obj instanceof List)
		{
			List<?> asList = (List<?>) obj;
			if ("size".equals(propertyName))
			{
				return new Integer(asList.size());
			}

			try
			{
				int i = Integer.parseInt(propertyName);
				return asList.get(i);
			}
			catch (NumberFormatException e)
			{
			}
		}

		String beanMethodName = "get"+propertyName.substring(0,1).toUpperCase()+propertyName.substring(1);
		try
		{
			Method m = obj.getClass().getMethod(beanMethodName, (Class[])null);
			Object rv = m.invoke(obj);
			return rv;
		}
		catch (NoSuchMethodException e)
		{
			//ignore
		}
		catch (Exception e)
		{
			throw new TemplateRuntimeException("Exception thrown by "
			+ obj.getClass().getName()+"."+beanMethodName+"() method", e);
		}

		try
		{
			Method m = obj.getClass().getMethod("get",
				new Class[] { String.class }
				);
			Object rv = m.invoke(obj, propertyName);
			return rv;
		}
		catch (NoSuchMethodException e)
		{
			// ignore
		}
		catch (Exception e)
		{
			throw new TemplateRuntimeException("Exception thrown by "+obj.getClass().getName()+".get() method", e);
		}

		if (obj instanceof Callable)
		{
			Callable<?> asCallable = (Callable<?>) obj;
			try
			{
			return evalHelper(asCallable.call());
			}
			catch (Exception e)
			{
				throw new TemplateRuntimeException("Exception thrown by "+obj.getClass().getName()+".call() method", e);
			}
		}

		throw new TemplateRuntimeException("Cannot access property '"+propertyName+"' of instance of "+obj.getClass().getName());
	}
}

package dragonfin.templates;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

class SetDirective implements Directive
{
	Expression lhs;
	Expression rhs;
	SetDirective(Expression lhs, Expression rhs)
	{
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		if (lhs instanceof GetProperty)
		{
			GetProperty gp = (GetProperty) lhs;
			setProperty(gp.subject, gp.propertyName, ctx);
		}
		else if (lhs instanceof Parser.Variable)
		{
			Parser.Variable var = (Parser.Variable) lhs;
			setVariable(var.variableName, ctx);
		}
		else
		{
			throw new TemplateRuntimeException("Invalid expression for assignment target");
		}
	}

	void executeOnHash(Context ctx, Map<String,Object> hash)
		throws TemplateRuntimeException
	{
		String k;
		if (lhs instanceof Parser.Variable) {
			Parser.Variable var = (Parser.Variable) lhs;
			k = var.variableName;
		}
		else {
			Object obj = lhs.evaluate(ctx);
			k = Value.asString(obj);
		}

		Object v = rhs.evaluate(ctx);
		hash.put(k, v);
	}

	private void setProperty(Expression subject, String propertyName, Context ctx)
		throws IOException, TemplateRuntimeException
	{
		Object obj = subject.evaluate(ctx);
		if (obj == null)
		{
			throw new TemplateRuntimeException("Cannot assign property on null object");
		}

		Object v = rhs.evaluate(ctx);
		if (obj instanceof Map)
		{
			@SuppressWarnings("unchecked")
			Map<String,Object> asMap = (Map)obj;
			asMap.put(propertyName, v);
			return;
		}

		if (obj instanceof List)
		{
			@SuppressWarnings("unchecked")
			List<Object> asList = (List)obj;
			try
			{
				int i = Integer.parseInt(propertyName);
				asList.set(i, v);
				return;
			}
			catch (NumberFormatException e)
			{
			}
		}

		String beanMethodName = "set"+propertyName.substring(0,1).toUpperCase()+propertyName.substring(1);
		try
		{
			Method m = obj.getClass().getMethod(beanMethodName, new Class[] { String.class } );
			m.invoke(obj, ""+v);
			return;
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
			Method m = obj.getClass().getMethod("pet",
				new Class[] { String.class, String.class }
				);
			m.invoke(obj, propertyName, ""+v);
			return;
		}
		catch (NoSuchMethodException e)
		{
			// ignore
		}
		catch (Exception e)
		{
			throw new TemplateRuntimeException("Exception thrown by "+obj.getClass().getName()+".put() method", e);
		}

		throw new TemplateRuntimeException("No mutator for property '"+propertyName+"' on "+obj.getClass().getName());
	}

	private void setVariable(String variableName, Context ctx)
		throws IOException, TemplateRuntimeException
	{
		Object v = rhs.evaluate(ctx);
		((ScopedVariables)ctx.vars).put(variableName, v);
	}
}

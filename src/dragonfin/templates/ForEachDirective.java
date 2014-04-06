package dragonfin.templates;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

class ForEachDirective implements Directive
{
	String loopVariableName;
	Expression listExpr;
	Directive content;

	ForEachDirective(String loopVariableName, Expression listExpr, Directive content)
	{
		this.loopVariableName = loopVariableName;
		this.listExpr = listExpr;
		this.content = content;
	}

	private void executeHelper(Context ctx, List<Object> asList)
		throws IOException, TemplateRuntimeException
	{
		@SuppressWarnings("unchecked")
		Map<String,Object> varsMap = (Map<String,Object>)ctx.vars;
		for (Object o : asList)
		{
			varsMap.put(loopVariableName, o);
			content.execute(ctx);
		}
	}

	void exHelper(Context ctx, Object v)
		throws IOException, TemplateRuntimeException
	{
		if (v == null)
		{
			//same as an empty list
			return;
		}

		if (v instanceof List)
		{
			@SuppressWarnings("unchecked")
			List<Object> asList = (List<Object>) v;

			executeHelper(ctx, asList);
			return;
		}

		if (v instanceof Object[])
		{
			// Note- this handles arrays of Object references only;
			// primitive arrays will still fail
			List<Object> asList = Arrays.asList((Object[])v);
			executeHelper(ctx, asList);
			return;
		}

		if (v instanceof Callable)
		{
			Callable<?> asCallable = (Callable<?>) v;
			try
			{
				Object v2 = asCallable.call();
				if (v2 != v) {
					exHelper(ctx, v2);
					return;
				}
			}
			catch (Exception e)
			{
				throw new TemplateRuntimeException("Exception thrown by "+v.getClass().getName()+".call() method", e);
			}
		}

		throw new TemplateRuntimeException("cannot iterate over non-list object, "+v.getClass());
	}

	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		Object v = listExpr.evaluate(ctx);
		exHelper(ctx, v);
	}
}

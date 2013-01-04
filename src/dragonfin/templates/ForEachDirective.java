package dragonfin.templates;

import java.io.*;
import java.util.*;

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

	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		Object v = listExpr.evaluate(ctx);
		if (v instanceof List)
		{
			@SuppressWarnings("unchecked")
			List<Object> asList = (List<Object>) v;
			@SuppressWarnings("unchecked")
			Map<String,Object> varsMap = (Map<String,Object>)ctx.vars;
			for (Object o : asList)
			{
				varsMap.put(loopVariableName, o);
				content.execute(ctx);
			}
		}
		else if (v == null)
		{
			//not an error
		}
		else
		{
			throw new TemplateRuntimeException("cannot iterate over non-list object");
		}
	}
}

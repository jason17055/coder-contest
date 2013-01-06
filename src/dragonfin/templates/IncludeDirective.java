package dragonfin.templates;

import java.io.*;
import java.util.*;

class IncludeDirective implements Directive
{
	Expression pathExpr;
	List<SetDirective> assignments;

	IncludeDirective(Expression pathExpr)
	{
		this.pathExpr = pathExpr;
		this.assignments = new ArrayList<SetDirective>();
	}

	void addAssignment(SetDirective d)
	{
		assignments.add(d);
	}

	protected HashMap<String,Object> makeArgs(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		HashMap<String,Object> args = new HashMap<String,Object>();
		for (SetDirective sd : assignments)
		{
			Parser.Variable var = (Parser.Variable) sd.lhs;
			Object val = sd.rhs.evaluate(ctx);
			args.put(var.variableName, val);
		}
		return args;
	}

	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		HashMap<String,Object> args = makeArgs(ctx);
		executeHelper(args, ctx);
	}

	protected void executeHelper(HashMap<String,Object> args, Context ctx)
		throws IOException, TemplateRuntimeException
	{
		String pathValue = Value.asString(pathExpr.evaluate(ctx));
		Map<String,?> oldVars = ctx.vars;
		ctx.vars = new Parameters(args, oldVars);
		try
		{
		ctx.toolkit.processHelper(pathValue, ctx);
		}
		catch (TemplateSyntaxException e)
		{
			throw new TemplateRuntimeException("Parse error on included file", e);
		}
		finally
		{
			ctx.vars = oldVars;
		}
	}
}

package dragonfin.templates;

import java.io.*;
import java.util.*;
import javax.script.Bindings;
import javax.script.SimpleBindings;

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

	protected Bindings makeArgs(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		SimpleBindings args = new SimpleBindings();
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
		Bindings args = makeArgs(ctx);
		executeHelper(args, ctx);
	}

	protected void executeHelper(Bindings args, Context ctx)
		throws IOException, TemplateRuntimeException
	{
		String pathValue = Value.asString(pathExpr.evaluate(ctx));
		Bindings oldVars = ctx.vars;
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

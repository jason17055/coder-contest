package dragonfin.templates;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

class IfDirective implements Directive
{
	Expression condition;
	Directive trueAction;
	Directive falseAction;

	public IfDirective(Expression condition, Directive trueAction, Directive falseAction)
	{
		this.condition = condition;
		this.trueAction = trueAction;
		this.falseAction = falseAction;
	}

	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		boolean eval = Value.asBoolean(condition.evaluate(ctx));
		if (eval)
			trueAction.execute(ctx);
		else
			falseAction.execute(ctx);
	}
}

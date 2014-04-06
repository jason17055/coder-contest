package dragonfin.templates;

import java.util.*;
import javax.script.SimpleBindings;

class FunctionCall extends Expression
{
	String functionName;
	List<Argument> arguments;

	FunctionCall(String functionName, List<Argument> arguments)
	{
		this.functionName = functionName;
		this.arguments = arguments;
	}

	@Override
	Object evaluate(Context ctx)
		throws TemplateRuntimeException
	{
		SimpleBindings argValues = new SimpleBindings();
		int unnamedCount = 0;
		for (int i = 0; i < arguments.size(); i++) {
			Argument arg = arguments.get(i);
			Object v = arg.expr.evaluate(ctx);
			String k;
			if (arg instanceof NamedArgument) {
				k = ((NamedArgument) arg).name;
			} else {
				k = new Integer(++unnamedCount).toString();
			}
			argValues.put(k, v);
		}
		argValues.put("#", new Integer(unnamedCount));

		if (ctx.vars.containsKey(functionName)) {
			Object x = ctx.vars.get(functionName);
			if (x instanceof Function) {
				Function f = (Function) x;
				try {
				return f.invoke(argValues);
				}
				catch (TemplateRuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new TemplateRuntimeException("Exception thrown by "+f.getClass().getName()+".invoke() method", e);
				}
			}
			throw new TemplateRuntimeException("property '"+functionName+"' is not a function");
		}

		if (ctx.toolkit.filters.containsKey(functionName)) {
			// apply a filter
			Filter f = ctx.toolkit.filters.get(functionName);
			return f.apply(Value.asString(argValues.get("1")));
		}

		throw new TemplateRuntimeException("function '"+functionName+"' is not defined");
	}
}

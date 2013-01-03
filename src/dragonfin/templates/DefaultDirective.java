package dragonfin.templates;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

class DefaultDirective implements Directive
{
	List<SetDirective> assignments;

	public DefaultDirective(List<SetDirective> assignments)
	{
		this.assignments = assignments;
	}

	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		for (SetDirective d : assignments)
		{
			Object v = d.lhs.evaluate(ctx);
			if (!Value.asBoolean(v))
			{
				d.execute(ctx);
			}
		}
	}
}

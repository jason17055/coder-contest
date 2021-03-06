package dragonfin.templates;

import java.io.*;
import java.util.*;
import javax.script.Bindings;

public class Document extends Block
{
	TemplateToolkit toolkit;

	Document(TemplateToolkit toolkit)
	{
		this.toolkit = toolkit;
	}

	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		Bindings oldMap = ctx.vars;
		ctx.vars = new ScopedVariables(oldMap);
		try
		{
			super.execute(ctx);
		}
		finally
		{
			ctx.vars = oldMap;
		}
	}
}

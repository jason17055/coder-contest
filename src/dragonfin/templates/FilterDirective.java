package dragonfin.templates;

import java.io.*;
import java.util.*;

class FilterDirective implements Directive
{
	String filterName;
	Directive content;

	FilterDirective(String filterName, Directive content)
	{
		this.filterName = filterName;
		this.content = content;
	}

	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		Filter f = ctx.toolkit.getFilter(filterName);
		if (f == null)
			throw new TemplateRuntimeException("undefined filter: "+filterName);

		StringWriter capture = new StringWriter();
		Writer oldWriter = ctx.out;
		ctx.out = capture;
		try
		{
			content.execute(ctx);
		}
		finally
		{
			ctx.out = oldWriter;
		}

		String s = f.apply(capture.toString());
		ctx.out.write(s);
	}
}

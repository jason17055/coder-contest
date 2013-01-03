package dragonfin.templates;

import java.io.*;
import java.util.*;

public class TemplateToolkit
{
	ResourceLoader resourceLoader;

	public TemplateToolkit(ResourceLoader resourceLoader)
	{
		this.resourceLoader = resourceLoader;
	}

	static class Context
	{
		String templateName;
		Map<String, Object> vars;
		Writer out;
	}

	void processDirective(Context ctx, String directive)
		throws IOException
	{
		directive = directive.trim();

		Object v = ctx.vars.get(directive);
		if (v != null)
		{
			String s = v.toString();
			ctx.out.write(s);
		}
		else
		{
			ctx.out.write("<!-- unrecognized directive "+directive+" -->");
		}
	}

	public void process(String templateName, Map<String,Object> vars, Writer out)
		throws IOException
	{
		Context ctx = new Context();
		ctx.templateName = templateName;
		ctx.vars = vars;
		ctx.out = out;

		if (ctx.vars == null)
		{
			ctx.vars = new HashMap<String,Object>();
		}

		BufferedReader in = new BufferedReader(
				new InputStreamReader(
					resourceLoader.getResourceStream(
							templateName),
					"UTF-8"
					)
				);

		int st = 0;
		int c;
		StringBuilder buf = null;
		while ( (c = in.read()) != -1 )
		{
			switch(st)
			{
			case 0:
				if (c == '[') {
					st = 1;
				} else {
					out.write((char)c);
				}
				break;
			case 1:
				if (c == '%') {
					st = 2;
					buf = new StringBuilder();
				} else {
					out.write('[');
					out.write((char)c);
					st = 0;
				}
				break;
			case 2:
				if (c == '%') {
					st = 3;
				} else {
					buf.append((char)c);
				}
				break;
			case 3:
				if (c == ']') {
					processDirective(ctx, buf.toString());
					st = 0;
				} else {
					buf.append('%');
					buf.append((char)c);
					st = 2;
				}
				break;
			default:
				throw new Error("Should be unreachable");
			}
		}
		in.close();
	}
}

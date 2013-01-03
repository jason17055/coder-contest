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

	public void process(String templateName, Map<String,?> vars, Writer out)
		throws IOException, TemplateSyntaxException, TemplateRuntimeException
	{
		Context ctx = new Context();
		ctx.toolkit = this;
		ctx.templateName = templateName;
		ctx.vars = vars;
		ctx.out = out;

		if (ctx.vars == null)
		{
			ctx.vars = new HashMap<String,Object>();
		}
		processHelper(templateName, ctx);
	}

	void processHelper(String templateName, Context ctx)
		throws IOException, TemplateSyntaxException, TemplateRuntimeException
	{
		BufferedReader in = new BufferedReader(
				new InputStreamReader(
					resourceLoader.getResourceStream(
							templateName),
					"UTF-8"
					)
				);
		Parser parser = new Parser(this, in);

		Document doc = parser.parseDocument();
		in.close();

		doc.execute(ctx);
	}

	public static void main(String [] args)
		throws Exception
	{
		TemplateToolkit toolkit = new TemplateToolkit(
				new DefaultResourceLoader()
				);
		OutputStreamWriter w = new OutputStreamWriter(System.out);
		toolkit.process(args[0], System.getenv(), w);
		w.close();
	}
}
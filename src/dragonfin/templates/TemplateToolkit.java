package dragonfin.templates;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import javax.script.Bindings;
import javax.script.SimpleBindings;

public class TemplateToolkit
{
	ResourceLoader resourceLoader;
	Map<String,Filter> filters;

	public TemplateToolkit(ResourceLoader resourceLoader)
	{
		this.resourceLoader = resourceLoader;
		this.filters = new HashMap<String,Filter>();
		this.filters.put("html", new HtmlFilter());
		this.filters.put("uc", new UppercaseFilter());
		this.filters.put("lc", new LowercaseFilter());
		this.filters.put("uri", new UriFilter());
	}

	public void process(String templateName, Bindings vars, Writer out)
		throws IOException, TemplateSyntaxException, TemplateRuntimeException
	{
		Context ctx = new Context();
		ctx.toolkit = this;
		ctx.templateName = templateName;
		ctx.vars = vars;
		ctx.out = out;

		if (ctx.vars == null)
		{
			ctx.vars = new SimpleBindings();
		}
		processHelper(templateName, ctx);
	}

	void processHelper(String templateName, Context ctx)
		throws IOException, TemplateSyntaxException, TemplateRuntimeException
	{
		InputStream stream = resourceLoader.getResourceStream(templateName);
		if (stream == null)
		{
			throw new FileNotFoundException(templateName);
		}

		BufferedReader in = new BufferedReader(
				new InputStreamReader(stream, "UTF-8")
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
		toolkit.process(args[0], new ScopedVariables(System.getenv()), w);
		w.close();
	}

	static class HtmlFilter implements Filter
	{
		public String apply(String s)
		{
			return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
		}
	}

	static class UriFilter implements Filter
	{
		public String apply(String s)
		{
			try
			{
			return URLEncoder.encode(s, "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				throw new Error("unexpected: "+e,e);
			}
		}
	}

	static class LowercaseFilter implements Filter
	{
		public String apply(String s)
		{
			return s.toLowerCase();
		}
	}

	static class UppercaseFilter implements Filter
	{
		public String apply(String s)
		{
			return s.toUpperCase();
		}
	}

	public Filter getFilter(String filterName)
	{
		return filters.get(filterName);
	}
}

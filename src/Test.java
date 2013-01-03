import org.apache.velocity.*;
import org.apache.velocity.runtime.resource.*;
import org.apache.velocity.runtime.resource.loader.*;
import org.apache.velocity.app.Velocity;
import java.io.*;

public class Test
{
	public static void main(String [] args)
		throws Exception
	{
		//Velocity.setProperty("resource.loader", "mine");
		//Velocity.setProperty("mine.resource.loader.instance", new MyResourceLoader());

		Velocity.init();
		VelocityContext ctx = new VelocityContext();
		ctx.put("name", "Jason");

		Template tmpl = Velocity.getTemplate("build/classes/templates/mytemplate.vm");

		OutputStreamWriter osw = new OutputStreamWriter(System.out);
		//StringWriter sw = new StringWriter();
		tmpl.merge(ctx, osw);

		//System.out.println("merge complete");
		//System.out.println(sw.toString().length() + " bytes");
		//System.out.println(sw);
		osw.close();
	}
}

class MyResourceLoader extends ResourceLoader
{
	@Override
	public void init(org.apache.commons.collections.ExtendedProperties configuration)
	{
	}

	@Override
	public InputStream getResourceStream(String source)
	{
		return ResourceLoader.class.getResourceAsStream("/templates/"+source);
	}

	@Override
	public boolean isSourceModified(Resource rsrc)
	{
		return false;
	}

	@Override
	public long getLastModified(Resource rsrc)
	{
		return 0;
	}
}

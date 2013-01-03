package dragonfin.templates;

import java.io.*;
import java.util.*;

public class Document
{
	TemplateToolkit toolkit;
	ArrayList<Object> parts;

	Document(TemplateToolkit toolkit)
	{
		this.toolkit = toolkit;
		this.parts = new ArrayList<Object>();
	}

	public void execute(Context ctx)
		throws IOException
	{
		for (Object o : parts)
		{
			if (o instanceof Directive)
			{
				((Directive)o).execute(ctx);
			}
			else
			{
				ctx.out.write(o.toString());
			}
		}
	}
}

package dragonfin.templates;

import java.io.*;
import java.util.*;

class Block implements Directive
{
	ArrayList<Object> parts;

	Block()
	{
		this.parts = new ArrayList<Object>();
	}

	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
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

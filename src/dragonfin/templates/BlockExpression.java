package dragonfin.templates;

import java.io.*;
import java.util.*;
import javax.script.Bindings;

class BlockExpression extends Expression
{
	Block content;

	BlockExpression(Block content)
	{
		this.content = content;
	}

	@Override
	Object evaluate(Context ctx)
		throws TemplateRuntimeException
	{
		// temporarily redirect output to in-memory buffer
		StringWriter contentCapture = new StringWriter();
		Writer oldWriter = ctx.out;
		ctx.out = contentCapture;
		try
		{
			content.execute(ctx);
		}
		catch (IOException e)
		{
			throw new TemplateRuntimeException(e.getMessage(), e);
		}
		finally
		{
			ctx.out = oldWriter;
		}

		return contentCapture.toString();
	}
}

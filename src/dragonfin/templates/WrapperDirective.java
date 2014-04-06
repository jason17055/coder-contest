package dragonfin.templates;

import java.io.*;
import java.util.*;
import javax.script.Bindings;

class WrapperDirective extends IncludeDirective
{
	Block content;

	WrapperDirective(Expression pathExpr)
	{
		super(pathExpr);
	}

	@Override
	public void execute(Context ctx)
		throws IOException, TemplateRuntimeException
	{
		Bindings args = makeArgs(ctx);

		StringWriter contentCapture = new StringWriter();

		// temporarily redirect output to in-memory buffer
		Writer oldWriter = ctx.out;
		ctx.out = contentCapture;
		try
		{
			content.execute(ctx);
		}
		finally
		{
			ctx.out = oldWriter;
		}

		args.put("content", contentCapture.toString());
		executeHelper(args, ctx);
	}
}

package dragonfin.templates;

import java.io.*;
import java.util.*;

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
		HashMap<String,Object> args = makeArgs(ctx);

		StringWriter contentCapture = new StringWriter();

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

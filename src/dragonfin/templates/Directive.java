package dragonfin.templates;

import java.io.*;

public interface Directive
{
	void execute(Context ctx)
		throws IOException, TemplateRuntimeException;
}

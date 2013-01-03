package dragonfin.templates;

import java.io.*;

public interface ResourceLoader
{
	InputStream getResourceStream(String path)
		throws FileNotFoundException;
}

package dragonfin.templates;

import java.io.*;

public class DefaultResourceLoader
	implements ResourceLoader
{
	//implements ResourceLoader
	public InputStream getResourceStream(String path)
		throws FileNotFoundException
	{
		FileInputStream file = new FileInputStream(path);
		return file;
	}
}

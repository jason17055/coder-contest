package dragonfin.contest.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.common.CommonFunctions.escapeUrl;

public class File
{
	final HttpServletRequest servletRequest;

	public String id;
	public String name;
	public String hash;
	private String text_content_cached;

	private static Charset UTF8 = Charset.forName("UTF-8");
	private static final Logger log = Logger.getLogger(
			File.class.getName());

	public static File byId(HttpServletRequest req, String id)
	{
		if (id == null) {
			return null;
		}

		File f = new File(req);
		f.id = id;
		return f;
	}

	public File(HttpServletRequest req)
	{
		this.servletRequest = req;
	}

	public String getUrl()
	{
		return servletRequest.getContextPath()+"/_f/"+escapeUrl(this.id)+"/"+escapeUrl(this.name);
	}

	public String getInline_text_url()
	{
		return getUrl() + "?type=text";
	}

	public String diff_url(Object [] args)
	{
		if (args.length == 0 || args[0] == null) {
			return servletRequest.getContextPath()+"/_f/diff?b="+escapeUrl(this.id);
		}

		if (args.length != 1) {
			throw new UnsupportedOperationException("diff_url: wrong number of arguments");
		}

		if (args[0] instanceof File) {
			File refFile = (File) args[0];
			return servletRequest.getContextPath()+"/_f/diff?a="+escapeUrl(refFile.id)+"&b="+escapeUrl(this.id);
		}
		else {
			throw new UnsupportedOperationException("diff_url: wrong argument type: "+args[0].getClass().getName());
		}
	}

	public Key getKey()
	{
		return KeyFactory.createKey("File", id);
	}

	public String getText_content()
		throws EntityNotFoundException, IOException
	{
		if (text_content_cached != null) {
			return text_content_cached;
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity fileEnt = ds.get(getKey());

		String contentType = (String)fileEnt.getProperty("content_type");
		//FIXME- had to comment this out because for some reason
		//some files are not being uploaded as text/plain!
		//if (!contentType.startsWith("text/")) {
		//	return null;
		//}

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		outputChunk(bytes, ds, (Key) fileEnt.getProperty("head_chunk"));

		text_content_cached = new String(bytes.toByteArray(), UTF8);
		return text_content_cached;
	}

	public static void outputChunk(OutputStream out, DatastoreService ds, Key chunkKey)
		throws IOException
	{
		try {
			new FileRetriever(ds).output(out, chunkKey);
		}
		catch (EntityNotFoundException e) {
			log.warning("Chunk "+e.getKey()+" not found");
			return;
		}
	}
}

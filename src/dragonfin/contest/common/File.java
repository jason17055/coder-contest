package dragonfin.contest.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;
import com.google.appengine.api.datastore.*;

public class File
{
	public String id;
	public String url;
	public String name;
	public String inline_text_url;
	private String text_content_cached;

	private static Charset UTF8 = Charset.forName("UTF-8");
	private static final Logger log = Logger.getLogger(
			File.class.getName());

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
		if (!contentType.startsWith("text/")) {
			return null;
		}

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		outputChunk(bytes, ds, (Key) fileEnt.getProperty("head_chunk"));

		text_content_cached = new String(bytes.toByteArray(), UTF8);
		return text_content_cached;
	}

	public static void outputChunk(OutputStream out, DatastoreService ds, Key chunkKey)
		throws IOException
	{
		Entity ent;
		try {
			ent = ds.get(chunkKey);
		}
		catch (EntityNotFoundException e) {
			log.warning("Chunk "+chunkKey+" not found");
			return;
		}

		@SuppressWarnings("unchecked")
		List<Key> partsList = (List<Key>) ent.getProperty("parts");
		if (partsList != null && !partsList.isEmpty()) {

			for (Key k : partsList) {
				outputChunk(out, ds, k);
			}
		}

		Blob b = (Blob) ent.getProperty("data");
		if (b != null) {

			out.write(b.getBytes());
		}
	}
}
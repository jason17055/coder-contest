package dragonfin.contest.common;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.google.appengine.api.datastore.*;
import java.util.logging.Logger;

public class FileUploadFormHelper
{
	private static final Logger log = Logger.getLogger(
			FileUploadFormHelper.class.getName());

	static final Charset UTF8 = Charset.forName("UTF-8");
	static String readStream(InputStream in)
		throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int nread;
		byte[] buf = new byte[8192];
		while ( (nread = in.read(buf)) != -1) {
			bytes.write(buf, 0, nread);
		}

		return new String(bytes.toByteArray(), UTF8);
	}

	static String byteArray2Hex(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(String.format("%02x", bytes[i]));
		}
		return sb.toString();
	}

	static class UploadHelper
	{
		static final int MAX_CHUNK_SIZE = 16*1024;
		static final int MAX_BRANCHES = 4;

		DatastoreService ds;
		InputStream stream;
		byte [] buf = new byte[MAX_CHUNK_SIZE];

		int chunkCount = 0;
		long fileLen = 0;

		static MessageDigest createDigestObject()
		{
			try {
				return MessageDigest.getInstance("SHA-1");
			}
			catch (NoSuchAlgorithmException e) {
				throw new Error("Unexpected "+e.getMessage(), e);
			}
		}

		MessageDigest md = createDigestObject();

		FilePart nextChunk()
			throws IOException
		{
			int off = 0;
			while (off < buf.length)
			{
				int nread = stream.read(buf, off, buf.length-off);
				if (nread == -1) {
					break;
				}
				off += nread;
			}

			if (off == 0) {
				return null;
			}

			chunkCount++;
			fileLen += off;

			FilePart p = new FilePart();
			p.data = Arrays.copyOfRange(buf, 0, off);
			makeDigest(p);
			return p;
		}

		void makeDigest(FilePart p)
		{
			md.update(p.data);
			byte [] digestBytes = md.digest();
			p.digestHex = byteArray2Hex(digestBytes);
		}

		/**
		 * @return key of head chunk generated by input stream.
		 */
		Key processStream(InputStream stream)
			throws IOException
		{
			this.stream = stream;

			ChunkLayer leafs = new ChunkLayer();

			FilePart p;
			while ( (p = nextChunk()) != null )
			{
				Key chunkKey = KeyFactory.createKey("FileChunk", p.digestHex);
				Entity ent = new Entity(chunkKey);
				ent.setProperty("last_touched", new Date());
				ent.setProperty("data", new Blob(p.data));
				ds.put(ent);

				leafs.add(chunkKey);
			}

			return leafs.getRoot();
		}

		class ChunkLayer
		{
			ArrayList<Key> chunks = new ArrayList<Key>();
			ChunkLayer parent;
			int depth;

			void add(Key chunk)
			{
				if (chunks.size() >= MAX_BRANCHES) {
					requireParent();
					Key myKey = uploadChunkIndex(this);
					parent.add(myKey);
					chunks.clear();
				}
				chunks.add(chunk);
			}

			void requireParent()
			{
				if (parent == null) {
					parent = new ChunkLayer();
					parent.depth = this.depth+1;
				}
			}

			Key getRoot()
			{
				if (parent == null && chunks.size() == 1) {
					return chunks.get(0);
				}

				Key myKey = uploadChunkIndex(this);
				if (parent == null) {
					return myKey;
				}
				else {
					parent.add(myKey);
					return parent.getRoot();
				}
			}
		}

		Key uploadChunkIndex(ChunkLayer idx)
		{
			for (Key ch : idx.chunks) {
				String name = ch.getName();
				md.update(name.getBytes(UTF8));
			}
			byte [] digestBytes = md.digest();
			String digestHex = byteArray2Hex(digestBytes);

			Key idxKey = KeyFactory.createKey("FileChunk", digestHex+"-idx-"+idx.depth);
			Entity ent = new Entity(idxKey);
			ent.setProperty("last_touched", new Date());
			ent.setProperty("parts", idx.chunks);
			ds.put(ent);

			return idxKey;
		}
	}

	static class FilePart
	{
		byte [] data;
		String digestHex;
	}

	File handleFileUpload(FileItemStream item)
		throws ServletException, IOException
	{
		String fileName = item.getName();
		String contentType = item.getContentType();

		if (fileName == null || fileName.equals("")) {
			return null;
		}

		UploadHelper helper = new UploadHelper();
		helper.ds = DatastoreServiceFactory.getDatastoreService();

		InputStream stream = item.openStream();
		Key headChunk = helper.processStream(stream);

		helper.md.reset();
		helper.md.update(fileName.getBytes(UTF8));
		helper.md.update((byte)0);
		helper.md.update(contentType.getBytes(UTF8));
		helper.md.update((byte)0);
		helper.md.update(headChunk.getName().getBytes(UTF8));

		byte[] digestBytes = helper.md.digest();
		String digestHex = byteArray2Hex(digestBytes);

		Key fileKey = KeyFactory.createKey("File", digestHex);
		Entity ent = new Entity(fileKey);

		ent.setProperty("uploaded", new Date());
		ent.setProperty("given_name", fileName);
		ent.setProperty("content_type", contentType);

		ent.setProperty("head_chunk", headChunk);
		
		helper.ds.put(ent);

		File f = new File();
		f.id = digestHex;
		f.name = fileName;
		return f;
	}

	public Map<String,String> processMultipartForm(HttpServletRequest req)
		throws ServletException, IOException
	{
		try {

		Map<String,String> formFields = new HashMap<String,String>();

		ServletFileUpload upload = new ServletFileUpload();
		FileItemIterator it = upload.getItemIterator(req);
		while (it.hasNext()) {
			FileItemStream item = it.next();

			if (item.isFormField()) {
				String name = item.getFieldName();
				String value = readStream(item.openStream());
				formFields.put(name, value);
			}
			else {
				String name = item.getFieldName();
				String fileName = item.getName();
				File f = handleFileUpload(item);
				formFields.put(name, f != null ? f.id : null);
				if (f != null) {
					formFields.put(name+".name", f.name);
				}
			}
		}

		req.setAttribute("POST", formFields);
		return formFields;

		} catch (FileUploadException e) {
			throw new ServletException(e);
		}
	}

}

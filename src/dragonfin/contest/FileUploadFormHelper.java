package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.File;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.google.appengine.api.datastore.*;

public class FileUploadFormHelper
{
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
		DatastoreService ds;
		InputStream stream;
		byte [] buf = new byte[16*1024];

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
			int nread = stream.read(buf);
			if (nread == -1) {
				return null;
			}

			FilePart p = new FilePart();
			p.data = Arrays.copyOfRange(buf, 0, nread);
			makeDigest(p);
			return p;
		}

		void makeDigest(FilePart p)
		{
			md.update(p.data);
			byte [] digestBytes = md.digest();
			p.digestHex = byteArray2Hex(digestBytes);
		}

		Key[] processStream(InputStream stream)
			throws IOException
		{
			this.stream = stream;

			ArrayList<Key> chunks = new ArrayList<Key>();

			FilePart p;
			while ( (p = nextChunk()) != null )
			{
				Key chunkKey = KeyFactory.createKey("FileChunk", p.digestHex);
				Entity ent = new Entity(chunkKey);
				ent.setProperty("data", p.data);
				ds.put(ent);

				chunks.add(chunkKey);
			}

			return chunks.toArray(new Key[0]);
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

		UploadHelper helper = new UploadHelper();
		helper.ds = DatastoreServiceFactory.getDatastoreService();

		InputStream stream = item.openStream();
		Key [] chunks = helper.processStream(stream);

		helper.md.reset();
		helper.md.update(fileName.getBytes(UTF8));
		helper.md.update((byte)0);
		helper.md.update(contentType.getBytes(UTF8));
		helper.md.update((byte)0);
		for (Key chunkKey : chunks) {
			String name = chunkKey.getName();
			helper.md.update(name.getBytes(UTF8));
		}

		byte[] digestBytes = helper.md.digest();
		String digestHex = byteArray2Hex(digestBytes);

		Key fileKey = KeyFactory.createKey("File", digestHex);
		Entity ent = new Entity(fileKey);

		ent.setProperty("uploaded", new Date());
		ent.setProperty("given_name", fileName);
		ent.setProperty("content_type", contentType);

		ent.setProperty("chunks", chunks);
		
		helper.ds.put(ent);

		File f = new File();
		f.id = digestHex;
		f.name = fileName;
		return f;
	}

	Map<String,String> processMultipartForm(HttpServletRequest req)
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
				formFields.put(name, f.name);
			}
		}

		req.setAttribute("POST", formFields);
		return formFields;

		} catch (FileUploadException e) {
			throw new ServletException(e);
		}
	}

}

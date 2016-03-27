package dragonfin.contest.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.google.appengine.api.datastore.*;

public class FileRetriever
{
	DatastoreService ds;
	static final int SIMUL_FETCH = 8;

	private static final Logger log = Logger.getLogger(
			FileRetriever.class.getName());

	public FileRetriever(DatastoreService ds)
	{
		this.ds = ds;
	}

	public void output(OutputStream out, Key chunkKey)
		throws IOException, EntityNotFoundException
	{
		Entity ent = ds.get(chunkKey);
		outputFromEntity(out, ent);
	}

	void outputChunkList(OutputStream out, List<Key> partsList)
		throws IOException, EntityNotFoundException
	{
		// Fetch several parts at once
		for (int i = 0; i < partsList.size(); i += SIMUL_FETCH) {
			int j = Math.min(i + SIMUL_FETCH, partsList.size());
			List<Key> subList = partsList.subList(i, j);
			Map<Key, Entity> entities = ds.get(subList);
			for (Key k : subList) {
				outputFromEntity(out, entities.get(k));
			}
		}
	}

	void outputFromEntity(OutputStream out, Entity ent)
		throws IOException, EntityNotFoundException
	{
		@SuppressWarnings("unchecked")
		List<Key> partsList = (List<Key>) ent.getProperty("parts");
		if (partsList != null && !partsList.isEmpty()) {

			outputChunkList(out, partsList);
		}

		Blob b = (Blob) ent.getProperty("data");
		if (b != null) {

			out.write(b.getBytes());
		}
	}
}

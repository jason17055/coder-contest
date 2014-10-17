package dragonfin.contest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import dragonfin.contest.common.FileUploadFormHelper;
import static dragonfin.contest.TemplateVariables.makeFileUrl;

public class PostJobResultServlet extends HttpServlet
{
	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String jobIdS = req.getParameter("job");
		long jobId;
		try {
			jobId = Long.parseLong(jobIdS);
		}
		catch (NumberFormatException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Map<String,String> POST = uploadForm.processMultipartForm(req);

		String statusStr = POST.get("status");
		statusStr = statusStr != null ? statusStr : "";

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key jobKey = KeyFactory.createKey("TestJob", jobId);
			Entity ent = ds.get(jobKey);

			String outputHash = POST.get("output_upload");
			if (outputHash != null) {
				Key fileKey = KeyFactory.createKey("File", outputHash);
				ent.setProperty("output", fileKey);
			}

			String detailHash = POST.get("detail_upload");
			if (detailHash != null) {
				Key fileKey = KeyFactory.createKey("File", detailHash);
				ent.setProperty("result_detail", fileKey);
			}

			ent.setProperty("result_status", statusStr);
			ent.setProperty("finished", Boolean.TRUE);
			ent.setProperty("last_touched", new Date());
			ent.removeProperty("claimed");
			ent.removeProperty("owner");
			ds.put(ent);

			txn.commit();
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		JobCompletedTask.enqueueTask(jobId);
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
}

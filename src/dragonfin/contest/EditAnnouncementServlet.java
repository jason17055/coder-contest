package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.TemplateVariables.makeUserKey;

public class EditAnnouncementServlet extends CoreServlet
{
	String getTemplate() {
		return "edit_announcement.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
	{
		HashMap<String,String> form = new HashMap<String,String>();
		form.put("recipient", "*");
		form.put("message", "");
		ctx.put("f", form);
	}

	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		FileUploadFormHelper.FormData POST = uploadForm.processMultipartForm(req);

		if (POST.containsKey("action:cancel")) {
			doCancel(req, resp);
		}
		else if (POST.containsKey("action:create_announcement")) {
			doCreateAnnouncement(POST, req, resp);
		}
		else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String u = req.getParameter("next");
		if (u == null) {
			u = makeContestUrl(req.getParameter("contest"), "controller");
		}
		resp.sendRedirect(u);
	}

	void doCreateAnnouncement(FileUploadFormHelper.FormData POST, HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		// url parameters
		String contestId = req.getParameter("contest");
		if (contestId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		// form parameters
		String recipient = POST.get("recipient");
		String message = POST.get("message");
		if (recipient == null || message == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if (recipient.startsWith("*")) {
			// true announcement
			try {

			createAnnouncement(contestId, recipient, message, null);

			}
			catch (EntityNotFoundException e) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}
		else {
			// a private message
			createPrivateMessage(contestId, recipient, message);
		}

		doCancel(req, resp);
	}

	static void createAnnouncement(String contestId, String recipientGroup, String message, String url)
		throws EntityNotFoundException
	{
		Key contestKey = KeyFactory.createKey("Contest", contestId);

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();
		try {

			Entity contestEnt = ds.get(contestKey);
			long announceId = contestEnt.hasProperty("last_announcement_id") ?
				((Long)contestEnt.getProperty("last_announcement_id")).longValue() : 0;
			announceId++;

			contestEnt.setProperty("last_announcement_id", new Long(announceId));
			ds.put(contestEnt);

			Key announceKey = KeyFactory.createKey(contestKey,
				"Announcement", announceId);
			Entity ent1 = new Entity(announceKey);
			ent1.setProperty("created", new Date());
			ent1.setProperty("recipient_group", recipientGroup);
			ent1.setProperty("message", message);
			ent1.setProperty("number", announceId);
			if (url != null) {
				ent1.setProperty("url", url);
			}
			ds.put(ent1);

			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}

	void createPrivateMessage(String contestId, String recipient, String message)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		Key userKey = makeUserKey(contestId, recipient);
		Entity ent = new Entity("Message", userKey);
		ent.setProperty("created", new Date());
		ent.setProperty("message", message);
		ent.setProperty("dismissed", Boolean.FALSE);
		ds.put(ent);
	}
}

package dragonfin.contest;

import java.io.IOException;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.fasterxml.jackson.core.*;

import static dragonfin.contest.TemplateVariables.makeUserKey;

public class CheckMessageServlet extends HttpServlet
{
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		CheckMessage m = new CheckMessage();
		m.req = req;
		m.resp = resp;
		m.timeout = req.getParameter("timeout");
		m.type = req.getParameter("type");
		m.after = req.getParameter("after");

		m.userKey = null;
		HttpSession sess = req.getSession(false);
		if (sess != null) {
			String sesContestId = (String) sess.getAttribute("contest");
			String username = (String) sess.getAttribute("username");
			if (sesContestId != null && username != null) {
				m.userKey = makeUserKey(sesContestId, username);
			}
		}

		String dismissMessage = req.getParameter("dismiss_message");
		if (dismissMessage != null) {
			m.dismissMessage(dismissMessage);
		}

		if (m.checkForMessage(req, resp)) {
			return;
		}

		resp.setContentType("text/json;charset=UTF-8");
		JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
		out.writeStartObject();
		out.writeEndObject();
		out.close();
	}

	class CheckMessage
	{
		HttpServletRequest req;
		HttpServletResponse resp;
		Key userKey;
		DatastoreService ds;

		String timeout;
		String type;
		String after;

		CheckMessage()
		{
			this.ds = DatastoreServiceFactory.getDatastoreService();
		}

		void dismissMessage(String messageId)
		{
			Key messageKey = KeyFactory.createKey(userKey, "Message", Long.parseLong(messageId));
			ds.delete(messageKey);
		}

		boolean checkForMessage(HttpServletRequest req, HttpServletResponse resp)
			throws IOException
		{
			if (userKey == null) {
				return false;
			}

			Query q = new Query("Message");
			q.setAncestor(userKey);
			q.addSort("created");

			PreparedQuery pq = ds.prepare(q);
			for (Entity ent : pq.asIterable()) {
				emitMessageDetails(ent);
				return true;
			}

			return false;
		}

		void emitMessageDetails(Entity ent)
			throws IOException
		{
			resp.setContentType("text/json;charset=UTF-8");
			JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
			out.writeStartObject();
			out.writeStringField("message_id", Long.toString(ent.getKey().getId()));
			out.writeStringField("message", (String)ent.getProperty("message"));
			out.writeStringField("url", (String)ent.getProperty("url"));
			out.writeStringField("messagetype", "N");
			out.writeEndObject();
			out.close();
		}
	}
}

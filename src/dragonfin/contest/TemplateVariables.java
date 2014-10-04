package dragonfin.contest;

import dragonfin.contest.model.*;

import java.util.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class TemplateVariables
{
	final DatastoreService ds;
	final HttpServletRequest req;

	public TemplateVariables(HttpServletRequest req)
	{
		this.ds = DatastoreServiceFactory.getDatastoreService();
		this.req = req;
	}

	ContestInfo getContest()
		throws DataHelper.NotFound
	{
		String contestId = req.getParameter("contest");
		if (contestId == null) {
			return null;
		}

		return DataHelper.loadContest(contestId);
	}
}

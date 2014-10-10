package dragonfin.contest;

import java.io.IOException;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

public class UpdateScoreTask extends HttpServlet
{
	static TaskOptions makeUrl()
	{
		return TaskOptions.Builder.withUrl("/_task/update_score");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
	}
}

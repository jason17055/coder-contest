package dragonfin.contest;

import javax.script.SimpleBindings;
import com.google.appengine.api.datastore.*;

public class TestResultServlet extends ProblemCoreServlet
{
	@Override
	public String getTemplate()
	{
		return "problem_test_result.tt";
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException
	{
		String jobId = tv.req.getParameter("id");

		TemplateVariables.TestJob j = tv.fetchTestJob(jobId);
		ctx.put("test_job", j);
	}
}

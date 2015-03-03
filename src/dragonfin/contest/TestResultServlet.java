package dragonfin.contest;

import java.io.IOException;
import javax.script.SimpleBindings;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.common.CommonFunctions.escapeUrl;

public class TestResultServlet extends ProblemCoreServlet
{
	@Override
	public String getTemplate()
	{
		return "problem_test_result.tt";
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws IOException, EntityNotFoundException
	{
		super.moreVars(tv, ctx);

		String contestId = tv.req.getParameter("contest");
		String problemId = tv.req.getParameter("problem");
		String jobId = tv.req.getParameter("id");

		TemplateVariables.TestJob j = tv.fetchTestJob(jobId);
		ctx.put("test_job", j);

		String problemUrl = makeContestUrl(contestId, "problem."+problemId+"/");
		String anotherTestUrl = problemUrl + "test?source=" + escapeUrl(j.sourceFileKey.getName());
		if (j.inputFileKey != null) {
			anotherTestUrl += "&input=" + escapeUrl(j.inputFileKey.getName());
		}
		if (tv.req.getParameter("next") != null) {
			anotherTestUrl += "&next=" + escapeUrl(tv.req.getParameter("next"));
		}
		ctx.put("another_test_url", anotherTestUrl);
	}
}

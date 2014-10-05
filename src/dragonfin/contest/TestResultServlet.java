package dragonfin.contest;

import java.io.IOException;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class TestResultServlet extends ProblemCoreServlet
{
	@Override
	public String getTemplate()
	{
		return "problem_test_result.tt";
	}
}

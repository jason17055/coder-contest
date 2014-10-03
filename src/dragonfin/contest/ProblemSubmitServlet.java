package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class ProblemSubmitServlet extends ProblemCoreServlet
{
	@Override
	public String getTemplatePageName()
	{
		return "problem_submit.tt";
	}
}

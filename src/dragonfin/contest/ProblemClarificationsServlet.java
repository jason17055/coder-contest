package dragonfin.contest;

import dragonfin.contest.common.*;
import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class ProblemClarificationsServlet extends ProblemCoreServlet
{
	public String getTemplate() {
		return "problem_clarifications.tt";
	}

}

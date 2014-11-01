package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class ProblemDescriptionServlet extends ProblemCoreServlet
{
	public String getTemplate() {
		return "problem_description.tt";
	}
}

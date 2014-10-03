package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class ResultInfo implements java.io.Serializable
{
	public String contest;
	public String username;
	public String problemId;
	public String url;
	public String score_html;
	public boolean opened;
}


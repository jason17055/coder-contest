package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class ContestBean implements java.io.Serializable
{
	public String id;
	public String title;
	public String createdBy;

	public ContestBean()
	{
	}

	public String getId()
	{
		return id;
	}

	public String getTitle()
	{
		return title;
	}

	public String getCreatedBy()
	{
		return createdBy;
	}
}

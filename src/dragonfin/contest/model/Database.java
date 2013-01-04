package dragonfin.contest.model;

import java.sql.*;
import javax.naming.*;
import javax.sql.*;

public class Database
{
	private Database() {}

	public static Connection getConnection()
		throws NamingException, SQLException
	{
		Context initContext = new InitialContext();
		Context envContext = (Context)initContext.lookup("java:/comp/env");
		DataSource dataSource = (DataSource)envContext.lookup("jdbc/contestDB");
		Connection conn = dataSource.getConnection();
		return conn;
	}
}

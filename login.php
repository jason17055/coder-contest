<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	$sql = "SELECT team_number FROM team
		WHERE contest=".db_quote($_REQUEST['contest'])."
		AND user=".db_quote($_REQUEST['username'])."
		AND password=SHA1(".db_quote($_REQUEST['password']).")
		";
	$result = mysql_query($sql);
	if ($user_info = mysql_fetch_row($result))
	{
		// found team login
		$_SESSION['is_team'] = $user_info[0];
		unset($_SESSION['is_judge']);
		unset($_SESSION['is_director']);
		unset($_SESSION['is_sysadmin']);
		$_SESSION['uid'] = "$_REQUEST[contest]/$_REQUEST[username]";
		$_SESSION['username'] = $_REQUEST['username'];

		$url = $_REQUEST['next_url'];
		if (!$url)
		{
			$url = ".";
		}
		header("Location: $url");
		exit();
	}

	$sql = "SELECT judge_id FROM judge
		WHERE contest=".db_quote($_REQUEST['contest'])."
		AND judge_user=".db_quote($_REQUEST['username'])."
		AND judge_password=SHA1(".db_quote($_REQUEST['password']).")
		";
	$result = mysql_query($sql);
	if ($user_info = mysql_fetch_row($result))
	{
		// found judge login
		$_SESSION['is_judge'] = $user_info[0];
		unset($_SESSION['is_team']);
		unset($_SESSION['is_director']);
		unset($_SESSION['is_sysadmin']);
		$_SESSION['uid'] = "$_REQUEST[contest]/$_REQUEST[username]";
		$_SESSION['username'] = $_REQUEST['username'];

		$url = $_REQUEST['next_url'];
		if (!$url)
		{
			$url = ".";
		}
		header("Location: $url");
		exit();
	}

	$sql = "SELECT contest_id FROM contest
		WHERE contest_id=".db_quote($_REQUEST['contest'])."
		AND director=".db_quote($_REQUEST['username'])."
		AND director_password=SHA1(".db_quote($_REQUEST['password']).")
		";
	$result = mysql_query($sql)
		or die("SQL error: " . mysql_error());
	if ($row = mysql_fetch_row($result))
	{
		// found director login
		unset($_SESSION['is_team']);
		unset($_SESSION['is_judge']);
		unset($_SESSION['is_sysadmin']);
		$_SESSION['is_director'] = $row[0];
		$_SESSION['uid'] = "$_REQUEST[contest]/$_REQUEST[username]";
		$_SESSION['username'] = $_REQUEST['username'];

		$url = $_REQUEST['next_url'];
		if (!$url)
		{
			$url = ".";
		}
		header("Location: $url");
		exit();
	}

	$sql = "SELECT * FROM passwd
		WHERE username=".db_quote($_REQUEST['username'])."
		AND password=SHA1(".db_quote($_REQUEST['password']).")
		";
	$result = mysql_query($sql);
	$user_info = mysql_fetch_assoc($result);
	if ($user_info)
	{
		$_SESSION['username'] = $_REQUEST['username'];
		$_SESSION['uid'] = $_REQUEST['username'];
		$_SESSION['is_sysadmin'] = ($user_info['is_sysadmin'] == 'Y');
		$url = $_REQUEST['next_url'];
		if (!$url)
		{
			$url = "index.php";
		}
		header("Location: $url");
		exit();
	}
	$message = "Error: invalid username/password";
}

if (isset($_REQUEST['logout']))
{
	unset($_SESSION['uid']);
	unset($_SESSION['username']);
	unset($_SESSION['is_sysadmin']);
	unset($_SESSION['is_director']);
	unset($_SESSION['is_judge']);
	unset($_SESSION['is_team']);
	header("Location: login.php");
	exit();
}


begin_page("Login");

if ($message) { ?>
<p class="error"><?php echo htmlspecialchars($message)?></p>
<?php } ?>
<form method="post" action="login.php">
<table>
<tr>
<td>Contest:</td>
<td><select name="contest"><?php
	$sql = "SELECT contest_id,title FROM contest
		WHERE enabled='Y'
		ORDER BY contest_id";
	$query = mysql_query($sql)
		or die("SQL error: " . mysql_error());
	while ($row = mysql_fetch_assoc($query)) { ?>
<option value="<?php echo htmlspecialchars($row['contest_id'])?>"<?php
	if ($row['contest_id'] == $_REQUEST['contest']) { echo ' selected="selected"'; } ?>><?php echo htmlspecialchars($row['title'])?></option>
<?php }
	?><option value="">(system administration)</option></select></td>
</tr>
<tr>
<td>Username:</td>
<td><input type="text" name="username" value="<?php echo htmlspecialchars($_REQUEST['username'])?>"></td>
</tr>
<tr>
<td>Password:</td>
<td><input type="password" name="password"></td>
</tr>
</table>
<div><button type="submit">Login</button></div>
</form>

<?php end_page()?>

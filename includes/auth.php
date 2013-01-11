<?php

session_start();

function require_auth()
{
	if ($_SESSION['username'])
		return true;

	$url = $_SERVER['PHP_SELF'];
	if ($_SERVER['QUERY_STRING'])
	{
		$url .= "?" . $_SERVER['QUERY_STRING'];
	}

	$login_url = "login.php?next_url=".urlencode($url);
	header("Location: $login_url");
	exit();
}

function is_director($contest_id)
{
	if (!$contest_id)
		die("Invalid contest_id");

	if (is_sysadmin())
		return true;

	return ($_SESSION['is_director'] == $contest_id);
}

function is_judge_of($contest_id)
{
	if (is_director($contest_id))
		return true;

	$judge_info = is_a_judge();
	if (!$judge_info)
		return false;

	return ($judge_info['contest'] == $contest_id);
}

function is_judge_of_problem($contest_id, $problem_judged_by)
{
	if (is_director($contest_id))
		return true;

	if (!is_judge_of($contest_id))
		return false;

	$judges_array = explode(',', $problem_judged_by);
	if (FALSE === array_search($_SESSION['username'], $judges_array))
		return false;

	return true;
}

function is_sysadmin()
{
	return $_SESSION['uid'] && $_SESSION['is_sysadmin'];
}

function get_team_identity()
{
	$sql = "SELECT team_number,team_name,contest,ordinal,is_contestant,is_judge
		FROM team
		WHERE team_number=".db_quote($_SESSION['is_team']);
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$team_info = mysql_fetch_assoc($result)
		or die("Error: $_SESSION[username] is not a team");
	return $team_info; 
}

function is_in_a_contest()
{
	if (preg_match('/^(\\d+)\\//', $_SESSION['uid'], $m))
	{
		return $m[1];
	}
	else
	{
		return 0;
	}
}

function is_a_team()
{
	$sql = "SELECT team_number,contest,
		CASE WHEN last_refreshed IS NULL OR last_refreshed<DATE_SUB(NOW(),INTERVAL 2 MINUTE) THEN 'N' ELSE 'Y' END AS online
		FROM team
		WHERE team_number=".db_quote($_SESSION['is_team']);
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$team_info = mysql_fetch_assoc($result);
	return $team_info;
}

function is_a_judge()
{
	$sql = "SELECT judge_id,contest,
		CASE WHEN last_refreshed IS NULL OR last_refreshed<DATE_SUB(NOW(),INTERVAL 2 MINUTE) THEN 'N' ELSE 'Y' END AS online
		FROM judge
		WHERE judge_id=".db_quote($_SESSION['is_judge']);
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$judge_info = mysql_fetch_assoc($result);
	return $judge_info;
}

function is_a_director()
{
	if ($_SESSION['is_director'])
	{
		return array(
		"contest" => $_SESSION['is_director'],
		);
	}

	$sql = "SELECT contest_id FROM contest
		WHERE director=".db_quote($_SESSION['uid']);
	$result = mysql_query($sql);
	if ($row = mysql_fetch_assoc($result))
	{
		return $row['contest_id'];
	}
	return 0;
}

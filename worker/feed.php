<?php

openlog('scoreboard',LOG_PID | LOG_PERROR, LOG_LOCAL0);
syslog(LOG_NOTICE, 'in feed.php');

// connect to database
require_once('../config.php');
require_once('../includes/functions.php');
require_once('../includes/notify.php');
require_once('../includes/test_workflow.php');

$remote_user = $_SERVER['REMOTE_ADDR'] . " "
		. $_SERVER['HTTP_USER_AGENT'];
$accepted_languages_str = strtolower($_SERVER['HTTP_ACCEPTED_LANGUAGES']);
$worker_status_str = $_SERVER['HTTP_WORKER_STATUS'];

if ($_REQUEST['action:register'])
{
	$sql = "DELETE FROM worker
		WHERE last_refreshed < DATE_SUB(NOW(), INTERVAL 60 MINUTE)";
	mysql_query($sql);

	$sql = "INSERT INTO worker (id,accepted_languages,description,last_refreshed)
		VALUES (
		" . db_quote($remote_user) . ",
		" . db_quote($accepted_languages_str) . ",
		" . db_quote($_REQUEST['description']) . ",
		NOW())";
	if (!mysql_query($sql))
	{
		header("Content-Type: text/plain", true, "500");
		echo("Error: ".mysql_error());
		exit();
	}

	wakeup_listeners();
	header("HTTP/1.0 202 No content");
	exit();
}

$sql = "SELECT 1 FROM worker
	WHERE id=".db_quote($remote_user);
$query = mysql_query($sql)
	or die("SQL error: ".mysql_error());
$worker_info = mysql_fetch_assoc($query);
if (!$worker_info)
{
	header("Content-Type: text/plain", true, "500");
	echo("Error: worker not registered");
	exit();
}

$sql = "UPDATE worker
	SET last_refreshed=NOW(),
	accepted_languages=" . db_quote($accepted_languages_str) . ",
	status=" . db_quote($worker_status_str) . ",
	busy='N'
	WHERE id=".db_quote($remote_user);
mysql_query($sql)
	or die("SQL error: " . mysql_error());

function make_like_clause($ext)
{
	return "LOWER(source_name) LIKE ".db_quote("%.$ext");
}

function check_for_a_job($remote_user, $accepted_languages)
{
	$accepted_languages_sql = join(" OR ",
		array_map("make_like_clause", $accepted_languages));
	$sql = "SELECT * FROM test_job
		WHERE owner IS NULL
		AND ($accepted_languages_sql)
		ORDER BY id
		LIMIT 1";
	$result = mysql_query($sql);
	$job_info = mysql_fetch_assoc($result);
	if ($job_info)
	{
		if (isset($_POST['action:claim']))
		{
			$sql = "UPDATE test_job
				SET owner=".db_quote($remote_user).",
				last_touched=NOW()
				WHERE id=".db_quote($job_info['id'])."
				AND owner IS NULL";
			mysql_query($sql)
				or die("SQL error: ".mysql_error());

			// verify that owner was set to this requester
			$sql = "SELECT 1 FROM test_job
				WHERE owner=".db_quote($remote_user)."
				AND id=".db_quote($job_info['id']);
			$query = mysql_query($sql)
				or die("SQL error: ".mysql_error());
			$arow = mysql_fetch_row($query);
			if (!$arow)
			{
				return 'claim-failed';
			}

			$sql = "UPDATE worker
				SET last_refreshed=NOW(),
				busy='Y'
				WHERE id=".db_quote($remote_user);
			mysql_query($sql)
				or die("SQL error: " . mysql_error());
		}

		header("Content-Type: text/plain");
		echo "id $job_info[id]\n";
		echo "type $job_info[type]\n";
		echo "hash $job_info[source_file]\n";
		echo "source_file ../file.php/$job_info[source_file]/$job_info[source_name]\n";
		echo "input_file ../file.php/$job_info[input_file]/input.txt\n";
		if ($job_info['expected_file']) {
			echo "expected_file ../file.php/$job_info[expected_file]/expected.txt\n";
		}
		if ($job_info['actual_file']) {
			echo "actual_file ../file.php/$job_info[actual_file]/actual.txt\n";
		}
		echo "timeout $job_info[runtime_limit]\n";
		exit();
	}

	return 'no-jobs';
}

if ($_SERVER['REQUEST_METHOD'] == "POST" && $_REQUEST['id'])
{
	$sql = "SELECT * FROM test_job
		WHERE id=".db_quote($_REQUEST['id']);
	$result = mysql_query($sql);
	$job_info = mysql_fetch_assoc($result)
		or die("invalid job id");

	$f = $_FILES['output_file'];
	if ($f && $f['tmp_name'])
	{
		$hash = sha1_file($f['tmp_name']);
		$new_name = "../$uploaddir/$hash.txt";
		if (!move_uploaded_file($f['tmp_name'], $new_name))
		{
			die("Error: invalid upload file");
		}

		$_REQUEST['output_file'] = $hash;
	}

	$addl_sql = "";
	if (isset($_REQUEST['output_file']))
	{
		$addl_sql .= ", output_file=".db_quote($_REQUEST['output_file']);
	}
	else if ($_POST['status'] == 'No Error')
	{
		$_POST['detail'] .= "Warning: output file could not be saved because it was too large.\n";
	}

	$sql = "UPDATE test_job
		SET result_status=".db_quote($_POST['status']) . ",
		result_detail=".db_quote($_POST['detail']) . ",
		last_touched=NOW()
		$addl_sql
		WHERE id=".db_quote($_REQUEST['id']);
	mysql_query($sql)
		or die("SQL error: ".mysql_error());

	test_completed($_REQUEST['id']);
	header("HTTP/1.0 202 No content");
	exit();
}
else
{
	// look for a job that hasn't been executed yet
	$accepted_languages = explode(',', $accepted_languages_str);
	if (count($accepted_languages) == 0)
	{
		// no accepted languages, no jobs
		header("Content-Type: text/plain", true, "404");
		echo "Error: no Accepted-Languages header found\n";
		exit();
	}

	$remaining = 60;
	$stop_time = time() + $remaining;
	while ($remaining >= 1)
	{
		$s = check_for_a_job($remote_user, $accepted_languages);

		// no jobs, wait for up to 60 seconds
		$wait_time = $s == 'claim-failed' ? 1 : $remaining;
		wait_worker($wait_time);
		$remaining = $stop_time - time();
	}

	header("Content-Type: text/plain", true, "404");
	echo "No jobs at this time\n";
	exit();
}

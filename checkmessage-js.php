<?php

openlog('scoreboard',LOG_PID | LOG_PERROR, LOG_LOCAL0);
syslog(LOG_NOTICE, 'in checkmessage-js.php');

require_once('config.php');
require_once('includes/auth.php');
require_once('includes/notify.php');
require_once('includes/js_quote.php');

$contest_id = 0;
$targets = array();
$targets[] = db_quote('*');
$targets[] = db_quote($_SESSION['username']);
if ($team_info = is_a_team())
{
	$targets[] = db_quote('teams');
	$contest_id = $team_info['contest'];

	$updates = array();
	$updates[] = "last_refreshed=NOW()";
	if ($_REQUEST['after'])
	{
		$updates[] = "last_message_acked=".db_quote($_REQUEST['after']);
	}

	$sql = "UPDATE team SET ".join(',',$updates)."
		WHERE contest=".db_quote($contest_id)."
		AND team_number=".db_quote($team_info['team_number']);
	mysql_query($sql);

	if ($team_info['online'] == 'N')
		wakeup_listeners();
}
else if ($director_info = is_a_director())
{
	$targets[] = db_quote('judges');
	$contest_id = $director_info['contest'];
}
else if ($judge_info = is_a_judge())
{
	$targets[] = db_quote('judges');
	$contest_id = $judge_info['contest'];

	$updates = array();
	$updates[] = "last_refreshed=NOW()";
	if ($_REQUEST['after'])
	{
		$updates[] = "last_message_acked=".db_quote($_REQUEST['after']);
	}

	$sql = "UPDATE team SET ".join(',',$updates)."
		WHERE contest=".db_quote($contest_id)."
		AND team_number=".db_quote($judge_info['judge_id']);
	mysql_query($sql);

	if ($judge_info['online'] == 'N')
		wakeup_listeners();
}
else if ($_REQUEST['contest'])
{
	$targets[] = db_quote('scoreboard');
	$contest_id = $_REQUEST['contest'];
}

$sql = "SELECT * FROM contest
		WHERE contest_id=" . db_quote($contest_id);
$result = mysql_query($sql);
$contest_info = mysql_fetch_assoc($result);

session_write_close(); // ensure session lock is released before we start
			// going into a wait loop

function check_for_message()
{
	global $targets;
	global $contest_id;
	global $contest_info;

	$targets_sql = implode(", ", $targets);
	$other_sql = "";
	if ($_REQUEST['type'])
	{
		$other_sql = " AND messagetype IN (".db_quote($_REQUEST['type']).")";
	}
	if ($contest_info['scoreboard_popups'] != 'Y'
			|| $contest_info['scoreboard'] != 'Y')
	{
		$other_sql .= " AND messagetype<>'S'";
	}

	$sql = "SELECT * FROM announcement
		WHERE contest=".db_quote($contest_id) . "
		AND id>".db_quote($_REQUEST['after']) . "
		AND NOW()<expires
		AND (recipient IS NULL OR recipient IN ($targets_sql))
		$other_sql
		ORDER BY id
		LIMIT 1";
if ($_REQUEST['debug']) {
	echo htmlspecialchars($sql);
	exit();
	}
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$row = mysql_fetch_assoc($result);
	if ($row)
	{
		?>{
		<?php if ($row['messagetype'] == 'S' && $contest_info['scoreboard_fanfare'] == 'Y') { ?>
"fanfare": true,
<?php } ?>
"message_id": <?php echo js_quote($row['id'])?>,
"message": <?php echo js_quote($row['message'])?>,
"duration": <?php echo js_quote($row['duration'])?>,
"url": <?php echo js_quote($row['url'])?>,
"messagetype": <?php echo js_quote($row['messagetype'])?>
}
<?php
	syslog(LOG_NOTICE, 'checkmessage: returning message');
	exit();
}
	// no rows, so return to caller
	return false;
}

function check_for_online_status_change()
{
	$teams_on = array();
	$teams_off = array();

	foreach (explode(',', $_REQUEST['onlinestatuschange']) as $statcode)
	{
		if (preg_match('/^(team)_(\d+)_(Y|N)$/', $statcode, $matches))
		{
			if ($matches[1] == 'team')
			{
				if ($matches[3] == 'Y')
					$teams_on[] = $matches[2];
				else
					$teams_off[] = $matches[2];
			}
		}
		
	}

	$sql_stmts = array();
	if (count($teams_off))
	{
		$sql_stmts[] = "SELECT COUNT(*) FROM team
		WHERE team_number IN (" . join(',',$teams_off) . ")
			AND NOT (last_refreshed IS NULL OR last_refreshed < DATE_SUB(NOW(), INTERVAL ".USER_ONLINE_TIMEOUT."))";
	}
	if (count($teams_on))
	{
		$sql_stmts[] = "SELECT COUNT(*) FROM team
		WHERE team_number IN (" . join(',',$teams_on) . ")
			AND (last_refreshed IS NULL OR last_refreshed < DATE_SUB(NOW(),INTERVAL ".USER_ONLINE_TIMEOUT."))";
	}

	foreach ($sql_stmts as $sql)
	{
		$query = mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$row = mysql_fetch_row($query);
		if ($row[0])
		{
			?>{
"class":"online_status_change"
}
<?php
			syslog(LOG_NOTICE, 'checkmessage: returning online_status_change');
			exit();
		}
	}
}

function check_for_job_completion()
{
	$jobcodes = array();
	foreach (explode(',', $_REQUEST['jobcompletion']) as $jobcode)
	{
		$jobcodes[] = db_quote($jobcode);
	}
	if (count($jobcodes) == 0)
		return;

	$sql = "SELECT id FROM test_job
		WHERE id IN (".join(',', $jobcodes).")
		AND result_status<>''
		AND result_status IS NOT NULL";
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$row = mysql_fetch_row($query);
	if ($row)
	{
		?>{
"class":"job_completion",
"job_id":<?php echo js_quote($row[0])?>
}
<?php
		syslog(LOG_NOTICE, 'checkmessage: returning job_completion ' . $row[0]);
		exit();
	}
}

function check_for_new_submission()
{
	$submission_limit = array();
	if (preg_match('/submission(\d+)/', $_REQUEST['newsubmissionsafter'], $m))
	{
		$submission_limit[] = "id>".$m[1];
	}

	$clarification_limit = array();
	if (preg_match('/clarification(\d+)/', $_REQUEST['newsubmissionsafter'], $m))
	{
		$clarification_limit[] = "id>".$m[1];
	}

	global $targets;
	$targets_sql = implode(", ", $targets);

	$submission_limit[] = "j.user IN ($targets_sql)";
	$clarification_limit[] = "j.user IN ($targets_sql)";
	$submission_limit[] = "(s.status IS NULL OR s.status='')";
	$clarification_limit[] = "(s.status IS NULL OR s.status='')";

	global $contest_id;
	$sql = "
	SELECT 'submission' AS type,id
	FROM submission s
	JOIN team t ON s.team=t.team_number
	JOIN problem p ON s.problem=p.problem_number AND t.contest=p.contest
	LEFT JOIN team j ON j.team_number=s.judge
	WHERE t.contest=" . db_quote($contest_id) . "
	AND ".join(' AND ',$submission_limit)."
	UNION
	SELECT 'clarification' AS type,id
	FROM clarification s
	JOIN team t ON t.team_number=s.team
	JOIN problem p ON p.contest=t.contest AND p.problem_number=s.problem_number
	LEFT JOIN team j ON j.team_number=s.judge
	WHERE s.contest=".db_quote($contest_id)."
	AND ".join(' AND ',$clarification_limit)."
	ORDER BY id ASC";

	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$row = mysql_fetch_assoc($result);

	if ($row) {
	?>{
"class":"new_submission",
"m":"foo"
}
<?php
		syslog(LOG_NOTICE, 'checkmessage: returning new_submission');
		exit();
	}
	return;
}

function check_for_event()
{
	check_for_message();

	if ($_REQUEST['onlinestatuschange'])
	{
		check_for_online_status_change();
	}
	if ($_REQUEST['jobcompletion'])
	{
		check_for_job_completion();
	}
	if (isset($_REQUEST['newsubmissionsafter']))
	{
		check_for_new_submission();
	}
}

$remaining = 0;
if (preg_match('/^\d+$/', $_REQUEST['timeout']))
{
	$remaining = $_REQUEST['timeout'];
	$remaining = ($remaining <= 60 ? $remaining : $remaining);
}

$stop_time = time() + $remaining;

check_for_event();

while ($remaining >= 1)
{
	wait_for_event($remaining);
	check_for_event();
	$remaining = $stop_time - time();
}

?>
{}

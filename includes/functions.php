<?php

require_once('notify.php');
require_once('auth.php');

// update the 'score' value for a given team; it queries the results
// for all the problems for this team, and calculates the cumulative
// score.
function update_score($team_id)
{
	$main_points = 0;
	$alt_points = 0;

	$sql = "SELECT r.score AS score,r.score_alt AS score_alt
		FROM results r
		JOIN team t
			ON t.team_number=r.team_number
		JOIN problem p
			ON p.contest=t.contest
			AND p.problem_number=r.problem_number
		WHERE r.team_number=" . db_quote($team_id) . "
		AND p.visible='Y'";
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	while ($row = mysql_fetch_assoc($result))
	{
		$main_points += $row['score'];
		$alt_points += $row['score_alt'];
	}

	$sql = "UPDATE team SET score=" . db_quote($main_points) . ",
			score_alt=" . db_quote($alt_points) . "
		WHERE team_number=" . db_quote($team_id);
	mysql_query($sql);
}

function update_result($team_id, $problem_number, $do_not_update_team_score)
{
	//calculate the time of first correct submission
	//calculate the number of incorrect submissions
	$sql = "SELECT
		(
			SELECT CASE WHEN status IN ('Correct','Accepted') THEN minutes ELSE 0 END
			FROM submission
			WHERE team=".db_quote($team_id)."
			AND problem=".db_quote($problem_number)."
			ORDER BY id DESC
			LIMIT 1
		) AS thetime,
		(
			SELECT COUNT(*)
			FROM submission
			WHERE team=".db_quote($team_id)."
			AND problem=".db_quote($problem_number)."
			AND status IS NOT NULL
			AND status <> ''
			AND status NOT IN ('Correct', 'Accepted')
		) AS incorrect_submissions,
		(
			SELECT COUNT(*)
			FROM challenge ch
			JOIN submission s9
				ON s9.id=ch.submission
			WHERE ch.creator=t.team_number
			AND s9.problem=p.problem_number
			AND ch.status='Challenge successful'
		) AS successful_challenges,
		c.score_system AS score_system,
		p.difficulty AS difficulty,
		p.allocated_minutes AS allocated_minutes
		FROM contest c
		JOIN team t
			ON t.contest=c.contest_id
		JOIN problem p
			ON p.contest=c.contest_id
		WHERE t.team_number=".db_quote($team_id)."
		AND p.problem_number=".db_quote($problem_number);

	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$row = mysql_fetch_assoc($result);
	$thetime = $row['thetime'];
	$incorrect_submissions = $row['incorrect_submissions'];
	$score_system = $row['score_system'];
	$incorrects_penalty = 20;
	$difficulty = $row['difficulty'];
	$allocated_minutes = $row['allocated_minutes'];
	$nchallenges = $row['successful_challenges'];

	$sql = "SELECT * FROM results
		WHERE team_number=" . db_quote($team_id) . "
		AND problem_number=" . db_quote($problem_number);
	$result = mysql_query($sql);
	$row = mysql_fetch_assoc($result);

	if (!$row['thetime'] && $thetime >= 1)
	{
		//this problem is now solved
		$sql = "INSERT INTO announcement (contest,recipient,messagetype,message,duration,expires)
			SELECT t.contest AS contest,
				'*' AS recipient,
				'S' AS messagetype,
				CONCAT('Problem ''', problem_name, ''' has been solved by ''',
				team_name, ''' in ',".db_quote($thetime) . ", ' minutes.') AS message,
				15 AS duration,
				DATE_ADD(NOW(), INTERVAL 5 MINUTE)
			FROM team t
			JOIN problem p ON t.contest=p.contest
			WHERE team_number=" . db_quote($team_id) . "
			AND problem_number=" . db_quote($problem_number);
		mysql_query($sql)
			or die("SQL error: " . mysql_error());
	}

	if ($score_system == 'T')
	{
		if ($thetime >= 1)
		{
			$score1 = ceil($difficulty * (0.3 + 0.7 * pow($allocated_minutes,2) / (10 * pow($thetime,2) + pow($allocated_minutes,2))));
			$score2 = 0;

			if ($score1 < 1) { $score1 = 1; }
		}
		else
		{
			$score1 = 0;
			$score2 = 0;
		}

		$score1 += 50 * $nchallenges;
	}
	else
	{
		$score1 = ($thetime >= 1 ? 1 : 0);
		$score2 = ($thetime >= 1 ? -($incorrect_submissions * $incorrects_penalty + $thetime) : 0);
	}

	if ($row)
	{
		$sql = "UPDATE results
			SET thetime=" . db_quote($thetime) . ",
			incorrect_submissions=" . db_quote($incorrect_submissions) . ",
			score=".db_quote($score1) . ",
			score_alt=".db_quote($score2) . "
			WHERE team_number=".db_quote($team_id)."
			AND problem_number=".db_quote($problem_number);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());
	}
	else
	{
		$query = "INSERT INTO results (team_number, problem_number, thetime, incorrect_submissions, score, score_alt)
				VALUES (
			".db_quote($team_id).",
			".db_quote($problem_number).",
			".db_quote($thetime).",
			".db_quote($incorrect_submissions) . ",
			".db_quote($score1) . ",
			".db_quote($score2) . ")";
		mysql_query($query)
			or die("SQL error: ".mysql_error());
	}

	if (!$do_not_update_team_score)
		update_score($team_id);
}

function update_results_for_problem($contest_id, $problem_number)
{
	$sql = "SELECT team_number
		FROM team t
		WHERE t.contest=".db_quote($contest_id);
	$query = mysql_query($sql);
	while ($row = mysql_fetch_assoc($query))
	{
		update_result($row['team_number'], $problem_number);
	}
}

function update_results_for_all_problems($contest_id)
{
	$seen_teams = array();

	$sql = "SELECT team_number, problem_number
		FROM team t
		JOIN problem p
			ON p.contest=t.contest
		WHERE t.contest=".db_quote($contest_id);
	$query = mysql_query($sql);
	while ($row = mysql_fetch_assoc($query))
	{
		$seen_teams[$row['team_number']] = true;
		update_result($row['team_number'], $row['problem_number'], true);
	}

	foreach ($seen_teams as $team_id => $x)
	{
		update_score($team_id);
	}
}

function create_job($jobtype, $source_file, $source_name, $input_file)
{
	$sql = "INSERT INTO test_job
		(type,source_file,source_name,input_file)
		VALUES (
		".db_quote($jobtype).",
		".db_quote($source_file).",
		".db_quote($source_name).",
		".db_quote($input_file).")";
	mysql_query($sql)
		or die("SQL error: ".mysql_error());

	notify_worker();

	return mysql_insert_id();
}

function generate_tests_for_system_test($contest_id,
		$problem_number, $test_number)
{
	$sql = "SELECT s.id AS submission_id,input_file,
		s.file AS source_file,s.given_name AS source_name
		FROM submission s
		JOIN team t
			ON s.team=t.team_number
		JOIN system_test st
			ON s.problem=st.problem_number
			AND t.contest=st.contest
		WHERE st.contest=".db_quote($contest_id)."
		AND st.problem_number=".db_quote($problem_number)."
		AND st.test_number=".db_quote($test_number) . "
		AND s.file IS NOT NULL
		ORDER BY s.id";
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	return generate_tests_helper($result);
}

function generate_tests($submission_id)
{
	$sql = "SELECT s.id AS submission_id,input_file,
		s.file AS source_file,s.given_name AS source_name
		FROM submission s JOIN team t
			ON s.team=t.team_number
		JOIN system_test st
			ON s.problem=st.problem_number AND t.contest=st.contest
		WHERE s.id=".db_quote($submission_id)."
		AND s.file IS NOT NULL
		ORDER BY test_number";
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	return generate_tests_helper($result);
}

function generate_tests_helper($result)
{
	while ($row = mysql_fetch_assoc($result))
	{
		$sql = "SELECT COUNT(*) AS count FROM test_result
			WHERE submission=".db_quote($row['submission_id'])."
			AND test_file=".db_quote($row['input_file']);
		$result2 = mysql_query($sql);
		$row2 = mysql_fetch_assoc($result2);
		if ($row2['count']==0)
		{
			$job_id = create_job("S",
				$row['source_file'],
				$row['source_name'],
				$row['input_file']
				);
			$sql = "INSERT INTO test_result
			(submission,test_file,job)
			VALUES (
				".db_quote($row['submission_id']).",
				".db_quote($row['input_file']).",
				".db_quote($job_id).")";
			mysql_query($sql)
				or die("SQL error: ".mysql_error() . $sql);
		}
	}
}

function redo_expected_output($contest, $problem_number)
{
	$sql = "SELECT input_file,
		p.solution_file AS source_file,
		p.solution_name AS source_name,
		st.contest AS contest,
		st.problem_number AS problem_number
		FROM system_test st
		JOIN problem p
			ON p.contest=st.contest
			AND p.problem_number=st.problem_number
		WHERE st.contest=".db_quote($contest)."
		AND st.problem_number=".db_quote($problem_number)."
		AND p.solution_file IS NOT NULL";
	$result = mysql_query($sql)
		or die("SQL error: " .mysql_error());

	while ($row = mysql_fetch_assoc($result))
	{
		$job_id = create_job("E",
			$row['source_file'],
			$row['source_name'],
			$row['input_file']
			);
		$sql = "UPDATE system_test
			SET expected_file=NULL,
			expected_file_job=".db_quote($job_id)."
			WHERE contest=".db_quote($row['contest'])."
			AND problem_number=".db_quote($row['problem_number'])."
			AND input_file=".db_quote($row['input_file']);
		mysql_query($sql);
	}
}

function handle_binary_upload_file($name)
{
	$f = $_FILES[$name.'_upload'];
	if ($f && $f['tmp_name'])
	{
		$hash = sha1_file($f['tmp_name']);
		$given_name = basename($f['name']);

		global $uploaddir;
		$new_name = "$uploaddir/$hash.txt";
		if (!move_uploaded_file($f['tmp_name'], $new_name))
		{
			die("Error: invalid upload file");
		}

		$_REQUEST[$name.'_file'] = $hash;
		$_REQUEST[$name.'_name'] = $given_name;
	}
	else if ($_REQUEST[$name.'_replace'])
	{
		$_REQUEST[$name.'_file'] = "";
		$_REQUEST[$name.'_name'] = "";
	}
}

function handle_upload_file($name)
{
	global $uploaddir;

	$f = $_FILES[$name.'_upload'];
	if ($f && $f['tmp_name'])
	{
		if (!is_uploaded_file($f['tmp_name']))
		{
			die("Error: invalid upload file");
		}

		$fp_orig = fopen($f['tmp_name'], "r")
			or die("Error: cannot read temp file");

		$tmp_dest = tempnam($uploaddir, "upload_orig");
		$fp_dest = fopen($tmp_dest, "w")
			or die("Error: cannot write temp file");

		while (($tmp = fgets($fp_orig)) !== FALSE)
		{
			$line = rtrim($tmp, "\n\r");
			fwrite($fp_dest, "$line\n");
		}
		fclose($fp_orig)
			or die("Error reading $f[tmp_name]");
		unlink($f['tmp_name']);

		fclose($fp_dest)
			or die("Error writing $tmp_dest");

		$hash = sha1_file($tmp_dest)
			or die("invalid hash");
		$given_name = basename($f['name']);

		$new_name = "$uploaddir/$hash.txt";
		rename($tmp_dest, $new_name)
			or die("Error: cannot put $new_name");

		$_REQUEST[$name.'_file'] = $hash;
		$_REQUEST[$name.'_name'] = $given_name;
	}
	else if ($_REQUEST[$name.'_replace'])
	{
		$_REQUEST[$name.'_file'] = "";
		$_REQUEST[$name.'_name'] = "";
	}
	else if (isset($_REQUEST[$name."_content"]))
	{
		$x = str_replace("\r", "", $_REQUEST[$name.'_content']);
		if (strlen($x) != 0 && substr($x, -1) != "\n")
		{
			$x .= "\n";
		}
		$hash = sha1($x);
		$new_name = "$uploaddir/$hash.txt";
		$fh = fopen("$new_name.tmp", "w")
			or die("Error: cannot create $new_name.tmp");
		fwrite($fh, $x);
		fclose($fh)
			or die("Error: $new_name.tmp");
		rename("$new_name.tmp", $new_name)
			or die("Error: $new_name cannot be created");

		$_REQUEST[$name.'_file'] = $hash;
		if (!isset($_REQUEST[$name.'_name']))
		{
			$_REQUEST[$name.'_name'] = $name.'.txt';
		}
	}
}

function get_accepted_file_types()
{
	$sql = "SELECT accepted_languages
		FROM worker
		WHERE last_refreshed >= DATE_SUB(NOW(),INTERVAL 5 MINUTE)
		";
	$query = mysql_query($sql)
		or die("SQL error: " . mysql_error());
	$found = array();
	while ($row = mysql_fetch_row($query))
	{
		foreach (explode(',',$row[0]) as $ext)
		{
			$found[$ext] = 1;
		}
	}
	ksort($found);
	return array_keys($found);
}

function choose_judge($contest_id, $team_ordinal, $problem_number)
{
	$sql = "SELECT judged_by
		FROM problem
		WHERE contest=".db_quote($contest_id)."
		AND problem_number=".db_quote($problem_number);
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$row = mysql_fetch_row($query);
	if (!$row)
		return "director";

	if (!$row[0])
		return "judges";

	$judges_array = explode(',',$row[0]);
	$i = abs($team_ordinal-1) % count($judges_array);
	return $judges_array[$i];
}

function send_message($type, $contest_id, $recipient, $message, $url)
{
	$sql = "INSERT INTO announcement (messagetype,contest,recipient,message,url,expires)
		SELECT ".db_quote($type).",
			".db_quote($contest_id).",
			".db_quote($recipient).",
			".db_quote($message).",
			".db_quote($url).",
			DATE_ADD(NOW(), INTERVAL 60 MINUTE)
		FROM dual";
	mysql_query($sql)
		or die("SQL error: ".mysql_error());

	wakeup_listeners();
}

function get_language_from_name($source_name)
{
	if (preg_match('/\\.c$/', $source_name))
		return 'text/x-csrc';
	else if (preg_match('/\\.(cpp|cc)$/', $source_name))
		return 'text/x-c++src';
	else if (preg_match('/\\.(cs)$/', $source_name))
		return 'text/x-csharp';
	else if (preg_match('/\\.rb$/', $source_name))
		return 'text/x-ruby';
	else if (preg_match('/\\.pl$/', $source_name))
		return 'text/x-perl';
	else if (preg_match('/\\.py$/', $source_name))
		return 'text/x-python';
	else
		return 'text/x-java';
}

function determine_phases(&$contest_info)
{
	$phases = array();
	for ($i = 1; $i <= 4; $i++)
	{
		if ($contest_info["phase{$i}_ends"])
		{
			$phases[$i] = $contest_info["phase{$i}_ends"];
		}
	}
	asort($phases);

	$cur_time = time();
	unset($contest_info['current_phase']);
	foreach ($phases as $i => $phase_ends)
	{
		$phase_time = strtotime($phase_ends);
		if ($phase_time > $cur_time)
		{
			if ($contest_info['current_phase']) {
				$contest_info['next_phase'] = $i;
				break;
			}
			$contest_info['current_phase'] = $i;
			$contest_info['current_phase_name'] = $contest_info["phase{$i}_name"];
			$contest_info['current_phase_ends'] = $phase_ends;
			$contest_info['current_phase_timeleft'] = $phase_time - $cur_time;
		}
	}

	if (!$contest_info['current_phase'])
		$contest_info['current_phase'] = '0';
	if (!$contest_info['next_phase'])
		$contest_info['next_phase'] = '0';

	return $contest_info;
}

// Returns a populated $contest_info structure, containing all the
// contest properties needed for the basic user interface.
//
function get_basic_contest_info($contest_id, $extra = '')
{
	$sql = "
		SELECT title,
			phase1_name, phase1_ends,
			phase2_name, phase2_ends,
			phase3_name, phase3_ends,
			phase4_name, phase4_ends,
			teams_can_write_code
			$extra
		FROM contest
		WHERE contest_id=".db_quote($contest_id);

	$result = mysql_query($sql)
		or die("Error: ".mysql_error());
	$contest_info = mysql_fetch_assoc($result)
		or die("Error: contest $contest_id not found");

	determine_phases($contest_info);
	return $contest_info;
}

function get_problem_info($problem_number)
{
	global $team_info;
	global $contest_id;

	$is_problem_readable_sql = is_judge_of($contest_id) ? "1<2"
		: check_phase_option_bool('pp_read_problem');

$sql = "SELECT problem_name,spec_file,spec_name,
		".check_phase_option_sql('pp_read_solution')." AS solution_visible,
		".check_phase_option_sql('pp_read_opponent')." AS read_opponent,
		".check_phase_option_sql('pp_challenge')." AS challenge_phase,
		solution_file,solution_name,
		r.source_file AS source_file,
		r.source_name AS source_name,
		(SELECT COUNT(*) FROM clarification cl
			WHERE cl.contest=p.contest
			AND cl.problem_number=p.problem_number
			AND cl.status='reply-all') AS clarification_count
	FROM problem p
	LEFT JOIN results r
		ON r.team_number=".db_quote($team_info['team_number'])."
		AND r.problem_number=p.problem_number
	WHERE contest=".db_quote($contest_id)."
	AND p.problem_number=".db_quote($problem_number)."
	AND $is_problem_readable_sql
	";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
$problem_info = mysql_fetch_assoc($result)
	or die("invalid problem");

$sql = "INSERT IGNORE INTO results (team_number,problem_number)
	VALUES (".db_quote($team_info['team_number']).",
	".db_quote($problem_number)."
	)";
mysql_query($sql)
	or die("MySQL error");

$sql = "UPDATE results
	SET opened=NOW()
	WHERE team_number=".db_quote($team_info['team_number'])."
	AND problem_number=".db_quote($problem_number)."
	AND (opened IS NULL OR opened>NOW())";
mysql_query($sql)
	or die("MySQL error");

	return $problem_info;
}

function check_phase_option_sql($perphase_option)
{
	global $contest_info;
	$j = $contest_info['current_phase'];

	return "CASE WHEN $perphase_option & ".(1<<$j)." <> 0 THEN 'Y' ELSE 'N' END";
}

function check_phase_option_bool($perphase_option)
{
	global $contest_info;
	$j = $contest_info['current_phase'];

	return "$perphase_option & ".(1<<$j)." <> 0";
}

function set_submission_status($submission_id, $submission_status)
{
syslog(LOG_NOTICE, 'in set_submission_status');

	$sql = "SELECT s.status AS old_status,
			s.team AS contestant_id,
			s.problem AS problem_number
		FROM submission s
		WHERE s.id=".db_quote($submission_id);
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$submission_info = mysql_fetch_assoc($query)
		or die("submission $submission_id not found");

syslog(LOG_NOTICE, 'old status = '.$submission_info['old_status']."; desired status = $submission_status");
	if ($submission_status == $submission_info['old_status'])
		return;

	// update the submission, but do it in a way that protects
	// against multiple processes doing it at the same time

	$sql = "UPDATE submission
		SET status=".db_quote($submission_status)."
		WHERE id=".db_quote($submission_id)."
		AND (status IS NULL
			OR status=".db_quote($submission_info['old_status'])."
			)";
	mysql_query($sql)
		or die("SQL error: ".mysql_error());

	$rows = mysql_affected_rows();
	syslog(LOG_NOTICE, "$rows affected rows");
	if ($rows)
	{
		// give the submitter the news
		update_result(
				$submission_info['contestant_id'],
				$submission_info['problem_number']
				);
		if ($submission_status)
		{
			notify_judgment($submission_id);
		}
	}
}

function notify_judgment($submission_id)
{
	$sql = "SELECT t.contest AS contest,
			t.user AS contestant_username,
			p.problem_name AS problem_name
		FROM submission s
		JOIN team t
			ON t.team_number=s.team
		JOIN problem p
			ON p.contest=t.contest
			AND p.problem_number=s.problem
		WHERE s.id=".db_quote($submission_id);
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$submission_info = mysql_fetch_assoc($query)
		or die("submission $submission_id not found");

	$message = "Your solution for $submission_info[problem_name] has been judged.";
	$url = "solution.php?id=".urlencode($submission_id);
	send_message('N', $submission_info['contest'],
		$submission_info['contestant_username'], $message, $url);
}

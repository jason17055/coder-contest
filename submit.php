<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

require_auth();
$team_info = get_team_identity();
$contest_id = $team_info['contest'];

$contest_info = get_basic_contest_info($contest_id, ",collaboration");

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	if (isset($_REQUEST['action:cancel']))
	{
		$url = $_REQUEST['next_url'] ? $_REQUEST['next_url'] : "team_menu.php";
		header("Location: $url");
		exit();
	}

	handle_upload_file("source");

	$team_info['team_number']
		or die("Error: could not determine team number");
	$_REQUEST['problem']
		or die("Error: problem not specified");
	$_REQUEST['source_file']
		or die("Error: no file provided");

	$sql = "SELECT NOW() AS curtime,
			p.start_time AS problem_start_time,
			c.started AS contest_start_time,
			(SELECT opened FROM results
				WHERE team_number=".db_quote($team_info['team_number'])."
				AND problem_number=p.problem_number
				) AS access_time,
			score_by_access_time
		FROM problem p
		JOIN contest c
				ON c.contest_id=p.contest
		WHERE p.contest=".db_quote($contest_id)."
		AND p.problem_number=".db_quote($_REQUEST['problem']);
	$result = mysql_query($sql);
	$time_info = mysql_fetch_assoc($result)
		or die("Problem $_REQUEST[problem] in contest $contest_id not found");

	$basis_time = $time_info['problem_start_time'] ?
			$time_info['problem_start_time'] : $time_info['contest_start_time'];

	if ($time_info['score_by_access_time'] == 'Y'
			&& $time_info['access_time'])
	{
		$basis_time = $time_info['access_time'];
	}

	$sql = "SELECT TIMESTAMPDIFF(MINUTE,".db_quote($basis_time).",".db_quote($time_info['curtime']).") AS minutes";
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$minutes_info = mysql_fetch_assoc($result);
	$minutes = max(1, $minutes_info['minutes'] * 1);

	$judge_user = choose_judge($team_info['contest'], $team_info['ordinal'], $_REQUEST['problem']);
	if ($judge_user)
	{
		$sql = "SELECT judge_id FROM judge
			WHERE contest=".db_quote($contest_id)."
			AND judge_user=".db_quote($judge_user);
		$query = mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$row = mysql_fetch_row($query);
		$judge_id = $row[0];
	}

	$coauthors = NULL;
	if ($_REQUEST['coauthor'])
	{
		$coauthors = join(',',$_REQUEST['coauthor']);
	}

	$sql = "INSERT INTO submission
		(team,problem,submitted,minutes,file,given_name,judge,coauthors)
		VALUES (
		".db_quote($team_info['team_number']).",
		".db_quote($_REQUEST['problem']).",
		".db_quote($time_info['curtime']).",
		".db_quote($minutes).",
		".db_quote($_REQUEST['source_file']).",
		".db_quote($_REQUEST['source_name']).",
		".db_quote($judge_id).",
		".db_quote($coauthors).")";
	mysql_query($sql)
		or die("SQL error: ".mysql_error());

	$submission_id = mysql_insert_id();
	generate_tests($submission_id);

	// send message to appropriate judge
	if (SEND_JUDGE_MESSAGES)
	{
		$sql = "SELECT problem_name FROM problem
			WHERE contest=".db_quote($contest_id)."
			AND problem_number=".db_quote($_REQUEST['problem']);
		$query = mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$problem_info = mysql_fetch_assoc($query)
			or die("Error: problem $_REQUEST[problem] not found");

		$url = 'submission.php?id='.urlencode($submission_id);
		$message = "There is a new submission for $problem_info[problem_name].";
		send_message('N', $team_info['contest'], $judge_user,
				$message, $url);
	}
	else
	{
		wakeup_listeners();
	}

	$url = "team_menu.php";
	header("Location: $url");
	exit();
}

begin_page("Submit a Solution");

?>
<form method="post" enctype="multipart/form-data" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<table>
<tr>
<td valign="top">Problem:</td>
<td valign="top"><select name="problem">
<option value=""<?php echo($_REQUEST['problem'] ? '' : ' selected="selected"')?>>--select--</option>
<?php
	$sql = "SELECT problem_number,problem_name
		FROM problem
		WHERE contest=".db_quote($contest_id)."
		AND ".check_phase_option_bool('pp_submit')."
		ORDER BY problem_number";
	$result = mysql_query($sql);
while ($row = mysql_fetch_assoc($result)) {
	?><option value="<?php echo htmlspecialchars($row['problem_number'])?>"<?php
			echo($_REQUEST['problem'] == $row['problem_number'] ? ' selected="selected"' : '')?>><?php
			echo htmlspecialchars($row['problem_name'])?></option>
<?php }

?></select></td>
</tr>
<tr>
<td valign="top">Source code:</td>
<td valign="top"><div><input type="file" name="source_upload"></div>
<div><small>Accepted file types:
<?php
	$found = get_accepted_file_types();
	echo htmlspecialchars(join(', ', $found));
?></small></div>
</td>
</tr>
<?php if ($contest_info['collaboration'] == 'Y') { ?>
<tr>
<td valign="top">Co-authors:</td>
<td valign="top"><select name="coauthor[]" multiple="multiple" style="min-width: 2in">
<?php
	$sql = "SELECT team_number,team_name,user
		FROM team
		WHERE contest=".db_quote($contest_id)."
		AND team_number<>".db_quote($team_info['team_number'])."
		ORDER BY team_name,user";
	$query = mysql_query($sql);
	while ($row = mysql_fetch_assoc($query)) {
	?><option value="<?php echo htmlspecialchars($row['team_number'])?>"><?php
			echo htmlspecialchars("$row[team_name] ($row[user])")?></option>
<?php
	} //end loop
?></select>
<div><small>If anyone helped you with this submission, please pick their names from the list.</small></div>
</td>
</tr>
<?php } //end if collaboration is ad hoc ?>
</table>

<div>
<button type="submit">Submit</button>
<button type="submit" name="action:cancel">Cancel</button>
</div>

</form>


<?php
end_page();
?>

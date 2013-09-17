<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

require_auth();
is_director($_REQUEST['contest'])
	or die("Error: not authorized");

$contest_id = $_REQUEST['contest'] ?: 1;
$sql = "SELECT * FROM contest
	WHERE contest_id=" . db_quote($contest_id);
$result = mysql_query($sql);
$contest_info = mysql_fetch_assoc($result)
	or die("Error: contest $contest_id not found");
determine_phases($contest_info);

$problems_list = array();

begin_page($contest_info['title']);
$edit_contest_url = "contest.php?contest=".urlencode($contest_id);
?>

<table id="controller_teams_table" class="realtable"><tr><th>Contestant</th>
<?php
$sql = "SELECT * FROM problem
	WHERE contest=" . db_quote($contest_id) . "
	AND visible='Y'
	ORDER BY problem_number ASC";
$result = mysql_query($sql);

while ($problem = mysql_fetch_assoc($result)) {
	$problem_url = "open_problem.php?problem=" . urlencode($problem['problem_number']) . "&contest=".urlencode($contest_id);
	$problems_list[] = $problem['problem_number'];

	?><th><a href="<?php echo htmlspecialchars($problem_url) ?>"><?php echo htmlspecialchars($problem['problem_name'])?></a></th>
<?php
} //end loop through each problem
?>
<th>Totals</th></tr>
<?php

$sql = "SELECT *,
	CASE WHEN last_refreshed IS NULL OR last_refreshed < DATE_SUB(NOW(),INTERVAL ".USER_ONLINE_TIMEOUT.") THEN 'N' ELSE 'Y' END AS online
	FROM team
	WHERE contest=" . db_quote($contest_id) . "
	AND is_contestant='Y'
	ORDER BY ordinal,team_name ASC";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());

while ($team = mysql_fetch_assoc($result)) {
	if ($team['team_name']=="") { $team['team_name'] = "(no name)"; }
	$team_url = "user.php?id=" . urlencode($team['team_number'])
		. "&next_url=" . urlencode($_SERVER['REQUEST_URI']);

	?><tr><td height="32">
<?php $online = $team['online'];
	$online_img = $online == 'Y' ? "images/plus.png" : "images/minus.png";
	$online_lbl = $online == 'Y' ? "[Online]" : "[Offline]";
?>
<div>
<img src="<?php echo $online_img?>" alt="<?php echo $online_lbl?>" width="14" height="14" class="online-indicator" id="ind_online_team_<?php echo htmlspecialchars($team['team_number'] . "_" . $online)?>">
<a href="<?php echo htmlspecialchars($team_url)?>"><?php echo htmlspecialchars($team['team_name'])?></a>
<?php if ($team['visible'] == 'N') {
	echo " (Invisible)";
	}?>
</div>
</td>
<?php

	$correct = 0;
	foreach ($problems_list as $problem_number)
	{
		$result_url = "result.php?problem=" . $problem_number
				. "&team=" . $team['team_number'];
		?><td class="results_cell"><a href="<?php echo htmlspecialchars($result_url)?>"><?php
		
		$query3 = 'SELECT * FROM results
			WHERE team_number=' . db_quote($team['team_number']) . '
			AND problem_number=' . db_quote($problem_number);
		$result3 = mysql_query($query3);
		
		if ($row = mysql_fetch_assoc($result3)) {
			if ($row['score'])
			{
				echo format_score($row['score'], $row['score_alt']);
			}
			else if ($row['opened'])
			{
				echo '0';
			}
			else
			{
				echo '-';
			}
		}
		else
		{	echo '-';
		}
		echo '</a></td>';
	}
	
	?>
<td><?php echo format_score($team['score'], $team['score_alt'])?></td>
</tr><?php
}
$teams_url = "users.php?contest=" . urlencode($contest_id);
$problems_url = "problems.php?contest=" . urlencode($contest_id);
$list_submissions_url = "listsubmissions.php?contest=".urlencode($contest_id);
$scoreboard_url = "scoreboard.php?contest=" . urlencode($contest_id);
$announcement_url = "sendmessage.php?contest=" . urlencode($contest_id);
$test_url = 'submit_test.php?next_url=' . urlencode($_SERVER['REQUEST_URI']);

?>
</table>

<?php
$sql = "SELECT user,team_number AS judge_id,
		CASE WHEN last_refreshed IS NULL OR last_refreshed < DATE_SUB(NOW(), INTERVAL ".USER_ONLINE_TIMEOUT.") THEN 'N' ELSE 'Y' END AS online,
		(SELECT COUNT(*) FROM submission WHERE judge=team_number
			AND (status IS NULL OR status='')) AS submissions_pending,
		(SELECT COUNT(*) FROM submission WHERE judge=team_number
			) AS submissions_total,
		(SELECT COUNT(*) FROM clarification WHERE judge=team_number
			AND (status IS NULL OR status='')) AS clarifications_pending,
		(SELECT COUNT(*) FROM clarification WHERE judge=team_number
			) AS clarifications_total
	FROM team
	WHERE contest=".db_quote($_REQUEST['contest'])."
	AND is_contestant='N'
	ORDER BY ordinal,team_name ASC";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
if (mysql_num_rows($result)) {
?>
<table id="controller_judges_table" class="realtable">
<tr>
<th>Judge Name</th>
<th>Totals</th>
<?php
while ($judge_info = mysql_fetch_assoc($result))
{
	$edit_judge_url = "user.php?id=".urlencode($judge_info['judge_id'])
			. "&next_url=".urlencode($_SERVER['REQUEST_URI']);
	$online = $judge_info['online'];
	$online_img = $online == 'Y' ? "images/plus.png" : "images/minus.png";
	$online_lbl = $online == 'Y' ? '[Online]' : '[Offline]';
	$judge_info['submissions_done'] = $judge_info['submissions_total'] - $judge_info['submissions_pending'];
	$judge_info['clarifications_done'] = $judge_info['clarifications_total'] - $judge_info['clarifications_pending'];
	?><tr>
<td height="32"><img class="online-indicator" id="ind_online_team_<?php echo htmlspecialchars($judge_info['judge_id'] . "_" . $online)?>" src="<?php echo $online_img?>" alt="<?php echo $online_lbl?>" width='14' height='14'>
<a href="<?php echo htmlspecialchars($edit_judge_url)?>"><?php echo htmlspecialchars($judge_info['user'])?></a>
</td>
<td style="padding-left: 6pt; padding-right: 6pt">
<img src="images/clarification.png" alt="Clarifications:"
	class="task-count-indicator"
	id="ind_clarificationcount_judge_<?php echo htmlspecialchars($judge_info['judge_id'])?>_<?php echo $judge_info['clarifications_total']?>">
<?php echo "$judge_info[clarifications_pending] ($judge_info[clarifications_done])"?>
<img style="margin-left: 1em" src="images/submission.png" alt="Submissions:"
	class="task-count-indicator"
	id="ind_submissioncount_judge_<?php echo htmlspecialchars($judge_info['judge_id'])?>_<?php echo $judge_info['submissions_total']?>">
<?php echo "$judge_info[submissions_pending] ($judge_info[submissions_done])"?>
</td>
</tr>
<?php
}
?>
</table>
<?php } //end if any judges for this contest
?>

<table id="controller_workers_table" class="realtable">
<tr>
<th>Worker</th>
<th>Supported File Types</th>
<th>Details</th>
</tr>
<?php
	$sql = "SELECT id,accepted_languages,description,status,busy
		FROM worker
		WHERE last_refreshed IS NOT NULL
		AND last_refreshed >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)";
	$query = mysql_query($sql);
	$count = 0;
	while ($row = mysql_fetch_assoc($query))
	{
		$count++;
		$id_parts = explode(' ', $row['id']);
		$ua_parts = explode('/', $id_parts[1]);
		$worker_name = $id_parts[0] . "[" . $ua_parts[1] . "]";
		?><tr>
<td height="32">
<div><img src="images/plus.png" alt="[Online]" width="14" height="14">
<?php echo htmlspecialchars($worker_name)?></div>
<div><?php echo($row['busy'] == 'Y' ? "Busy" : "Idle")?></div>
<div><?php echo htmlspecialchars($row['status'])?></div>
</td>
<td><?php echo htmlspecialchars($row['accepted_languages'])?></td>
<td><?php echo htmlspecialchars($row['description'])?></td>
</tr>
<?php
	}
	if ($count == 0) { ?><tr>
<td colspan="3" height="32">
<img src="images/minus.png" alt="" width='14' height='14'>
<span style="color: red; font-weight: bold">No workers are online</span>
<div><small><a href="worker/">Click here to find out how to start a worker</a></small></div></td>
</tr>
<?php
	}
?>
</table>

<p style="clear:both">
<a href="<?php echo htmlspecialchars($edit_contest_url)?>">Contest Options</a> |
<a href="<?php echo htmlspecialchars($problems_url)?>">Define Problems</a> |
<a href="<?php echo htmlspecialchars($teams_url)?>">Define Users</a> |
<a href="<?php echo htmlspecialchars($announcement_url)?>">Send Message</a> |
<a href="<?php echo htmlspecialchars($list_submissions_url)?>">View Submissions</a> |
<a href="<?php echo htmlspecialchars($scoreboard_url)?>">Live Scoreboard</a>
| <a href="<?php echo htmlspecialchars($test_url)?>">Test a Solution</a>
| <a href="sysadmin.php">System Administration</a>
</p>

<?php end_page() ?>

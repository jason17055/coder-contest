<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

require_auth();
$team_info = get_team_identity();
$contest_id = $team_info['contest'];

$contest_info = get_basic_contest_info($contest_id);

begin_page($contest_info['title']);
//contest_clock($contest_info);

$edit_team_url = "user.php?id=".urlencode($team_info['team_number']);
$scoreboard_url = "scoreboard.php?contest=".urlencode($team_info['contest']);
?>
<p>
Your name: <a href="<?php echo htmlspecialchars($edit_team_url)?>"><?php echo htmlspecialchars($team_info['team_name'])?></a>
</p>
<?php

$sql = "SELECT p.problem_number,problem_name,spec_file,spec_name,
	thetime,incorrect_submissions,opened,difficulty
	FROM problem p
	LEFT JOIN results r
	ON r.problem_number=p.problem_number
	AND team_number=".db_quote($team_info['team_number'])."
	WHERE contest=" . db_quote($contest_id) . "
	AND ".check_phase_option_bool('pp_read_problem')."
	ORDER BY problem_number
		";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
if ($row = mysql_fetch_assoc($result)) {
?>
<h3>Problems</h3>
<table border="1">
<tr><th>Problem</th><th>Status</th></tr>
<?php

while ($row) {
	$open_url = "open_problem.php?problem=" . urlencode($row['problem_number']);
	$status = "Unopened";
	if ($row['thetime']) {
		$status = "Solved";
	} else if ($row['incorrect_submissions']) {
		$status = "Attempted";
	} else if ($row['opened']) {
		$status = "Opened at $row[opened]";
	}
?>
<tr><td><a href="<?php echo htmlspecialchars($open_url)?>">
<?php echo htmlspecialchars($row['problem_name']);
if ($row['difficulty']) { echo htmlspecialchars(" ($row[difficulty])"); }
?>
</a></td>
<td><?php echo htmlspecialchars($status)?></td>
</tr>
<?php

$row = mysql_fetch_assoc($result);
} //end list of problems in order
?>
</table>
<?php } //end if any problems have spec files
?>

<h3 page-reload-safe="page-reload-safe">Submissions/Responses</h3>

<?php

$sql = "SELECT id,submitted,problem_name,COALESCE(status,'Pending') AS status,
		'solution' AS type,
		given_name AS source_name,
		NULL AS request, NULL AS response
	FROM submission s
	JOIN team t
		ON t.team_number=s.team
	JOIN problem p
		ON p.problem_number=s.problem
		AND p.contest=t.contest
	WHERE team=".db_quote($team_info['team_number'])."
		AND ".check_phase_option_bool('p.pp_submit')."
	UNION
	SELECT id,submitted,problem_name,COALESCE(status,'Pending') AS status,
		'clarification' AS type,
		NULL AS source_name,
		request,response
	FROM clarification s
	JOIN problem p
		ON p.contest=s.contest
		AND p.problem_number=s.problem_number
	WHERE s.contest=".db_quote($contest_id)."
	AND (team=".db_quote($team_info['team_number'])."
		OR status='reply-all')
	AND ".check_phase_option_bool('p.pp_submit')."
	ORDER BY submitted ASC, id ASC";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
if (mysql_num_rows($result) > 0) {
?>
<table border="1">
<tr><th>Time</th>
<th>Problem</th>
<th>Submitted</th>
<th>Response</th>
</tr>
<?php
while ($row = mysql_fetch_assoc($result)) { ?>
<?php
		$status = $row['status'] == 'reply-one' ? "Responded" :
			($row['status'] == 'reply-all' ? "Broadcasted" :
			$row['status']);
		$url = $row['type'] == 'solution' ?
			"solution.php?id=".urlencode($row['id']) :
			"clarification.php?id=".urlencode($row['id']);
?><tr>
<td><?php echo htmlspecialchars($row['submitted'])?></td>
<td><?php echo htmlspecialchars($row['problem_name'])?></td>
<td><?php 
	if ($row['type'] == 'solution') { ?>
Solution: <?php echo htmlspecialchars($row['source_name']);
	}
	if ($row['type'] == 'clarification') { ?>
Clarification: <?php echo htmlspecialchars(substr($row['request'],0,60));
	}
	?></td>
<td><a href="<?php echo htmlspecialchars($url)?>"><?php echo htmlspecialchars($status)?></a></td>
</tr>
<?php
	}
?>
</table>
<?php } else {
	// no submissions to show ?>
	<div>None at this time.</div>

<?php } // endif no submissions/clarifications to show ?>

<h3>Actions</h3>
<div>
<a href="<?php echo htmlspecialchars($scoreboard_url)?>" target="_new">Scoreboard</a>
<!--
| <a href="submit_test.php">Test a Solution</a>
| <a href="submit.php">Submit a Solution</a>
| <a href="newclarification.php">Request Clarification</a>
-->
</div>

<?php
end_page();
?>

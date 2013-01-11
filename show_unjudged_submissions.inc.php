<h3>Submissions</h3>
<?php

$team_info['is_judge'] == 'Y'
	or die("Error: invalid invocation");

$filter_sql = "(s.status IS NOT NULL AND s.status <> '')
	AND (judge_id IS NULL OR judge_id=".db_quote($_SESSION['is_judge']).")";

$sql = "SELECT 'submission' AS type,id,submitted,team AS team_id,team_name,problem_name,judge_user,status
	FROM submission s
	JOIN team t ON s.team=t.team_number
	JOIN problem p ON s.problem=p.problem_number AND t.contest=p.contest
	LEFT JOIN judge j ON j.judge_id=s.judge
	WHERE t.contest=" . db_quote($contest_id) . "
	AND $filter_sql
	UNION
	SELECT 'clarification' AS type,id,submitted,team AS team_id,team_name,problem_name,judge_user,status
	FROM clarification s
	JOIN team t ON t.team_number=s.team
	JOIN problem p ON p.contest=t.contest AND p.problem_number=s.problem_number
	LEFT JOIN judge j ON j.judge_id=s.judge
	WHERE s.contest=".db_quote($contest_id)."
	AND $filter_sql
	ORDER BY submitted ASC, id ASC";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
if (mysql_num_rows($result) != 0)
{
?>
<!-- this table id is special; the Ajax code will recognize it as a list
of unjudged submissions for this user -->
<table border="1" id="submissions_table">
<tr>
<th>Submitted</th>
<th>Problem</th>
<th>Type</th>
<th>Team</th>
<th>Judge</th>
<th>Status</th>
</tr>
<?php

$count = 0;
while ($submission = mysql_fetch_assoc($result))
{
	$count++;
	$edit_url = $submission['type'] == 'submission' ?
		"submission.php?id=".urlencode($submission['id'])."&next_url=".urlencode($_SERVER['REQUEST_URI']) :
		"answer_clarification.php?id=".urlencode($submission['id']);
?><tr id="<?php echo htmlspecialchars("$submission[type]$submission[id]")?>">
<td><a href="<?php echo htmlspecialchars($edit_url)?>">
<?php echo htmlspecialchars($submission['submitted'])?></a></td>
<td><?php echo htmlspecialchars($submission['problem_name'])?></td>
<td>
<img src="images/<?php echo htmlspecialchars($submission['type'])?>.png" alt="">
<?php echo htmlspecialchars($submission['type'])?></td>
<td><?php echo htmlspecialchars($submission['team_name'])?></td>
<td><?php echo htmlspecialchars($submission['judge_user'])?></td>
<td><?php echo htmlspecialchars($submission['status'])?></td>
</tr>
<?php
} //end for each submission/clarification
?>
</table>
<?php
} //end if at least one submission
else
{
?>
<p class="none_at_this_time">Nothing to judge at this time.</p>
<?php
} //endif count == 0
?>

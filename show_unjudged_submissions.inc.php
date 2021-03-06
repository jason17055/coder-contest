<h3>Unjudged Submissions</h3>
<p>
These are team submissions and clarification requests
that are in your queue.
</p>
<audio id="newSubmissionAudio" src="audio/microsoft/REMINDER.ogg"></audio>
<script type="text/javascript"><!--
function ringaling()
{
	document.getElementById('newSubmissionAudio').play();
}
if (localStorage.getItem('ringaling'))
{
	localStorage.removeItem('ringaling');
	ringaling();
}
//--></script>

<?php

$team_info['is_judge'] == 'Y'
	or die("Error: invalid invocation");

$filter_sql = "(s.status IS NULL OR s.status = '' OR s.status = 'Pending')
	AND (j.team_number=".db_quote($_SESSION['is_judge']).")";

$sql = "SELECT 'submission' AS type,id,submitted,team AS team_id,t.team_name AS team_name,problem_name,status
	FROM submission s
	JOIN team t ON s.team=t.team_number
	JOIN problem p ON s.problem=p.problem_number AND t.contest=p.contest
	LEFT JOIN team j ON j.team_number=s.judge
	WHERE t.contest=" . db_quote($contest_id) . "
	AND $filter_sql
	UNION
	SELECT 'clarification' AS type,id,submitted,team AS team_id,t.team_name AS team_name,problem_name,status
	FROM clarification s
	JOIN team t ON t.team_number=s.team
	JOIN problem p ON p.contest=t.contest AND p.problem_number=s.problem_number
	LEFT JOIN team j ON j.team_number=s.judge
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
<table class="realtable auto_reload_trigger" data-auto-reload-type="submissions_table" id="submissions_table">
<tr>
<th>Submitted</th>
<th>Problem</th>
<th>Type</th>
<th>Team</th>
<th>Status</th>
</tr>
<?php

$count = 0;
while ($submission = mysql_fetch_assoc($result))
{
	$count++;
	$edit_url = $submission['type'] == 'submission' ?
		"submission.php?id=".urlencode($submission['id'])."&next_url=".urlencode($_SERVER['REQUEST_URI']) :
		"answer_clarification.php?id=".urlencode($submission['id'])."&next_url=".urlencode($_SERVER['REQUEST_URI']);
?><tr data-submission-id="<?php echo htmlspecialchars("$submission[type]$submission[id]")?>">
<td><a href="<?php echo htmlspecialchars($edit_url)?>">
<?php echo format_sqldatetime($submission['submitted'])?></a></td>
<td><?php echo htmlspecialchars($submission['problem_name'])?></td>
<td>
<img src="images/<?php echo htmlspecialchars($submission['type'])?>.png" alt="">
<?php echo htmlspecialchars($submission['type'])?></td>
<td><?php echo htmlspecialchars($submission['team_name'])?></td>
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
<p class="none_at_this_time auto_reload_trigger" data-auto-reload-type="submissions_table">Nothing to judge at this time.</p>
<?php
} //endif count == 0
?>

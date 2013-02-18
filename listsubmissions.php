<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');

require_auth();
is_judge_of($_REQUEST['contest'])
	or die("Error: not authorized");

$contest_id = $_REQUEST['contest'];
$sql = "SELECT * FROM contest
	WHERE contest_id=" . db_quote($contest_id);
$result = mysql_query($sql);
$contest_info = mysql_fetch_assoc($result)
	or die("Error: contest $contest_id not found");

if ($_SERVER['REQUEST_METHOD'] == 'POST')
{
	die("Not implemented");
}

$filters = array();
$filters[] = array(name => "Judged");
$filters[] = array(name => "Unjudged");
$filters[] = array(name => "All");
if (!isset($_REQUEST['filter']))
{
	$_REQUEST['filter'] = 'Unjudged';
}

begin_page("List Submissions");
?>

<p>
Status:
<?php
$count = 0;
foreach ($filters as $f)
{
	if ($count > 0) echo " | ";
	$count++;
	$url = "listsubmissions.php?contest=".urlencode($_REQUEST['contest'])
			. "&filter=".$f['name'];
	$selected = $f['name'] == $_REQUEST['filter'];
	?>
<a href="<?php echo htmlspecialchars($url)?>"
	<?php if ($selected) { ?> class="selected"<?php } ?>><?php echo htmlspecialchars($f['name'])?></a>
<?php
}
?>
</p>

<form method="post" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<table border="1">
<tr>
<th>&nbsp;</th>
<th>Submitted</th>
<th>Problem</th>
<th>Type</th>
<th>Team</th>
<th>Judge</th>
<th>Status</th>
</tr>
<?php

$filter_sql = "2>1";
if ($_REQUEST['filter'] == "Judged")
{
	$filter_sql = "(s.status IS NOT NULL AND s.status <> '')";
}
else if ($_REQUEST['filter'] == "Unjudged")
{
	$filter_sql = "(s.status IS NULL OR s.status = '')";
}

if (!is_director($contest_id))
{
	$filter_sql = $filter_sql . " AND (judge_id IS NULL OR judge_id=".db_quote($_SESSION['is_judge']).")";
}

$sql = "SELECT 'submission' AS type,id,submitted,team AS team_id,team_name,problem_name,judge_user,status
	FROM submission s
	JOIN team t ON s.team=t.team_number
	JOIN problem p ON s.problem=p.problem_number AND t.contest=p.contest
	LEFT JOIN judge j ON j.judge_id=s.judge
	WHERE t.contest=" . db_quote($contest_id) . "
	AND $filter_sql
	UNION
	SELECT 'clarification' AS type,id,submitted,team AS team_id,t.team_name AS team_name,problem_name,j.user AS judge_user,status
	FROM clarification s
	JOIN team t ON t.team_number=s.team
	JOIN problem p ON p.contest=t.contest AND p.problem_number=s.problem_number
	LEFT JOIN team j ON j.team_number=s.judge
	WHERE s.contest=".db_quote($contest_id)."
	AND $filter_sql
	ORDER BY submitted ASC, id ASC";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
$count = 0;
while ($submission = mysql_fetch_assoc($result))
{
	$count++;
	$edit_url = $submission['type'] == 'submission' ?
		"submission.php?id=".urlencode($submission['id'])."&next_url=".urlencode($_SERVER['REQUEST_URI']) :
		"answer_clarification.php?id=".urlencode($submission['id']);
?><tr id="<?php echo htmlspecialchars("$submission[type]$submission[id]")?>">
<td><input type="checkbox" name="<?php echo htmlspecialchars("$submission[type]$submission[id]")?>"></td>
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
if ($count == 0) { ?>
<tr>
<td colspan="7" class="none_at_this_time" valign="top">None at this time.</td>
</tr>
<?php
} //endif count == 0
?>
</table>
<p>
Assign selected submissions to:
<select name="assign_to">
<option value="">--nobody--</option>
<?php
	$sql = "SELECT team_number,team_name
		FROM team
		WHERE contest=".db_quote($contest_id)."
		AND is_judge='Y'
		ORDER BY ordinal,team_name";
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	while ($row = mysql_fetch_assoc($query)) {
		?><option value="<?php echo htmlspecialchars($row['team_number'])?>"><?php echo htmlspecialchars($row['team_name'])?></option>
		<?php
	} ?></select>
	<button name="action:assign">Go</button>
</p>

</form>
<?php

$controller_url = "controller.php?contest=".urlencode($contest_id);
$problems_url = "problems.php?contest=".urlencode($contest_id);
$scoreboard_url = "scoreboard.php?contest=" . urlencode($contest_id);

?>
<p>
<?php if (is_director($_REQUEST['contest'])) { ?>
<a href="<?php echo htmlspecialchars($controller_url)?>">Controller</a> |
<?php } else { ?>
<a href=".">Home</a> |
<?php } /* endif is_director */?>
<a href="<?php echo htmlspecialchars($problems_url)?>">Define Problems</a> |
<a href="<?php echo htmlspecialchars($scoreboard_url)?>" target="_new">Live Scoreboard</a>
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
end_page();
?>

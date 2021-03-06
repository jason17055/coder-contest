<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/notify.php');
require_once('includes/functions.php');

require_auth();
is_judge_of($_REQUEST['contest'])
	or die("Error: not authorized");

$contest_id = $_REQUEST['contest'];
$contest_info = get_basic_contest_info($contest_id);

if ($_SERVER['REQUEST_METHOD'] == 'POST')
{
	if (isset($_REQUEST['action:assign']))
	{
		is_director($contest_id)
			or die("Only director can reassign work.");

		$clarifications = array();
		$submissions = array();
		foreach ($_POST as $k => $v)
		{
			if (preg_match('/^clarification(\d+)$/', $k, $m))
			{
				$clarifications[] = db_quote($m[1]);
			}
			else if (preg_match('/^submission(\d+)$/', $k, $m))
			{
				$submissions[] = db_quote($m[1]);
			}
		}

		$target = $_REQUEST['assign_to'];

		// check that target is a judge of this contest
		if ($target != '') {
			$sql = "SELECT 1 FROM team
				WHERE team_number=".db_quote($target)."
				AND contest=".db_quote($contest_id)."
				AND is_judge='Y'";
			$result = mysql_query($sql);
			$is_valid = mysql_fetch_assoc($result)
				or die("Error: specified judge $target not found");
		}

		if (count($clarifications))
		{
			$sql = "UPDATE clarification
					SET judge=".db_quote($target)."
					WHERE id IN (".join(',',$clarifications).")
					AND contest=".db_quote($contest_id);
			mysql_query($sql);
		}
		if (count($submissions))
		{
			$sql = "UPDATE submission
					SET judge=".db_quote($target)."
					WHERE id IN (".join(',',$submissions).")
					AND team IN (SELECT team_number FROM team
						WHERE contest=".db_quote($contest_id)."
						)";
			mysql_query($sql);
		}

		wakeup_listeners();

		$next_url = $_SERVER['REQUEST_URI'];
		header("Location: $next_url");
		exit();
	}
	else
	{
		die("Not implemented");
	}
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
<table class="realtable">
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
	$filter_sql = "(s.status IS NOT NULL AND s.status <> '' AND s.status NOT IN ('Accepted'))";
}
else if ($_REQUEST['filter'] == "Unjudged")
{
	$filter_sql = "(s.status IS NULL OR s.status = '' OR s.status IN ('Accepted'))";
}

if (!is_director($contest_id))
{
	$filter_sql = $filter_sql . " AND (j.team_number IS NULL OR j.team_number=".db_quote($_SESSION['is_judge']).")";
}

$sql = "SELECT 'submission' AS type,id,submitted,team AS team_id,t.team_name AS team_name,problem_name,j.user AS judge_user,status
	FROM submission s
	JOIN team t ON s.team=t.team_number
	JOIN problem p ON s.problem=p.problem_number AND t.contest=p.contest
	LEFT JOIN team j ON j.team_number=s.judge
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
		"answer_clarification.php?id=".urlencode($submission['id'])."&next_url=".urlencode($_SERVER['REQUEST_URI']);
?><tr id="<?php echo htmlspecialchars("$submission[type]$submission[id]")?>">
<td><input type="checkbox" name="<?php echo htmlspecialchars("$submission[type]$submission[id]")?>"></td>
<td><a href="<?php echo htmlspecialchars($edit_url)?>">
<?php echo format_sqldatetime($submission['submitted'])?></a></td>
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

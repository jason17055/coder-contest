<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

require_auth();

$problem_number = $_REQUEST['problem'];
$team_number = $_REQUEST['team'];

$sql = "SELECT contest FROM team
	WHERE team_number=".db_quote($_REQUEST['team']);
$result = mysql_query($sql);
$team_info = mysql_fetch_assoc($result)
	or die("Error: not authorized");  //invalid team number

$contest_id = $team_info['contest'];
is_director($contest_id)
	or die("Error: not authorized");

if ($_REQUEST['reset_opened'])
{
	$sql = "UPDATE results
		SET opened=NULL
		WHERE team_number=".db_quote($team_number) . "
		AND problem_number=".db_quote($problem_number);
	mysql_query($sql)
		or die("SQL error: ".mysql_error());
}

$sql = "SELECT * FROM results
	WHERE team_number=" . db_quote($team_number) . "
	AND problem_number=" . db_quote($problem_number);
$result = mysql_query($sql);
$row = mysql_fetch_assoc($result)
	or $row = array('thetime'=>0, 'incorrect_submissions'=>0);

if ($_POST['team'])
{
	die("not implemented");
}

$sql = "SELECT * FROM team WHERE team_number=".db_quote($team_number);
$result = mysql_query($sql);
$team_info = mysql_fetch_assoc($result)
	or die("Error: team $_REQUEST[team] not found");

$sql = "SELECT * FROM problem
	WHERE contest=".db_quote($contest_id) . "
	AND problem_number=".db_quote($problem_number);
$result = mysql_query($sql);
$problem_info = mysql_fetch_assoc($result)
	or die("Error: problem $problem_number not found");

$contest_info = get_basic_contest_info($contest_id);

begin_page("Results for $team_info[team_name]");
$problem_url = 'problem.php?contest='.urlencode($contest_id).'&id='.urlencode($problem_info['problem_number']);

?>

<table>
<tr>
<td>Team:</td>
<td><?php echo htmlspecialchars($team_info['team_name'])?></td>
</tr>
<tr>
<td>Problem:</td>
<td><a href="<?php echo htmlspecialchars($problem_url)?>"><?php echo htmlspecialchars($problem_info['problem_name'])?></a></td>
</tr>
<tr>
<td>Opened:</td>
<td><?php if ($row['opened']) {
	echo htmlspecialchars("Yes, at ".$row['opened']);
	$reset_url = "result.php?problem=".urlencode($_REQUEST['problem'])
		."&team=".urlencode($_REQUEST['team'])
		."&reset_opened=1";
	?> (<a href="<?php echo htmlspecialchars($reset_url)?>">Reset</a>)
	<?php
} else {
	echo "No";
}
	?>
</td>
</tr>
<tr>
<td>Solved:</td>
<td><?php if ($row['thetime']) {
		echo "Yes, in $row[thetime] minutes";
	} else {
		echo "No";
	}?></td>
</tr>
<tr>
<td>Incorrect Submissions:</td>
<td><?php if ($row['incorrect_submissions']) {
	echo htmlspecialchars($row['incorrect_submissions']);
} else {
	echo "0";
	} ?></td>
</tr>
<tr>
<td>Score:</td>
<td><?php echo format_score($row['score'], $row['score_alt'])?></td>
</tr>
</table>

<?php
$sql = "SELECT * FROM $schema.submission
	WHERE team=" . db_quote($_REQUEST['team']) . "
	AND problem=" . db_quote($_REQUEST['problem']) . "
	ORDER BY minutes,submitted,id";
$result = mysql_query($sql);
$submission_info = mysql_fetch_assoc($result);
if ($submission_info)
{
	$count=0;
?><table border="1">
<tr><th>Count</th><th>Submitted</th><th>Source Code</th><th>Status</th></tr>
<?php
	for (; $submission_info; $submission_info = mysql_fetch_assoc($result))
	{
		$count++;
		$edit_url = "submission.php?id=".urlencode($submission_info['id']);
		$file_url = "file.php/$submission_info[file]/$submission_info[given_name]";
		?>
<tr>
<td><a href="<?php echo htmlspecialchars($edit_url)?>"><?php echo $count?></a></td>
<td><?php echo htmlspecialchars("Time $submission_info[minutes]")?></td>
<td><a href="<?php echo htmlspecialchars($file_url)?>"><?php echo htmlspecialchars($submission_info['given_name'])?></a></td>
<td><?php echo htmlspecialchars($submission_info['status'])?></td>
</tr>
<?php
	}
	?>
</table>

<?php
} //end if any submissions found


$add_submission_url = "submission.php?team=".urlencode($_REQUEST['team'])."&problem=".urlencode($_REQUEST['problem']);
$controller_url = "controller.php?contest=".urlencode($team_info['contest']);

?>

<div><a href="<?php echo htmlspecialchars($add_submission_url)?>">Add Submission</a></div>

<div><a href="<?php echo htmlspecialchars($controller_url)?>">Back to Controller</a></div>

<?php
end_page();
?>

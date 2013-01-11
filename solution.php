<?php

require_once('config.php');
require_once('includes/functions.php');
require_once('includes/skin.php');
require_once('includes/auth.php');

require_auth();
$team_info = get_team_identity();

$sql = "SELECT id,team,
		s.problem AS problem_number,
		problem_name,minutes,file,given_name,
		COALESCE(status,'Pending') AS status
	FROM submission s
	JOIN team t
		ON t.team_number=s.team
	JOIN problem p
		ON p.contest=t.contest
		AND p.problem_number=s.problem
	WHERE id=".db_quote($_REQUEST['id']);
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
$submission_info = mysql_fetch_assoc($result)
	or die("Error: not authorized"); //invalid submission id
$submission_info['team'] == $team_info['team_number']
	or die("Error: not authorized");

$contest_id = $team_info['contest'];
$contest_info = get_basic_contest_info($contest_id);

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	die("not implemented");
}

begin_page('Solution');
$problem_url = "open_problem.php?problem=".urlencode($submission_info['problem_number']);

?>
<table>
<tr>
<td>Contestant:</td>
<td><?php echo htmlspecialchars($team_info['team_name'])?></td>
</tr>
<tr>
<td>Problem:</td>
<td><a href="<?php echo htmlspecialchars($problem_url)?>"><?php echo htmlspecialchars($submission_info['problem_name'])?></a></td>
</tr>
<tr>
<td>Minutes:</td>
<td><?php echo htmlspecialchars($submission_info['minutes'])?></td>
</tr>
<tr>
<td>Source code:</td>
<td><?php if ($submission_info['file']) {
	$file_url = "file.php/$submission_info[file]/$submission_info[given_name]";
	?>
<a href="<?php echo htmlspecialchars($file_url)?>"><?php echo htmlspecialchars($submission_info['given_name'])?></a>
<?php } else { ?>
<input name="program" type="file">
<?php } ?>
</td>
</tr>
<tr>
<td>Judge's Reply:</td>
<td><?php echo htmlspecialchars($submission_info['status']);

if ($submission_info['status'] == 'Accepted') {
	echo "  ( Note: this is a preliminary reply; your solution will be subjected to further testing before a final reply is given. )";
	}
	?></td>
</tr>
</table>

<p><a href="team_menu.php">Contestant Menu</a></p>
<?php
end_page();
?>

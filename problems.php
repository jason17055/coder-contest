<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

$contest_id = $_REQUEST['contest'];

require_auth();
is_judge_of($contest_id)
	or die("Error: not authorized");

$contest_info = get_basic_contest_info($contest_id);

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	die("not implemented");

	header("Location: problems.php?contest=" . urlencode($_REQUEST['contest']));
	exit();
}

begin_page("Define Problems");
?>

<form method="post" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">

<table border="1">
<tr>
<th>Problem Name</th>
<th>Scored</th>
<th>Open</th>
<th>Judged By</th>
<th>Solution</th>
<th>Test Files</th>
<th>Difficulty</th>
</tr>
<?php

$sql = "SELECT problem_number,problem_name,scoreboard_solved_image,
		".check_phase_option_sql('pp_scoreboard')." AS visible,
		".check_phase_option_sql('pp_submit')." AS allow_submissions,
		judged_by,
		solution_file,
		(SELECT COUNT(*) FROM system_test st
				WHERE st.problem_number=p.problem_number
				AND st.contest=p.contest) count_system_tests,
		difficulty
	FROM problem p
	WHERE contest=".db_quote($_REQUEST['contest'])."
	ORDER BY problem_number,problem_name";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
$count = 0;
while ($row = mysql_fetch_assoc($result))
{
	$count++;
	$edit_problem_url = "problem.php?id=".urlencode($row['problem_number'])."&contest=".urlencode($_REQUEST['contest']);
	$balloon_image = $row['scoreboard_solved_image'] ?
			('scoreboard_images/' . $row['scoreboard_solved_image']) :
			'scoreboard_images/balloon_red.png';
	?><tr>
<td>
<img style="vertical-align: middle" src="<?php echo htmlspecialchars($balloon_image)?>" alt="">
<a href="<?php echo htmlspecialchars($edit_problem_url)?>"><?php echo htmlspecialchars($row['problem_name'])?></a></td>
<td><?php echo htmlspecialchars($row['visible'])?></td>
<td><?php echo htmlspecialchars($row['allow_submissions'])?></td>
<td><?php echo htmlspecialchars($row['judged_by'])?></td>
<td align="center"><?php echo($row['solution_file'] ? '<img src="images/text_file.png" alt="On file">' : "-")?></td>
<td align="center"><?php echo htmlspecialchars($row['count_system_tests'])?></td>
<td align="center"><?php echo htmlspecialchars($row['difficulty'])?></td>
</tr>
<?php
}

$new_problem_url = "problem.php?contest=".urlencode($_REQUEST['contest']);
$controller_url = "controller.php?contest=".urlencode($_REQUEST['contest']);
$submissions_url = "listsubmissions.php?contest=".urlencode($_REQUEST['contest']);
$scoreboard_url = "scoreboard.php?contest=".urlencode($_REQUEST['contest']);
?>
</table>

</form>

<?php if (is_director($_REQUEST['contest'])) { ?>
<p>
<a href="<?php echo htmlspecialchars($new_problem_url)?>">New Problem</a> |
<a href="<?php echo htmlspecialchars($controller_url)?>">Controller</a>
</p>
<?php } else { ?>
<p>
<a href="<?php echo htmlspecialchars($submissions_url)?>">Submissions</a> |
<a href="<?php echo htmlspecialchars($scoreboard_url)?>" target="_new">Live Scoreboard</a>
</p>
<?php } ?>

<?php end_page() ?>

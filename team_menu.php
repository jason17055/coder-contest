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

if ($team_info['is_judge'] == 'Y')
{
	require('show_unjudged_submissions.inc.php');
}

require('show_problems_list.inc.php');

if ($team_info['is_contestant'] == 'Y')
{
	require('show_my_submissions.inc.php');
}

?>

<h3>Actions</h3>
<div>
<?php
if (is_director($contest_id)) {
	$url = "controller.php?contest=".urlencode($contest_id);
	?><a href="<?php echo htmlspecialchars($url)?>">Controller</a> |
<?php
}
?>
<a href="<?php echo htmlspecialchars($scoreboard_url)?>" target="_blank">Scoreboard</a>
<?php
if ($team_info['is_judge'] == 'Y' || $team_info['is_director'] == 'Y') {
$url="listsubmissions.php?contest=".urlencode($contest_id);
?>| <a href="<?php echo htmlspecialchars($url)?>">View Submissions</a>
<?php } ?>
<!--
| <a href="submit_test.php">Test a Solution</a>
| <a href="submit.php">Submit a Solution</a>
| <a href="newclarification.php">Request Clarification</a>
-->
</div>

<?php
end_page();
?>

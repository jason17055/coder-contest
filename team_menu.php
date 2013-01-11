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

if (TRUE || $team_info['is_contestant'] == 'Y')
{
	require('show_problems_list.inc.php');
}

if ($team_info['is_contestant'] == 'Y')
{
	require('show_my_submissions.inc.php');
}

?>

<h3>Actions</h3>
<div>
<a href="<?php echo htmlspecialchars($scoreboard_url)?>" target="_blank">Scoreboard</a>
<!--
| <a href="submit_test.php">Test a Solution</a>
| <a href="submit.php">Submit a Solution</a>
| <a href="newclarification.php">Request Clarification</a>
-->
</div>

<?php
end_page();
?>

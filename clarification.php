<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');

require_auth();
$team_info = get_team_identity();
$contest_id = $team_info['contest'];

$sql = "SELECT problem_name,request,response,status,team_number,team_name,c.contest AS contest
	FROM clarification c
	JOIN problem p
		ON p.contest=c.contest
		AND p.problem_number=c.problem_number
	JOIN team t
		ON t.team_number=c.team
	WHERE id=".db_quote($_REQUEST['id']);
$result = mysql_query($sql);
$clarification_info = mysql_fetch_assoc($result)
	or die("Error: clarification $_REQUEST[id] not found");
$clarification_info['contest'] == $team_info['contest']
	or die("Error: clarification $_REQUEST[id] not found");
($clarification_info['team_number'] == $team_info['team_number']
		|| $clarification_info['status'] == 'reply-all')
	or die("Error: clarification $_REQUEST[id] not found");

$contest_info = get_basic_contest_info($contest_id);

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	die("not implemented");
}

begin_page("Clarification");

?>

<table>
<tr>
<td>Problem:</td>
<td><?php echo htmlspecialchars($clarification_info['problem_name'])?></td>
</tr>
<tr>
<td valign="top">Message:</td>
<td>
<div>From team
"<?php echo htmlspecialchars($clarification_info['team_name'])?>"</div>
<textarea rows="10" cols="72" readonly="readonly"><?php
		echo htmlspecialchars($clarification_info['request'])
	?></textarea></td>
</tr>
<tr valign="top">
<td>Response:</td>
<td><div>
<?php echo(

	$clarification_info['status'] == "reply-one" ? "Only to your team" :
	($clarification_info['status'] == "reply-all" ? "To all teams" :
		"Pending-- The judge has not yet responded to your request.")
	) ?></div>
<?php
	if ($clarification_info['status']) { ?>
<textarea rows="10" cols="72" readonly="readonly"><?php
		echo htmlspecialchars($clarification_info['response'])
	?></textarea>
<?php } ?>
</td>
</tr>
</table>

<div>
<a href="team_menu.php">Team Menu</a>
</div>


<?php
end_page();
?>

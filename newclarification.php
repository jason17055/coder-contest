<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

require_auth();
$team_info = get_team_identity();
$contest_id = $team_info['contest'];

$clarification_info = array();
if ($_REQUEST['id'])
{
	$sql = "SELECT * FROM clarification
		WHERE id=".db_quote($_REQUEST['id']);
	$result = mysql_query($sql);
	$clarification_info = mysql_fetch_assoc($result)
		or die("Error: clarification $_REQUEST[id] not found");
}

$contest_info = get_basic_contest_info($contest_id);

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	if (isset($_REQUEST['action:cancel']))
	{
		$url = "team_menu.php";
		header("Location: $url");
		exit();
	}

	$_REQUEST['problem']
		or die("Error: problem not specified");

	$judge_user = choose_judge($contest_id, $team_info['ordinal'], $_REQUEST['problem']);
	$judge_id = null;
	if ($judge_user)
	{
		$sql = "SELECT judge_id FROM judge
			WHERE contest=".db_quote($contest_id)."
			AND judge_user=".db_quote($judge_user);
		$query = mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$row = mysql_fetch_row($query);
		$judge_id = $row[0];
	}

	$sql = "INSERT INTO clarification
		(contest,team, problem_number, submitted, request, judge)
		VALUES (
		".db_quote($contest_id).",
		".db_quote($team_info['team_number']).",
		".db_quote($_REQUEST['problem']).",
		NOW(),
		".db_quote($_REQUEST['message']).",
		".db_quote($judge_id).")";
	mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$clarification_id = mysql_insert_id();

	// send message to appropriate judge
	if (SEND_JUDGE_MESSAGES)
	{
		$sql = "SELECT problem_name FROM problem
			WHERE contest=".db_quote($contest_id)."
			AND problem_number=".db_quote($_REQUEST['problem']);
		$query = mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$problem_info = mysql_fetch_assoc($query)
			or die("Error: problem $_REQUEST[problem] not found");

		$url = 'answer_clarification.php?id='.urlencode($clarification_id);
		$message = "There is a new clarification request for $problem_info[problem_name].";
		send_message('N', $contest_id, $judge_user, $message, $url);
	}
	else
	{
		wakeup_listeners();
	}

	$url = "team_menu.php";
	header("Location: $url");
	exit();
}

begin_page("Clarification");

?>
<p>
Confused by a problem specification? Request a clarification here.
</p>

<form method="post" action="newclarification.php">
<table>
<tr>
<td>Problem:</td>
<td><select name="problem">
<option value="" selected="selected">--select--</option>
<?php
	$sql = "SELECT problem_number,problem_name
		FROM problem
		WHERE contest=".db_quote($contest_id)."
		AND ".check_phase_option_bool('pp_submit')."
		ORDER BY problem_number";
	$result = mysql_query($sql);
while ($row = mysql_fetch_assoc($result)) {
	?><option value="<?php echo htmlspecialchars($row['problem_number'])?>"
		<?php
	if ($row['problem_number'] == $clarification_info['problem_number']) {
		echo ' selected="selected"';
	}?>><?php echo htmlspecialchars($row['problem_name'])?></option>
<?php }

?></select></td>
</tr>
<tr>
<td valign="top">Message:</td>
<td><textarea name="message" rows="10" cols="72"><?php
		echo htmlspecialchars($clarification_info['request'])
	?></textarea></td>
</tr>
</table>

<div>
<button type="submit">Submit</button>
<button type="submit" name="action:cancel">Cancel</button>
</div>

</form>


<?php
end_page();
?>

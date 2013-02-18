<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');
require_once('includes/notify.php');

require_auth();

if ($_REQUEST['id'])
{
	$sql = "SELECT id,team,c.contest AS contest,request,response,
			problem_name,status
		FROM clarification c
		JOIN problem p
			ON p.contest=c.contest
			AND p.problem_number=c.problem_number
		WHERE c.id=".db_quote($_REQUEST['id']);
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$clarification_info = mysql_fetch_assoc($result)
		or die("Error: invalid clarification or not authorized");

	$contest_id = $clarification_info['contest'];
	is_judge_of($contest_id)
		or die("Error: invalid clarification or not authorized");
}
else
{
	$contest_id = $_REQUEST['contest'];
	is_judge_of($contest_id)
		or die("Error: invalid contest");

	// get information about the problem
	$sql = "SELECT problem_name
		FROM problem
		WHERE contest=".db_quote($contest_id)."
		AND problem_number=".db_quote($_REQUEST['problem']);
	$query = mysql_query($sql);
	$problem_info = mysql_fetch_assoc($query)
		or die("Error: invalid problem");

	$clarification_info = array(
		'contest' => $_REQUEST['contest'],
		'problem' => $_REQUEST['problem'],
		'problem_name' => $problem_info['problem_name']
		);
}

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	$next_url = $_REQUEST['next_url'] ?: "listsubmissions.php?contest=".urlencode($contest_id);

	if (isset($_REQUEST['action:cancel']))
	{
		header("Location: $next_url");
		exit();
	}
	else if (isset($_REQUEST['action:delete_clarification']))
	{
		is_director($contest_id)
			or die("Must be director to delete clarifications");

		$sql = "DELETE FROM clarification
			WHERE id=".db_quote($_REQUEST['id'])."
			AND contest=".db_quote($contest_id);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		header("Location: $next_url");
		exit();
	}

	if ($_REQUEST['id'])
	{
		// update an existing clarification
		$s = $_REQUEST['status'] ? $_REQUEST['status'] : null;
		$sql = "UPDATE clarification
			SET request=".db_quote($_REQUEST['request']).",
			response=".db_quote($_REQUEST['response']).",
			status=".db_quote($s)."
			WHERE id=".db_quote($_REQUEST['id']);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());
	}
	else
	{
		// create a new clarification
		$s = 'reply-all';
		$sql = "INSERT INTO clarification (contest,problem_number,
				submitted,response,status)
				VALUES (
				".db_quote($contest_id).",
				".db_quote($_REQUEST['problem']).",
				NOW(),
				".db_quote($_REQUEST['response']).",
				".db_quote($s).")";
		mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$_REQUEST['id'] = mysql_insert_id();
	}

	if ($s != $clarification_info['status'])
	{
		$url = "clarification.php?id=".urlencode($_REQUEST['id']);
		if ($s == 'reply-one')
		{
			$message = "Your clarification request for $clarification_info[problem_name] has been answered.";

			$sql = "INSERT INTO announcement
				(contest,recipient,message,url,expires)
				SELECT contest,user,
				".db_quote($message).",
				".db_quote($url).",
				DATE_ADD(NOW(),INTERVAL 60 MINUTE)
				FROM team
				WHERE contest=".db_quote($contest_id)."
				AND team_number=".db_quote($clarification_info['team']);
			mysql_query($sql)
				or die("SQL error: ".mysql_error());
			wakeup_listeners();
		}
		else if ($s == 'reply-all')
		{
			$message = "A clarification for $clarification_info[problem_name] has been issued.";
			
			$sql = "INSERT INTO announcement
				(contest,recipient,message,url,expires)
				VALUES (
				".db_quote($contest_id).",
				'teams',
				".db_quote($message).",
				".db_quote($url).",
				DATE_ADD(NOW(),INTERVAL 60 MINUTE)
				)";
			mysql_query($sql)
				or die("SQL error: ".mysql_error());
			wakeup_listeners();
		}
	}

	header("Location: $next_url");
	exit();
}

begin_page("Answer Clarification");

?>
<script type="text/javascript"><!--

function checkAnswerClarificationForm()
{
	if (document.answer_clarification_form.status.value == '')
	{
		return confirm('Really continue without responding to clarification?');
	}
	return true;
}

//--></script>

<form method="post" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>" name="answer_clarification_form">
<table>
<tr>
<td>Problem:</td>
<td><?php echo htmlspecialchars($clarification_info['problem_name'])?></td>
</tr>
<?php
	if ($clarification_info['team']) {
	?>
<tr>
<td valign="top">Message:</td>
<td><textarea name="request" rows="10" cols="72"><?php
		echo htmlspecialchars($clarification_info['request'])
	?></textarea></td>
</tr>
<tr>
<td valign="top">Response Type:</td>
<td><select name="status"><?php
	$a = array("|--choose--","reply-one|Reply Only to Team","reply-all|Reply to All Teams");
	foreach ($a as $c) {
		list($c1,$c2) = explode("|",$c);
		?><option value="<?php echo htmlspecialchars($c1)?>"<?php
			echo($c1==$clarification_info['status'] ? ' selected="selected"' : '')
			?>><?php echo htmlspecialchars($c2)?></option>
<?php
	}
?>
</select></td>
</tr>
<?php } /* end if clarification came from a team  */ ?>
<tr>
<td valign="top">Response Message:</td>
<td>
<textarea name="response" rows="10" cols="72"><?php
		echo htmlspecialchars($clarification_info['response'])
	?></textarea></td>
</tr>
</table>

<div>
<button type="submit" onclick='return checkAnswerClarificationForm()'>Submit</button>
<?php if ($_REQUEST['id'] && is_director($contest_id)) { ?>
<button type="submit" name="action:delete_clarification" onclick='return confirm("Really delete this clarification?")'>Delete</button>
<?php } ?>
<button type="submit" name="action:cancel">Cancel</button>
</div>

</form>


<?php
end_page();
?>

<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/notify.php');

require_auth();
$contest_id = $_REQUEST['contest'];
is_director($contest_id)
	or die("Error: not authorized");

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	if (isset($_POST['action:cancel']))
	{
		$url = "controller.php?contest=".urlencode($contest_id);
		header("Location: $url");
		exit();
	}

	if (isset($_POST['action:create_announcement']))
	{
		$sql = "INSERT INTO announcement (messagetype,contest,recipient,message,duration,expires)
			SELECT 'N',
				" . db_quote($contest_id) . ",
				" . db_quote($_REQUEST['recipient']) . ",
				" . db_quote($_REQUEST['message']) . ",
				" . db_quote($_REQUEST['duration']) . ",
				DATE_ADD(NOW(), INTERVAL 1 HOUR)
			FROM dual";
		mysql_query($sql)
			or die("SQL error: " . mysql_error() . "... $sql");
		wakeup_listeners();

		$url = "controller.php?contest=".urlencode($contest_id);
		header("Location: $url");
		exit();
	}
}

begin_page("Send Message");
?>
<form method="post" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<table>
<tr><td>Send to:</td>
<td><select name="recipient">
<option value="*">--everyone--</option>
<option value="teams">--all contestants--</option>
<option value="judges">--all judges--</option>
<!--
<option value="scoreboard">--scoreboard--</option>
-->
<?php
	$sql = "SELECT user,team_name FROM team
		WHERE contest=".db_quote($contest_id)."
		ORDER BY ordinal";
	$query = mysql_query($sql)
		or die("SQL error: " . mysql_error());
	while ($row = mysql_fetch_assoc($query))
	{ ?>
<option value="<?php echo htmlspecialchars($row['user'])?>"><?php echo htmlspecialchars("$row[user] - $row[team_name]")?></option>
<?php
	}
?>
</select>
</td>
</tr>
<tr>
<td>Message (HTML):</td>
<td>
<textarea name="message" rows="10" cols="60"><?php echo htmlspecialchars($row['message'])?></textarea>
</td>
</tr>
</table>
<p>
<button type="submit" name="action:create_announcement">Create</button>
<button type="submit" name="action:cancel">Cancel</button>
</p>
</form>
<?php
end_page();
?>

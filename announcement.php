<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');

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
		$sql = "INSERT INTO announcement (messagetype,recipient,message,duration)
			SELECT 'N',"
				. db_quote("contest$contest_id") . ", "
				. db_quote($_REQUEST['message']) . ", "
				. db_quote($_REQUEST['duration']) . "
			FROM dual";
		mysql_query($sql)
			or die("SQL error: " . mysql_error() . "... $sql");
		$url = "controller.php?contest=".urlencode($contest_id);
		header("Location: $url");
		exit();
	}
}

begin_page("Create Announcement");
?>
<form method="post" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<div>Message (HTML):
<textarea name="message" rows="10" cols="60"><?php echo htmlspecialchars($row['message'])?></textarea>
</div>
<div>Duration (seconds):
<input type="text" name="duration" value="<?php echo htmlspecialchars($row['duration'])?>">
</div>
<p>
<button type="submit" name="action:create_announcement">Create</button>
<button type="submit" name="action:cancel">Cancel</button>
</p>
</form>
<?php
end_page();
?>

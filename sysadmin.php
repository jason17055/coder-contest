<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');

require_auth();
is_sysadmin()
	or die("Error: not authorized");

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	if ($_REQUEST['new_passwd'])
	{
		$sql = "UPDATE passwd
		SET password=SHA1(".db_quote($_REQUEST['new_passwd']).")
		WHERE username=".db_quote($_SESSION['uid']);
		mysql_query($sql)
			or die("SQL error: " . mysql_error());

		$url = "sysadmin.php";
		header("Location: $url");
		exit();
	}
}

begin_page("System Administration");
?>
<table border="1">
<tr>
<th>Contest</th><th>Subtitle</th><th>Enabled</th>
</tr>
<?php
	$sql = "SELECT * FROM contest
		ORDER BY title, subtitle";
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
while ($row = mysql_fetch_assoc($result)) {
	$director_url = "controller.php?contest=".urlencode($row['contest_id']);
	?><tr>
<td><a href="<?php echo htmlspecialchars($director_url)?>"><?php echo htmlspecialchars($row['title'])?></a></td>
<td><?php echo htmlspecialchars($row['subtitle'])?></td>
<td><?php echo htmlspecialchars($row['enabled'])?></td>
</tr>
<?php
}?>
</table>
<p>
<a href="contest.php">New Contest</a>
</p>

<form method="post" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<p>Change sysadmin password:
<input type="password" name="new_passwd">
<button type="submit">Change Password</button>
</p>
</form>

<?php end_page() ?>

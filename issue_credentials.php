<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');
require_once('includes/borrowed.inc.php');

$contest_id = $_REQUEST['contest'];

require_auth();
is_director($contest_id)
	or die("Error: not authorized");

$contest_info = get_basic_contest_info($contest_id);

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	if (isset($_REQUEST['action:cancel']))
	{
		$next_url = "users.php?contest=".urlencode($_REQUEST['contest']);
		header("Location: $next_url");
		exit();
	}

	$passwords = array();

	$sql = "SELECT team_number,team_name,description,ordinal,user
		FROM team
		WHERE contest=".db_quote($_REQUEST['contest'])."
		ORDER BY team_name";
	$result = mysql_query($sql);
	while ($row = mysql_fetch_assoc($result))
	{
		if ($_POST["reset$row[team_number]"])
		{
			$pass = $_POST["password$row[team_number]"];
			$sql = "UPDATE team
				SET password=SHA1(".db_quote($pass).")
				WHERE team_number=".db_quote($row['team_number']);
			mysql_query($sql)
				or die("SQL error: ".mysql_error());

			$passwords[] = array(
				team_name => $row['team_name'],
				description => $row['description'],
				username => $row['user'],
				password => $pass,
				);
		}
	}

	$next_url = "users.php?contest=".urlencode($_REQUEST['contest']);
	if ($_POST['do_printout'])
	{
		require("credentials_handouts.inc.php");
	}
	else
	{
		header("Location: $next_url");
	}
	exit();
}

begin_page("Issue Login Credentials");
?>
<form method="post">
<p>Select which teams/judges you want to set passwords on:</p>
<table class="realtable">
<tr>
<th>Name</th>
<th>Username</th>
<th>Password</th>
</tr>
<?php
	$sql = "SELECT team_number AS id,team_name,user,password,ordinal
		FROM team t
		WHERE contest=".db_quote($_REQUEST['contest'])."
		ORDER BY is_contestant DESC,ordinal,team_name";
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
while ($row = mysql_fetch_assoc($result)) {
	$pass = generatePassword(5);
	?><tr>
<td><input type="checkbox" id="reset<?php echo htmlspecialchars($row['id'])?>_btn" name="reset<?php echo htmlspecialchars($row['id'])?>"<?php
		if (!$row['password']) {
			echo ' checked="checked"';
		}?>>
<label for="reset<?php echo htmlspecialchars($row['id'])?>_btn"><?php echo htmlspecialchars($row['team_name'])?></label></td>
<td><?php echo htmlspecialchars($row['user'])?></td>
<td><input type="text" name="password<?php echo htmlspecialchars($row['id'])?>" value="<?php echo htmlspecialchars($pass)?>"></td>
</tr>
<?php
} //end while
?>
</table>

<p>
<input type="checkbox" name="do_printout" id="do_printout_btn"
checked="checked">
<label for="do_printout_btn">Generate per-team username/password handouts</label>
</p>

<p>
<button type="submit">Set Passwords</button>
<button type="submit" name="action:cancel">Cancel</button>
</p>

</form>

<?php end_page() ?>

<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

$contest_id = $_REQUEST['contest'];

require_auth();
is_director($contest_id)
	or die("Error: not authorized");

$contest_info = get_basic_contest_info($contest_id, "
	, (SELECT COUNT(*) FROM team WHERE contest=contest_id AND is_contestant='Y') AS contestant_count
	, (SELECT COUNT(*) FROM team WHERE contest=contest_id AND is_contestant='N') AS judge_count
		");

$contestant_count = $contest_info['contestant_count'];
$judge_count = $contest_info['judge_count'];

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	if (isset($_POST['action:autocreate']))
	{
		for ($i = 1; $i <= $_REQUEST['count']; $i++)
		{
			$name = "team$i";
			$sql = "INSERT INTO team (team_name,contest,user,ordinal,is_contestant)
				SELECT ".db_quote($name).",
				".db_quote($_REQUEST['contest']).",
				".db_quote($name).",
				$i,
				'Y'
				FROM dual
				WHERE NOT EXISTS (SELECT 1 FROM team WHERE contest=".db_quote($_REQUEST['contest'])." AND user=".db_quote($name).")
				";
			mysql_query($sql)
				or die("SQL error: " . mysql_error());
		}
		if ($_REQUEST['count'] < $contestant_count)
		{
			die("Oops, you already have more than $_REQUEST[count] teams. You cannot delete teams this way.");
		}
	}

	if (isset($_POST['action:autocreate_judge']))
	{
		for ($i = 1; $i <= $_REQUEST['jcount']; $i++)
		{
			$name = "judge$i";
			$sql = "INSERT INTO team (team_name,description,contest,user,ordinal,is_contestant,is_judge)
				SELECT ".db_quote($name).",
				'Contest Judge',
				".db_quote($_REQUEST['contest']).",
				".db_quote($name).",
				$i,
				'N','Y'
				FROM dual
				WHERE NOT EXISTS (SELECT 1 FROM team WHERE contest=".db_quote($_REQUEST['contest'])." AND user=".db_quote($name).")
				";
			mysql_query($sql)
				or die("SQL error: " . mysql_error());
		}

		for ($i = $_REQUEST['jcount'] + 1; $i <= $judge_count; $i++)
		{
			$name = "judge$i";
			$sql = "DELETE FROM team
				WHERE contest=".db_quote($_REQUEST['contest'])."
				AND user=".db_quote($name)."
				AND is_contestant='N' AND is_director='N'";
			mysql_query($sql)
				or die("SQL error: " . mysql_error());
		}
	}

	$sql = "SELECT team_number
		FROM team
		WHERE contest=".db_quote($_REQUEST['contest']);
	$query = mysql_query($sql)
		or die("SQL error: " . mysql_error());
	while ($row = mysql_fetch_assoc($query))
	{
		$name_key = "team$row[team_number]_name";
		$desc_key = "team$row[team_number]_description";

		if (isset($_REQUEST[$name_key]))
		{
			$sql = "UPDATE team
			SET team_name=".db_quote($_REQUEST[$name_key]).",
			description=".db_quote($_REQUEST[$desc_key])."
			WHERE team_number=".db_quote($row['team_number']);
			mysql_query($sql)
				or die("SQL error: " . mysql_error());
		}
	}
	
	header("Location: users.php?contest=" . urlencode($_REQUEST['contest']));
	exit();
}

begin_page("Define Users");
?>

<form method="post" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<h3>Contestants</h3>

<p>
Number of contestants:
<input type="text" name="count" value="<?php echo $contestant_count?>" size="3">
<button type="submit" name="action:autocreate">Go</button>
</p>
<table border="1">
<tr>
<th>Contestant ID</th>
<th>Display Name</th>
<th>Description</th>
</tr>
<?php

$sql = "SELECT * FROM team
	WHERE contest=".db_quote($_REQUEST['contest'])."
	AND is_contestant='Y'
	ORDER BY ordinal,team_name";
$result = mysql_query($sql);
$count = 0;
while ($row = mysql_fetch_assoc($result))
{
	$count++;
	$edit_team_url = "user.php?id=".urlencode($row['team_number']);
	?><tr>
<td><a href="<?php echo htmlspecialchars($edit_team_url)?>"><?php echo htmlspecialchars($row['user'])?></a>
<?php if ($row['visible'] == 'N') {
	?> (Invisible)<?php
	}?>
	</td>
<td><input type="text" name="team<?php echo htmlspecialchars($row['team_number'])?>_name" value="<?php echo htmlspecialchars($row['team_name'])?>" size="20"></td>
<td><input type="text" name="team<?php echo htmlspecialchars($row['team_number'])?>_description" value="<?php echo htmlspecialchars($row['description'])?>" size="40"></td>
</tr>
<?php
}

$new_team_url = "user.php?contest=".urlencode($_REQUEST['contest']);
$issue_creds_url = "issue_credentials.php?contest=".urlencode($_REQUEST['contest']);
$controller_url = "controller.php?contest=".urlencode($_REQUEST['contest']);
?>
</table>

<h3>Judges</h3>

<p>
Number of judges:
<input type="text" name="jcount" value="<?php echo $judge_count?>" size="3">
<button type="submit" name="action:autocreate_judge">Go</button>
</p>

<table border="1">
<tr>
<th>Judge ID</th>
</tr>
<?php

$sql = "SELECT * FROM team
	WHERE contest=".db_quote($_REQUEST['contest'])."
	AND is_contestant='N'
	ORDER BY ordinal,team_name";
$result = mysql_query($sql);
$count = 0;
while ($row = mysql_fetch_assoc($result))
{
	$count++;
	$edit_judge_url = "user.php?id=".urlencode($row['team_number']);
	?><tr>
<td><a href="<?php echo htmlspecialchars($edit_judge_url)?>"><?php echo htmlspecialchars($row['user'])?></a></td>
</tr>
<?php
}

$new_team_url = "user.php?contest=".urlencode($_REQUEST['contest']);
$issue_creds_url = "issue_credentials.php?contest=".urlencode($_REQUEST['contest']);
$controller_url = "controller.php?contest=".urlencode($_REQUEST['contest']);
?>
</table>
<p>
<button type="submit">Save Changes</button>
</p>

</form>

<p>
<!--
<a href="<?php echo htmlspecialchars($new_team_url)?>">New Team</a> |
-->
<a href="<?php echo htmlspecialchars($issue_creds_url)?>">Generate Passwords</a>
| <a href="<?php echo htmlspecialchars($controller_url)?>">Controller</a>
</p>

<?php end_page() ?>

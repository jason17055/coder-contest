<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

if (isset($_REQUEST['id']))
{
	$query = "SELECT * FROM team
		WHERE team_number=" . db_quote($_REQUEST['id']);
	$result = mysql_query($query);
	$team_info = mysql_fetch_assoc($result);
}
else
{
	$team_info = array(
		contest => $_REQUEST['contest'],
		visible => 'Y',
		);
}

$contest_id = $team_info['contest'];
$contest_info = get_basic_contest_info($contest_id, "
	, teams_can_change_name,teams_can_change_description,
		teams_can_change_password");

require_auth();
if (is_director($team_info['contest']))
{
	$can_delete = true;
	$can_change_ordinal = true;
	$can_change_name = true;
	$can_change_description = true;
	$can_change_username = true;
	$can_change_password = true;
	$can_change_visibility = true;
	$can_change_roles = true;
}
else
{
	$can_delete = false;
	$can_change_ordinal = false;
	$can_change_name = $contest_info['teams_can_change_name'] == 'Y';
	$can_change_description = $contest_info['teams_can_change_description'] == 'Y';
	$can_change_username = false;
	$can_change_password = $contest_info['teams_can_change_password'];
	$can_change_visibility = false;
	$can_change_roles = false;

	$_SESSION['is_team'] == $team_info['team_number']
		or die("Error: not authorized");
}


if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	$next_url = $_REQUEST['next_url'] ? $_REQUEST['next_url'] :
		($_SESSION['is_team'] ? "team_menu.php"
		: "users.php?contest=".urlencode($team_info['contest']));

	if (isset($_POST['action:cancel']))
	{
		header("Location: $next_url");
		exit();
	}
	else if (isset($_POST['action:create_team']))
	{
		die("not implemented");
	}
	elseif (isset($_POST['action:delete_team']))
	{
		is_director($contest_id)
			or die("Error: not authorized to delete a team");

		$sql = "DELETE FROM results
			WHERE team_number=" . db_quote($_REQUEST['id']);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		$sql = "DELETE FROM test_result
				WHERE submission IN (SELECT id FROM submission
						WHERE team=".db_quote($_REQUEST['id']).")";
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		$sql = "DELETE FROM submission
			WHERE team=" . db_quote($_REQUEST['id']);
		mysql_query($sql)
			or die("SQL error: " . mysql_error());

		$sql = "DELETE FROM clarification
			WHERE team=".db_quote($_REQUEST['id']);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		$sql = "DELETE FROM team
			WHERE team_number=" . db_quote($_REQUEST['id']);
		mysql_query($sql)
			or die("SQL error: " . mysql_error());

		$url = "users.php?contest=".urlencode($contest_id);
		header("Location: $next_url");
		exit();
	}
	else
	{
		//teams are allowed to change their name/description here

		$updates = array();
		if ($can_change_ordinal && isset($_REQUEST['ordinal']))
		{
			$updates[] = "ordinal=".db_quote($_REQUEST['ordinal']);
		}
		if ($can_change_name && $_REQUEST['team_name'])
		{
			$updates[] = "team_name=".db_quote($_REQUEST['team_name']);
		}
		if ($can_change_description && isset($_REQUEST['description']))
		{
			$updates[] = "description=".db_quote($_REQUEST['description']);
		}
		if ($can_change_username && $_REQUEST['user'])
		{
			$updates[] = "user=".db_quote($_REQUEST['user']);
		}
		if ($can_change_password && $_REQUEST['new_passwd'])
		{
			$updates[] = "password=SHA1(".db_quote($_REQUEST['new_passwd']).")";
		}
		if ($can_change_visibility && $_REQUEST['visible'])
		{
			$updates[] = "visible=".db_quote($_REQUEST['visible']);
		}
		if ($can_change_roles)
		{
			foreach (array("is_contestant","is_judge","is_director") as $k)
			{
				$v = $_REQUEST[$k];
				$updates[] = "$k=".($v?"'Y'":"'N'");
			}
		}

		if (count($updates))
		{
			$sql = "UPDATE team SET " . join(',', $updates) ."
				WHERE team_number=" . db_quote($_REQUEST['id']);
			mysql_query($sql)
				or die("SQL error: " . mysql_error());
		}

		header("Location: $next_url");
		exit();
	}
}

begin_page("Edit Contestant");
?>
<form method="post">
<table>
<tr>
<td>Contest:</td>
<td><?php echo htmlspecialchars($contest_info['title'])?></td>
</tr>
<?php if ($can_change_ordinal) { ?>
<tr>
<td>Order:</td>
<td><input type="text" name="ordinal" value="<?php echo htmlspecialchars($team_info['ordinal'])?>"></td>
</tr>
<?php } // endif can change ordinal ?>
<?php if ($can_change_visibility) { ?>
<tr>
<td>Visible:</td>
<td><select name="visible">
<option value="Y"<?php echo ($team_info['visible'] == 'Y' ? ' selected="selected"' : '')?>>Yes</option>
<option value="N"<?php echo ($team_info['visible'] == 'N' ? ' selected="selected"' : '')?>>No</option>
</select></td>
</tr>
<?php } // endif can change visibility ?>
<?php if ($can_change_roles) { ?>
<tr>
<td>Roles:</td>
<td>
<label><input type="checkbox" name="is_contestant"<?php echo ($team_info['is_contestant'] == 'Y' ? ' checked="checked"' : '')?>>Contestant</label>
<label><input type="checkbox" name="is_judge"<?php echo ($team_info['is_judge'] == 'Y' ? ' checked="checked"' : '')?>>Judge</label>
<label><input type="checkbox" name="is_director"<?php echo ($team_info['is_director'] == 'Y' ? ' checked="checked"' : '')?>>Director</label>
</td>
</tr>
<?php } // endif can change roles ?>
<tr>
<td>Display Name:</td>
<td><input type="text" name="team_name" value="<?php echo htmlspecialchars($team_info['team_name'])?>"<?php echo($can_change_name ? '' : ' disabled="disabled"')?>></td>
</tr>
<tr>
<td>Description:</td>
<td><input type="text" name="description" value="<?php echo htmlspecialchars($team_info['description'])?>"<?php echo($can_change_description ? '' : ' disabled="disabled"')?>></td>
</tr>
<tr>
<td>Username:</td>
<td><input type="text" name="user" value="<?php echo htmlspecialchars($team_info['user'])?>"<?php echo($can_change_username ? '' : ' disabled="disabled"')?>></td>
</tr>
<tr>
<td>Password:</td>
<td><input type="password" name="new_passwd"<?php echo($can_change_password ? '' : ' disabled="disabled"')?>></td>
</tr>
</table>
<div>
<?php if (isset($_REQUEST['id'])) { ?>
<button type="submit">Update</button>
<?php if ($can_delete) { ?>
<button type="submit" name="action:delete_team" onclick='return confirm("Really delete this team and all their submissions?")'>Delete</button>
<?php } /*endif can_delete */ ?>
<?php } else { ?>
<button type="submit" name="action:create_team">Create</button>
<?php } //endif ?>
<button type="submit" name="action:cancel">Cancel</button>
</div>
</form>
<?php
end_page();
?>

<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

if (isset($_REQUEST['contest']))
{
	$query = "SELECT * FROM contest
		WHERE contest_id=" . db_quote($_REQUEST['contest']);
	$result = mysql_query($query);
	$row = mysql_fetch_assoc($result);
}
else
{
	$timestamp = 60*30 * ceil(time() / (60*30));
	$row = array(
		"teams_can_change_password" => "Y",
		"score_system" => "A",
		"scoreboard_images" => "Y",
		"scoreboard_popups" => "Y",
		"scoreboard_order" => 'n',
		"scoreboard_fanfare" => 'Y',
		"started" => strftime('%Y-%m-%d %H:%M:%S', $timestamp),
		);
}
$contest_info = $row;
determine_phases($contest_info);

require_auth();
if ($_REQUEST['contest'])
{
	is_director($_REQUEST['contest'])
		or die("Error: not authorized");
}
else
{
	is_sysadmin()
		or die("Error: not authorized");
}

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	$checkboxes = array('scoreboard_images', 'scoreboard_popups',
		'scoreboard_fanfare',
		'teams_can_change_name', 'teams_can_change_description',
		'teams_can_change_password', 'teams_can_write_code');
	$updates = array();
	foreach ($checkboxes as $c) {
		if ($_REQUEST[$c]) {
			$updates[] = "$c='Y'";
		} else {
			$updates[] = "$c='N'";
		}
	}

	if ($_REQUEST['director_password'])
	{
		$updates[] = "director_password=SHA1(".db_quote($_REQUEST['director_password']).")";
	}

	if (is_sysadmin())
	{
		if (isset($_REQUEST['auth_method'])) {
			$updates[] = "auth_method=".db_quote($_REQUEST['auth_method']);
		}
		if (isset($_REQUEST['enabled'])) {
			$updates[] = "enabled=".db_quote($_REQUEST['enabled']);
		}
	}

	for ($i = 1; $i <= 4; $i++)
	{
		$updates[] = "phase{$i}_name=".db_quote($_REQUEST["phase{$i}_name"]);
		if ($_REQUEST["phase{$i}_ends"] == '')
			$updates[] = "phase{$i}_ends=NULL";
		else
			$updates[] = "phase{$i}_ends=".db_quote($_REQUEST["phase{$i}_ends"]);
	}

	if (isset($_POST['action:cancel']))
	{
		$next_url = $_REQUEST['contest'] ?
			"controller.php?contest=".urlencode($_REQUEST['contest']) :
			"sysadmin.php";
		header("Location: $next_url");
		exit();
	}
	if (isset($_POST['action:create_contest']))
	{
		$director_username = "director";

		is_sysadmin()
			or die("Error: not authorized");
		$sql = "INSERT INTO contest (title,subtitle,logo,director,collaboration,score_system,scoreboard,scoreboard_order,started,handout_html)
			VALUES (
			".db_quote($_REQUEST['title']).",
			".db_quote($_REQUEST['subtitle']).",
			".db_quote($_REQUEST['logo']).",
			".db_quote($director_username).",
			".db_quote($_REQUEST['collaboration']).",
			".db_quote($_REQUEST['score_system']).",
			".db_quote($_REQUEST['scoreboard']).",
			".db_quote($_REQUEST['scoreboard_order']).",
			".db_quote($_REQUEST['started']).",
			".db_quote($_REQUEST['handout_html']).")";
		mysql_query($sql)
			or die("SQL error: " . mysql_error() . "... $sql");
		$contest_id = mysql_insert_id();

		if (count($updates) > 0)
		{
			$sql = "UPDATE contest SET " . join(',',$updates) . "
			WHERE contest_id=" . db_quote($contest_id);
			mysql_query($sql)
				or die("SQL error: " . mysql_error());
		}

		$url = "controller.php?contest=".urlencode($contest_id);
		header("Location: $url");
		exit();
	}
	elseif (isset($_POST['action:delete_contest']))
	{
		die("not implemented");
	}
	else
	{
		$sql = "UPDATE contest
			SET title=" . db_quote($_REQUEST['title']) . ",
			subtitle=" . db_quote($_REQUEST['subtitle']) . ",
			logo=" . db_quote($_REQUEST['logo']) . ",
			collaboration=" . db_quote($_REQUEST['collaboration']) . ",
			score_system=" . db_quote($_REQUEST['score_system']) . ",
			scoreboard=" . db_quote($_REQUEST['scoreboard']) . ",
			scoreboard_images=" . db_quote($scoreboard_use_images) . ",
			scoreboard_popups=" . db_quote($scoreboard_use_popups) . ",
			scoreboard_order=" . db_quote($_REQUEST['scoreboard_order']) . ",
			started=".db_quote($_REQUEST['started']).",
			handout_html=".db_quote($_REQUEST['handout_html'])."
			WHERE contest_id=" . db_quote($_REQUEST['contest']);
		mysql_query($sql)
			or die("SQL error: " . mysql_error());

		if (count($updates) > 0)
		{
			$sql = "UPDATE contest SET " . join(',',$updates) . "
			WHERE contest_id=" . db_quote($_REQUEST['contest']);
			mysql_query($sql)
				or die("SQL error: " . mysql_error());
		}

		if ($_REQUEST['score_system'] != $contest_info['score_system'])
		{
			update_results_for_all_problems($_REQUEST['contest']);
		}

		$url = "controller.php?contest=".urlencode($_REQUEST['contest']);
		header("Location: $url");
		exit();
	}
}

begin_page($_REQUEST['contest'] ? "Edit Contest" : "New Contest");

?>
<form method="post">
<table>
<tr>
<td>Title:</td>
<td><input type="text" name="title" size="40"
	value="<?php echo htmlspecialchars($row['title'])?>"></td>
</tr>
<tr>
<td>Subtitle:</td>
<td><input type="text" name="subtitle" size="40"
	value="<?php echo htmlspecialchars($row['subtitle'])?>"></td>
</tr>
<tr>
<td>Logo:</td>
<td><input type="text" name="logo" value="<?php echo htmlspecialchars($row['logo'])?>">
<small>URL to a 100x100 image to display on the live scoreboard</small></td>
</tr>
<tr>
<td>Director Login:</td>
<td><b><?php echo htmlspecialchars($row['director'])?></b> -
Change Password: <input type="text" name="director_password" value="">
</td>
</tr>
<?php if (is_sysadmin()) { ?>
<tr>
<td>Enabled:</td>
<td><?php select_option_widget('enabled',
		array("Y|Enabled","N|Disabled"), $row['enabled'])?></td>
</tr>
<tr>
<td>Auth method:</td>
<td><?php select_option_widget('auth_method',
		array("|Internal", "CAS|CAS"), $row['auth_method'])?></td>
</tr>
<?php } /* end if sysadmin */ ?>
<tr>
<td>Contestant options:</td>
<td><?php
	foreach (array(
		"teams_can_change_name|Change name",
		"teams_can_change_description|Change description",
		"teams_can_change_password|Change password",
		"teams_can_write_code|Write code"
		) as $option_info)
	{
		list($name,$label) = explode("|", $option_info);
		$selected = $row[$name] == 'Y' ? ' checked="checked"' : '';
		?><input type="checkbox" name="<?php echo htmlspecialchars($name)?>" id="<?php echo htmlspecialchars($name)?>_btn"<?php echo $selected?>>
<label for="<?php echo htmlspecialchars($name)?>_btn"><?php echo htmlspecialchars($label)?></label>
<?php
	}
?>
</td>
</tr>
<tr>
<td>Judge options:</td>
<td><?php
	foreach (array(
		"judges_can_change_password|Change password",
		) as $option_info)
	{
		list($name,$label) = explode("|", $option_info);
		$selected = $row[$name] == 'Y' ? ' checked="checked"' : '';
		?><input type="checkbox" name="<?php echo htmlspecialchars($name)?>" id="<?php echo htmlspecialchars($name)?>_btn"<?php echo $selected?>>
<label for="<?php echo htmlspecialchars($name)?>_btn"><?php echo htmlspecialchars($label)?></label>
<?php
	}
?>
</td>
</tr>
<tr>
<td>Collaboration:</td>
<td><?php select_option_widget('collaboration',
		array("N|None", "Y|Ad hoc"), $row['collaboration'])?></td>
</tr>
<tr>
<td>Score system:</td>
<td><?php select_option_widget('score_system',
		array("A|ACM","T|TopCoder"), $row['score_system'])?></td>
</tr>
<tr>
<td>Scoreboard:</td>
<td><?php select_option_widget('scoreboard',
		array("Y|Enabled","N|Disabled"), $row['scoreboard'])?></td>
</tr>
<tr>
<td>Scoreboard options:</td>
<td><?php
	foreach (array(
		"scoreboard_images|Show balloon/bug icons",
		"scoreboard_popups|Show notifications on live scoreboard",
		) as $option_info)
	{
		list($name,$label) = explode("|", $option_info);
		$selected = $row[$name] == 'Y' ? ' checked="checked"' : '';
		?><input type="checkbox" name="<?php echo htmlspecialchars($name)?>" id="<?php echo htmlspecialchars($name)?>_btn"<?php echo $selected?>>
<label for="<?php echo htmlspecialchars($name)?>_btn"><?php echo htmlspecialchars($label)?></label>
<?php
	}
?>
</td>
</tr>
<tr>
<td>Scoreboard sorting:</td>
<td>
<input type="radio" name="scoreboard_order" value="n" id="scoreboard_order_n"
<?php if ($row['scoreboard_order']=='n') { echo ' checked="checked"'; } ?>
><label for="scoreboard_order_n">Team name</label>
<input type="radio" name="scoreboard_order" value="o" id="scoreboard_order_o"
<?php if ($row['scoreboard_order']=='o') { echo ' checked="checked"'; } ?>
><label for="scoreboard_order_o">Team ordinal</label>
<input type="radio" name="scoreboard_order" value="s" id="scoreboard_order_s"
<?php if ($row['scoreboard_order']=='s') { echo ' checked="checked"'; } ?>
><label for="scoreboard_order_s">Score</label>
</td>
</tr>
<tr>
<td>Scoreboard audio:</td>
<td>
<?php
	foreach (array(
		"scoreboard_fanfare|Play fanfare on accepted solution",
		) as $option_info)
	{
		list($name,$label) = explode("|", $option_info);
		$selected = $row[$name] == 'Y' ? ' checked="checked"' : '';
		?><input type="checkbox" name="<?php echo htmlspecialchars($name)?>" id="<?php echo htmlspecialchars($name)?>_btn"<?php echo $selected?>>
<label for="<?php echo htmlspecialchars($name)?>_btn"><?php echo htmlspecialchars($label)?></label>
<?php
	}
	?>
</td>
</tr>
<tr>
<td>Started:</td>
<td><input type="text" name="started" value="<?php echo htmlspecialchars($row['started'])?>">
<small>Used to calculate penalty points for team submissions.</small></td>
</tr>
<tr>
<td valign="top">Contest phases:</td>
<td valign="top"><table>
<tr><th>Phase</th><th>Display As</th><th>Ends (DATE TIME)</th>
<td rowspan="5" valign="middle" style="max-width: 200pt">
<small>Contest phases allow problems to be made available
at different times during a single contest,
and also facilitate the display of a contest clock.</small>
</td>
</tr>
<?php
	$order = explode(',',$contest_info['phases']);
	for ($i = 1; $i <= 4; $i++) {
	?>
  <tr>
    <td><?php echo $i;
		if ($contest_info['current_phase'] == $i) {
			echo "*";
		} ?></td>
    <td><input type="text" name="<?php echo "phase{$i}_name"?>" value="<?php echo htmlspecialchars($row["phase{$i}_name"])?>"></td>
	<td><input type="text" name="<?php echo "phase{$i}_ends"?>" value="<?php echo htmlspecialchars($row["phase{$i}_ends"])?>"></td>
  </tr>
  <?php
    }
	?>
	</table></td>
</tr>
<tr>
<td valign="top"><div>Handout HTML:</div>
</td>
<td><textarea name="handout_html" rows="4" cols="70"><?php echo htmlspecialchars($row['handout_html'])?></textarea><div><small>This gets added to the printed information sheets given to teams, if using team logins.</small></div></td>
</tr>
</table>
<p>
<?php if (!isset($_REQUEST['contest'])) { ?>
<button type="submit" name="action:create_contest">Create</button>
<?php } else { ?>
<button type="submit">Update</button>
<?php if (is_sysadmin()) { ?>
<button type="submit" name="action:delete_contest">Delete</button>
<?php } //endif sysadmin ?>
<?php } //endif contest specified in URL ?>
<button type="submit" name="action:cancel">Cancel</button>
</p>

</form>
<?php
end_page();
?>

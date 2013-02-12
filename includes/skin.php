<?php

function end_page()
{
	?></div>
</body>
</html>
<?php
}

function begin_page($page_title, $options)
{
	global $static_files_url;
	global $contest_info;

	if (!$options) { $options = array(); }

	session_start();
?><!DOCTYPE HTML>
<html>
<head>
<title>Programming Contest: <?php echo htmlspecialchars($page_title)?></title>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" type="text/css" href="<?php echo $static_files_url?>/scoreboard.css">
<link rel="stylesheet" type="text/css" href="<?php echo $static_files_url?>/CodeMirror-2.33/lib/codemirror.css">
<script type="text/javascript" src="http://code.jquery.com/jquery-latest.min.js"></script>
<script type="text/javascript" src="<?php echo $static_files_url?>/scoreboard_functions.js"></script>
<script type="text/javascript" src="<?php echo $static_files_url?>/popup_message.js"></script>
<?php
	if ($options['javascript_files']) {
		foreach ($options['javascript_files'] as $f) {
?><script type="text/javascript" src="<?php echo "$static_files_url/$f"?>"></script>
	<?php
		}
	} ?>
</head>
<body class="adminpage">
<div id="top_banner" style="border: 1px solid green">
<h1>Programming Contest</h1>

<div id="sessionctl">
<?php
	if ($_SESSION['username']) { ?>
<div><?php echo htmlspecialchars(strtoupper($_SESSION['username']))?></div>
<div><a href="login.php?logout=1">Logout</a></div>
<?php } else { ?>
<div><a href="login.php">Login</a></div>
<?php } /*endif */ ?>
</div>
<?php if ($contest_info && $contest_info['current_phase_name']) {
	countdown_clock($contest_info);
} ?>


<div style="clear:both"></div>
</div>
<div id="contentarea">
<div id="announcementPopup">
<div style="text-align:center" id="announcementTitleBar">Message</div>
<div id="announcementContent">&nbsp;</div>
<div id="announcementButtons">
<button type="button" id="announcementOpenBtn" onclick="openAnnouncement()">Open</button>
<button type="button" onclick="dismissAnnouncement()">Dismiss</button>
</div>
</div>
<?php if (!$options['notitle']) { ?>
<h2><?php echo htmlspecialchars($page_title)?></h2>
<?php } ?>

<?php
}

function problem_actions_tabnav($show_mode)
{
	global $problem_info;
	global $team_info;

	$problem_number = $_REQUEST['problem'];
	$purl = "open_problem.php?problem=".urlencode($problem_number);

	$show_write_tab = ($team_info['is_contestant'] == 'Y');
	$show_test_tab = 1;
	$show_submit_tab = ($team_info['is_contestant'] == 'Y');
	$show_solutions_tab = ($problem_info && ($problem_info['read_opponent'] == 'Y' || $problem_info['read_solution'] == 'Y' || $team_info['is_judge'] == 'Y'));

?>
<div id="problem_action_buttons_bar">
<ul class="tabnav">
<li<?php if ($show_mode=='problem') { echo ' class="selected"'; }?>><a href="<?php echo htmlspecialchars("$purl&show=problem")?>">Problem</a></li>
<li<?php if ($show_mode=='clarifications') { echo ' class="selected"'; }?>><a href="<?php echo htmlspecialchars($purl.'&show=clarifications')?>">Clarifications<?php
		if ($problem_info['clarification_count']) {
			echo htmlspecialchars(" ($problem_info[clarification_count])");
		}?></a></li>
<?php if ($show_write_tab) { ?>
<li<?php if ($show_mode=='write') { echo ' class="selected"'; }?>><a href="<?php echo htmlspecialchars($purl.'&show=write')?>">Write Code</a></li>
<?php } ?>
<?php if ($show_test_tab) { ?>
<li<?php if ($show_mode=='test') { echo ' class="selected"'; }?>><a href="<?php echo htmlspecialchars("submit_test.php?problem=".urlencode($problem_number))?>">Test</a></li>
<?php } ?>
<?php if ($show_submit_tab) { ?>
<li<?php if ($show_mode=='submit') { echo ' class="selected"'; }?>><a href="<?php echo htmlspecialchars($purl.'&show=submit')?>">Submit</a></li>
<?php } ?>
<?php if ($show_solutions_tab) { ?>
<li<?php if ($show_mode=='solutions') { echo ' class="selected"'; }?>><a href="<?php echo htmlspecialchars($purl.'&show=solutions')?>">Solutions</a></li>
<?php } ?>
</ul>
<button type="button" onclick="doClose()">Close</button>
<script language="javascript"><!--
function doClose()
{
	window.location.href = 'team_menu.php';
}
//--></script>
</div>
<?php
}

function select_file_widget($name, $record)
{
	$file_id = $record[$name . "_file"];
	$given_name = $record[$name . "_name"];

	if ($file_id) {
		$file_url = "file.php/$file_id/$given_name";
		?>
<a href="<?php echo htmlspecialchars($file_url)?>"><?php echo htmlspecialchars($given_name)?></a>
- <input type="checkbox" id="<?php echo htmlspecialchars($name)?>_replace_btn" name="<?php echo htmlspecialchars($name)?>_replace"><label for="<?php echo htmlspecialchars($name)?>_replace_btn">Replace with:</label>
<?php
	}
		?>
<input id="<?php echo htmlspecialchars($name)?>_upload_field" name="<?php echo htmlspecialchars($name)?>_upload" type="file">
<?php
}

// Example:
//   select_option_widget('system',
//                        array("A|ACM","T|TopCoder"),
//                        $row['system']);
//
function select_option_widget($name, $choices, $value)
{
?><select name="<?php echo htmlspecialchars($name)?>"><?php
    foreach ($choices as $c) {
		list($c1,$c2) = explode("|",$c);
		?><option value="<?php echo htmlspecialchars($c1)?>"<?php
		echo($c1==$value ? ' selected="selected"' : '')
		?>><?php echo htmlspecialchars($c2)?></option>
<?php
    }
	?></select><?php
}

function contest_clock($contest_info)
{
	?>
	<div id="contest_clock">
	<div>Contest is not yet active!</div>
	<div>Starts in <span id="actual_clock">3:25</span></div>
	</div>
	<?php
}

function countdown_clock($contest_info)
{
	if (!$contest_info['current_phase_name'])
		return;

	?>
<div class="contest_clock">
	<div class="contest_clock_phase"><?php echo htmlspecialchars($contest_info['current_phase_name'])?></div>
	<div class="time_left" time-left="<?php echo htmlspecialchars($contest_info['current_phase_timeleft'])?>"></div>
</div>
<?php
}

function format_score($score, $score_alt)
{
	if ($score_alt > 0)
		return htmlspecialchars("$score (+$score_alt)");
	else if ($score_alt != 0)
		return htmlspecialchars("$score ($score_alt)");
	else
		return htmlspecialchars($score);
}

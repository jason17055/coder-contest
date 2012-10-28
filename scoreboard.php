<?php

// connect to database
require_once('config.php');
require_once('includes/auth.php');
require_once('includes/functions.php');
require_once('includes/skin.php');

// figure out which contest is being displayed
$contest_id = $_REQUEST['contest'] ?: 1;
$contest_info = get_basic_contest_info($contest_id, ",scoreboard,scoreboard_images,score_system,subtitle,scoreboard_popups,
	logo,scoreboard_order");
$contest = $contest_info
	or die("Error: contest $contest_id not found");

if (!is_director($contest_id) && $contest['scoreboard'] != 'Y')
{
?><!DOCTYPE HTML>
<html>
<head>
<title>Scoreboard Disabled</title>
<meta http-equiv="refresh" content="180">
<script type="text/javascript"><!--
sessionStorage.removeItem("lastmessage");
//--></script>
</head>
<body>
<h1>Scoreboard Disabled</h1>
</body>
</html>
<?php
	exit();
}

?><!DOCTYPE HTML>
<html>
 <head>
    <title>
	<?php echo htmlspecialchars($contest['title']) ?>
	-
	<?php echo htmlspecialchars($contest['subtitle']) ?>
    </title>
<script type="text/javascript" src="http://code.jquery.com/jquery-latest.min.js"></script>
<script type="text/javascript"><!--
function doReload()
{
	window.location.reload();
}
<?php if (isset($_REQUEST['last_message_id'])) { ?>
sessionStorage.setItem("lastmessage", <?php echo $_REQUEST['last_message_id']?>);
<?php } ?>
var last_message_id = sessionStorage.getItem("lastmessage");
if (last_message_id == null) {
	last_message_id = <?php
$sql2 = "SELECT COALESCE(MAX(id),0) AS last_id FROM announcement";
$result2 = mysql_query($sql2);
$row2 = mysql_fetch_assoc($result2);
echo $row2['last_id']; ?>;
}
var contest = '<?php echo $contest_id?>';

function hideAnnouncement()
{
	$("#announcementPopup").fadeOut(1000, doReload);
}
function displayAnnouncement(data)
{
	var myDelay = data.duration != 0 ? data.duration : 15;
	var delayThenHide = function() {
		setTimeout("hideAnnouncement()", myDelay*1000);
	};
	$("#announcementContent").html(data.message);
	sessionStorage.setItem("lastmessage", data.message_id);

	if (data.messagetype == 'S')
	{
<?php if (!$_REQUEST['nosound']) { ?>
		if (data.fanfare)
			document.getElementById('announcementAudio').play();
<?php } ?>
		document.getElementById('announcementImage').style.visibility = 'visible';
	}
	else
	{
		document.getElementById('announcementImage').style.visibility = 'hidden';
	}

	$("#announcementPopup").fadeIn(2500, delayThenHide);
}

var origTime = new Date().getTime();
function checkForAnnouncement()
{
	var startTime = new Date().getTime();
	if (startTime - origTime > 180000) { return doReload(); }

	var callback = function(data, textStatus, xhr)
		{
			if (data.message != null) {
				displayAnnouncement(data);
			}
			else {
				var nextTime = startTime + 10000;
				var curTime = new Date().getTime();
				var delay = nextTime - curTime;
				if (delay < 1) { delay = 1; }
				setTimeout("checkForAnnouncement()", delay);
			}
		};
	
			//+ "&timeout=30"
	var url = "checkmessage-js.php?type=S"
			+ "&after=" + escape(last_message_id)
			+ '&contest=' + escape(contest);
	jQuery.getJSON(url, null, callback);
}
<?php if ($contest['scoreboard_popups'] == 'Y') { ?>
setTimeout("checkForAnnouncement()", 1000);
<?php } else { ?>
setTimeout("doReload()", 120000); //every two minutes
<?php } ?>
//--></script>
<style type="text/css"><!--
body {
	color: black;
	background-color: white;
}
#announcementTitleBar {
	width: 100%;
	background-color: white;
	color: black;
	font-family: sans-serif;
	font-weight: bold;
}
#announcementPopup {
	position: fixed;
	height: 300px;
	width: 50%;
	background-color: #883333;
	color: white;
	border: 10px solid black;
	margin-top: -50px;
	z-index: 1;
	left: 25%;
	display: none;
	font-size: 16pt;
}

.team_name {
	white-space:nowrap;
	text-align:left;
}
.team_description {
	font-size: small;
	text-align: left;
}
.correct {
	background-color: #ccffcc;
}
.attempted {
}
td.column1 {
	min-width: 2in;
	height: 28pt;
	}
td.pcolumn {
	min-width: 0.7in;
	}
//--></style>
  </head>

<body>
  
<center>
<table border="0">
  
  <tr>
	<?php if ($contest['logo']) { ?>
    <td><img src="<?php echo htmlspecialchars($contest['logo'])?>" alt="[Logo]"></td>
	<?php } ?>

    <td><center><h1><?php echo htmlspecialchars($contest['title'])?></h1>
        <h2><?php echo htmlspecialchars($contest['subtitle'])?></h2>
        <div>(Current Scores)</div></center>
    </td>

	<?php if ($contest['logo']) { ?>
    <td><img src="<?php echo htmlspecialchars($contest['logo'])?>" alt="[Logo]"></td>
	<?php } ?>
  </tr>
 </table>

</center>

<div id="announcementPopup">
<div id="announcementTitleBar" align="center">Message</div>
<div>&nbsp;</div>
<img id="announcementImage" src="scoreboard_images/balloons.png" align="left">
<div id="announcementContent">&nbsp;</div>
<div id="announcementButtons"></div>
<audio id="announcementAudio" src="Medieval_Fanfare.ogg"></audio>
</div>

  <center>
<table border="1"><tr><th>Contestant</th>
<?php
$sql = "SELECT problem_number,problem_name FROM problem
	WHERE contest=" . db_quote($contest_id) . "
	AND ".check_phase_option_bool('pp_scoreboard')."
	ORDER BY problem_number ASC";
$result = mysql_query($sql);

$problems_list = array();
while ($problem = mysql_fetch_assoc($result)) {
	$problems_list[] = $problem['problem_number'];
	echo '<th>' . htmlspecialchars($problem['problem_name']) . '</th>';
}
?>
<th>TOTAL</th></tr>
<?php

function correct($row, $balloon_image)
{
	global $contest;
	global $contest_info;

	if (!$balloon_image)
	{
		$balloon_image = "balloon_red.png";
	}

	$label = $contest_info['score_system'] == 'T' ?
			$row['score'] : $row['thetime'];
	if ($contest_info['scoreboard_images'] == 'Y')
	{
		return '<img src="scoreboard_images/'.htmlspecialchars($balloon_image).'" width="14" height="24" alt="">' . htmlspecialchars($label);
	}
	else {
		return htmlspecialchars($label);
	}
}
function wrong($count)
{
	global $contest;
	if ($count == 0) { return ""; }
	if ($contest['scoreboard_images'] == 'Y') {
		$image_html = '<img src="scoreboard_images/bug.png" height="18" alt="X">';
		if ($count == 1) { return $image_html; }
		if ($count == 2) { return $image_html . $image_html; }
		else { return $image_html . "x$count"; }
	} else {
		return "+" . ($count * 20);
	}
}
function display_result($row)
{
	$result_str = "&nbsp;";
	$css_class = "";

	global $contest_info;

	if ($row['score'] || $row['score_alt'] || $row['incorrect_submissions'])
	{
		if ($row['score'] > 0)
		{
			$css_class = "correct";

			if ($contest_info['scoreboard_images'] == 'Y') {
				$result_str = wrong($row['incorrect_submissions']) . correct($row, $row['scoreboard_solved_image']);
			}
			else {
				$result_str = correct($row, $row['scoreboard_solved_image']) . wrong($row['incorrect_submissions']);
			}
		} else if ($row['incorrect_submissions'] > 0) {
			$result_str = wrong($row['incorrect_submissions']);
			$css_class = "attempted";
		}
	}
	else if ($row['source_file'])
	{
		$result_str = '<img src="scoreboard_images/pencil.png" alt="Writing" width="24" height="24">';
	}
	else if ($row['opened'])
	{
		$result_str = '<img src="scoreboard_images/eye4.png" alt="Opened" width="24" height="24">';
	}

	for ($i = 0; $i < $row['good_challenges']; $i++)
	{
		$result_str .= '<img src="scoreboard_images/green_flag.png" alt="">';
	}

	echo "<td valign='center' class='pcolumn $css_class'>";
	echo $result_str;
	echo '</td>';
}

$orderby = $contest['scoreboard_order'] == 'n' ? "team_name ASC, ordinal ASC" :
	($contest['scoreboard_order'] == 'o' ? "ordinal ASC, team_name ASC" :
	"score DESC, score_alt DESC, ordinal ASC, team_name ASC");
$query = "SELECT * FROM team
	WHERE contest=" . db_quote($contest_id) . "
	ORDER BY $orderby";
$result = mysql_query($query);

$count = 0;
while ($team = mysql_fetch_assoc($result)) {
	$count++;
	?><tr><td class="column1"><div class="team_name"><?php
		//echo $count . '. ';
		$name = $team['team_name'];
		//$name = preg_replace('/^\\d+\. /','', $name);
		echo htmlspecialchars($name) . '</div>';
	if ($team['description'])
	{
		?><div class="team_description"><?php echo htmlspecialchars($team['description'])?></div>
<?php
	}
	echo '</td>';
	
	foreach ($problems_list as $problem_number) {
	
		$query3 = "SELECT thetime,incorrect_submissions,scoreboard_solved_image,
				score,score_alt,opened,source_file,
				(SELECT COUNT(*) FROM challenge ch
					JOIN submission s ON s.id=ch.submission
					WHERE ch.creator=team_number
					AND s.problem=p.problem_number
					AND ch.status='Challenge successful') AS good_challenges
			FROM results r
			JOIN problem p
				ON p.contest=".db_quote($contest_id)."
				AND p.problem_number=r.problem_number
			WHERE team_number = " . db_quote($team['team_number']) . "
			AND r.problem_number = " . db_quote($problem_number);
		$result3 = mysql_query($query3);
		$row = mysql_fetch_assoc($result3);
		if (!$row)
		{
			$row = array(
				score => 0,
				score_alt => 0
				);
		}

		display_result($row);
	}
	
	echo '<td align="center">' . format_score($team['score'],$team['score_alt']) . '</td></tr>';
}
?>
</table>
</center>

</body>
</html>

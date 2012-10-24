<?php
$sql = "SELECT title,subtitle,logo,handout_html
	FROM contest
	WHERE contest_id=".db_quote($_REQUEST['contest']);
$result = mysql_query($sql);
$contest_info = mysql_fetch_assoc($result)
	or die("invalid contest id");
?>
<html>
<head>
<title><?php echo htmlspecialchars($contest_info['title'])?></title>
<style type="text/css">
.credentials_break_even {
	page-break-after: always;
}
.credentials_break_odd {
	/*border: solid 1px green;*/
	height: 0.5em;
	width: 100%;
	margin-top: 0.5em;
	margin-bottom: 2.5em;
}
.contest_title {
	width: 100%;
	border: 1px solid black;
	font-family: sans-serif;
	text-align: center;
}
.contest_title h1 {
	margin: 0;
	font-size: 24pt;
}
.contest_title h2 {
	margin: 0;
	font-size: 14pt;
}
#print_instructions {
	background-color: #88ffff;
	color: black;
	border: 1px solid black;
	width: 100%;
	font-size: 14pt;
	text-align: center;
	margin-bottom: 12pt;
}
@media print {
	#print_instructions { display: none }
}
.username_line { margin-top: 8pt; }
.team_name_and_description {
	font-size: 14pt;
	font-family: sans-serif;
}
tt {
	font-size: 12pt;
}
</style>
</head>
<body>
<div id="print_instructions">
Print this page!
Then click <a href="<?php echo htmlspecialchars($next_url)?>">here</a>.
</div>
<?php
$count = 0;
foreach ($passwords as $row)
{
	if ($count++) {
	?>
<div class="credentials_break <?php
	echo($count % 2 == 0 ? "credentials_break_odd" : "credentials_break_even")?>"></div>
<?php } ?>
<div class="credentials_block">
<div class="contest_title">
<h1><?php echo htmlspecialchars($contest_info['title'])?></h1>
<h2><?php echo htmlspecialchars($contest_info['subtitle'])?></h2>
</div>
<!--
<p>Team:
<span class="team_name_and_description">
<span><?php echo htmlspecialchars($row['team_name'])?></span>
<?php if ($row['description']) { ?>
<span>(<?php echo htmlspecialchars($row['description'])?>)</span>
<?php } ?>
</span>
</p>
-->
<p>
Welcome to the programming contest!
To submit solutions to the problems, you will need to log into the
programming contest system.  To login, follow these instructions:
</p>
<ol>
<li>Open a web browser to <tt><?php echo htmlspecialchars($app_url)?>/</tt></li>
<li>Enter the following information at the login screen:
<div class="username_line">Username: <tt><?php echo htmlspecialchars($row['username'])?></tt></div>
<div class="password_line">Password: <tt><?php echo htmlspecialchars($row['password'])?></tt></div>
</li>
</ol>
<p>
Please note your password is Case-Sensitive.
</p>
<p>
Once logged in, create a solution for the sample problem and submit it.
You can also try requesting a clarification.
Become familiar with the system before the start of the contest, so
you don't waste time learning the system during the contest itself.
</p>

<?php
echo $contest_info['handout_html'];

}
?>
</body>
</html>

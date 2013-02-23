<?php

require_once('config.php');
require_once('includes/auth.php');

require_auth();

if (isset($_REQUEST['test_number']))
{
$sql = "SELECT job,result_status,expected_file,team_name,t.contest AS contest,
	st.problem_number AS problem_number,
	p.problem_name AS problem_name
	FROM test_result tr
	JOIN system_test st
		ON st.input_file=tr.test_file
	JOIN submission s
		ON s.problem=st.problem_number
		AND s.id=tr.submission
	JOIN team t
		ON t.team_number=s.team
		AND t.contest=st.contest
	JOIN problem p
		ON p.problem_number=st.problem_number
		AND p.contest=st.contest
	WHERE s.id=".db_quote($_REQUEST['submission'])."
	AND st.test_number=".db_quote($_REQUEST['test_number']);
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
$test_result_info = mysql_fetch_assoc($result)
	or die("Error: not authorized");
}
else if (isset($_REQUEST['custom_test']))
{
	$sql = "SELECT ".db_quote($_REQUEST['rhs_test'])." AS job,
		team_name,
		t.contest AS contest,
		s.problem AS problem_number,
		p.problem_name AS problem_name,
		(SELECT output_file FROM test_job
			WHERE id=".db_quote($_REQUEST['lhs_test']).") AS expected_file
		FROM submission s
		JOIN team t
			ON t.team_number=s.team
		JOIN problem p
			ON p.problem_number=s.problem
			AND p.contest=t.contest
		WHERE s.id=".db_quote($_REQUEST['submission']);
	$result = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$test_result_info = mysql_fetch_assoc($result)
		or die("Error: not authorized");
	$test_result_info['job'] = $_REQUEST['rhs_test'];
}
else
{
	die("Error: invalid request");
}

is_judge_of($test_result_info['contest'])
	or die("Error: not authorized");

$sql = "SELECT *
	FROM test_job
	WHERE id=".db_quote($test_result_info['job']);
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
$job_info = mysql_fetch_assoc($result)
	or die("Error: job for this test result not found");

$problem_url = "problem.php?contest=".urlencode($test_result_info['contest'])
		."&id=".urlencode($test_result_info['problem_number']);

$submission_url = "submission.php?id=".urlencode($_REQUEST['submission']);
$system_test_url = "system_test.php"
	. "?contest=".urlencode($test_result_info['contest'])
	. "&problem=".urlencode($test_result_info['problem_number'])
	. "&test_number=".urlencode($_REQUEST['test_number']);

$source_file_url = "file.php/$job_info[source_file]/$job_info[source_name]?show_line_numbers=1";
$input_file_url = "file.php/$job_info[input_file]/input.txt";
$job_output_url = "job_result.php?id=".urlencode($job_info['id']);

if ($test_result_info['expected_file']) {
	$expected_file_url = "diff.php?f2=$test_result_info[expected_file]";
	$output_file_url = "diff.php?f1=$test_result_info[expected_file]&f2=$job_info[output_file]";
}
else {
	$expected_file_url = "";
	$output_file_url = "file.php/$job_info[output_file]/output.txt";
}

?><!DOCTYPE HTML>
<html><head>
<link type="text/css" href="scoreboard.css" rel="stylesheet">
</head>
<body>
<p class="test_result_properties">
Problem: <a href="<?php echo htmlspecialchars($problem_url)?>"><?php echo htmlspecialchars($test_result_info['problem_name'])?></a>

Submission:
<a href="<?php echo htmlspecialchars($submission_url)?>">
<?php echo htmlspecialchars($_REQUEST['submission'])?></a>

<?php if (isset($_REQUEST['test_number'])) { ?>
System Test:
<a href="<?php echo htmlspecialchars($system_test_url)?>">
<?php echo htmlspecialchars($_REQUEST['test_number'])?></a>
</p>
<?php } //endif test_number defined ?>

<p class="test_result_actions">
<button type="button" onclick="window.open('<?php
	echo htmlspecialchars($source_file_url)?>')">Source Code</button>

<?php
if (isset($_REQUEST['test_number']))
{
	$sql="SELECT MIN(test_number) t
	FROM system_test st
	WHERE contest=".db_quote($test_result_info['contest'])."
	AND problem_number=".db_quote($test_result_info['problem_number'])."
	AND test_number<".db_quote($_REQUEST['test_number']);
	$result = mysql_query($sql);
	$r = mysql_fetch_assoc($result);
	if ($r['t'])
	{
		$url="test_result.php?submission=".urlencode($_REQUEST['submission'])
		."&test_number=".urlencode($r['t']);
		?>
<button type="button" onclick="window.location='<?php echo htmlspecialchars($url)?>'">Previous Test</button>
<?php
	}
	$sql="SELECT MIN(test_number) t
	FROM system_test st
	WHERE contest=".db_quote($test_result_info['contest'])."
	AND problem_number=".db_quote($test_result_info['problem_number'])."
	AND test_number>".db_quote($_REQUEST['test_number']);
	$result = mysql_query($sql);
	$r = mysql_fetch_assoc($result);
	if ($r['t'])
	{
		$url="test_result.php?submission=".urlencode($_REQUEST['submission'])
		."&test_number=".urlencode($r['t']);
		?>
<button type="button" onclick="window.location='<?php echo htmlspecialchars($url)?>'">Next Test</button>
<?php
	}
} //end if $_REQUEST['test_result']
else if (isset($_REQUEST['custom_test']))
{
	$add_systest_url = "system_test.php?contest=".urlencode($test_result_info['contest']).
			"&problem=".urlencode($test_result_info['problem_number']).
			"&input_file=".urlencode($_REQUEST['custom_test']);
	if (!$test_result_info['expected_file'])
	{
		$add_systest_url .= "&expected_file=".urlencode($job_info['output_file']);
	}
?>
<button type="button" onclick='location.href="<?php echo htmlspecialchars($add_systest_url)?>";'>Add to Test Suite (experimental)</button>

<?php
} //end if $_REQUEST['custom_test']

?>

<!--
Judge now:
<select><option>--select--</option>
<option>Wrong Answer</option>
<option>Output Format Error</option>
<option>Excessive Output</option>
</select>
-->
<button type="button" onclick="location.href='<?php echo htmlspecialchars($submission_url)?>'">Done</button>

</p>
<div style="clear:both"></div>


<table width="100%">
<tr>
<td width="50%" valign="top">
<b>Input File</b><br>
<iframe src="<?php echo htmlspecialchars($input_file_url)?>"
	width="100%"></iframe>
</td>
<td width="50%" valign="top">
<b>Compiler/Error Output</b><br>
<iframe src="<?php echo htmlspecialchars($job_output_url)?>"
	width="100%"></iframe>
</td>
</tr>
<tr>
<td width="50%" valign="top">
<b>Expected Output</b><br>
<?php if ($test_result_info['expected_file']) { ?>
<iframe id='expected_output_frame' src="<?php echo htmlspecialchars($expected_file_url)?>"
	width="100%"></iframe>
<?php } else { ?>
n/a
<?php } ?>
</td>
<td width="50%" valign="top">
<div><b>Actual Output</b>
<span id="diffControls" style='display:none'>
<span id="diffCount">FOO</span> <a id="hilite_first_btn" href="#">First</a>
<a id="hilite_next_btn" href="#">Next</a>
</span>
</div>
<iframe id="actual_output_frame" src="<?php echo htmlspecialchars($output_file_url)?>"
	width="100%"></iframe>
</td>
</tr>

</table>
<script type="text/javascript"><!--
var diffs = null;
var diffIdx = 0;
function showDiff(idx)
{
	var rhs_frame = document.getElementById('actual_output_frame');
	var lhs_frame = document.getElementById('expected_output_frame');

	if (lhs_frame.contentWindow
		&& lhs_frame.contentWindow.hiliteLines
		&& rhs_frame.contentWindow
		&& rhs_frame.contentWindow.hiliteLines)
	{
	if (idx >= 0 && idx < diffs.length)
	{
		rhs_frame.contentWindow.hiliteLines(diffs[idx].rhs_start,diffs[idx].rhs_end);

		lhs_frame.contentWindow.hiliteLines(diffs[idx].lhs_start,diffs[idx].lhs_end);
	}
	else
	{
		rhs_frame.contentWindow.hiliteLines(0,0);
		lhs_frame.contentWindow.hiliteLines(0,0);
	}
	diffIdx = idx;
	}
	else
	{
	}
}

function reportDiffInformation(diffInfo)
{
	diffs = diffInfo;

	var countLabel = diffInfo.length == 1 ? "1 difference" : (diffInfo.length + " differences");
	document.getElementById('diffCount').innerHTML = countLabel;

	document.getElementById('diffControls').style.display = 'inline';

	showDiff(0);
}

document.getElementById('hilite_first_btn').onclick = function()
	{
		showDiff(0);
	};
document.getElementById('hilite_next_btn').onclick = function()
	{
		showDiff((diffIdx+1) % diffs.length);
	};

var pageHeight = window.innerHeight;
if (pageHeight)
{
	var ifr = document.getElementById('actual_output_frame');
	var obj = ifr;
	var iframeTop = 0;
	while (obj != null)
	{
		iframeTop += obj.offsetTop;
		obj = obj.offsetParent;
	}

	var remain = pageHeight - iframeTop - 12;
	document.getElementById('expected_output_frame').style.height = remain;
	document.getElementById('actual_output_frame').style.height = remain;
}
//--></script>

<pre>
<?php

function try_diff($f_a, $f_b, $options, $show_result)
{
	$type = $options == "" ? "Binary comparison:" : "Fuzzy comparison ($options):";

	$redir = $show_result ? "2>&1" : ">/dev/null 2>&1";
	echo "$type\n";
	system("diff $options $f_a $f_b $redir", $retval);

	echo($retval == 0 ? "No differences found\n" : "Some differences found\n");
	echo("\n");
}

$f_a = "uploaded_files/$test_result_info[expected_file].txt";
$f_b = "uploaded_files/$job_info[output_file].txt";
try_diff($f_a, $f_b, "", false);
try_diff($f_a, $f_b, "-B -i -E -b -a", true);

?>
</body>
</html>

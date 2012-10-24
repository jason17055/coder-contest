<?php

require_once('config.php');
require_once('includes/functions.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/js_quote.php');
require_auth();

if (isset($_REQUEST['id']))
{
	$sql = "SELECT * FROM submission WHERE id=".db_quote($_REQUEST['id']);
	$result = mysql_query($sql);
	$submission_info = mysql_fetch_assoc($result)
		or die("Error: not authorized"); //invalid submission id
	$_REQUEST['team'] = $submission_info['team'];
	$_REQUEST['problem'] = $submission_info['problem'];
}
else
{
	$submission_info = array();
}

if (!isset($_REQUEST['team']) || !isset($_REQUEST['problem']))
{
	die("invalid access");
}

$sql = "SELECT * FROM team WHERE team_number=".db_quote($_REQUEST['team']);
$result = mysql_query($sql);
$team_info = mysql_fetch_assoc($result)
	or die("Error: not authorized"); //invalid team number

$sql = "SELECT * FROM problem WHERE problem_number=".db_quote($_REQUEST['problem'])." AND contest=".db_quote($team_info['contest']);
$result = mysql_query($sql);
$problem_info = mysql_fetch_assoc($result)
	or die("Error: not authorized"); //invalid problem number

//check authorization before doing anything
$contest_id = $team_info['contest'];
is_judge_of($contest_id)
	or die("Error: not authorized");

$contest_info = get_basic_contest_info($contest_id);

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	if (isset($_REQUEST['action:cancel']))
	{
		$url = $_REQUEST['next_url'] ?: "result.php?team=".urlencode($_REQUEST['team'])."&problem=".urlencode($_REQUEST['problem']);
		header("Location: $url");
		exit();
	}

	if (isset($_REQUEST['action:redo_tests']))
	{
		$sql = "DELETE FROM test_result
			WHERE submission=".db_quote($_REQUEST['id']);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		generate_tests($_REQUEST['id']);
		$url = "submission.php?id=".urlencode($_REQUEST['id']);
		header("Location: $url");
		exit();
	}
	else if (isset($_REQUEST['action:custom_test']))
	{
		handle_upload_file("input");

		// create a job to run the judge's solution against this input
		$test1 = null;
		if ($problem_info['solution_file'])
		{
		$sql = "INSERT INTO test_job
				(type,user_uid,source_file,source_name,input_file,created)
				VALUES ('U',
				".db_quote($_SESSION['uid']).",
				".db_quote($problem_info['solution_file']).",
				".db_quote($problem_info['solution_name']).",
				".db_quote($_REQUEST['input_file']).",
				NOW())";
		mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$test1 = mysql_insert_id();
		}

		// create a job to run the team's solution against this input
		$sql = "INSERT INTO test_job
				(type,user_uid,source_file,source_name,input_file,created)
				VALUES ('U',
				".db_quote($_SESSION['uid']).",
				".db_quote($submission_info['file']).",
				".db_quote($submission_info['given_name']).",
				".db_quote($_REQUEST['input_file']).",
				NOW())";
		mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$test2 = mysql_insert_id();

		notify_worker();

		$test_result_url = null;
		if ($test1)
		{
			$test_result_url = "test_result.php?submission=".urlencode($_REQUEST['id'])
			. "&custom_test=".urlencode($_REQUEST['input_file'])
			. "&lhs_test=".urlencode($test1)
			. "&rhs_test=".urlencode($test2);
			$user_test2_url = "user_test.php?id=" . urlencode($test2) .
				"&label=" . urlencode("Team's solution") .
				"&onany=" . urlencode($test_result_url) .
				"&next_url=" . urlencode($_SERVER['REQUEST_URI']);
			$user_test1_url = "user_test.php?id=" . urlencode($test1) .
				"&label=" . urlencode("Judge's solution") .
				"&onsuccess=" . urlencode($user_test2_url) .
				"&next_url=" . urlencode($_SERVER['REQUEST_URI']);
			header("Location: $user_test1_url");
		}
		else
		{
			$test_result_url = "test_result.php?submission=".urlencode($_REQUEST['id'])
			. "&custom_test=".urlencode($_REQUEST['input_file'])
			. "&rhs_test=".urlencode($test2);
			$user_test_url = "user_test.php?id=" . urlencode($test2) .
				"&label=" . urlencode("Team's solution") .
				"&onany=" . urlencode($test_result_url) .
				"&next_url=".urlencode($_SERVER['REQUEST_URI']);
			header("Location: $user_test_url");
		}
		exit();
	}

	handle_upload_file('source');

	if ($_REQUEST['status']=='**other**')
	{
		$_REQUEST['status'] = $_REQUEST['status_other'];
	}
	if ($_REQUEST['status']=='')
	{
		$_REQUEST['status'] = null;
	}

	if (isset($_POST['action:create']))
	{
		is_director($contest_id)
			or die("Must be director to create submissions for another user");

		$sql = "INSERT INTO submission (team,problem,submitted,minutes,file,given_name,status)
			VALUES (
			" . db_quote($_REQUEST['team']) . ",
			" . db_quote($_REQUEST['problem']) . ",
				NOW(),
			" . db_quote($_REQUEST['minutes']) . ",
			" . db_quote($_REQUEST['source_file']) . ",
			" . db_quote($_REQUEST['source_name']) . ",
			" . db_quote($_REQUEST['status']) . ")";
		mysql_query($sql)
			or die("SQL error: " . mysql_error());

		$submission_id = mysql_insert_id();
		generate_tests($submission_id);
		update_result($_REQUEST['team'],$_REQUEST['problem']);
		$url = $_REQUEST['next_url'] ?: "result.php?team=".urlencode($_REQUEST['team'])."&problem=".urlencode($_REQUEST['problem']);
		header("Location: $url");
		exit();
	}
	else if (isset($_POST['action:delete_submission']))
	{
		if (!is_director($contest_id))
		{
			die("Must be director to delete submissions");
		}

		$sql = "DELETE FROM test_result
			WHERE submission=".db_quote($_REQUEST['id']);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		$sql = "DELETE FROM submission
			WHERE id=".db_quote($_REQUEST['id']);
		mysql_query($sql)
			or die("SQL error: " . mysql_error());
		update_result($_REQUEST['team'],$_REQUEST['problem']);
		$url = $_REQUEST['next_url'] ?: "result.php?team=".urlencode($_REQUEST['team'])."&problem=".urlencode($_REQUEST['problem']);
		header("Location: $url");
		exit();
	}
	else
	{
		$updates = array();
		$updates[] = "status=".db_quote($_REQUEST['status']);
		if (is_director($contest_id))
		{
			if (isset($_REQUEST['source_file']))
			{
			$updates[] = "file=".db_quote($_REQUEST['source_file']);
			$updates[] = "given_name=".db_quote($_REQUEST['source_name']);
			}
			if (isset($_REQUEST['minutes']))
			{
			$updates[] = "minutes=".db_quote($_REQUEST['minutes']);
			}
			if (isset($_REQUEST['submitted']))
			{
			$updates[] = "submitted=".db_quote($_REQUEST['submitted']);
			}
		}
		$sql = "UPDATE submission SET ".join(',',$updates)."
			WHERE id=" . db_quote($_REQUEST['id']);
		mysql_query($sql)
			or die("SQL error: " . mysql_error());

		update_result($_REQUEST['team'],$_REQUEST['problem']);

		if ($_REQUEST['status'] != $submission_info['status'])
		{
			if ($_REQUEST['status'])
			{
				$message = "Your solution for $problem_info[problem_name] has been judged.";
				$url = "solution.php?id=".urlencode($_REQUEST['id']);
				send_message('N', $contest_id,
					$team_info['user'], $message, $url);
			}
		}

		if (isset($_REQUEST['source_file']))
		{
			generate_tests($_REQUEST['id']);
		}
		$def_url = is_director($contest_id) ?
			"result.php?team=".urlencode($_REQUEST['team'])."&problem=".urlencode($_REQUEST['problem']) :
			"listsubmissions.php?contest=".urlencode($contest_id);
		$url = $_REQUEST['next_url'] ?: $def_url;
		header("Location: $url");
		exit();
	}
}

$problem_url = 'problem.php?id='.urlencode($submission_info['problem']).'&contest='.urlencode($contest_id);
begin_page($submission_info['id'] ? "Submission $submission_info[id]" : 'Create Submission');
?>
<table border="0" cellspacing="0" width="100%">
<tr>
<td id="lhs_column" width="60%" valign="top">

<form method="post" enctype="multipart/form-data" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<table>
<tr>
<td valign="top">Contestant:</td>
<td valign="top"><?php
if ($submission_info['coauthors'])
{
	$team_numbers = array();
	$team_numbers[] = db_quote($submission_info['team']);
	foreach (explode(',',$submission_info['coauthors']) as $tn) {
		$team_numbers[] = db_quote($tn);
	}

	$sql = "SELECT team_name,user
		FROM team
		WHERE contest=".db_quote($contest_id)."
		AND team_number IN (".join(',', $team_numbers).")
			ORDER BY team_name,user";
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	while ($coauthor_row = mysql_fetch_assoc($query))
	{
		?><div><?php echo htmlspecialchars("$coauthor_row[team_name] ($coauthor_row[user])")?></div>
<?php
	}
} else {
	echo htmlspecialchars("$team_info[team_name] ($team_info[user])");
}
?></td>
</tr>
<tr>
<td>Problem:</td>
<td><a href="<?php echo htmlspecialchars($problem_url)?>"><?php echo htmlspecialchars($problem_info['problem_name'])?></a></td>
</tr>
<?php if (is_director($contest_id)) { ?>
<tr>
<td>Minutes:</td>
<td><input type="text" name="minutes" value="<?php echo htmlspecialchars($submission_info['minutes'])?>"></td>
</tr>
<?php if ($submission_info['submitted']) {?>
<tr>
<td><label>Submission Time:</label></td>
<td><input type="text" name="submitted" value="<?php echo htmlspecialchars($submission_info['submitted'])?>"></td>
</tr>
<?php } //endif ?>
<tr>
<td>Source code:</td>
<td><?php
	$submission_info['source_file'] = $submission_info['file'];
	$submission_info['source_name'] = $submission_info['given_name'];
	select_file_widget('source', $submission_info);
	?>
</td>
</tr>
<?php } else { // is not director ?>
<tr>
<td>Minutes:</td>
<td><?php echo htmlspecialchars($submission_info['minutes'])?></td>
</tr>
<tr>
<td>Source code:</td>
<td><a href="<?php
	$source_file_url = 'file.php/' . $submission_info['file'] . '/' . $submission_info['given_name'];
	echo htmlspecialchars($source_file_url)?>"><?php echo htmlspecialchars($submission_info['given_name'])?></a>
</td>
</tr>

<?php } //endif is not director ?>
<tr>
<td>Status:</td>
<td>
<select name="status">
<?php
	$found = false;
?>
<option value=""<?php if ($submission_info['status']=="") {
		$found=true;
		echo ' selected="selected"';
	 }?>>(not judged)</option>
<?php
	$status_choices = array(
		"Accepted", "Correct",
		"Wrong Answer", "Output Format Error",
		"Excessive Output", "Compilation Error",
		"Run-Time Error", "Time-Limit Exceeded",
		);
	foreach ($status_choices as $choice)
	{
		?><option value="<?php echo htmlspecialchars($choice)?>"<?php
			if ($choice == $submission_info['status']) {
				$found=true;
				echo ' selected="selected"';
			}?>><?php
			echo htmlspecialchars($choice)?></option>
<?php
	}
	?>
<option value="**other**"<?php
		echo($found?"":' selected="selected"')?>>(other)</option>
</select>
<input name="status_other" type="text" value="<?php
		echo($found?"":htmlspecialchars($submission_info['status']))?>">
</td>
</tr>
</table>
<div>
<?php if ($submission_info['id']) { ?>
<button type="submit">Save Changes</button>
<?php if (is_director($contest_id)) { ?>
<button type="submit" name="action:delete_submission" onclick='return confirm("Really delete this submission?")'>Delete</button>
<?php } //endif is director ?>
<?php } else { //creating a submission ?>
<button type="submit" name="action:create">Add Submission</button>
<?php } ?>
<button type="submit" name="action:cancel">Cancel</button>
</div>
</form>

<?php if ($submission_info['file']) {
	$source_file_url = "file.php/$submission_info[file]/$submission_info[given_name]";
?>

<h3 class="submission_h3">Source Code:</h3>
<iframe id="source_code_iframe" src="<?php echo htmlspecialchars($source_file_url)?>"></iframe>

</td>
<td id="rhs_column" width="40%" valign="top">
<h3 class="submission_h3">Test Results:</h3>
<table border="1" width="100%">
<!--
<tr><th>Test</th><th>Result</th></tr>
-->
<?php

$first_test_result_url = null;
$sql = "SELECT test_number,tr.result_status AS test_result_status,
		test_file,tj.output_file AS output_file,
		expected_file,tj.id AS job,
		tj.result_status AS result_status
	FROM test_result tr
	JOIN submission s
		ON s.id = tr.submission
	JOIN team t
		ON s.team = t.team_number
	JOIN system_test st
		ON st.contest = t.contest
		AND st.problem_number = s.problem
		AND st.input_file = tr.test_file
	JOIN test_job tj
		ON tj.id = tr.job
	WHERE tr.submission=".db_quote($submission_info['id'])."
	ORDER BY test_number";
$result = mysql_query($sql);
while ($row = mysql_fetch_assoc($result))
{
	$test_result_url = "test_result.php?submission=".urlencode($_REQUEST['id'])
		. "&test_number=".urlencode($row['test_number']);
	if (!$first_test_result_url)
		$first_test_result_url = $test_result_url;

	$input_file_url = "file.php/$row[test_file]/input.txt";
	$expected_file_url = "file.php/$row[expected_file]/expected.txt";
	$job_result_url = "job_result.php?id=".urlencode($row['job']);
	$diff_url = "diff.php?f1=$row[expected_file]&f2=$row[output_file]";

	if ($row['test_result_status'] == "No Error")
	{
		if ($row['output_file'] == $row['expected_file'])
			$row['test_result_status'] = "Correct";
		else
			$row['test_result_status'] = "Wrong";
	}
	$js = 'showTestResult('.js_quote($input_file_url).",".js_quote($diff_url).");return false";

	?>
<tr>
<td><a href="<?php echo htmlspecialchars($test_result_url)?>" onclick='<?php echo htmlspecialchars($js)?>'><?php echo htmlspecialchars("Test $row[test_number]")?></a></td>
<td><?php
	if ($row['result_status']) {
?><a href="<?php echo htmlspecialchars($test_result_url)?>"><?php echo htmlspecialchars($row['test_result_status'])?></a>
<?php } else {
?><img src="ajax-loader.gif" class="job-incomplete-indicator" id="ind_job_<?php echo htmlspecialchars($row['job'])?>">
Pending
<?php } ?>
</td>
</tr><?php
}

?>
</table>

<form method="post">
<p>
<?php if ($first_test_result_url) { ?>
<button type="button" onclick='location.href="<?php echo $first_test_result_url?>"'>View Test Results</button>
<button type="submit" name="action:redo_tests">Redo Tests</button>
<?php } ?>

<button type="button" onclick="doCustomTest()">Custom Test</button>
</p>
</form>

<div id="customTestDiv" style="display:none">
<form name="customTestForm" method="post" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<div><b>Input for Custom Test:</b></div>
<input type="hidden" name="source_name" value="<?php echo htmlspecialchars($submission_info['given_name'])?>">
<input type="hidden" name="source_file" value="<?php echo htmlspecialchars($submission_info['file'])?>">
<input type="hidden" name="next_url" value="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<textarea name="input_content" rows="12" cols="40"></textarea>
<div><button name="action:custom_test" type="submit">Submit</button></div>
</form>
</div>

<div id="testResultDiv" style="display:none">
<div><b>Input File</b></div>
<iframe id="testResult_Input"></iframe>
<div><b>Output File</b></div>
<iframe id="testResult_Output"></iframe>
</div>

</td>
</tr>
</table>

<script type="text/javascript"><!--

var lastInputFileUrl;
function showTestResult(inputFile, outputFile)
{
	lastInputFileUrl = inputFile;
	document.getElementById('testResult_Input').contentWindow.location.href = inputFile;
	document.getElementById('testResult_Output').contentWindow.location.href = outputFile;
	$('#customTestDiv').hide();
	$('#testResultDiv').show();
	onResize();
}

function onResize()
{
	var $i = $('#testResult_Input');
	var $o = $('#testResult_Output');
	var y = $o.offset().top - $i.height();

	var slack = window.innerHeight - y;
	var idealHeight = Math.floor(slack / 2) - 13;
	idealHeight = idealHeight < 100 ? 100 : idealHeight;
	document.getElementById('testResult_Input').height = idealHeight;
	document.getElementById('testResult_Output').height = idealHeight;

	var w = Math.floor(window.innerWidth * .4 - 10);
	document.getElementById('testResult_Input').width = w;
	document.getElementById('testResult_Output').width = w;

	var w = Math.floor(window.innerWidth * .6 - 60);
	document.getElementById('source_code_iframe').width = w;

	var h = window.innerHeight - $('#source_code_iframe').offset().top - 25;
	document.getElementById('source_code_iframe').height = h;
}
$(onResize);
window.onresize = onResize;

function doCustomTest()
{
	$('#testResultDiv').hide();
	if (!lastInputFileUrl)
	{
		$('#customTestDiv').show();
		return;
	}

	var onSuccess = function(data, tStatus)
	{
		document.customTestForm.input_content.value = data;
		$('#customTestDiv').show();
	};

	$.ajax({
		url: lastInputFileUrl,
		success: onSuccess,
		dataType: 'text'
		});
}
//--></script>

<?php } /*endif submission has source_file */ ?>
<?php
end_page();
?>

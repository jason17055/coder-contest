<?php

require_once('config.php');
require_once('includes/functions.php');
require_once('includes/skin.php');
require_once('includes/auth.php');

if (!isset($_REQUEST['contest']) || !isset($_REQUEST['problem']))
{
	die("invalid access");
}

$contest_id = $_REQUEST['contest'];

require_auth();
is_judge_of($contest_id)
	or die("Error: not authorized");

if (isset($_REQUEST['test_number']))
{
	$sql = "SELECT contest,problem_number,test_number,
			input_file,input_name,
			expected_file,'expected.txt' AS expected_name,
			example_input,autojudge
		FROM system_test
		WHERE contest=".db_quote($contest_id) . "
		AND problem_number=".db_quote($_REQUEST['problem']) . "
		AND test_number=".db_quote($_REQUEST['test_number']);
	$result = mysql_query($sql);
	$record = mysql_fetch_assoc($result)
		or die("Error: not authorized"); //invalid system test number
}
else
{
	$record = array(
		input_file => $_REQUEST['input_file'],
		input_name => 'input.txt',
		expected_file => $_REQUEST['expected_file'],
		expected_name => 'expected.txt',
		example_input => 'N',
		autojudge => 'N',
		);
}

$sql = "SELECT * FROM problem WHERE problem_number=".db_quote($_REQUEST['problem'])." AND contest=".db_quote($contest_id);
$result = mysql_query($sql);
$problem_info = mysql_fetch_assoc($result)
	or die("Error: problem $_REQUEST[problem] of contest $contest_id not found");

$contest_info = get_basic_contest_info($contest_id);

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	handle_upload_file('input');
	handle_upload_file('expected');

	$checkboxes = array('example_input', 'autojudge');
	$updates = array();
	foreach ($checkboxes as $c) {
		if ($_REQUEST[$c]) {
			$updates[] = "$c='Y'";
		} else {
			$updates[] = "$c='N'";
		}
	}

	if (isset($_POST['action:cancel']))
	{
		$url = "problem.php?contest=".urlencode($_REQUEST['contest'])."&id=".urlencode($_REQUEST['problem']);
		header("Location: $url");
		exit();
	}
	else if (isset($_POST['action:create']))
	{
		//determine next test number
		$sql = "SELECT COALESCE(MAX(test_number),0)+1 AS test_number
			FROM system_test
			WHERE contest=".db_quote($_REQUEST['contest'])."
			AND problem_number=".db_quote($_REQUEST['problem']);
		$result = mysql_query($sql);
		$row = mysql_fetch_assoc($result)
			or die("unexpected");

		//make the test
		$sql = "INSERT INTO system_test (contest,problem_number,test_number,input_file,input_name,expected_file)
			VALUES (
			" . db_quote($_REQUEST['contest']) . ",
			" . db_quote($_REQUEST['problem']) . ",
			" . db_quote($row['test_number']) . ",
			" . db_quote($_REQUEST['input_file']) . ",
			" . db_quote($_REQUEST['input_name'] ?: 'input.txt') . ",
			" . db_quote($_REQUEST['expected_file']) . ")";
		mysql_query($sql)
			or die("SQL error: " . mysql_error());

		if (count($updates) > 0)
		{
			$sql = "UPDATE system_test SET ".join(',',$updates)."
			WHERE contest=".db_quote($_REQUEST['contest'])."
			AND problem_number=".db_quote($_REQUEST['problem'])."
			AND test_number=".db_quote($row['test_number']);
			mysql_query($sql)
				or die("SQL error: ".mysql_error());
		}

		generate_tests_for_system_test(
			$_REQUEST['contest'], $_REQUEST['problem'],
			$row['test_number']);

		$url = "problem.php?contest=".urlencode($_REQUEST['contest'])."&id=".urlencode($_REQUEST['problem']);
		header("Location: $url");
		exit();
	}
	else if (isset($_REQUEST['action:delete']))
	{
		$sql_submissions = "
			SELECT id
			FROM submission s
			JOIN team t
				ON s.team=t.team_number
			WHERE s.problem=".db_quote($_REQUEST['problem'])."
			AND t.contest=".db_quote($_REQUEST['contest'])."
			";
		$sql = "DELETE FROM test_result
			WHERE submission IN ($sql_submissions)
			AND test_file=".db_quote($record['input_file']);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		$sql = "DELETE FROM system_test
			WHERE contest=".db_quote($_REQUEST['contest'])."
			AND problem_number=".db_quote($_REQUEST['problem'])."
			AND test_number=".db_quote($_REQUEST['test_number']);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		//back to the problem definition
		$url = "problem.php?contest=".urlencode($_REQUEST['contest'])."&id=".urlencode($_REQUEST['problem']);
		header("Location: $url");
		exit();
	}
	else if (isset($_REQUEST['action:redo_tests']))
	{
		$sql_submissions = "
			SELECT id
			FROM submission s
			JOIN team t
				ON s.team=t.team_number
			WHERE s.problem=".db_quote($_REQUEST['problem'])."
			AND t.contest=".db_quote($_REQUEST['contest'])."
			";
		$sql = "DELETE FROM test_result
			WHERE submission IN ($sql_submissions)
			AND test_file=".db_quote($record['input_file']);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		generate_tests_for_system_test(
			$_REQUEST['contest'], $_REQUEST['problem'],
			$_REQUEST['test_number']);
		$url = "system_test.php?contest=".urlencode($_REQUEST['contest'])
			. "&problem=".urlencode($_REQUEST['problem'])
			. "&test_number=".urlencode($_REQUEST['test_number']);
		header("Location: $url");
		exit();
	}
	else
	{
		if (isset($_REQUEST['expected_file']))
		{
			$updates[] = "expected_file=".db_quote($_REQUEST['expected_file']);
		}
		if (isset($_REQUEST['input_file']))
		{
			$updates[] = "input_file=".db_quote($_REQUEST['input_file']);
			$updates[] = "input_name=".db_quote($_REQUEST['input_name']);
		}
		if (count($updates))
		{
			$sql = "UPDATE system_test SET ".join(',',$updates)."
			WHERE contest=".db_quote($_REQUEST['contest'])."
			AND problem_number=".db_quote($_REQUEST['problem'])."
			AND test_number=".db_quote($_REQUEST['test_number']);
			mysql_query($sql)
				or die("SQL error: ".mysql_error());
		}

		generate_tests_for_system_test(
			$_REQUEST['contest'], $_REQUEST['problem'],
			$_REQUEST['test_number']);

		$url = "problem.php?contest=".urlencode($_REQUEST['contest'])."&id=".urlencode($_REQUEST['problem']);
		header("Location: $url");
		exit();
	}
}

begin_page("System Test");
?>
<form method="post" enctype="multipart/form-data">
<table>
<tr>
<td>Problem:</td>
<td><?php echo htmlspecialchars($problem_info['problem_name'])?></td>
</tr>
<tr>
<td>Input File:</td>
<td><?php select_file_widget("input", $record)?></td>
</tr>
<tr>
<td>Expected Output:</td>
<td><?php select_file_widget("expected", $record)?></td>
</tr>
<tr>
<td valign="top">Auto-Judge Options:</td>
<td valign="top">
<div><label><input type="checkbox" name="example_input"<?php echo($record['example_input']=='Y'?' checked="checked"':'')?>>This is an Example input file for the problem</label></div>
<div><label><input type="checkbox" name="autojudge"<?php echo($record['autojudge']=='Y'?' checked="checked"':'')?>>Immediate judge any submissions which fail this test</label></div>
</td>
</tr>
</table>
<div>
<?php if ($record['test_number']) { ?>
<button type="submit">Save Changes</button>
<button type="submit" name="action:delete">Delete</button>
<?php } else { ?>
<button type="submit" name="action:create">Add</button>
<?php } ?>
<button type="submit" name="action:cancel">Cancel</button>
</div>
</form>

<?php if ($record['test_number']) { ?>

<p>Test Results:</p>
<table border="1">
<tr><th>Submission</th><th>Team</th><th>Source Code</th><th>Output</th>
<th>Result</th></tr>
<?php

$sql = "SELECT s.id AS submission, t.team_name AS team_name,
	s.file AS source_file, given_name AS source_name,
	tj.output_file AS output_file,
	tj.id AS job,
	tj.result_status AS result_status
	FROM $schema.test_result tr
	JOIN $schema.submission s
		ON s.id = tr.submission
	JOIN $schema.team t
		ON t.team_number = s.team
	JOIN $schema.test_job tj
		ON tj.id = tr.job
	WHERE test_file=".db_quote($record['input_file'])."
	AND t.contest=".db_quote($record['contest'])."
	AND s.problem=".db_quote($record['problem_number'])."
	ORDER BY s.id";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
while ($tr_info = mysql_fetch_assoc($result)){
	$test_result_url = "test_result.php?submission=".urlencode($tr_info['submission'])
		. "&test_number=".urlencode($_REQUEST['test_number']);
	$source_file_url = "file.php/$tr_info[source_file]/$tr_info[source_name]";
	$output_file_url = "file.php/$tr_info[output_file]/output.txt";
	$job_result_url = "job_result.php?id=".urlencode($tr_info['job']);
	?>
<tr>
<td><a href="<?php echo htmlspecialchars($test_result_url)?>"><?php echo htmlspecialchars($tr_info['submission'])?></a></td>
<td><?php echo htmlspecialchars($tr_info['team_name'])?></td>
<td><a href="<?php echo htmlspecialchars($source_file_url)?>">
<?php echo htmlspecialchars($tr_info['source_name'])?></a></td>
<td><?php
	if ($tr_info['output_file']) {
	?><a href="<?php echo htmlspecialchars($output_file_url)?>">output.txt</a><?php
	} else {
		?>n/a<?php
	}
?></td>
<td><?php
	if ($tr_info['result_status']) {
	?><a href="<?php echo htmlspecialchars($job_result_url)?>"><?php echo htmlspecialchars($tr_info['result_status'])?></a><?php
	} else {
		?>
<img src="ajax-loader.gif" class="job-incomplete-indicator" id="ind_job_<?php
			echo htmlspecialchars($tr_info['job'])?>">
Pending
<?php
	}
?></td>
</tr>
<?php
}
?>
</table>
<form method="post">
<button type="submit" name="action:redo_tests">Redo Tests</button>
</form>
<?php } /* endif editing an existing test */ ?>

<?php
end_page();
?>

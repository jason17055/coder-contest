<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

$contest_id = $_REQUEST['contest'];

if (isset($_REQUEST['id']))
{
	$query = "SELECT * FROM problem
		WHERE problem_number=" . db_quote($_REQUEST['id']) . "
		AND contest=" . db_quote($contest_id);
	$result = mysql_query($query);
	$row = mysql_fetch_assoc($result);
}
else
{
	$row = array(
		contest => $contest_id,
		runtime_limit => 50,
		pp_scoreboard => 1,
		pp_read_problem => 1,
		pp_submit => 1,
		pp_read_opponent => 0,
		pp_challenge => 0,
		pp_read_solution => 0
		);
}
$problem_info = $row;

require_auth();
is_judge_of_problem($row['contest'], $problem_info['judged_by'])
	or die("Error: not authorized");

$contest_info = get_basic_contest_info($contest_id);

$PER_PHASE_OPTIONS = array(
			'pp_scoreboard' => 'On scoreboard',
			'pp_read_problem' => 'Problem statement visible',
			'pp_submit' => 'Submit solutions',
			'pp_read_opponent' => "See other contestants' solutions",
			'pp_challenge' => 'Challenge phase',
			'pp_read_solution' => "See judge's solution",
			);

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	if (isset($_POST['action:cancel']))
	{
		$url = "problems.php?contest=".urlencode($_REQUEST['contest']);
		header("Location: $url");
		exit();
	}

	handle_binary_upload_file("spec");
	handle_upload_file("solution");
	handle_upload_file("input_validator");
	handle_upload_file("output_validator");

	$updates = array();

	$checkboxes = array(
		'visible',
		'allow_submissions',
		'solution_visible'
		);
	foreach ($checkboxes as $c) {
		if ($_REQUEST[$c]) {
			$updates[] = "$c='Y'";
		} else {
			$updates[] = "$c='N'";
		}
	}

	foreach ($PER_PHASE_OPTIONS as $k => $v)
	{
		$sum = 0;
		for ($j = 0; $j <= MAX_PHASE; $j++)
		{
			if ($_REQUEST["{$k}_{$j}"])
			{
				$sum += (1 << $j);
			}
		}
		$updates[] = "$k=$sum";
	}

	if ($_REQUEST['difficulty']) {
		$updates[] = "difficulty=".db_quote($_REQUEST['difficulty']);
	} else {
		$updates[] = "difficulty=NULL";
	}

	$updates[] = "score_by_access_time=".db_quote($_REQUEST['score_by_access_time']);
	$updates[] = "allocated_minutes=".db_quote($_REQUEST['allocated_minutes']);
	$updates[] = "runtime_limit=".db_quote($_REQUEST['runtime_limit']);

	if ($_REQUEST['start_time']) {
		$updates[] = "start_time=".db_quote($_REQUEST['start_time']);
	} else {
		$updates[] = "start_time=NULL";
	}

	if (isset($_REQUEST['spec_file']))
	{
		$updates[] = "spec_file=".db_quote($_REQUEST['spec_file'] ? $_REQUEST['spec_file'] : NULL);
		$updates[] = "spec_name=".db_quote($_REQUEST['spec_name'] ? $_REQUEST['spec_name'] : NULL);
	}
	foreach (array('solution','input_validator','output_validator') as $p)
	{
		if (isset($_REQUEST["{$p}_file"]))
		{
			$updates[] = "{$p}_file=".db_quote($_REQUEST["{$p}_file"] ? $_REQUEST["{$p}_file"] : NULL);
			$updates[] = "{$p}_name=".db_quote($_REQUEST["{$p}_name"] ? $_REQUEST["{$p}_name"] : NULL);
		}
	}

	if (isset($_POST['action:redo_expected_output']))
	{
		redo_expected_output($_REQUEST['contest'], $_REQUEST['id']);
		$url = "problem.php?id=" . urlencode($_REQUEST['id'])
			. "&contest=" . urlencode($_REQUEST['contest']);
		header("Location: $url");
	}
	else if (isset($_POST['action:create_problem']))
	{
		$sql = "SELECT COALESCE(MAX(problem_number),0)+1 problem_number
			FROM problem
			WHERE contest=" . db_quote($_REQUEST['contest']);
		$query = mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$r = mysql_fetch_row($query);
		$problem_number = $r[0];

		$sql = "INSERT INTO problem (problem_number,problem_name,contest,judged_by,scoreboard_solved_image)
			SELECT ".db_quote($problem_number).", "
				. db_quote($_REQUEST['problem_name']) . ", "
				. db_quote($_REQUEST['contest']) . ",
				".db_quote($_REQUEST['judged_by']) . ",
				".db_quote($_REQUEST['scoreboard_solved_image']) . "
			FROM dual";
		mysql_query($sql)
			or die("SQL error: " . mysql_error() . "... $sql");

		if (count($updates) > 0)
		{
			$sql = "UPDATE problem SET " . join(',',$updates)."
			WHERE problem_number=".db_quote($problem_number)."
			AND contest=".db_quote($_REQUEST['contest']);
			mysql_query($sql)
				or die("SQL error: ".mysql_error());
		}

		$url = "problems.php?contest=".urlencode($_REQUEST['contest']);
		header("Location: $url");
		exit();
	}
	elseif (isset($_POST['action:delete_problem']))
	{
		$sql = "DELETE FROM problem
			WHERE problem_number=" . db_quote($_REQUEST['id']) . "
			AND contest=" . db_quote($_REQUEST['contest']);
		mysql_query($sql)
			or die("SQL error: " . mysql_error());
		$url = "problems.php?contest=".urlencode($_REQUEST['contest']);
		header("Location: $url");
		exit();
	}
	else
	{
		$updates[] = "problem_name=" . db_quote($_REQUEST['problem_name']);
		$updates[] = "judged_by=".db_quote($_REQUEST['judged_by']);
		$updates[] = "scoreboard_solved_image=".db_quote($_REQUEST['scoreboard_solved_image']);

		$a = join(',',$updates);
		$sql = "UPDATE problem SET ".join(',', $updates)."
			WHERE problem_number=" . db_quote($_REQUEST['id']) . "
			AND contest=" . db_quote($_REQUEST['contest']);
		mysql_query($sql)
			or die("SQL error: " . mysql_error());

		if ($_REQUEST['difficulty'] != $row['difficulty'] ||
			$_REQUEST['allocated_minutes'] != $row['allocated_minutes'] ||
			$_REQUEST['visible'] != $row['visible'])
		{
			update_results_for_problem($contest_id, $_REQUEST['id']);
		}

		$url = "problems.php?contest=".urlencode($_REQUEST['contest']);
		header("Location: $url");
		exit();
	}
}

begin_page("Problem Definition");

?>
<form method="post" enctype="multipart/form-data" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<table>
<tr>
<td>Name:</td>
<td>
<input type="text" name="problem_name" value="<?php echo htmlspecialchars($row['problem_name'])?>" size="40">
</td>
</tr>
<tr>
<td valign="top">Visible:</td>
<td valign="top">
<div><input type="checkbox" name="visible"<?php echo($row['visible']=='Y'?' checked="checked"' : '')?> id="visible_btn">
<label for="visible_btn">Counted as part of total score</label></div>
<div><label><input type="checkbox" name="allow_submissions"<?php echo($row['allow_submissions']=='Y'?' checked="checked"' : '')?>>
Allow contestants to submit solutions</label></div>
</td>
</tr>
<tr>
<td>Description (PDF):</td>
<td><?php select_file_widget("spec", $row)?></td>
</tr>
<tr>
<td valign="top">Reference Solution:</td>
<td valign="top">
<div><?php select_file_widget("solution", $row)?></div>
<?php
	if ($problem_info['solution_file']) {
		$url = "submit_test.php?contest=".urlencode($contest_id)
		."&problem=".urlencode($_REQUEST['id'])
		."&source_file=".urlencode($problem_info['solution_file'])
		."&source_name=".urlencode($problem_info['solution_name'])
		."&next_url=".urlencode($_SERVER['REQUEST_URI']);
		?>
<div><small><a href="<?php echo htmlspecialchars($url)?>">Test the Reference Solution</a></small></div>
<?php } ?>
</td>
</tr>
<tr>
<td valign="top">Input Validator:</td>
<td valign="top">
<div><?php select_file_widget("input_validator", $row)?></div>
<?php
	if ($problem_info['input_validator_file']) {
		$url = "submit_test.php?contest=".urlencode($contest_id)
		."&problem=".urlencode($_REQUEST['id'])
		."&source_file=".urlencode($problem_info['input_validator_file'])
		."&source_name=".urlencode($problem_info['input_validator_name'])
		."&next_url=".urlencode($_SERVER['REQUEST_URI']);
		?>
<div><small><a href="<?php echo htmlspecialchars($url)?>">Test the Input Validator</a></small></div>
<?php } ?>
</td>
</tr>
<tr>
<td valign="top">Output Validator:</td>
<td valign="top">
<div><?php select_file_widget("output_validator", $row)?></div>
</td>
</tr>
<tr>
<td>Judged by:</td>
<td><input type="text" name="judged_by" value="<?php echo htmlspecialchars($row['judged_by'])?>">
<small>Comma-separated list of judges' usernames (e.g. judge1,judge2)</small>
</td>
</tr>
<tr>
<td>Balloon:</td>
<td><select name="scoreboard_solved_image">
<option value=""<?php echo(!$row['scoreboard_solved_image'] ? ' selected="selected"' : '')?>>--default--</option>
<?php
	if ($dh = opendir('scoreboard_images')) {
		while (false !== ($file = readdir($dh))) {
			if (preg_match('/^balloon_(.*)\.png/', $file, $m))
			{
				?><option value="<?php echo htmlspecialchars($file)?>"<?php
						echo($file == $row['scoreboard_solved_image'] ? ' selected="selected"' : '')
						?>><?php echo htmlspecialchars($m[1])?></option>
<?php
			}
		}
		closedir($dh);
	}
	?></select>
</tr>
<tr>
<td>Difficulty value:</td>
<td><input type="text" name="difficulty" value="<?php echo htmlspecialchars($row['difficulty'])?>"></td>
</tr>
<tr>
<td valign="top">Per-Phase Options:</td>
<td valign="top">
    <table class="problem_per_phase_options">
	<tr>
	<th></th>
	<th valign="top" class="phase_header">0</th>
	<?php for ($i = 1; $i <= MAX_PHASE; $i++) { ?>
	<th valign="top" class="phase_header"><?php echo $i?><div class="phase_name"><?php
		echo htmlspecialchars($contest_info["phase{$i}_name"])?></div></th>
	<?php } ?>
	</tr>
	<?php
	foreach ($PER_PHASE_OPTIONS as $k => $v)
	{
		?><tr>
	<th class="attr"><?php echo htmlspecialchars($v)?></th>
	<?php for ($j = 0; $j <= MAX_PHASE; $j++) {
			$c = ($row[$k] & (1 << $j)) != 0;
			$cl = $j == $contest_info['current_phase'] ? 'current_phase' :
				($j == $contest_info['next_phase'] ? 'next_phase' : '');
			?>
	<td class="<?php echo $cl?>">
		<input type="checkbox" name="<?php echo "{$k}_{$j}"?>"<?php
				echo($c ? ' checked="checked"' : '')?>>
	</td>
<?php } /* end loop $j */ ?>
	</tr>
	<?php
	}
	?>
	</table>
</td>
</tr>
<tr>
<td valign="top">Scoring Basis:</td>
<td valign="top">
	<div><label><input type="radio" name="score_by_access_time" value="Y"<?php
	echo ($row['score_by_access_time']=='Y'?' checked="checked"':'')?>>
	When contestant opens the problem</label></div>
	<div><label><input type="radio" name="score_by_access_time" value="N"<?php
	echo ($row['score_by_access_time']!='Y'?' checked="checked"':'')?>>
	At a fixed time:</label>
	<input type="text" name="start_time" value="<?php echo htmlspecialchars($row['start_time'])?>">
	<small>Leave blank to use contest start time.</small>
	</div>
	</td>
</tr>
<tr>
<tr>
<td>Allocated Minutes:</td>
<td><input type="text" name="allocated_minutes" value="<?php echo htmlspecialchars($row['allocated_minutes'])?>">
<small>Used to calculate points in a topcoder-style contest.</small>
</td>
</tr>
<tr>
<td>Run time limit:</td>
<td><input type="text" name="runtime_limit" value="<?php echo htmlspecialchars($row['runtime_limit'])?>">
<small>Time limit for submissions (seconds). (Not currently implemented.)</small>
</td>
</tr>
</table>

<div>
<?php if (!isset($_REQUEST['id'])) { ?>
<button type="submit" name="action:create_problem">Create</button>
<?php } else { ?>
<button type="submit">Update</button>
<button type="submit" name="action:delete_problem" onclick="return confirm('Really delete this problem?')">Delete</button>
<?php } //endif problem_number specified in URL ?>
<button type="submit" name="action:cancel">Cancel</button>
</div>

<?php

if (isset($_REQUEST['id'])) {
$sql = "SELECT contest,problem_number,test_number,
		st.input_file AS input_file,st.input_name AS input_name,
		st.expected_file AS expected_file,
		expected_file_job,
		j.result_status AS expected_job_result,
		example_input,autojudge
	FROM system_test st
	LEFT JOIN test_job j
			ON j.id=st.expected_file_job
	WHERE contest=".db_quote($contest_id)."
	AND problem_number=".db_quote($_REQUEST['id'])."
	ORDER BY test_number";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
if ($row = mysql_fetch_assoc($result))
{
	?><p>System Tests</p><table border="1">
<tr><th>Test</th><th>Input</th><th>Expected Output</th><th>Example?</th><th>Auto-judge?</th></tr>
<?php
	for (; $row; $row = mysql_fetch_assoc($result))
	{
		$url = "system_test.php?contest=".urlencode($row['contest'])."&problem=".urlencode($row['problem_number'])."&test_number=".urlencode($row['test_number']);
		$input_file_url = "file.php/$row[input_file]/$row[input_name]";
		$expected_file_url = "file.php/$row[expected_file]/expected.txt";
		?><tr>
<td><a href="<?php echo htmlspecialchars($url)?>"><?php echo htmlspecialchars($row['test_number'])?></a></td>
<td><a href="<?php echo htmlspecialchars($input_file_url)?>"><?php echo htmlspecialchars($row['input_name'])?></a></td>
<td><?php
	if ($row['expected_file']) {
		?><a href="<?php echo htmlspecialchars($expected_file_url)?>">expected.txt</a><?php
	} else if ($row['expected_file_job'] && !$row['expected_job_result']) {
		?><img src="ajax-loader.gif" class="job-incomplete-indicator" id="ind_job_<?php echo htmlspecialchars($row['expected_file_job'])?>">Generating<?php
	} else {
		?>n/a<?php
	}
?></td>
<td><?php echo($row['example_input']=='Y'?"Yes":"No")?></td>
<td><?php echo($row['autojudge']=='Y'?"Yes":"No")?></td>
</tr>
<?php
	}
	?></table>

	<div><button type="submit" name="action:redo_expected_output">Redo Expected Output for All Tests</button></div>

<?php
} //endif any system tests defined

//
// show clarifications issued for this problem
//

$sql = "SELECT id,
		'clarification' AS type
		FROM clarification
		WHERE contest=".db_quote($contest_id)."
		AND problem_number=".db_quote($_REQUEST['id'])."
		ORDER BY id";
$query = mysql_query($sql)
	or die("SQL error: ".mysql_error());
if (mysql_num_rows($query))
{
	?><p>Clarifications</p><table border="1">
	<?php
	$count = 0;
	while ($row = mysql_fetch_assoc($query))
	{
		$count++;
		$edit_url = "answer_clarification.php?id=".urlencode($row['id'])
			."&next_url=".urlencode($_SERVER['REQUEST_URI']);
	?>
	<tr>
	<td><a href="<?php echo htmlspecialchars($edit_url)?>">
	Clarification <?php echo htmlspecialchars($row['id'])?></td>
	</tr>
	<?php
	} //end while loop
?>
</table>
<?php
} // end if any clarifications defined


$add_system_test_url = "system_test.php?contest=".urlencode($contest_id)."&problem=".urlencode($_REQUEST['id']);
$add_clarification_url = "answer_clarification.php?contest=".urlencode($contest_id)
		."&problem=".urlencode($_REQUEST['id'])
		."&next_url=".urlencode($_SERVER['REQUEST_URI']);

?>

<p>
<a href="<?php echo htmlspecialchars($add_system_test_url) ?>">Add System Test</a>
|
<a href="<?php echo htmlspecialchars($add_clarification_url)?>">Issue Clarification</a>
<?php
	if ($problem_info['solution_file']) {
		$url = "submit_test.php?source_file=".urlencode($problem_info['solution_file'])
		."&source_name=".urlencode($problem_info['solution_name']);
		?>
| <a href="<?php echo htmlspecialchars($url)?>">Test the Reference Solution</a>
<?php
} //endif problem has a reference solution
?>
</p>
<?php } //endif problem_number specified in URL ?>
</form>

<?php
end_page();
?>

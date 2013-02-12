<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');
require_once('includes/notify.php');

require_auth();
if ($_REQUEST['contest'])
{
	$contest_id = $_REQUEST['contest'];
	is_judge_of($contest_id)
		or die("Error: not a judge in contest $contest_id");
}
else
{
	$team_info = get_team_identity();
	$contest_id = $team_info['contest'];
}
$contest_info = get_basic_contest_info($contest_id);

if ($_REQUEST['template'])
{
	$sql = "SELECT source_file,source_name,input_file
		FROM test_job
		WHERE id=".db_quote($_REQUEST['template'])."
		AND type='U'
		AND user_uid=".db_quote($_SESSION['uid']);
	$result = mysql_query($sql);
	$test_info = mysql_fetch_assoc($result)
		or die("invalid template number");
}
else if ($_REQUEST['problem'])
{
	$problem_number = $_REQUEST['problem'];
	$problem_info = get_problem_info($problem_number);

	$test_info = array(
	'source_file' => ($_REQUEST['source_file'] ?: $problem_info['source_file']),
	'source_name' => ($_REQUEST['source_name'] ?: $problem_info['source_name']),
	);
}

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	$next_url = $_REQUEST['next_url'] ? $_REQUEST['next_url'] : '.';
	if (isset($_REQUEST['action:cancel']))
	{
		header("Location: $next_url");
		exit();
	}

	handle_upload_file("source");
	handle_upload_file("input");

	$_REQUEST['source_file']
		or die("Error: no source file provided");
	$_REQUEST['input_file']
		or die("Error: no input file provided");

	if ($team_info && $problem_number)
	{
		$sql = "UPDATE results
			SET source_file=".db_quote($_REQUEST['source_file']).",
			source_name=".db_quote($_REQUEST['source_name'])."
			WHERE team_number=".db_quote($team_info['team_number'])."
			AND problem_number=".db_quote($problem_number);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());
	}

	$sql = "INSERT INTO test_job
		(type,user_uid,source_file,source_name,input_file,created)
		VALUES ('U',
		".db_quote($_SESSION['uid']).",
		".db_quote($_REQUEST['source_file']).",
		".db_quote($_REQUEST['source_name']).",
		".db_quote($_REQUEST['input_file']).",
		NOW())";
	mysql_query($sql)
		or die("SQL error: ".mysql_error());
	notify_worker();

	$test_id = mysql_insert_id();
	$url = "user_test.php?id=" . urlencode($test_id);
	if ($team_info && $problem_number)
	{
		$url .= "&problem=".urlencode($problem_number);
	}
	if ($_REQUEST['next_url'])
	{
		$url .= "&next_url=".urlencode($_REQUEST['next_url']);
	}
	header("Location: $url");
	exit();
}

begin_page("Test a Solution",
	array('notitle' => 1
		));

if ($problem_info)
{
problem_actions_tabnav('test', $problem_info);
?>
<h2><?php echo htmlspecialchars($problem_info['problem_name'])?></h2>
<?php
}

?>

<h3>Test a Solution</h3>
<p>
Use this form to test your code with the same system/environment that
the judges are using. Your source code will be compiled and executed,
using the specified input file. Then the output from your program will
be displayed.
</p>

<form name="form1" method="post" enctype="multipart/form-data" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<table>
<tr>
<td valign="top">Source code:</td>
<td>
<?php if ($test_info['source_file']) {
		$url = "file.php/$test_info[source_file]/$test_info[source_name]";
		?>
<div>
<input type="hidden" name="source_name" value="<?php echo htmlspecialchars($test_info['source_name'])?>">
<input type="hidden" name="source_file" value="<?php echo htmlspecialchars($test_info['source_file'])?>">
Use same source code file:
<a href="<?php echo htmlspecialchars($url)?>"><?php echo htmlspecialchars($test_info['source_name'])?></a></div>
<div>or upload a new file:
<?php } else { ?>
<div>Upload a file:
<?php } ?>
<input type="file" name="source_upload"></div>
<div><small>Accepted file types:
<?php
	$found = get_accepted_file_types();
	echo htmlspecialchars(join(', ', $found));
?></small></div>
</td>
</tr>
<tr>
<td valign="top">Input file:</td>
<td>
<?php
	$contest_id = is_in_a_contest();
	$sql = "SELECT input_file,
			CONCAT(problem_name,'_',test_number) AS name
		FROM system_test st
			JOIN problem p
			ON p.contest=st.contest
			AND p.problem_number=st.problem_number
		WHERE st.contest=".db_quote($contest_id)."
		AND example_input='Y'
		AND ".check_phase_option_bool('p.pp_read_problem')."
		ORDER BY p.problem_number, st.test_number
		";
	$result = mysql_query($sql);
	if (!$result) { die ("SQL error: ".mysql_error()); }
	if (mysql_num_rows($result) != 0)
	{
		?>Select example input file: <select id="example_input_file_select">
		<option value="">--select--</option>
		<?php
		while ($frow = mysql_fetch_assoc($result)) {
			?><option value="<?php echo htmlspecialchars($frow['input_file'])?>"><?php echo htmlspecialchars($frow['name'])?></option>
		<?php
		}
		?></select><br>
or upload a file:
<?php
	} else {
	?>Upload a file:
<?php
	}
	?>
<input type="file" name="input_upload"><br>
or enter text in this box:<br>
<textarea name="input_content" rows="12" cols="60"><?php
	if ($hash = $test_info['input_file']) {
		if (!preg_match("/^[0-9a-f]+$/", $hash))
		{
			die("invalid file hash $hash");
		}
		$file = "uploaded_files/$hash.txt";
		echo file_get_contents($file);
	}?></textarea></td>
</tr>
</table>
<script type="text/javascript"><!--
	$('#example_input_file_select').change(function(evt) {
			if (this.value) {
				$.ajax({
				type: "GET",
				url: "file.php/"+this.value+"/input.txt",
				dataType: "text",
				success: function(data) {
					document.form1.input_content.value=data;
					}
				});
			}
		});
		//--></script>
<div>
<button type="submit">Test</button>
<?php if ($_REQUEST['next_url']) { ?>
<input type="hidden" name="next_url" value="<?php echo htmlspecialchars($_REQUEST['next_url'])?>">
<button type="submit" name="action:cancel">Cancel</button>
<?php } ?>
</div>

</form>


<?php
end_page();
?>

<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

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

$sql = "SELECT * FROM test_job
	WHERE id=".db_quote($_REQUEST['id']);
$result = mysql_query($sql);
$test_info = mysql_fetch_assoc($result)
	or die("Error: test $_REQUEST[id] not found");
$test_info['type'] == 'U' && $test_info['user_uid'] == $_SESSION['uid']
	or die("Error: test $_REQUEST[id] not found");

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	die("Not implemented");
}

if (isset($_REQUEST['onsuccess'])
	&& $test_info['result_status'] == 'No Error')
{
	header("Location: ".$_REQUEST['onsuccess']);
	exit();
}
if (isset($_REQUEST['onany'])
	&& $test_info['result_status'])
{
	header("Location: ".$_REQUEST['onany']);
	exit();
}

begin_page("Test Results",
	array('notitle' => 1
		));

if ($_REQUEST['problem']) {
	$problem_number = $_REQUEST['problem'];
	$problem_info = get_problem_info($problem_number);
problem_actions_tabnav('test', $problem_info);
?>
<h2><?php echo htmlspecialchars($problem_info['problem_name'])?></h2>
<?php
	
}

$source_file_url = "file.php/$test_info[source_file]/$test_info[source_name]";
$input_file_url = "file.php/$test_info[input_file]/input.txt";
$output_file_url = "file.php/$test_info[output_file]/output.txt";
$job_result_url = "job_result.php?id=" . urlencode($_REQUEST['id']);

?>

<h3>Test Results</h3>
<table>
<tr>
<td>Test:</td>
<td><?php echo htmlspecialchars($test_info['id'])?></td>
</tr>
<tr>
<td>Source file:</td>
<td><a href="<?php echo htmlspecialchars($source_file_url)?>"><?php
	echo htmlspecialchars($test_info['source_name'])?></a>
	<?php
	if (isset($_REQUEST['label'])) {
		echo htmlspecialchars("($_REQUEST[label])");
		} ?></td>
</tr>
<tr>
<td>Input file:</td>
<td><a href="<?php echo htmlspecialchars($input_file_url)?>">input.txt</a></td>
</tr>
<tr>
<td valign="top">Status:</td>
<td><?php if ($test_info['result_status']) { ?>
<div><?php echo htmlspecialchars($test_info['result_status'])?></div>
<iframe width="640" height="120"
	src="<?php echo htmlspecialchars($job_result_url)?>"></iframe>
<?php } else {
	// no result status
?>
<div><img src="ajax-loader.gif" class="job-incomplete-indicator" id="ind_job_<?php echo htmlspecialchars($_REQUEST['id'])?>"><em><font color="#ff00ff">Testing... Please wait...</font></em></div>
<?php } /* endif no result status */ ?>
</td>
</tr>
<?php if ($test_info['output_file']) { ?>
<tr>
<td valign="top">Output:</td>
<td><iframe width="640" height="40%"
	src="<?php echo htmlspecialchars($output_file_url)?>"></iframe></td>
</tr>
<?php } ?>
</table>


<?php
$another_test_url = "submit_test.php?template=".urlencode($_REQUEST['id']);
if ($_REQUEST['next_url']) {
	$another_test_url .= "&next_url=".urlencode($_REQUEST['next_url']);
}
?>

<p>
<a href="<?php echo htmlspecialchars($another_test_url)?>">Another Test</a>
<?php if ($_REQUEST['next_url']) { ?>
| <a href="<?php echo htmlspecialchars($_REQUEST['next_url'])?>">Continue</a>
<?php } ?>
| <a href=".">Menu</a>
</p>

<?php
end_page();
?>

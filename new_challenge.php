<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

require_auth();
$team_info = get_team_identity();
$contest_id = $team_info['contest'];
$contest_info = get_basic_contest_info($contest_id);

$sql = "SELECT problem_name, problem_number,
	input_validator_file,input_validator_name
	FROM submission s
	JOIN team t
		ON t.team_number=s.team
	JOIN problem p
		ON p.problem_number=s.problem
		AND p.contest=t.contest
	WHERE s.id=".db_quote($_REQUEST['submission'])."
	AND p.contest=".db_quote($contest_id)."
	AND ".check_phase_option_bool('p.pp_challenge')."
	";
$query = mysql_query($sql)
	or die("SQL error: ".mysql_error());
$problem_info = mysql_fetch_assoc($query);
if (!$problem_info)
{
	// not allowed to challenge
	header("Location: .");
	exit();
}

if ($_SERVER['REQUEST_METHOD'] == "POST")
{
	$next_url = $_REQUEST['next_url'] ?: "open_problem.php?problem=".urlencode($problem_info['problem_number'])."&show=solutions";

	if (isset($_REQUEST['action:cancel']))
	{
		header("Location: $next_url");
		exit();
	}
	else
	{
		handle_upload_file("input");

		//
		// create a job to validate the input
		//

		if (!$problem_info['input_validator_file'])
		{
			die("Cannot challenge-- The contest director has not provided an input validator for this problem.");
		}

		$sql = "INSERT INTO challenge (creator,submission,input_file,status)
				VALUES (".db_quote($team_info['team_number']).",
				".db_quote($_REQUEST['submission']).",
				".db_quote($_REQUEST['input_file']).",
				'Checking input file')";
		mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$challenge_id = mysql_insert_id();

		$sql = "INSERT INTO test_job
			(type,source_file,source_name,input_file,created,callback_data)
			VALUES ('V',
			".db_quote($problem_info['input_validator_file']).",
			".db_quote($problem_info['input_validator_name']).",
			".db_quote($_REQUEST['input_file']).",
			NOW(),
			".db_quote("challenge $challenge_id").")";
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		notify_worker();

		header("Location: $next_url");
		exit();
	}
}

begin_page("Challenge a Solution");

?>
<p>
Use this page to challenge another contestant's solution to a problem.
You do this by providing a legal input file for which their program
does not generate a correct answer.
</p>
<p>
Be sure the input you provide is legal input. If it does not exactly match
the format given in the problem definition, the challenge will fail.
</p>


<form method="post" enctype="multipart/form-data" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<table>
<tr>
<td valign="top">Submission:</td>
<td valign="top"><?php echo htmlspecialchars($_REQUEST['submission'])?></td>
</tr>
<tr>
<td valign="top">Problem:</td>
<td valign="top"><?php echo htmlspecialchars($problem_info['problem_name'])?></td>
</tr>
<tr>
<td valign="top">Input file:</td>
<td>
Upload a file:
<input type="file" name="input_upload"><br>
or enter text in this box:<br>
<textarea name="input_content" rows="12" cols="60"></textarea></td>
</tr>
</table>

<div>
<button type="submit">Submit</button>
<button type="submit" name="action:cancel">Cancel</button>
</div>

</form>


<?php
end_page();
?>

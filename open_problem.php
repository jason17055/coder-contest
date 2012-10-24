<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

require_auth();
$team_info = get_team_identity();
$contest_id = $team_info['contest'];
$contest_info = get_basic_contest_info($contest_id, ', collaboration');

$problem_number = $_REQUEST['problem'];

$sql = "SELECT problem_name,spec_file,spec_name,
		".check_phase_option_sql('pp_read_solution')." AS solution_visible,
		".check_phase_option_sql('pp_read_opponent')." AS read_opponent,
		".check_phase_option_sql('pp_challenge')." AS challenge_phase,
		solution_file,solution_name,
		r.source_file AS source_file,
		r.source_name AS source_name
	FROM problem p
	LEFT JOIN results r
		ON r.team_number=".db_quote($team_info['team_number'])."
		AND r.problem_number=p.problem_number
	WHERE contest=".db_quote($contest_id)."
	AND p.problem_number=".db_quote($problem_number)."
	AND ".check_phase_option_bool('pp_read_problem')."
	";
$result = mysql_query($sql);
$problem_info = mysql_fetch_assoc($result)
	or die("invalid problem");

$sql = "INSERT IGNORE INTO results (team_number,problem_number)
	VALUES (".db_quote($team_info['team_number']).",
	".db_quote($problem_number)."
	)";
mysql_query($sql)
	or die("MySQL error");

$sql = "UPDATE results
	SET opened=NOW()
	WHERE team_number=".db_quote($team_info['team_number'])."
	AND problem_number=".db_quote($problem_number)."
	AND (opened IS NULL OR opened>NOW())";
mysql_query($sql)
	or die("MySQL error");

if ($_SERVER['REQUEST_METHOD'] == 'POST')
{
	if (isset($_REQUEST['action:save_code']))
	{
		handle_upload_file("source");

		$sql = "UPDATE results
				SET source_file=".db_quote($_REQUEST['source_file']).",
				source_name=".db_quote($_REQUEST['source_name'])."
				WHERE team_number=".db_quote($team_info['team_number'])."
				AND problem_number=".db_quote($problem_number);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		$url = $_SERVER['REQUEST_URI'];
		header("Location: $url");
		exit();
	}
	else
	{
		die("Invalid method");
	}
}

$show_mode = $_REQUEST['show'];
if (!$show_mode)
{
	$show_mode = $problem_info['spec_file'] && !$problem_info['source_file'] ? 'problem' : 'write';
}

begin_page($problem_info['problem_name'],
	array('notitle' => 1,
		'javascript_files' => array(
					'CodeMirror-2.33/lib/codemirror.js',
					'CodeMirror-2.33/mode/clike/clike.js',
					'CodeMirror-2.33/mode/perl/perl.js',
					'CodeMirror-2.33/mode/python/python.js',
					'CodeMirror-2.33/mode/ruby/ruby.js'
					)
		));
$url = "file.php/$problem_info[spec_file]/$problem_info[spec_name]";
$purl = "open_problem.php?problem=".urlencode($problem_number);

problem_actions_tabnav($show_mode, $problem_info);
?>
<h2><?php echo htmlspecialchars($problem_info['problem_name'])?></h2>
<?php
if ($show_mode == 'problem')
{
?>
<iframe id="main_content_container" src="<?php echo htmlspecialchars($url)?>"></iframe>
<?php

} //end if showing problem statement
else if ($show_mode == 'clarifications')
{
	$sql = "SELECT request,response
		FROM clarification
		WHERE contest=".db_quote($contest_id)."
		AND problem_number=".db_quote($_REQUEST['problem'])."
		AND
		(status='reply-all'
		OR (status='reply-one' AND team=".db_quote($team_info['team_number'])."
		  )
		)
		ORDER BY id";
	$query = mysql_query($sql);
	$count = 0;
	while ($row = mysql_fetch_assoc($query))
	{
		$count++;
		?><hr>
		<p><?php echo htmlspecialchars($row['request'])?></p>
		<p><?php echo htmlspecialchars($row['response'])?></p>
		<?php
	}

	if ($count == 0)
	{
		?><p>
		There have not been any clarifications issued for this problem.
		</p>
		<?php
	}

	$clar_url = 'newclarification.php?problem='.urlencode($problem_number);
?>
<p>
Confused by a problem specification? Request a clarification here.
</p>

<form method="post" action="<?php echo htmlspecialchars($clar_url)?>">
<table>
<tr>
<td valign="top">Message:</td>
<td><textarea name="message" rows="10" cols="72"></textarea></td>
</tr>
</table>

<div>
<button type="submit">Submit</button>
</div>

</form>


<?php

} //end if showing clarifications
else if ($show_mode == 'write')
{
	$source_name = $problem_info['source_name'];
	if (!$source_name) { $source_name = 'Main.java'; }

	$hilite_mode = get_language_from_name($source_name);

	?>
	<form method="post" action="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<div>
<!--
<label>Programming Language:
<select name="language">
<option value="java" selected="selected">Java</option>
<option value="pl">Perl</option>
<option value="cpp">C++</option>
<option value="c">C</option>
</select>
</label>
-->
<label>Filename:
<input type="text" name="source_name" value="<?php echo htmlspecialchars($source_name)?>">
</label>
<button type="submit" name="action:save_code">Save</button>
<!--
<button type="button">Compile</button>
<span style="margin-left: 48pt"><a href="<?php echo htmlspecialchars($purl.'&show=upload')?>">Upload a file from your computer</a></span>
-->
</div>
<div>
<textarea id="main_content_container" name="source_content" rows="10" cols="70"><?php
	if ($hash = $problem_info['source_file']) {
		if (!preg_match("/^[0-9a-f]+$/", $hash))
		{
			die("invalid file hash $hash");
		}
		$file = "uploaded_files/$hash.txt";
		echo file_get_contents($file);
	}
?></textarea>
</div>
</form>

<script type="text/javascript"><!--

var myCM = CodeMirror.fromTextArea(
	document.getElementById('main_content_container'),
	{
		mode: "<?php echo($hilite_mode)?>",
		indentUnit: 4
	});

//--></script>

<?php
} //end if showing write
else if ($show_mode == 'test')
{
?>

<p>
Use this form to test your code with the same system/environment that
the judges are using. Your source code will be compiled and executed,
using the specified input file. Then the output from your program will
be displayed.
</p>

<form name="form1" method="post" enctype="multipart/form-data" action="submit_test.php">
<table>
<tr>
<td valign="top">Source code:</td>
<td valign="top">
<?php if ($problem_info['source_file']) {
		$url = "file.php/$problem_info[source_file]/$problem_info[source_name]";
		?>
<div>
<input type="hidden" name="source_name" value="<?php echo htmlspecialchars($problem_info['source_name'])?>">
<input type="hidden" name="source_file" value="<?php echo htmlspecialchars($problem_info['source_file'])?>">
Use existing source code file:
<a href="<?php echo htmlspecialchars($url)?>"><?php echo htmlspecialchars($problem_info['source_name'])?></a></div>
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
		AND p.allow_submissions='Y'
		AND p.problem_number=".db_quote($problem_number)."
		ORDER BY st.test_number
		";
	$result = mysql_query($sql);
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
<input type="hidden" name="next_url" value="<?php echo htmlspecialchars($_SERVER['REQUEST_URI'])?>">
<button type="submit">Test</button>
</div>

</form>

<?php
} // end if showing test
else if ($show_mode == 'submit')
{
	$submit_url = 'submit.php?problem='.urlencode($problem_number);
?>
<form method="post" enctype="multipart/form-data" action="<?php echo htmlspecialchars($submit_url)?>">
<table>
<tr>
<td valign="top">Source code:</td>
<td valign="top">
<?php if ($problem_info['source_file']) {
		$url = "file.php/$problem_info[source_file]/$problem_info[source_name]";
		?>
<div>
<input type="hidden" name="source_name" value="<?php echo htmlspecialchars($problem_info['source_name'])?>">
<input type="hidden" name="source_file" value="<?php echo htmlspecialchars($problem_info['source_file'])?>">
Use existing source code file:
<a href="<?php echo htmlspecialchars($url)?>"><?php echo htmlspecialchars($problem_info['source_name'])?></a></div>
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
<?php if ($contest_info['collaboration'] == 'Y') { ?>
<tr>
<td valign="top">Co-authors:</td>
<td valign="top"><select name="coauthor[]" multiple="multiple" style="min-width: 2in">
<?php
	$sql = "SELECT team_number,team_name,user
		FROM team
		WHERE contest=".db_quote($contest_id)."
		AND team_number<>".db_quote($team_info['team_number'])."
		ORDER BY team_name,user";
	$query = mysql_query($sql);
	while ($row = mysql_fetch_assoc($query)) {
	?><option value="<?php echo htmlspecialchars($row['team_number'])?>"><?php
			echo htmlspecialchars("$row[team_name] ($row[user])")?></option>
<?php
	} //end loop
?></select>
<div><small>If anyone helped you with this submission, please pick their names from the list.</small></div>
</td>
</tr>
<?php } //end if collaboration is ad hoc ?>
</table>

<div>
<button type="submit">Submit</button>
</div>

</form>


<?php
} // end if showing submit
else if ($show_mode == 'solutions')
{
	require('show_solutions.inc.php');

} // end if showing solutions

?>
<script type="text/javascript"><!--

function adjustContentContainerSize(cEl)
{
	if (!cEl)
		return;

	var h = window.innerHeight;
	var y = 0;
	var el = cEl;
	while (el.offsetParent)
	{
		y += el.offsetTop;
		el = el.offsetParent;
	}

	var desiredHeight = h-y-17;
	if (desiredHeight > 360)
	{
		$(cEl).css({height: desiredHeight+'px' });
	}
}
adjustContentContainerSize( document.getElementById('main_content_container') );

if ($('.CodeMirror-scroll').length != 0)
{
	adjustContentContainerSize( $('.CodeMirror-scroll').get(0) );
}

function doProblem()
{
	window.location.href='<?php
	$url = 'open_problem.php?problem='.urlencode($_REQUEST['problem']);
	echo $url;
	?>';
}

function doClarifications()
{
	window.location.href='<?php
	$url = 'open_problem.php?problem='.urlencode($_REQUEST['problem']).'&show=clarifications';
	echo $url;
	?>';
}

function doTest()
{
	window.location.href='<?php
	$my_url = $_SERVER['REQUEST_URI'];
	$url = 'submit_test.php?next_url='.urlencode($my_url);
	echo $url;
	?>';
}

function doSubmit()
{
	window.location.href='<?php
	$my_url = $_SERVER['REQUEST_URI'];
	$url = 'submit.php?problem='.urlencode($_REQUEST['problem']).'&next_url='.urlencode($my_url);
	echo $url;
	?>';
}

//--></script>

<?php
end_page();

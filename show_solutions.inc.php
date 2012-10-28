<?php
function show_challenge_status($challenge_id)
{
	global $team_info;

	$sql = "SELECT status FROM challenge
		WHERE id=".db_quote($challenge_id)."
		AND creator=".db_quote($team_info['team_number']);
	$query = mysql_query($sql);
	$info = mysql_fetch_assoc($query);

	if ($info)
	{
		?>
		<span class="challenge_status">** <?php
		echo htmlspecialchars($info['status'])?></span>
		<?php
	}
}

?>
<p>During the challenge phase, you can click on another contestant's
solution to view or challenge the correctness of it.</p>
<div id="all_solutions_table_container">
<table id="all_solutions_table">
<tr>
<th align="left">Place</th>
<th align="left">Contestant</th>
<th align="left">Submission</th>
<th>Problem Score</th>
<th>Total Contest Score</th>
</tr>
<?php
	if ($problem_info['solution_visible'] == 'Y') {
?>
<tr class="solution_row" solution-file="<?php echo htmlspecialchars("file.php/$problem_info[solution_file]/$problem_info[solution_name]")?>" solution-language="<?php echo get_language_from_name($problem_info['solution_name'])?>">
<td>-</td>
<td><em>Judge's solution</em></td>
<td><?php echo htmlspecialchars($problem_info['solution_name'])?></td>
<td>-</td>
<td>-</td>
</tr>
<?php
	}
	if ($problem_info['read_opponent'] == 'Y') {

	global $contest_id;
	$sql = "
		SELECT team_name,team_description,
			team_score,team_score_alt,
			problem_opened,
			problem_score,problem_score_alt,
			file AS source_file,
			most_recent_submission AS submission,
			given_name AS source_name,
			ss.status AS status,
			(SELECT MAX(id) FROM challenge
				WHERE creator=".db_quote($team_info['team_number'])."
				AND submission=most_recent_submission
				) AS most_recent_challenge
		FROM (
		SELECT
			team_name,
			description AS team_description,
			t.score AS team_score,
			t.score_alt AS team_score_alt,
			t.ordinal AS team_ordinal,
			(SELECT MAX(id) FROM submission s
			WHERE s.team=t.team_number
			AND s.problem=".db_quote($_REQUEST['problem'])."
			) AS most_recent_submission,
			r.score AS problem_score,
			r.score_alt AS problem_score_alt,
			CASE WHEN r.opened IS NOT NULL THEN 'Y' ELSE 'N' END AS problem_opened
		FROM team t
		LEFT JOIN results r
			ON r.team_number=t.team_number
			AND r.problem_number=".db_quote($_REQUEST['problem'])."
		WHERE t.contest=".db_quote($contest_id)."
		AND t.visible='Y'
			) x
		LEFT JOIN submission ss
			ON ss.id=x.most_recent_submission
		ORDER BY team_score DESC, team_score_alt DESC,
			team_ordinal ASC, team_name ASC
		";
		$query = mysql_query($sql)
			or die("SQL error: ".mysql_error());
		$place = 0;
		while ($row = mysql_fetch_assoc($query))
		{
			$place++;
			$url = $row['source_file'] ? "file.php/$row[source_file]/$row[source_name]" : "";
			?>
			<tr class="solution_row" solution-file="<?php echo htmlspecialchars($url)?>" solution-language="<?php
					echo get_language_from_name($row['source_name'])?>"
					<?php echo($problem_info['challenge_phase']=='Y'
						? ' challenge-link="'.htmlspecialchars("new_challenge.php?submission=".urlencode($row['submission'])).'"' : '')?>>
			<td><?php echo htmlspecialchars($place)?></td>
			<td><?php echo htmlspecialchars($row['team_name'])?></td>
			<td><?php echo htmlspecialchars($row['source_name'])?></td>
			<td><?php
			if ($row['problem_score']) {
				echo format_score($row['problem_score'],$row['problem_score_alt']);
			} else if ($row['problem_opened'] == 'N') {
				echo "Unopened";
			} else if ($row['status']) {
				echo htmlspecialchars($row['status']);
			} else {
				echo "Opened";
			}
			if ($row['most_recent_challenge'])
			{
				show_challenge_status($row['most_recent_challenge']);
			}
				?></td>
			<td><?php echo format_score($row['team_score'],$row['team_score_alt'])?></td>
			</tr>
			<?php
		}
	}
?>
</table>
</div>
<div id="display_label"><span id="display_filename"></span>
<span id="display_challenge_link"><a href="#">Challenge this solution</a></span>
</div>
<form>
<textarea id="main_content_container"></textarea>
<script language="javascript"><!--

var myCM = CodeMirror.fromTextArea(
	document.getElementById('main_content_container'),
	{
		indentUnit: 4,
		readOnly: true
	});

$('.solution_row').click(function() {
	$('.solution_row').removeClass('selected');
	$(this).addClass('selected');
	myCM.setValue('');
	$('#display_filename').text('');
	$('#display_challenge_link').hide();

	var solutionLang = $(this).attr('solution-language');
	if (solutionLang)
	{
		myCM.setOption('mode', solutionLang);
	}

	var challengeUrl = $(this).attr('challenge-link');
	var url = $(this).attr('solution-file');
	if (url)
	{
		var filename = url.replace(/^.*\//, '');
		$.ajax({
		type: "GET",
		url: url,
		dataType: "text",
		success: function(data) {
			myCM.setValue(data);
			$('#display_filename').text('Displaying '+filename);

			if (challengeUrl)
			{
				$('#display_challenge_link a').attr('href', challengeUrl);
				$('#display_challenge_link').show();
			}
			}
		});
	}

});
//--></script>

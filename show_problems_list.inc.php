<?php

$sql = "
	SELECT problem_number,problem_name,spec_file,spec_name,
		thetime,incorrect_submissions,opened,difficulty,
		s.submitted,s.status
	FROM (
	SELECT p.problem_number,problem_name,spec_file,spec_name,
	thetime,incorrect_submissions,opened,difficulty,
	(SELECT MAX(id) FROM submission WHERE team=r.team_number
		AND problem=p.problem_number) AS last_submission
	FROM problem p
	LEFT JOIN results r
	ON r.problem_number=p.problem_number
	AND team_number=".db_quote($team_info['team_number'])."
	WHERE contest=" . db_quote($contest_id) . "
	AND ".check_phase_option_bool('pp_read_problem')."
	) t1
	LEFT JOIN submission s
		ON s.id=t1.last_submission
	ORDER BY problem_number
		";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
if ($row = mysql_fetch_assoc($result)) {
?>
<h3>Problems</h3>
<table class="realtable">
<tr><th>Problem</th><th>Status</th></tr>
<?php

while ($row) {
	$open_url = "open_problem.php?problem=" . urlencode($row['problem_number']);
	$status_h = "Unopened";
	if ($row['thetime']) {
		$status_h = "Solved!";
	} else if ($row['incorrect_submissions']) {
		$status_h = "Attempted " . $row['incorrect_submissions'] . " times";
		if ($row['submitted']) {
			$status_h .= "; last submitted ".format_sqldatetime($row['submitted']) . " (".htmlspecialchars($row['status']).")";
		}
	} else if ($row['submitted']) {
		$status_h = "Submitted ".format_sqldatetime($row['submitted']);
		if ($row['status']) {
			$status_h .= " (" . htmlspecialchars($row['status']).")";
		}
	} else if ($row['opened']) {
		$status_h = "Opened ".format_sqldatetime($row['opened']). " $row[submission_count]";
	}
?>
<tr><td><a href="<?php echo htmlspecialchars($open_url)?>">
<?php echo htmlspecialchars($row['problem_name']);
if ($row['difficulty']) { echo htmlspecialchars(" ($row[difficulty])"); }
?>
</a></td>
<td><?php echo($status_h)?></td>
</tr>
<?php

$row = mysql_fetch_assoc($result);
} //end list of problems in order
?>
</table>
<?php } //end if any problems have spec files
?>

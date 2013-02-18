<h3>Submissions/Responses</h3>

<?php

$sql = "SELECT id,submitted,problem_name,COALESCE(status,'Pending') AS status,
		'solution' AS type,
		given_name AS source_name,
		NULL AS request, NULL AS response
	FROM submission s
	JOIN team t
		ON t.team_number=s.team
	JOIN problem p
		ON p.problem_number=s.problem
		AND p.contest=t.contest
	WHERE team=".db_quote($team_info['team_number'])."
		AND ".check_phase_option_bool('p.pp_submit')."
	UNION
	SELECT id,submitted,problem_name,COALESCE(status,'Pending') AS status,
		'clarification' AS type,
		NULL AS source_name,
		request,response
	FROM clarification s
	JOIN problem p
		ON p.contest=s.contest
		AND p.problem_number=s.problem_number
	WHERE s.contest=".db_quote($contest_id)."
	AND (team=".db_quote($team_info['team_number'])."
		OR status='reply-all')
	AND ".check_phase_option_bool('p.pp_submit')."
	ORDER BY submitted ASC, id ASC";
$result = mysql_query($sql)
	or die("SQL error: ".mysql_error());
if (mysql_num_rows($result) > 0) {
?>
<table class="realtable">
<tr><th>Time</th>
<th>Problem</th>
<th>Submitted</th>
<th>Response</th>
</tr>
<?php
while ($row = mysql_fetch_assoc($result)) { ?>
<?php
		$status = $row['status'] == 'reply-one' ? "Responded" :
			($row['status'] == 'reply-all' ? "Broadcasted" :
			$row['status']);
		$url = $row['type'] == 'solution' ?
			"solution.php?id=".urlencode($row['id']) :
			"clarification.php?id=".urlencode($row['id']);
?><tr>
<td><?php echo htmlspecialchars($row['submitted'])?></td>
<td><?php echo htmlspecialchars($row['problem_name'])?></td>
<td><?php 
	if ($row['type'] == 'solution') { ?>
Solution: <?php echo htmlspecialchars($row['source_name']);
	}
	if ($row['type'] == 'clarification') { ?>
Clarification: <?php echo htmlspecialchars(substr($row['request'],0,60));
	}
	?></td>
<td><a href="<?php echo htmlspecialchars($url)?>"><?php echo htmlspecialchars($status)?></a></td>
</tr>
<?php
	}
?>
</table>
<?php } else {
	// no submissions to show ?>
	<div>None at this time.</div>

<?php } // endif no submissions/clarifications to show ?>

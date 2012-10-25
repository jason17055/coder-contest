<?php

	$sql = "SELECT c.request AS request,
		c.response AS response,
		c.team AS q_author,
		t.team_name AS q_author_name,
		c.status AS status
		FROM clarification c
		LEFT JOIN team t
			ON t.team_number=c.team
		WHERE c.contest=".db_quote($contest_id)."
		AND c.problem_number=".db_quote($_REQUEST['problem'])."
		AND
		(c.status='reply-all'
		OR c.team=".db_quote($team_info['team_number'])."
		)
		ORDER BY id";
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$count = 0;
	while ($row = mysql_fetch_assoc($query))
	{
		$count++;
		if ($row['q_author'] == $team_info['team_number']) {
			$cl = "own-question";
		} else {
			$cl = "opponent-question";
		}
		if ($row['request']) {
		?><div class="clarification-question <?php echo $cl?>">
		<div class="clarification-author"><span class="author"><?php echo htmlspecialchars($row['q_author_name'])?></span></div>
		<p><?php echo htmlspecialchars($row['request'])?></p>
		</div>
		<?php
		}

		if ($row['status'] == 'reply-all') {
			$cl = "reply-all";
		} else {
			$cl = "reply-one";
		}
		if ($row['response']) {
			$a = $row['request'] ? 'Response' : 'Clarification';

		?><div class="clarification-answer <?php echo $cl?>">
		<div class="clarification-author"><span class="author"><?php echo $a?></span>
		</div>
		<p><?php echo htmlspecialchars($row['response'])?></p>
		</div>
		<?php
		}
		else
		{
		?><div class="clarification-answer-none">
			No response at this time.
			</div>
			<?php
		}
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
<hr>
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



<?php

require_once('notify.php');

function test_completed($job_id)
{
	$sql = "SELECT * FROM test_job
		WHERE id=".db_quote($job_id);
	$result = mysql_query($sql);
	$job_info = mysql_fetch_assoc($result)
		or die("invalid job $job_id");

	if ($job_info['type']=='S')    // system test of a contestant's program
	{
		//expected output?
//never tested...
//		$sql = "SELECT st.expected_file AS expected_file
//			FROM test_result tr
//			JOIN submission s
//				ON tr.submission = s.id
//			JOIN team t
//				ON s.team = t.team_number
//			JOIN system_test st
//				ON s.problem = st.problem_number
//				AND t.contest = st.contest
//				AND tr.test_file = st.input_file
//			WHERE tr.job=".db_quote($job_id);
//		$result = mysql_query($sql)
//			or die("SQL error: ".mysql_error());

		//find and update the system test that created this job
		$sql = "UPDATE test_result
			SET result_status=".db_quote($job_info['result_status'])."
			WHERE job=".db_quote($job_id);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		wakeup_listeners();
	}
	else if ($job_info['type']=='E')  // generating expected output
									  // for a system test
	{
		$sql = "UPDATE system_test st
		SET expected_file=".db_quote($job_info['output_file'])."
		WHERE input_file=".db_quote($job_info['input_file'])."
		AND EXISTS (
			SELECT 1 FROM problem p
			WHERE solution_file=".db_quote($job_info['source_file'])."
			AND solution_name=".db_quote($job_info['source_name'])."
			AND p.contest=st.contest
			AND p.problem_number=st.problem_number)
		";
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		wakeup_listeners();
	}
	else if ($job_info['type']=='V')   // validate a user-submitted
									// input file
	{
		if (preg_match('/^challenge (.*)$/', $job_info['callback_data'], $m))
		{
			$challenge_id = $m[1];
			challenge_step1_completed($challenge_id, $job_info);
		}
	}
	else if ($job_info['type']=='C')   // run a user-submitted input file
									// against the official problem solution
	{
		if (preg_match('/^challenge (.*)$/', $job_info['callback_data'], $m))
		{
			$challenge_id = $m[1];
			challenge_step2_completed($challenge_id, $job_info);
		}
	}
	else if ($job_info['type']=='D')   // run a user-submitted input file
									// against a submitted solution
	{
		if (preg_match('/^challenge (.*)$/', $job_info['callback_data'], $m))
		{
			$challenge_id = $m[1];
			challenge_step3_completed($challenge_id, $job_info);
		}
	}
	else if ($job_info['type']=='G')   // run a output validator
	{
		if (preg_match('/^challenge (.*)$/', $job_info['callback_data'], $m))
		{
			$challenge_id = $m[1];
			challenge_step4_completed($challenge_id, $job_info);
		}
	}
	else
	{
		wakeup_listeners();
	}
}

function challenge_step1_completed($challenge_id, $job_info)
{
	$sql = "SELECT
			p.solution_file AS solution_file,
			p.solution_name AS solution_name,
			p.runtime_limit AS runtime_limit,
			c.input_file AS input_file
		FROM challenge c
		JOIN submission s
			ON s.id = c.submission
		JOIN team t
			ON t.team_number=s.team
		JOIN problem p
			ON p.problem_number=s.problem
			AND p.contest=t.contest
		WHERE c.id=".db_quote($challenge_id);
	$query = mysql_query($sql);
	$challenge_info = mysql_fetch_assoc($query)
		or die("invalid challenge $challenge_id");

	if ($job_info['result_status'] == 'No Error')
	{
		$sql = "UPDATE challenge SET status='Generating expected output'
			WHERE id=".db_quote($challenge_id);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		//
		// create a job to run the judge's program (if available)
		//

		$sql = "INSERT INTO test_job
			(type,source_file,source_name,input_file,created,runtime_limit,callback_data)
			VALUES ('C',
			".db_quote($challenge_info['solution_file']).",
			".db_quote($challenge_info['solution_name']).",
			".db_quote($challenge_info['input_file']).",
			NOW(),
			".db_quote($challenge_info['runtime_limit']).",
			".db_quote("challenge $challenge_id").")";
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		notify_worker();
	}
	else // typically "Run-Time Error"
	{
		$sql = "UPDATE challenge SET status='Invalid input'
			WHERE id=".db_quote($challenge_id);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());
	}
}

function challenge_step2_completed($challenge_id, $job_info)
{
	$sql = "SELECT
			s.file AS source_file,
			s.given_name AS source_name,
			p.runtime_limit AS runtime_limit,
			c.input_file AS input_file
		FROM challenge c
		JOIN submission s
			ON s.id = c.submission
		JOIN team t
			ON t.team_number=s.team
		JOIN problem p
			ON p.problem_number=s.problem
			AND p.contest=t.contest
		WHERE c.id=".db_quote($challenge_id);
	$query = mysql_query($sql);
	$challenge_info = mysql_fetch_assoc($query)
		or die("invalid challenge $challenge_id");

	if ($job_info['result_status'] == 'No Error')
	{
		$sql = "UPDATE challenge
			SET expected_file=".db_quote($job_info['output_file']).",
			status='Running the challenged program'
			WHERE id=".db_quote($challenge_id);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		//
		// create a job to run the user's program
		//

		$sql = "INSERT INTO test_job
			(type,source_file,source_name,input_file,created,runtime_limit,callback_data)
			VALUES ('D',
			".db_quote($challenge_info['source_file']).",
			".db_quote($challenge_info['source_name']).",
			".db_quote($challenge_info['input_file']).",
			NOW(),
			".db_quote($challenge_info['runtime_limit']).",
			".db_quote("challenge $challenge_id").")";
		mysql_query($sql)
			or die("SQL error: ".mysql_error());

		notify_worker();
	}
	else // error running the judge's solution;
			// assume that the input was bad....
	{
		$sql = "UPDATE challenge SET status='Invalid input'
			WHERE id=".db_quote($challenge_id);
		mysql_query($sql)
			or die("SQL error: ".mysql_error());
	}
}

function challenge_step3_completed($challenge_id, $job_info)
{
	$sql = "SELECT
			p.output_validator_file AS output_validator_file,
			p.output_validator_name AS output_validator_name,
			c.input_file AS input_file,
			c.expected_file AS expected_file
		FROM challenge c
		JOIN submission s
			ON s.id = c.submission
		JOIN team t
			ON t.team_number=s.team
		JOIN problem p
			ON p.problem_number=s.problem
			AND p.contest=t.contest
		WHERE c.id=".db_quote($challenge_id);
	$query = mysql_query($sql);
	$challenge_info = mysql_fetch_assoc($query)
		or die("invalid challenge $challenge_id");

	// this is the result of running the user's program
	$sql = "UPDATE challenge
			SET output_file=".db_quote($job_info['output_file'])."
			WHERE id=".db_quote($challenge_id);
	mysql_query($sql)
		or die("SQL error: ".mysql_error());

	if ($job_info['result_status'] == 'No Error')
	{
		//
		// evaluate the result
		//

		if ($challenge_info['output_validator_file'])
		{
			$sql = "UPDATE challenge
				SET output_file=".db_quote($job_info['output_file']).",
				status='Evaluating the result'
				WHERE id=".db_quote($challenge_id);
			mysql_query($sql)
				or die("SQL error: ".mysql_error());

			$sql = "INSERT INTO test_job
				(type,source_file,source_name,input_file,expected_file,actual_file,created,callback_data)
				VALUES ('G',
				".db_quote($challenge_info['output_validator_file']).",
				".db_quote($challenge_info['output_validator_name']).",
				".db_quote($challenge_info['input_file']).",
				".db_quote($challenge_info['expected_file']).",
				".db_quote($job_info['output_file']).",
				NOW(),
				".db_quote("challenge $challenge_id").")";
			mysql_query($sql)
				or die("SQL error: ".mysql_error());

			notify_worker();
		}
		else
		{
			if ($job_info['output_file'] == $challenge_info['expected_file'])
			{
				resolve_challenge($challenge_id, "Challenge failed");
			}
			else
			{
				resolve_challenge($challenge_id, "Challenge successful");
			}
		}
	}
	else
	{
		// target user's program generated an error;
		// that means the challenge was successful

		resolve_challenge($challenge_id, "Challenge successful");
	}
}

function challenge_step4_completed($challenge_id, $job_info)
{
	$sql = "SELECT 1
		FROM challenge c
		JOIN submission s
			ON s.id = c.submission
		JOIN team t
			ON t.team_number=s.team
		JOIN problem p
			ON p.problem_number=s.problem
			AND p.contest=t.contest
		WHERE c.id=".db_quote($challenge_id);
	$query = mysql_query($sql);
	$challenge_info = mysql_fetch_assoc($query)
		or die("invalid challenge $challenge_id");

	if ($job_info['result_status'] == 'No Error')
	{
		// the output validator accepted the challenged program's
		// output, thus the challenge failed

		resolve_challenge($challenge_id, "Challenge failed");
	}
	else
	{
		// output validator errored
		resolve_challenge($challenge_id, "Challenge successful");
	}
}

function resolve_challenge($challenge_id, $status)
{
	$sql = "UPDATE challenge
			SET status=".db_quote($status)."
			WHERE id=".db_quote($challenge_id);
	mysql_query($sql)
		or die("SQL error: ".mysql_error());
}


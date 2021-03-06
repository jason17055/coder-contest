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
		system_test_step1_completed($job_info);
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
		else if (preg_match('/^system test (.*)$/', $job_info['callback_data'], $m))
		{
			system_test_step2_completed($m[1], $job_info);
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
	$sql = "SELECT submission,
				s.status AS submission_status
			FROM challenge ch
			JOIN submission s
				ON s.id=ch.submission
			WHERE ch.id =".db_quote($challenge_id);
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$challenge_info = mysql_fetch_assoc($query)
		or die("invalid challenge $challenge_id");

	$sql = "UPDATE challenge
			SET status=".db_quote($status)."
			WHERE id=".db_quote($challenge_id);
	mysql_query($sql)
		or die("SQL error: ".mysql_error());

	if ($challenge_info['submission_status'] == 'Accepted'
			&& $status == 'Challenge successful')
	{
		set_submission_status($challenge_info['submission'], 'Wrong Answer');
	}
}

// This is called after running the submitted program against one
// of the system test files.
// We check whether this problem has an "output validator". If so,
// we schedule an output verification job; otherwise, we require
// an exact match.
//
function system_test_step1_completed($job_info)
{
	// load information about this test
	$sql = "SELECT
			p.output_validator_file AS checker_file,
			p.output_validator_name AS checker_name,
			st.input_file AS input_file,
			st.expected_file AS expected_file
		FROM test_result tr
		JOIN submission s
			ON tr.submission = s.id
		JOIN team t
			ON s.team = t.team_number
		JOIN system_test st
			ON s.problem = st.problem_number
			AND t.contest = st.contest
			AND tr.test_file = st.input_file
		JOIN problem p
			ON p.contest=st.contest
			AND p.problem_number=st.problem_number
		WHERE tr.job=".db_quote($job_info['id']);
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$test_info = mysql_fetch_assoc($query)
		or die("No test record found for job $job_info[id]");

	if ($job_info['result_status'] == 'No Error')
	{
		//
		// evaluate the result
		//

		if ($test_info['checker_file'])
		{
			$sql = "INSERT INTO test_job
			(type,source_file,source_name,input_file,expected_file,actual_file,created,callback_data)
			VALUES ('G',
			".db_quote($test_info['checker_file']).",
			".db_quote($test_info['checker_name']).",
			".db_quote($test_info['input_file']).",
			".db_quote($test_info['expected_file']).",
			".db_quote($job_info['output_file']).",
			NOW(),
			".db_quote("system test $job_info[id]").")";
			mysql_query($sql)
				or die("SQL error: ".mysql_error());
			$check_job_id = mysql_insert_id();

			$sql = "UPDATE test_result
				SET check_job=".db_quote($check_job_id)."
				WHERE job=".db_quote($job_info['id']);
			mysql_query($sql)
				or die("SQL error: ".mysql_error());

			notify_worker();
		}
		else
		{
			if ($job_info['output_file'] == $test_info['expected_file'])
			{
				resolve_system_test($job_info['id'], "Correct");
			}
			else
			{
				resolve_system_test($job_info['id'], "Wrong Answer");
			}
		}
	}
	else
	{
		resolve_system_test($job_info['id'], $job_info['result_status']);
	}
}

// This is called after running a custom output checker against the
// results of a system test.
//
function system_test_step2_completed($orig_job_id, $job_info)
{
	if ($job_info['result_status'] == 'No Error')
	{
		resolve_system_test($orig_job_id, 'Correct');
	}
	else
	{
		resolve_system_test($orig_job_id, 'Wrong Answer');
	}
}

function resolve_system_test($tr_job_id, $result_status)
{
	//find and update the system test that created this job
	$sql = "UPDATE test_result
		SET result_status=".db_quote($result_status)."
		WHERE job=".db_quote($tr_job_id);
	mysql_query($sql)
		or die("SQL error: ".mysql_error());

	// check whether a preliminary judgment can be made for
	// this problem

	$sql = "SELECT
			st.autojudge AS autojudge,
			s.status AS status,
			s.id AS submission_id
		FROM test_result tr
		JOIN submission s
			ON s.id=tr.submission
		JOIN team t
			ON t.team_number=s.team
		JOIN system_test st
			ON st.contest=t.contest
			AND st.problem_number=s.problem
			AND st.input_file=tr.test_file
		WHERE tr.job=".db_quote($tr_job_id);
	$query = mysql_query($sql)
		or die("SQL error: ".mysql_error());
	$info = mysql_fetch_assoc($query)
		or die("submission not found");

	if ($info['autojudge'] == 'Y' && !$info['status'])
	{
		if ($result_status == 'Correct')
		{
			//
			// This test showed a CORRECT answer. Check whether there are
			// any autojudge tests still remaining that haven't been shown
			// correct. If there are none left, then report the submission
			// status as ACCEPTED.
			//

			$sql = "SELECT COUNT(*) AS autojudge_count,
					COUNT(IF(tr.result_status='Correct',1,NULL)) AS correct_count
				FROM submission s
				JOIN team t
					ON t.team_number=s.team
				JOIN system_test st
					ON st.contest=t.contest
					AND st.problem_number=s.problem
				JOIN test_result tr
					ON tr.submission=s.id
					AND tr.test_file=st.input_file
				WHERE s.id=".db_quote($info['submission_id'])."
				AND st.autojudge='Y'";
			$query=mysql_query($sql)
				or die("SQL error: ".mysql_error());
			$counts = mysql_fetch_assoc($query);

			if ($counts['autojudge_count'] >= 1
				&& $counts['correct_count'] == $counts['autojudge_count'])
			{
				set_submission_status($info['submission_id'], "Accepted");
			}
		}
		else
		{
			//
			// This test showed an INCORRECT answer, so immediately
			// report back to the contestant the result of their
			// submission.
			//

			set_submission_status($info['submission_id'], $result_status);

		}
	}

	wakeup_listeners();
}

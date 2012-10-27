prefix = /srv/contest/htdocs

script_files = \
	includes/auth.php \
	includes/skin.php \
	includes/functions.php \
	includes/borrowed.inc.php \
	includes/js_quote.php \
	includes/notify.php \
	includes/test_workflow.php \
	worker/feed.php \
	worker/script/worker.pl \
	answer_clarification.php \
	clarification.php \
	new_challenge.php \
	controller.php \
	issue_credentials.php \
	open_problem.php \
	problem.php \
	problems.php \
	scoreboard.php \
	show_problem_clarifications.inc.php \
	show_solutions.inc.php \
	solution.php \
	submission.php \
	submit_test.php \
	team_menu.php

data_files = \
	scoreboard.css

all:

install:
	for f in $(script_files) $(data_files); do \
		install $$f $(prefix)/$$f; \
	done

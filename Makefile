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
	index.php \
	issue_credentials.php \
	listsubmissions.php \
	login.php \
	open_problem.php \
	problem.php \
	problems.php \
	result.php \
	scoreboard.php \
	show_my_submissions.inc.php \
	show_problem_clarifications.inc.php \
	show_problems_list.inc.php \
	show_solutions.inc.php \
	show_unjudged_submissions.inc.php \
	solution.php \
	submission.php \
	submit.php \
	submit_test.php \
	team_menu.php \
	user.php \
	users.php \
	user_test.php

data_files = \
	popup_message.js \
	scoreboard.css \
	scoreboard_images/eye4.png \
	scoreboard_images/green_flag.png \
	scoreboard_images/pencil.png

all:

install:
	for f in $(script_files) $(data_files); do \
		install $$f $(prefix)/$$f; \
	done

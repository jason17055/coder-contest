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
	worker/index.php \
	worker/script/worker.pl \
	announcement.php \
	answer_clarification.php \
	checkmessage-js.php \
	clarification.php \
	contest.php \
	controller.php \
	credentials_handouts.inc.php \
	diff.php \
	file.php \
	index.php \
	issue_credentials.php \
	job_result.php \
	listsubmissions.php \
	login.php \
	make_contest_archive.php \
	new_challenge.php \
	newclarification.php \
	open_problem.php \
	problem.php \
	problems.php \
	result.php \
	scoreboard.php \
	sendmessage.php \
	show_my_submissions.inc.php \
	show_problem_clarifications.inc.php \
	show_problems_list.inc.php \
	show_solutions.inc.php \
	show_unjudged_submissions.inc.php \
	solution.php \
	submission.php \
	submit.php \
	submit_test.php \
	sysadmin.php \
	system_test.php \
	team_menu.php \
	test_result.php \
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

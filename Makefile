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
	open_problem.php
	

all:

install:
	for f in $(script_files); do \
		install $$f $(prefix)/$$f; \
	done

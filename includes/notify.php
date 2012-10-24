<?php

define('NOTIFY_BIN', '/usr/local/bin/jason_notify');
define('USERS_LOCK_FILE', '/tmp/jlong_scoreboard.notify');
define('WORKERS_LOCK_FILE', '/tmp/jlong_scoreboard_workers.notify');

function wakeup_listeners()
{
	//$x = file_get_contents('http://home.messiah.edu:12626/notify');
	system(NOTIFY_BIN . ' --notify ' . USERS_LOCK_FILE);
}

function wait_for_event($timeout = 60)
{
	system(NOTIFY_BIN . " --timeout=$timeout --wait " . USERS_LOCK_FILE);
}

function notify_worker()
{
	system(NOTIFY_BIN . ' --notify ' . WORKERS_LOCK_FILE);
}

function wait_worker($timeout = 60)
{
	system(NOTIFY_BIN . " --timeout=$timeout --wait " . WORKERS_LOCK_FILE);
}

<?php

require_once('config.php');
require_once('includes/auth.php');

require_auth();
if (is_sysadmin())
{
	header("Location: $app_url/sysadmin.php");
}
else if ($director_info = is_a_director())
{
	$contest_id = $director_info['contest'];
	$url = "controller.php?contest=".urlencode($contest_id);
	header("Location: $url");
}
else if ($judge_info = is_a_judge())
{
	header("Location: team_menu.php");
}
else if (is_a_team())
{
	header("Location: team_menu.php");
}
else
{
	header("Location: scoreboard.php");
}

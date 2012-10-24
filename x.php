<?php
$static_files_url = '/contestfiles';
if (preg_match('!^(/([^/]+))?/([^/]+)$!', $_SERVER['PATH_INFO'], $a))
{
	if ($a[1] && !$_REQUEST['contest'])
	{
		$_GET['contest'] = $a[2];
		$_REQUEST['contest'] = $a[2];
	}
	require($a[3]);
	exit();
}
else if ($_SERVER['PATH_INFO'] == '/')
{
	header("Location: login.php");
}
else
{
	die("invalid URL<!-- $_SERVER[PATH_INFO] -->");
}

<?php

$path = $_SERVER['PATH_INFO'];
$parts = explode("/", ltrim($path, "/"));

if (!preg_match("/^[0-9a-f]+$/", $parts[0]))
{
	die("invalid file hash $parts[0]");
}
$file = "uploaded_files/$parts[0].txt";

$content = file_get_contents($file);
if (is_null($content))
{
	header("HTTP/1.0 404 Not Found");
	exit();
}

$content_type = 'text/plain';
if (preg_match('/\\.pdf$/', $parts[1]))
{
	$content_type = 'application/pdf';
}
if (preg_match('/\\.html$/', $parts[1]))
{
	$content_type = 'text/html';
}

header("Content-Type: $content_type");
if ($_GET['show_line_numbers'])
{
	$lines = explode("\n", $content);
	$count = 0;
	foreach ($lines as $l)
	{
		$count++;
		echo "$count: $l\n";
	}
}
else
{
	echo $content;
}

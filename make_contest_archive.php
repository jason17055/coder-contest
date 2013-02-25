<?php

require_once('config.php');
require_once('includes/skin.php');
require_once('includes/auth.php');
require_once('includes/functions.php');

require_auth();
is_director($_REQUEST['contest'])
	or die("Error: not authorized");

$contest_id = $_REQUEST['contest'] ?: 1;
$contest_info = get_basic_contest_info($contest_id);

if ($_SERVER['REQUEST_METHOD'] != 'GET') {
	die("not implemented");
}

$zipfile = tempnam("tmp","zip");
$zip = new ZipArchive();
$zip->open($zipfile, ZIPARCHIVE::OVERWRITE)
	or die("zip creation failed");

$sql = "SELECT problem_number,problem_name,spec_file,spec_name,solution_file,solution_name
	FROM problem
	WHERE contest=".db_quote($contest_id)."
	ORDER BY problem_number";
$query = mysql_query($sql);
while ($problem_info = mysql_fetch_assoc($query))
{
	$name = $problem_info['problem_name'];
	$name = preg_replace('/[^A-Za-z ._0-9]/', '-', $name);

	if ($problem_info['spec_file']) {
		$zip->addFile("uploaded_files/$problem_info[spec_file].txt",
				"$name/$problem_info[spec_name]")
			or die("addFile error");
	}
	if ($problem_info['solution_file']) {
		$zip->addFile("uploaded_files/$problem_info[solution_file].txt",
				"$name/$problem_info[solution_name]")
			or die("addFile error");
	}

	$sql = "SELECT test_number,input_file,expected_file
		FROM system_test
		WHERE contest=".db_quote($contest_id)."
		AND problem_number=".db_quote($problem_info['problem_number'])."
		ORDER BY test_number";
	$query2 = mysql_query($sql);
	while ($row = mysql_fetch_assoc($query2))
	{
		if ($row['input_file']) {
			$zip->addFile("uploaded_files/$row[input_file].txt",
				"$name/input$row[test_number].txt")
			or die("addFile error");
		}
		if ($row['expected_file']) {
			$zip->addFile("uploaded_files/$row[expected_file].txt",
				"$name/output$row[test_number].txt")
			or die("addFile error");
		}
	}
}
$zip->close();

header("Content-Type: application/zip");
header("Content-Length: ".filesize($zipfile));
header("Content-Disposition: attachment; filename=\"problems.zip\"");
readfile($zipfile);
unlink($zipfile);
echo "ok ";
echo time();

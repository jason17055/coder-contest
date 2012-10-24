<?php

require_once('config.php');

$sql = "SELECT * FROM $schema.test_job
	WHERE id=".db_quote($_REQUEST['id']);
$result = mysql_query($sql);
$job_info = mysql_fetch_assoc($result)
	or die("Error job $_REQUEST[id] not found");

echo "<pre>".htmlspecialchars($job_info['result_detail'])."</pre>";

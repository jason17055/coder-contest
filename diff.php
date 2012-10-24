<?php

$f1_hash = $_REQUEST['f1'];
if ($f1_hash && !preg_match("/^[0-9a-f]+$/", $f1_hash))
	die("invalid file hash $f1_hash");

$f2_hash = $_REQUEST['f2'];
if (!preg_match("/^[0-9a-f]+$/", $f2_hash))
	die("invalid file hash $f2_hash");

$dir = "uploaded_files";
if ($f1_hash)
{
	$fp1 = popen("diff $dir/$f1_hash.txt $dir/$f2_hash.txt", "r")
		or die("cannot open diff");
}
$fp2 = fopen("$dir/$f2_hash.txt", "r")
	or die("cannot open $f2_hash.txt");

?><!DOCTYPE HTML>
<html>
<body>
<style type="text/css">
.o div { border-left: 3px solid #4f4; padding-left: 6px;
	white-space: pre;
	font-family: monospace; }
.o div.xxx { border-left: 3px solid #f00; color: #c00;}
.o div.missing { border-left: 3px solid #fff;
	color: #f00; font-style: italic;
	font-family: serif; }
.o div.hilited { background-color: #dfd; }
.o div.xxx.hilited { background-color: #fdd; }
.o div.xxx.hilited span { background-color: #fbb; }
.o div.missing.hilited { background-color: #fdd; }
</style>
<div class='o'><?php

$diffs = array();
$line_count = 1;
while ($fp1 && !feof($fp1))
{
	$l = rtrim(fgets($fp1),"\n");
	if (preg_match('/^(\d+)(?:,(\d+))?(a|c|d)(\d+)(?:,(\d+))?/', $l, $m))
	{
		$exp_start = $m[1];
		$exp_end = $m[2] ?: $m[1];
		$type = $m[3];
		$act_start = $m[4];
		$act_end = $m[5] ?: $m[4];

		if ($type == 'a')
		{
			$exp_start++;
			$exp_end++;
		}
		if ($type == 'd')
			$act_start++;

		$diffs[] = array(
			'type' => $type,
			'lhs_start' => $exp_start,
			'lhs_end' => $exp_end,
			'rhs_start' => $act_start,
			'rhs_end' => $act_end
			);

		while ($act_start > $line_count && !feof($fp2))
		{
			$ll = rtrim(fgets($fp2),"\n");
			echo "<div id='line$line_count'>".htmlspecialchars($ll)."&nbsp;</div>";
			$line_count++;
		}

		if ($type == 'd')
		{
			$c = $exp_end - $exp_start + 1;
			$cap = $c == 1 ? "line" : "lines";
			echo "<div class='missing' id='line${line_count}m'>*** $c $cap missing here ***</div>";
		}
		else
		{
		while ($line_count <= $act_end && !feof($fp2))
		{
			$ll = rtrim(fgets($fp2),"\n");
			echo "<div id='line$line_count' class='xxx'><span>".htmlspecialchars($ll)."</span>&nbsp;</div>";
			$line_count++;
		}
		}
	}
}

while (!feof($fp2))
{
	$l = fgets($fp2);
	echo "<div id='line$line_count'>".htmlspecialchars($l).'</div>';
	$line_count++;
}

pclose($fp1);
fclose($fp2);

?></div>
<script type="text/javascript"><!--
var diffInfo = <?php
		echo json_encode($diffs)?>;
var hilited = null;

function hiliteLines(begin,end)
{
	if (hilited)
	{
		for (var i in hilited)
		{
			var el = document.getElementById(hilited[i]);
			el.className = el.className.replace(/\s*hilited/g, '');
		}
	}

	var topPixel = -1;
	var bottomPixel = -1;

	var findPos = function(obj) {
		var curleft = curtop = 0;
		if (obj.offsetParent) {
			while (obj)
			{
				curleft += obj.offsetLeft;
				curtop += obj.offsetTop;
				obj = obj.offsetParent;
			}
		}
		return curtop;
	};

	var addHilite = function(elId)
	{
		var el = document.getElementById(elId);
		if (el)
		{
			el.className += ' hilited';
			hilited.push(elId);

			var p0 = findPos(el);
			var p1 = p0 + el.offsetHeight;
			if (topPixel == -1 || p0 < topPixel) {
				topPixel = p0;
			}
			if (bottomPixel < p1) {
				bottomPixel = p1;
			}
		}
	};

	hilited = new Array();
	for (var i = begin; i <= end; i++)
	{
		addHilite('line'+i);
	}
	if (end < begin)
	{
		addHilite('line'+begin+'m');
	}

	var winHeight;
	if (window.innerHeight) {
		winHeight = window.innerHeight;
	} else if (document.body) {
		winHeight = document.body.clientHeight;
	}

	var ofset = winHeight / 2 - (bottomPixel - topPixel) / 2;
	if (ofset < 0)
		window.scrollTo(0, topPixel);
	else if (topPixel - ofset < 0)
		window.scrollTo(0, 0);
	else
		window.scrollTo(0, topPixel - ofset);
}

if (window.parent.reportDiffInformation)
{
	window.parent.reportDiffInformation(diffInfo);
}
//--></script>
</body>
</html>

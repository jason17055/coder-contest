<?php

function js_quote($str)
{
	if (is_null($str))
	{
		return "null";
	}
	else
	{
		$str = str_replace("\\", "\\\\", $str);
		$str = str_replace("\"", "\\\"", $str);
		$str = str_replace("\n", "\\n", $str);

		return "\"$str\"";
	}
}


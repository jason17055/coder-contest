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
	for (var i = begin; i < end; i++)
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

function setDiffInfo(diffInfo)
{
	console.log("got diff info " +diffInfo.length+" elements");
	if (window.parent.reportDiffInformation)
	{
		window.parent.reportDiffInformation(diffInfo);
	}
}

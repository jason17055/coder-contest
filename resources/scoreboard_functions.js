function testNewSubmission()
{
	var $r = $('#submissions_table tr.aTemplate').clone();
	$r.removeClass('aTemplate');
	$('.submitted_column', $r).text('foo');
	$('#submissions_table').append($r);
}

function initializeClock()
{
	var $clock = $('.contest_clock .time_left');
	var timeLeft = $clock.attr('time-left');
	if (!timeLeft)
		return;

	var whenTerminated = new Date().getTime() + 1000*timeLeft;
	var pad2 = function(s) {
		s = ""+s;
		if (s.length < 2) {
			return "0"+s;
		} else {
			return s;
		}
	};

	var updateClock;
	updateClock = function() {
		var timeLeft = whenTerminated - new Date().getTime();
		if (timeLeft <= 0)
		{
			$clock.text('00:00:00');
		}
		else
		{
			var t = Math.floor(timeLeft/1000);
			$clock.text(pad2(Math.floor(t/3600))+":"+
					pad2(Math.floor(t/60)%60)+":"+
					pad2(t%60));
			var delay = timeLeft - (t*1000) + 50;
			setTimeout(updateClock, delay);
		}
	};

	updateClock();
}

$(initializeClock);

//////////////////////////////////////////////////////////////////////////////
/// Hilight element if one is selected by the url
///
$(function() {
	if (location.hash) {
		var el = document.getElementById(location.hash.substring(1));
		if (el) {
			flashElement(el, 0);
		}
		else {
			console.log('could not find '+location.hash);
		}
	}
});

function flashElement(el, count)
{
	if (count % 2 == 0) {
		$(el).addClass('hilight');
		$(el).removeClass('hilight_out');
	}
	else {
		$(el).addClass('hilight_out');
		$(el).removeClass('hilight');
	}
	if (count < 6) {
		setTimeout(function() { flashElement(el,count+1); }, 500);
	}
}

//////////////////////////////////////////////////////////////////////
// BEGIN CODE ORIGINALLY IN submission.php
//
var lastInputFileUrl;
function showTestResult(inputFile, outputFile)
{
	lastInputFileUrl = inputFile;
	document.getElementById('testResult_Input').contentWindow.location.href = inputFile;
	document.getElementById('testResult_Output').contentWindow.location.href = outputFile;
	$('#customTestDiv').hide();
	$('#testResultDiv').show();
	onResize();
}

function onResize()
{
	var $i = $('#testResult_Input');
	if ($i.length) {
		return onResize_editSubmissionPage();
	}
}

function onResize_editSubmissionPage()
{
	var $i = $('#testResult_Input');
	var $o = $('#testResult_Output');
	var y = $o.offset().top - $i.height();

	var slack = window.innerHeight - y;
	var idealHeight = Math.floor(slack / 2) - 13;
	idealHeight = idealHeight < 100 ? 100 : idealHeight;
	document.getElementById('testResult_Input').height = idealHeight;
	document.getElementById('testResult_Output').height = idealHeight;

	var w = Math.floor(window.innerWidth * .4 - 10);
	document.getElementById('testResult_Input').width = w;
	document.getElementById('testResult_Output').width = w;

	var w = Math.floor(window.innerWidth * .6 - 60);
	document.getElementById('source_code_iframe').width = w;

	var h = window.innerHeight - $('#source_code_iframe').offset().top - 25;
	document.getElementById('source_code_iframe').height = h;
}
$(onResize);
window.onresize = onResize;

function doCustomTest()
{
	$('#testResultDiv').hide();
	if (!lastInputFileUrl)
	{
		$('#customTestDiv').show();
		return;
	}

	var onSuccess = function(data, tStatus)
	{
		document.customTestForm.input_content.value = data;
		$('#customTestDiv').show();
	};

	$.ajax({
		url: lastInputFileUrl,
		success: onSuccess,
		dataType: 'text'
		});
}
//
//////////////////////////////////////////////////////////////////////

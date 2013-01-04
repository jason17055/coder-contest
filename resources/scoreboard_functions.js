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
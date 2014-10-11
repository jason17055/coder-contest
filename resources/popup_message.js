function doReload()
{
	window.location.reload();
}
var count = 0;
var last_message_id = sessionStorage.getItem("lastmessage");
var current_announcement = null;
var orig_title = null;
var blinking = false;

function blinkTitle()
{
	if (orig_title == null)
	{
		orig_title = document.title;
		document.title = "You have a message";
	}
	else
	{
		document.title = orig_title;
		orig_title = null;
	}

	if (orig_title != null || current_announcement != null)
	{
		setTimeout("blinkTitle()",1000);
	}
	else
	{
		blinking = false;
	}
}

function startBlinking()
{
	if (!blinking)
	{
		blinking = true;
		blinkTitle();
	}
}

function openAnnouncement()
{
	if (current_announcement != null
		&& current_announcement.url != null)
	{
		sessionStorage.setItem("lastmessage", current_announcement.message_id);
		window.location = current_announcement.url;
	}
}

function dismissAnnouncement()
{
	if (current_announcement != null)
	{
		sessionStorage.setItem("lastmessage", current_announcement.message_id);
		last_message_id = current_announcement.message_id;
		current_announcement = null;
	}

	var andThen = checkForAnnouncement;
	if ($('form').length == 0)
	{
		andThen = function() { window.location.reload(); };
	}
	$("#announcementPopup").fadeOut(1000, andThen);
}
function displayAnnouncement(data)
{
	current_announcement = data;
	var delayThenHide = function() {
		startBlinking();
		if (data.duration > 0)
			setTimeout("dismissAnnouncement()", data.duration*1000);
	};
	$("#announcementContent").html(data.message);
	document.getElementById('announcementOpenBtn').style.display =
		data.url != null ? 'inline' : 'none';

	$("#announcementPopup").fadeIn(1000, delayThenHide);
}
function checkForAnnouncement()
{
	var d = new Date();
	var startTime = d.getTime();

	var callback = function(data, textStatus, xhr)
	{
		if (data.message != null || data['class'] == 'message')
		{
			displayAnnouncement(data);
		}
		else if (data['class'] == 'job_completion')
		{
			location.reload();
		}
		else if (data['class'] == 'online_status_change')
		{
			location.reload();
		}
		else if (data['class'] == 'new_submission')
		{
			localStorage.setItem('ringaling', 'yes');
			location.href = '.';
		}
		else if (data['class'])
		{
			alert("not implemented: event " + data['class']);
		}
		else
		{
			var targetTime = startTime + 30000;
			var curTime = new Date().getTime();
			var delay = targetTime - curTime;
			if (delay < 1) { delay = 1; }

			setTimeout("checkForAnnouncement()", delay);
		}
	};
	var onError = function(jqxhr, status, err)
	{
		// Note- with Chrome (and possibly other browsers as well)
		// this Error handler gets called simply because the user
		// clicked on a link and as a result this AJAX call had to
		// be aborted.

		// but assuming that's not the reason, try again in 30 seconds
		setTimeout(function() {
			checkForAnnouncement();
			}, 30000);
	};

	var xtra = "";
	var job_incomplete_indicators = new Array();
	$('.job-incomplete-indicator').each(function(idx)
		{
			var id = this.id;
			if (id.match(/^ind_job_/))
			{
				job_incomplete_indicators.push(id.substr(8));
			}
		});
	if (job_incomplete_indicators.length)
	{
		xtra += "&jobcompletion=" + escape(job_incomplete_indicators.join(","));
	}

	var online_indicators = new Array();
	$('.online-indicator').each(function(el)
		{
			var id = this.id;
			if (id.match(/^ind_online_/))
			{
				online_indicators.push(id.substr(11));
			}
		});
	if (online_indicators.length)
	{
		xtra += "&onlinestatuschange=" + escape(online_indicators.join(","));
	}

	var process_submissions_table = function(el) {
		var items = [];
		$('[data-submission-id]', $(el)).each(function(i,el2)
			{
				var it = $(el2).attr('data-submission-id');
				items.push(it);
			});
		xtra += '&newsubmissionsafter='+escape(items.join(','));
	};

	$('.auto_reload_trigger').each(function(xx)
	{
		var typ = $(this).attr('data-auto-reload-type');
		if (typ == 'submissions_table') {
			process_submissions_table(this);
		}
	});

	var url_base = $('body').attr('data-checkmessage-url');
	var url = url_base+"?timeout=60&type=N" + xtra;
	if (last_message_id) {
		url += '&dismiss_message='+escape(last_message_id);
	}
	$.ajax({
		url: url,
		dataType: 'json',
		success: callback,
		error: onError
		});
}
$(checkForAnnouncement);

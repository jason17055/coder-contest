function doReload()
{
	window.location.reload();
}
var count = 0;
var last_message_id = sessionStorage.getItem("lastmessage");
if (last_message_id == null) {
	last_message_id = 0;
	sessionStorage.setItem("lastmessage", last_message_id);
}
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
	if ($('[page-reload-safe]').length)
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

	$('#submissions_table').each(function(el)
	{
		var lastSubmission = null;
		var lastClarification = null;
		$('tr', this).each(function(i,el2)
		{
			if (el2.id && el2.id.match(/clarification/))
				lastClarification = el2.id;
			else if (el2.id && el2.id.match(/submission/))
				lastSubmission = el2.id;
		});
		var p = new Array();
		if (lastSubmission) { p.push(lastSubmission); }
		if (lastClarification) { p.push(lastClarification); }
		xtra += '&newsubmissionsafter='+escape(p.join(','));
	});

	var url = "checkmessage-js.php?timeout=60&type=N&after=" + escape(last_message_id) + xtra;
	//url = 'http://home.messiah.edu:12626/wait?type=N&after='+escape(last_message_id)+xtra;
	$.ajax({
		url: url,
		dataType: 'json',
	//	headers: {
	//		cookie: document.cookie
	//		},
	//	xhrFields: { withCredentials: true },
		success: callback,
		error: onError
		});
	//jQuery.getJSON(url, null, callback);
}
$(checkForAnnouncement);
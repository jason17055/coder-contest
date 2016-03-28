function doReload()
{
	window.location.reload();
}
var count = 0;
var current_announcement = null;
var orig_title = null;
var blinking = false;
var pageLoadTime = new Date().getTime();
window.pageChecker = {
	'pageSpecific': null
	};

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
		var last_message_id = current_announcement.message_id;
		sessionStorage.setItem("lastmessage", current_announcement.message_id);
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

	if (data.message_date) {
		var d = new Date(data.message_date);
		var age = (new Date().getTime() - d.getTime())/1000;
		if (age < 180) {
			$('#announcementAge').hide();
		}
		else if (age < 90*60) {
			var minutes = Math.round(age/60);
			$('#announcementAge').text(minutes+' minutes ago');
			$('#announcementAge').show();
		}
		else if (age < 40*60*60) {
			var hours = Math.round(age/(60*60));
			$('#announcementAge').text(hours + ' hours ago');
			$('#announcementAge').show();
		}
		else {
			var days = Math.round(age/86400);
			$('#announcementAge').text(days + ' days ago');
			$('#announcementAge').show();
		}
	}
	else {
		$('#announcementAge').hide();
	}

	$("#announcementTitleBar").text(
			data.messagecount > 1 ? "Message (1 of "+data.messagecount+")" :
			"Message");
	$("#announcementPopup").fadeIn(1000, delayThenHide);
}
function checkPageSpecificActivity(andThen)
{
	if (window.pageChecker.pageSpecific) {
		var fn = window.pageChecker.pageSpecific;
		fn(andThen);
	}
	else {
		andThen();
	}
}
function checkForAnnouncement()
{
	var d = new Date();
	var startTime = d.getTime();
	var curInterval = (startTime - pageLoadTime < 30000) ? 1000 :
		(startTime - pageLoadTime < 300000) ? 5000 :
		25000;

	var callback = function(data, textStatus, xhr)
	{
		if (data.message != null || data['class'] == 'message')
		{
			displayAnnouncement(data);
		}
		else if (data['class'] == 'dismissed_message')
		{
			console.log('confirmed dismissal of '+data.message_id);
			sessionStorage.removeItem("lastmessage");
			checkForAnnouncement();
		}
		else if (data['class'] == 'job_completed' ||
			data['class'] == 'test_result_completed' ||
			data['class'] == 'online_status_change')
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
			var targetTime = startTime + curInterval;
			var curTime = new Date().getTime();
			var delay = targetTime - curTime;
			if (delay < 1) { delay = 1; }

			var andThen = function() {
				setTimeout("checkForAnnouncement()", delay);
			};
			checkPageSpecificActivity(andThen);
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

	var assertion_tags = [];
	$('.job-incomplete-indicator').each(function(idx,el)
		{
			var job_id = el.getAttribute('data-job-id');
			assertion_tags.push("jobcompletion="+job_id);
		});

	$('.test-result-incomplete-indicator').each(function(idx,el)
		{
			var test_result_id = el.getAttribute('data-test-result-id');
			assertion_tags.push("testresultcompletion="+test_result_id);
		});

	$('.test-result-status').each(function(idx, el)
		{
			var test_result_id = el.getAttribute('data-test-result-id');
			var cur_status = el.getAttribute('data-test-result-status');
			assertion_tags.push("testresultstatus="+test_result_id+"//"+cur_status);
		});

	$('.online-indicator').each(function(idx,el)
		{
			var id = this.id;
			var user = el.getAttribute('data-user');
			var online = el.getAttribute('data-user-online');
			assertion_tags.push("useronline="+user+","+online);
		});

	var process_submissions_table = function(el) {
		var items = [];
		$('[data-submission-id]', $(el)).each(function(i,el2)
			{
				var it = $(el2).attr('data-submission-id');
				items.push(it);
			});
		assertion_tags.push("newsubmissionafter="+items.join(','));
		console.log('found submissions ' + items.join(','));
	};

	$('.auto_reloading').each(function(xx)
	{
		var typ = $(this).attr('data-auto-reload-type');
		if (typ == 'submissions_table') {
			process_submissions_table(this);
		}
	});

	var url_base = $('body').attr('data-checkmessage-url');
	var url = url_base+"?timeout=60&type=N";
	var post = {
		assertions: assertion_tags
		};

	var last_message_id = sessionStorage.getItem("lastmessage");
	if (last_message_id) {
		console.log('requesting dismissal of message '+last_message_id);
		url += '&dismiss_message='+escape(last_message_id);
	}
	console.log('checking '+url);
	$.ajax({
		type: "POST",
		url: url,
		data: JSON.stringify(post),
		contentType: "application/json; charset=utf-8",
		dataType: 'json',
		success: callback,
		error: onError
		});
}
$(checkForAnnouncement);

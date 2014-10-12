function doReload() {
	window.location.reload();
}
function hideAnnouncement() {
	$('#announcementPopup').fadeOut(1000, doReload);
}
function displayAnnouncement(data)
{
	var myDelay = data.duration != 0 ? data.duration : 15;
	var delayThenHide = function() {
		setTimeout("hideAnnouncement()", myDelay*1000);
	};
	$('#announcementContent').html(data.message);
	sessionStorage.setItem('lastannouncement', data.message_id);

	if (data.messagetype == 'S') {
		if (data.fanfare) {
			document.getElementById('announcementAudio').play();
		}
		document.getElementById('announcementImage').style.visibility = 'visible';
	}
	else {
		document.getElementById('announcementImage').style.visibility = 'hidden';
	}

	$('#announcementPopup').fadeIn(2500, delayThenHide);
}

var origTime = new Date().getTime();
function checkForAnnouncement()
{
	var startTime = new Date().getTime();
	if (startTime - origTime > 180000) { return doReload(); }

	var callback = function(data, textStatus, xhr)
		{
			if (data.message != null) {
				displayAnnouncement(data);
			}
			else {
				var nextTime = startTime + 10000;
				var curTime = new Date().getTime();
				var delay = nextTime - curTime;
				if (delay < 1) { delay = 1; }
				setTimeout("checkForAnnouncement()", delay);
			}
		};

	var url_base = $('body').attr('data-checkmessage-url');
	var url = url_base + "?type=S"
		+ "&after=" + escape(last_message_id)
		+ '&contest=' + escape(contest);
	jQuery.getJSON(url, null, callback);
}

setTimeout("checkForAnnouncement()", 1000);

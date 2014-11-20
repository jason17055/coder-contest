<!DOCTYPE HTML>
<html>
<head>
<title>Programming Contest: [% page_title |html %]</title>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" type="text/css" href="[%resources_prefix|html%]/scoreboard.css">
<link rel="stylesheet" type="text/css" href="[%resources_prefix|html%]/CodeMirror-2.33/lib/codemirror.css">
<script type="text/javascript" src="[%resources_prefix|html%]/jquery-latest.min.js"></script>
<script type="text/javascript" src="[%resources_prefix|html%]/scoreboard_functions.js"></script>
<script type="text/javascript" src="[%resources_prefix|html%]/popup_message.js"></script>
[% FOREACH f IN javascript_files %]
<script type="text/javascript" src="[%"$resources_prefix/$f"|html%]"></script>
[% END %]
</head>
<body class="adminpage" data-checkmessage-url="[% ajax_url|html %]/checkmessage">
<div id="top_banner" style="border: 1px solid green">
<h1>Programming Contest</h1>

<div id="sessionctl">
[% IF session.username %]
<div>[% session.username |uc |html %]</div>
<div><a href="[% contest_links.logout |html %]">Logout</a></div>
[% ELSE %]
<div><a href="[% contest_links.login |html %]">Login</a></div>
[% END %]
</div>
[% IF contest.current_phase_name %]
[% INCLUDE "countdown_clock.inc" %]
[% END %]


<div style="clear:both"></div>
</div>
<div id="contentarea">
<div id="announcementPopup">
<div style="text-align:center" id="announcementTitleBar">Message</div>
<div id="announcementContent">&nbsp;</div>
<div id="announcementAge"></div>
<div id="announcementButtons">
<button type="button" id="announcementOpenBtn" onclick="openAnnouncement()">Open</button>
<button type="button" onclick="dismissAnnouncement()">Dismiss</button>
</div>
</div>
[% UNLESS notitle %]
<h2>[% page_title |html %]</h2>
[% END %]

[% content %]

</div><!-- end contentarea -->
</body>
</html>

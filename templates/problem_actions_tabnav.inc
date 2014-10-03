<div id="problem_action_buttons_bar">
<ul class="tabnav">
<li[% show_mode=='description' ? ' class="selected"' : '' %]><a href="[% problem.url |html %]description">Problem</a></li>
<li[% show_mode=='clarifications' ? ' class="selected"' : '' %]><a href="[% problem.url |html %]clarifications">Clarifications</a></li>
<li[% show_mode=='write' ? ' class="selected"' : '' %]><a href="[% problem.url |html %]write">Write Code</a></li>
<li[% show_mode=='test' ? ' class="selected"' : '' %]><a href="[% problem.url |html %]test">Test</a></li>
<li[% show_mode=='submit' ? ' class="selected"' : '' %]><a href="[% problem.url |html %]submit">Submit</a></li>
<li[% show_mode=='solutions' ? ' class="selected"' : '' %]><a href="[% problem.url |html %]solutions">Solutions</a></li>
</ul>
<button type="button" onclick="doClose()">Close</button>
<script language="javascript"><!--
function doClose() {
	window.location.href = '..';
}
//--></script>
<script type="text/javascript"><!--

function adjustContentContainerSize(cEl)
{
	if (!cEl)
		return;

	var h = window.innerHeight;
	var y = 0;
	var el = cEl;
	while (el.offsetParent)
	{
		y += el.offsetTop;
		el = el.offsetParent;
	}

	var desiredHeight = h-y-17;
	if (desiredHeight > 360)
	{
		$(cEl).css({height: desiredHeight+'px' });
	}
}
$(function() {

adjustContentContainerSize( document.getElementById('main_content_container') );
if ($('.CodeMirror-scroll').length != 0)
{
	adjustContentContainerSize( $('.CodeMirror-scroll').get(0) );
}

});

//--></script>
</div>

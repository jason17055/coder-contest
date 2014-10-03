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
</div>

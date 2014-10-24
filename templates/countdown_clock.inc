[% IF contest.current_phase_name %]
<div class="contest_clock">
	<div class="contest_clock_phase">[% contest.current_phase_name |html %]</div>
	<div class="time_left" time-left="[% contest.current_phase_timeleft |html %]"></div>
</div>
[% END #if current_phase_name %]

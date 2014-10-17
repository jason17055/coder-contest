[% css_class = result.correct ? 'correct' :
	result.incorrect_submissions ? 'attempted' : '' %]
<td class="pcolumn [% css_class %]">
[% IF result.incorrect_submissions > 2 %]
	<img src="[% images_url %]/scoreboard/bug.png" height="18" alt="X">x[% result.incorrect_submissions |html %]
[% ELSIF result.incorrect_submissions == 2 %]
	<img src="[% images_url %]/scoreboard/bug.png" height="18" alt="X">
	<img src="[% images_url %]/scoreboard/bug.png" height="18" alt="X">
[% ELSIF result.incorrect_submissions == 1 %]
	<img src="[% images_url %]/scoreboard/bug.png" height="18" alt="X">
[% END # if nonzero incorrect_submissions %]
[% IF result.correct %]
	<img src="[% images_url %]/scoreboard/balloon_red.png" width="14" height="24" alt="">
	[% result.minutes |html %]
[% END # if correct %]
</td>

<table class="realtable auto_reloading" data-auto-reload-type="submissions_table">
<tr>
<th>Submitted</th>
<th>Problem</th>
<th>Type</th>
<th>Contestant</th>
<th>Judge</th>
<th>Response</th>
</tr>
[% FOREACH u IN contest.submissions %]
<tr data-submission-id="[% u.id |html %]" data-hash="[% u.hash |html %]">
<td><a href="[% call_url(u.take_url) |html %]">
[% format_time(u.created) |html %]</a></td>
<td>[% u.problem.name |html %]</td>
<td>
<img src="[% images_url %]/[% u.type |html %].png" alt="">[% u.type |html %]</td>
<td>[% u.submitter.name |html %]</td>
<td>[% u.judge.name |html %]</td>
<td>[% u.response |html %]</td>
</tr>
[% END %]
[% IF contest.submissions.size == 0 %]
<tr>
<td colspan="7" class="none_at_this_time" valign="top">None at this time.</td>
</tr>
[% END %]
</table>

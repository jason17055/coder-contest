[% IF !submission.is_mine && submission.can_judge %]
<div class="not_the_owner_box">
Warning: <b>[% submission.judge.name |html %]</b>
[% IF submission.response %]
  has responded
[% ELSE %]
  is currently responding
[% END %]
to this submission. You cannot make changes on this screen unless you first
<button type="button" onclick="location.href='[% submission.steal_url |html %]'">Steal</button>
the submission from [% submission.judge.name |html %].
</div>
[% END %]

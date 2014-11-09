[% opt_suffix = binary ? '' : ':text' %]
[% IF value.id %]
<a href="[% value.url |html %]">[% value.name |html %]</a>
-
<label><input type="checkbox" name="[% name |html %]_replace">Replace with:</label>
[% END %]
<input id="[% name |html %]_upload" name="[% name |html %]_upload[% opt_suffix |html %]" type="file">

[% count=0
FOREACH lang IN contest.accepted_languages
     count ? ', ' : ''
     count=count+1
     lang |html
END #each lang
IF count==0 %]
	None (contest workers are not running)
[% END %]

function submissionsListChecker(andThen)
{
	var u = location.href;
	if (u.indexOf('?') != -1) {
		u += '&format=json';
	}
	else {
		u += '?format=json';
	}

	var onSuccess = function(data) {
		var seen={};
		$('tr[data-submission-id]').each(function(idx,el) {
			var sid = el.getAttribute('data-submission-id');
			var hash = el.getAttribute('data-hash');
			seen[sid] = hash;
			});
		var anyNew = false;
		var anyChange = false;
		for (var i = 0; i < data.length; i++) {
			var x = data[i];
			if (!(x.id in seen)) {
				anyNew = true;
				anyChange = true;
			}
			else if (x.hash != seen[x.id]) {
				anyChange = true;
			}
			delete seen[x.id];
		}
		for (var sid in seen) {
			anyChange = true;
		}

		if (anyNew) {
			// there's a new submission to show
			//TODO- decide whether I really want a sound played
			//localStorage.setItem('ringaling', 'yes');
			location.reload();
		}
		else if (anyChange) {
			// there's been a change
			location.reload();
		}

		andThen();
	};

	var onError = function() {
		console.log('ajax error');
		andThen();
	};

	$.ajax({
		type: "GET",
		url: u,
		dataType: 'json',
		success: onSuccess,
		error: onError
		});
}
window.pageChecker.pageSpecific = submissionsListChecker;

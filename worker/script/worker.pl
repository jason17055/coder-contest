#!/usr/bin/perl

use strict;
use warnings;
use LWP::UserAgent;
use Getopt::Long;
use URI::Escape;
use Time::HiRes "time";
use constant TIMEOUT => -33;
use File::stat;
use Sys::Hostname;

our $job_timeout = 10;
my $feed_timeout = 60;
GetOptions(
	) or exit 2;

my ($contest_url) = @ARGV
	or die "Usage: $0 [options] CONTEST_URL\n";
$contest_url =~ s{/+$}{}s; # removing any trailing slashes

my $worker_name = hostname() . "[$$]";

my $ua = LWP::UserAgent->new();
$ua->agent("worker.pl/$worker_name");
$ua->timeout(+$feed_timeout+30);

my $languages_str;
my %accepted_languages;

sub register_worker
{
	my $operating_system;
	if ($^O eq "MSWin32")
	{
		eval { require Win32; } or die;
		$operating_system = Win32::GetOSName();
	}
	else
	{
		$operating_system = `uname -s -p`;
		chomp $operating_system;
	}
	print "Detected operating system: $operating_system\n";

	my %language_details;

	my $java_version = `javac -version 2>&1`;
	if ($? == 0) {
		$java_version =~ s/\s+$//s;
		$language_details{"language:java"} = "$java_version ($operating_system)";
		print "Found Java: $java_version\n";
	} else {
		print "Did not find Java\n";
		undef $java_version;
	}

	my ($cpp_version) = `g++ --version 2>&1`;
	if ($? == 0) {
		$cpp_version =~ s/\s+$//s;
		$language_details{"language:cpp"} = "$cpp_version ($operating_system)";
		print "Found G++: $cpp_version\n";
	} else {
		print "Did not find G++\n";
		undef $cpp_version;
	}

	my ($py_version) = `python -V 2>&1`;
	if ($? == 0) {
		$py_version =~ s/\s+$//s;
		$language_details{"language:py"} = "$py_version ($operating_system)";
		print "Found Python: $py_version\n";
	} else {
		print "Did not find Python\n";
		undef $py_version;
	}

	my ($perl_version) = `perl -e 'print \$^V' 2>&1`;
	if ($? == 0) {
		$perl_version =~ s/\s+$//s;
		$perl_version =~ s/^(Perl\s*)?/Perl /is;
		$language_details{"language:pl"} = "$perl_version ($operating_system)";
		print "Found Perl: $perl_version\n";
	} else {
		print "Did not find Perl\n";
		undef $perl_version;
	}

	my ($ruby_version) = `ruby -v 2>&1`;
	if ($? == 0) {
		$ruby_version =~ s/\s+$//s;
		$language_details{"language:rb"} = "$ruby_version ($operating_system)";
		print "Found Ruby: $ruby_version\n";
	} else {
		print "Did not find Ruby\n";
		undef $ruby_version;
	}

	my ($csharp_version) = `csc 2>&1`;
	if ($? == 0) {
		$csharp_version =~ s/\s+$//s;
		$language_details{"language:cs"} = $csharp_version;
		print "Found C-Sharp: $csharp_version\n";
	} else {
		print "Did not find C-Sharp\n";
		undef $csharp_version;
	}

	%accepted_languages = (
		($cpp_version ?
			("c" => "PlainOldC",
			"cc" => "CPlusPlus",
			"cpp" => "CPlusPlus") : ()),
		($java_version ? (java => "Java") : ()),
		($py_version ? (py => "Python") : ()),
		($perl_version ? (pl => "Perl") : ()),
		($ruby_version ? (rb => "Ruby") : ()),
		);
	$languages_str = join(',', keys %accepted_languages);

	my $description = join("\n",
		$operating_system,
		($java_version ? ($java_version) : ()),
		($cpp_version ? ($cpp_version) : ()),
		);

	my $resp = $ua->post("$contest_url/register_worker",
			[
			"action:register" => 1,
			"languages" => $languages_str,
			%language_details,
			"system" => $operating_system,
			"name" => $worker_name,
			"description" => $description,
			],
			);
	$resp->is_success
		or return undef;

	my $feed_url = read_response($resp)->{feed_url}
		or die "Error: incompatible service at $contest_url\n";

	print "feed url is $feed_url\n";
	return $feed_url;
}
my $feed_url = register_worker()
	or die "Error: not a contest URL: $contest_url\n";

my $spinner = "|/-\\";
my $spinner_idx = 0;
my $sum_idle_time = 0;
my $sum_busy_time = 0;
my $job_count = 0;
my $start_time = time;
for (;;)
{
	print "Checking for jobs..." . substr($spinner, $spinner_idx, 1);
	STDOUT->flush;
	$spinner_idx = ($spinner_idx + 1) % length($spinner);

	my $resp = $ua->post($feed_url,
		[
		"action:claim" => 1,
		"worker_status" => make_worker_status_string(),
		"languages" => $languages_str,
		"timeout" => $feed_timeout,
		],
		Content_Type => "form-data",
		);

	print "\b \b"x21;
	STDOUT->flush;

	if ($resp->is_success)
	{
		my %props;
		foreach my $line (split /\n/, $resp->content)
		{
			my ($k, $v) = split / /, $line, 2;
			$props{$k} = $v;
		}

	if (!$props{id}){
			print STDERR $resp->as_string;
			}

		my $end_time = time;
		$sum_idle_time += ($end_time - $start_time);
		$start_time = $end_time;

		do_job(\%props);

		$end_time = time;
		$sum_busy_time += ($end_time - $start_time);
		$start_time = $end_time;
	}
	else
	{
	print $resp->status_line . "\n";

		my $elapsed = time - $start_time;
		if ($elapsed < 10)
		{
			sleep 10 - $elapsed;
		}

		my $end_time = time;
		$sum_idle_time += ($end_time - $start_time);
		$start_time = $end_time;

		if ($resp->code != 404) {
			# some other error, try re-registering
			re_register_worker();
		}
	}
}

sub re_register_worker
{
	my $give_up = 10;
	for (my $i = 0; $i < $give_up; $i++) {
		$feed_url = register_worker()
			and return;

		print "no service at $contest_url\n";
		print "...will try again in 60 seconds\n";
		sleep 60;
	}

	# one last attempt
	$feed_url = register_worker()
		and return;
	print "no service at $contest_url\n";
	print "giving up\n";
	exit 1;
}

my @detail_output;
sub detail
{
	push @detail_output, @_;
	print @_;
}

sub do_job
{
	my ($props) = @_;

	$job_count++;

	print "GOT A JOB : id = $props->{id}\n";
	local $job_timeout = $props->{timeout} || $job_timeout;

	mkdir $props->{hash};
	chdir $props->{hash}
		or die "Error: cannot chdir $props->{hash}: $!\n";

	my $source_file = undef;
	if ($props->{source_file})
	{
		$source_file = download_file($props->{source_file});
	}
	my $input_file = undef;
	if ($props->{input_file})
	{
		$input_file = download_file($props->{input_file}, "input.txt");
	}
	if ($props->{expected_file})
	{
		download_file($props->{expected_file}, "expected.txt");
	}
	if ($props->{actual_file})
	{
		download_file($props->{actual_file}, "actual.txt");
	}

	if (-e "output.txt")
	{
		unlink "output.txt"
			or die "output.txt: $!\n";
	}

	@detail_output = ();

	my $status = do_job_helper($props, $source_file, $input_file);

	my $post_url = $props->{post_result_to};
	print "post url is $post_url\n";
	my $resp = $ua->post(
		$post_url,
		[
			id => $props->{id},
			status => $status,
			detail_upload => [undef, 'error.txt', Content => join("", @detail_output)],
			(-f("output.txt") ? (output_upload => ["output.txt"]) : ()),
		],
		Content_Type => "form-data",
		);

	print $resp->as_string;
	chdir ".."
		or die "Error: cannot chdir ..: $!\n";
}

sub do_job_helper
{
	my ($props, $source_file, $input_file) = @_;

	if (!defined($source_file)) {
		detail "ERROR: missing source file\n";
		return "Compilation Error";
	}

	my $ext = ($source_file =~ /\.([^.]+)$/ and $1);
	my $lang = $accepted_languages{$ext};
	if (!$lang)
	{
		detail "ERROR: cannot tell what language file $source_file is\n";
		return "Compilation Error";
	}

	my $cmdline = $lang->compile_command($source_file);
	detail join(" ", @$cmdline) . "\n";

	my $started = time();
	my ($status, $result) = run_command($cmdline);
	my $elapsed = time() - $started;
	if (@$result)
	{
		detail ">>> BEGIN COMPILER OUTPUT\n";
		detail @$result;
		detail "<<< END COMPILER OUTPUT\n\n";
	}

	if ($status != 0)
	{
		# compile error
		detail sprintf("Took %.3f seconds.\n", $elapsed);
		detail "The compiler reported an error result ($status).\n";
		return "Compilation Error";
	}


	detail sprintf("Successful compile (%.3f seconds).\n", $elapsed);

	$cmdline = $lang->get_run_command($source_file);
	detail join(" ", @$cmdline) . "\n";
	$started = time();
	($status, $result) = run_command($cmdline, $input_file, "output.txt");
	$elapsed = time() - $started;
	if (@$result)
	{
		detail ">>> BEGIN RUNTIME ERROR OUTPUT\n";
		detail @$result;
		detail "<<< END RUNTIME ERROR OUTPUT\n\n";
	}

	if ($status == TIMEOUT)
	{
		# run-time error
		detail sprintf("Took %.3f seconds.\n", $elapsed);
		detail "Your program was terminated because it is taking too long.\n";
		return "Time-Limit Exceeded";
	}
	elsif ($status != 0)
	{
		# run-time error
		detail sprintf("Took %.3f seconds.\n", $elapsed);
		detail "Your program reported an error result ($status).\n";
		return "Run-Time Error";
	}
	else
	{
		my $sb = stat("output.txt");
		my $nbytes = $sb ? $sb->size : 0;
		detail sprintf("Successful run (%.3f seconds, %d bytes output).\n", $elapsed, $nbytes);
		return "No Error";
	}
}


sub download_file
{
	my ($url, $local_filename) = @_;

	if (!$local_filename)
	{
		$url =~ m{/([^/]+)$}s;
		$local_filename = $1;
	}
	$local_filename =~ s/(\.(?:java|c|c++|cpp|cxx))$/lc$1/ies;

	print "  downloading $local_filename\n";

	my $req = HTTP::Request->new(GET => $url);
	my $resp = $ua->request($req);
	if (not $resp->is_success)
	{
		die "Error: $url\n".$resp->status_line."\n";
	}
	open my $fh, ">", $local_filename
		or die "$local_filename: $!\n";
#	foreach my $line (split /\n/, $resp->content)
#	{
#		$line =~ s{\r$}{}s;
#		print $fh "$line\n";
#	}
	print $fh $resp->content;
	close $fh
		or die "$local_filename: $!\n";
	return $local_filename;
}

sub run_command
{
	my ($cmdline, $input_file, $output_file) = @_;

	use IPC::Open3;

	if ($^O ne "MSWin32") {
		unshift @$cmdline, "nice";
	}

	$input_file ||= ($^O eq "MSWin32" ? "NUL" : "/dev/null");
	open INPUT_FILE, "<", $input_file
		or die "$input_file: $!\n";

	open ERROR_FILE, ">", "error.txt"
		or die "error.txt: $!\n";

	if ($output_file)
	{
		open OUTPUT_FILE, ">", $output_file
			or die "$output_file: $!\n";
	}
	else
	{
		open OUTPUT_FILE, ">&ERROR_FILE"
			or die "cannot dup ERROR_FILE: $!\n";
	}

	my ($wtr, $rdr, $err);
	my $pid = open3("<&INPUT_FILE", ">&OUTPUT_FILE", ">&ERROR_FILE", @$cmdline);
	close ERROR_FILE;
	close OUTPUT_FILE;
	close INPUT_FILE;

	# wait, up to $job_timeout seconds, for child process to complete
	my $status;
	eval
	{
		local $SIG{ALRM} = sub { die "alarm\n" };
		alarm $job_timeout;
		wait;
		$status = $?;
		alarm 0;
	};
	if ($@)
	{
		die unless $@ eq "alarm\n"; #propagate unexpected errors

		# timed out
		kill 9, $pid;
		wait;
		$status = TIMEOUT;
	}

	open ERROR_FILE, "<", "error.txt"
		or die "error.txt: $!\n";
	my @error_output = <ERROR_FILE>;
	close ERROR_FILE
		or die "error.txt: $!\n";

	return ($status, \@error_output);
}

sub make_worker_status_string
{
	return sprintf "Did %d jobs; %.1f%% utilization",
		$job_count,
		100 * ($sum_busy_time ? ($sum_busy_time / ($sum_idle_time+$sum_busy_time)) : 0),
		;
}

sub read_response
{
	my ($resp) = @_;

	my %props;
	foreach my $line (split /\n/, $resp->content)
	{
		my ($k, $v) = split / /, $line, 2;
		$props{$k} = $v;
	}

	return \%props;
}

package Java;

sub compile_command
{
	my ($self, $source_file) = @_;
	return ["javac", $source_file];
}

sub get_run_command
{
	my ($self, $source_file) = @_;

	my $main_class = $source_file;
	$main_class =~ s/\.java$//is;
	return [ "java", "-Xmx1024M", $main_class ];
}

package PlainOldC;

sub compile_command
{
	my ($self, $source_file) = @_;
	return ["gcc", $source_file, "-lm", "-o", "prog"];
}

sub get_run_command
{
	my ($self, $source_file) = @_;

	return [ "./prog" ];
}

package CPlusPlus;

sub compile_command
{
	my ($self, $source_file) = @_;
	return ["g++", $source_file, "-lm", "-o", "prog"];
}

sub get_run_command
{
	my ($self, $source_file) = @_;

	return [ "./prog" ];
}

package Perl;

sub compile_command
{
	my ($self, $source_file) = @_;
	return ["perl", "-c", $source_file];
}

sub get_run_command
{
	my ($self, $source_file) = @_;

	return [ "perl", $source_file ];
}

package Python;

sub compile_command
{
	my ($self, $source_file) = @_;
	return ["python", "-m", "py_compile", $source_file];
}

sub get_run_command
{
	my ($self, $source_file) = @_;

	return [ "python", $source_file ];
}

package Ruby;

sub compile_command
{
	my ($self, $source_file) = @_;
	return ["ruby", "-c", $source_file];
}

sub get_run_command
{
	my ($self, $source_file) = @_;

	return [ "ruby", $source_file ];
}

1;

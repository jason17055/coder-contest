#!/usr/bin/perl

use strict;
use warnings;
use LWP::UserAgent;
use Getopt::Long;
use URI::Escape;
use Time::HiRes "time";
use constant TIMEOUT => -33;
use File::stat;

my $feed_url = "http://home.messiah.edu/contest/worker/feed.php";
our $timeout = 10;
GetOptions(
	) or exit 2;

my $ua = LWP::UserAgent->new();
$ua->agent("worker.pl/$$/".time());

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
	$language_details{"language:java"} = "$java_version ($operating_system)";
	print "Found Java: $java_version\n";
} else {
	print "Did not find Java\n";
	undef $java_version;
}

my ($cpp_version) = `g++ --version`;
if ($? == 0) {
	$language_details{"language:cpp"} = "$cpp_version ($operating_system)";
	print "Found G++: $cpp_version\n";
} else {
	print "Did not find G++\n";
	undef $cpp_version;
}

my ($py_version) = `python -V 2>&1`;
if ($? == 0) {
	$language_details{"language:py"} = "$py_version ($operating_system)";
	print "Found Python: $py_version\n";
} else {
	print "Did not find Python\n";
	undef $py_version;
}

my ($perl_version) = `perl -e 'print $^V' 2>&1`;
if ($? == 0) {
	$language_details{"language:pl"} = "$perl_version ($operating_system)";
	print "Found Perl: $perl_version\n";
} else {
	print "Did not find Perl\n";
	undef $perl_version;
}

my ($ruby_version) = `ruby -v 2>&1`;
if ($? == 0) {
	$language_details{"language:rb"} = "$ruby_version ($operating_system)";
	print "Found Ruby: $ruby_version\n";
} else {
	print "Did not find Ruby\n";
	undef $ruby_version;
}

my ($csharp_version) = `csc 2>&1`;
if ($? == 0) {
	$language_details{"language:cs"} = $csharp_version;
	print "Found C-Sharp: $csharp_version\n";
} else {
	print "Did not find C-Sharp\n";
	undef $csharp_version;
}

my %accepted_languages = (
	($cpp_version ?
		("c" => "PlainOldC",
		"cc" => "CPlusPlus",
		"cpp" => "CPlusPlus") : ()),
	($java_version ? (java => "Java") : ()),
	($py_version ? (py => "Python") : ()),
	($perl_version ? (pl => "Perl") : ()),
	($ruby_version ? (rb => "Ruby") : ()),
	);

my $resp = $ua->post($feed_url,
		[
		"action:register" => 1,
		"languages" => join(',',keys %accepted_languages),
		%language_details,
		"system" => $operating_system,
		"description" => $operating_system . $java_version . $cpp_version,
		],
		"Accepted-Languages" => join(',',keys %accepted_languages),
		);
$resp->is_success
	or die "Error: could not register at $feed_url\n";

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
		[ "action:claim" => 1,
		],
		"Accepted-Languages" => join(',',keys %accepted_languages),
		"Worker-Status" => make_worker_status_string(),
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
	}
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
	local $timeout = $props->{timeout} || $timeout;

	mkdir $props->{hash};
	chdir $props->{hash}
		or die "Error: cannot chdir $props->{hash}: $!\n";

	my $source_file = download_file($props->{source_file});
	download_file($props->{input_file}, "input.txt");

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

	my $status = do_job_helper($props, $source_file);

	my $resp = $ua->post(
		$feed_url,
		[
			id => $props->{id},
			status => $status,
			detail => join("", @detail_output),
			(-f("output.txt") ? (output_file => ["output.txt"]) : ()),
		],
		Content_Type => "form-data",
		);

	print $resp->as_string;
	chdir ".."
		or die "Error: cannot chdir ..: $!\n";
}

sub do_job_helper
{
	my ($props, $source_file) = @_;

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
	($status, $result) = run_command($cmdline, "input.txt", "output.txt");
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
	my ($rel_url, $local_filename) = @_;

	my $url = $feed_url;
	$url =~ s{[^/]+$}{}s;
	$url .= $rel_url;

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

	# wait, up to $timeout seconds, for child process to complete
	my $status;
	eval
	{
		local $SIG{ALRM} = sub { die "alarm\n" };
		alarm $timeout;
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

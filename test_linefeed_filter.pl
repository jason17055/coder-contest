#!/usr/bin/perl
use strict;
use warnings;

print "1\n";
print "2 space\n";
print " 3\n";
print "4\r\n";
print "5\r";

print "6 \n";
print "7 \r\n";
print "8 \r";

print "9\t\n";
print "10\t\r\n";
print "11\t\r";

print "preserve \t whitespace in this line\n";

print "following are double return sequences\n";

print "1\n\n";
print "2\r\n\r\n";
print "3\r\r";

print "ok\r";
print " space after a <CR>\n";

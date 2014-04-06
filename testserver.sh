#!/bin/sh

rm -rf war
mkdir war
(
cd war
jar -xvf ../falcon-contest.war
)
cp -v appengine-web.xml war/WEB-INF/

cmd.exe /c 'start C:\cygwin\home\jason\appengine-java-sdk-1.8.9\bin\dev_appserver.cmd war'

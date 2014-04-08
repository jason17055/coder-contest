#!/bin/sh

test -d war || mkdir war
(
cd war
jar -xvf ../falcon-contest.war
)
cp -v appengine-web.xml war/WEB-INF/

test -z "$APPENGINE_SDK" && APPENGINE_SDK=~/appengine-java-sdk-1.8.9
appenginedir=$(cygpath -w -a "$APPENGINE_SDK")
cmd.exe /c "start $appenginedir"'\bin\dev_appserver.cmd war'

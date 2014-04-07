#!/bin/sh

test -d war || mkdir war
(
cd war
jar -xvf ../falcon-contest.war
)
cp -v appengine-web.xml war/WEB-INF/

appenginedir=$(cygpath -w -a ~/opt/appengine-java-sdk-1.9.2)
cmd.exe /c "start $appenginedir"'\bin\dev_appserver.cmd war'

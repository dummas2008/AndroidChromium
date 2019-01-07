#!/bin/bash

function reset_dir()
{
	if [ ! -d $1 ]; then
		mkdir $1
	else
		rm -rf $1
		mkdir $1
	fi
	return 0
}



if [ ! $1 ]; then
    echo 'invalid parameter'
else
    work_dir=$1

	echo "copy all compile jars"
	target_path="../app/libs"
	reset_dir ${target_path}
    jar_path=${work_dir%*/}/out/Default/gen
    find ${jar_path} -name "*.javac.jar"  -exec ./genownjar.sh {} ${target_path} \;

	echo "copy chrome java"
	target_path="../app/src/main/java"
	reset_dir ${target_path}
    chrome_java_path=${work_dir%*/}/chrome/android/java/src
	cp -rf ${chrome_java_path}/com ${target_path}
	cp -rf ${chrome_java_path}/org ${target_path}
	rm -rf ${target_path}/org/chromium/chrome/browser/MonochromeApplication.java
	rm -rf ${target_path}/org/chromium/chrome/browser/preferences/password/PasswordEntryEditorPreference.java
	rm -rf ${target_path}/org/chromium/chrome/browser/offlinepages/evaluation/OfflinePageEvaluationBridge.java
	patch -p 0 -i ./my.patch ../app/src/main/java/org/chromium/chrome/browser/toolbar/ToolbarPhone.java
	
	echo "copy feed java"
	target_path="../app/src/main/feed"
	feed_java_path=${work_dir%*/}/chrome/android/feed/dummy/java/src
	cp -rf ${feed_java_path}/org ${target_path}
fi

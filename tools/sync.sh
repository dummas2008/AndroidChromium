#!/bin/bash
if [ ! $1 ]; then
    echo 'invalid parameter'
else
    work_dir=$1
# copy all jars
	target_path="../app/libs"
	if [ ! -d ${target_path} ]; then
	   mkdir ${target_path}
	else
	  rm -rf ${target_path}
	  mkdir ${target_path}
	fi   
    jar_path=${work_dir%*/}/out/Default/gen
	echo ${jar_path}
    find ${jar_path} -name "*.javac.jar"  -exec ./genownjar.sh {} ${target_path} \;
fi

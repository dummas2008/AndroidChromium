#!/bin/bash

function create_dir()
{
	if [ ! -d $1 ]; then
		mkdir $1
	fi
	return 0
}

if [ ! $1 ]; then
  echo 'invalid parameter'
else
  output_dir='../app/libs'
  if [ ! $2 ]; then
    create_dir ${output_dir}
  else
    output_dir=$2
    create_dir ${output_dir}
  fi
  final_path=${output_dir}
  flag=false
  i=1
  suffix='.javac.jar'
  ext='.jar'
  while((1==1))
  do
    split=`echo $1|cut -d "/" -f $i`
    if [ "$split" != "" ]
    then
      ((i++))
	  if [ "$flag" = false ] ; then
	    if [[ $split == "gen" ]] ; then
	      flag=true
		  continue
	    fi
	  fi
	  if [ "$flag" = false ]; then
	    continue
	  fi
      if [[ $split =~ $suffix ]]
      then
	output_jar=${final_path}/${split/${suffix}/${ext}}
#      	./build/android/gyp/filter_zip.py --input $1 --output ${output_jar} --exclude-globs='["*/R.class","*/R\$*.class","*/Manifest.class","*/Manifest$*.class"]' --include-globs=[]
      	./filter_zip.py --input $1 --output $output_jar --exclude-globs=[\"*/R.class\",\"*/R\$*.class\",\"*/Manifest.class\",\"*/Manifest\$*.class\",\"*/NativeLibraries.class\",\"*/BuildConfig.class\"] --include-globs=[]
	    echo $output_jar
      	break
      else
	      final_path=${final_path}/${split}
	      create_dir $final_path
      fi	
    else
      break
    fi
  done  
fi

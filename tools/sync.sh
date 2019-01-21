#!/bin/bash

function create_dir()
{
	if [ ! -d $1 ]; then
		mkdir $1
	fi
	return 0
}

function reset_dir()
{
	if [ ! -d $1 ]; then
		i=0
		final_path="."
		while((1==1))
		do
			split=`echo $1|cut -d "/" -f $i`
			((i++))
			if [ "$split" == "" ];then
				break
			else
				final_path=${final_path}/${split}
				create_dir ${final_path}
			fi
		done
	else
		rm -rf $1
		mkdir $1
	fi
	return 0
}

function copy_duplicate_file()
{
	if [[ $1 == */android_arch_core_common/common-1.0.0.jar ]]; then
		cp $1 $2/android_arch_core_common-1.0.0.jar
	elif [[ $1 == */android_arch_lifecycle_common/common-1.0.0.jar ]]; then
		cp $1 $2/android_arch_lifecycle_common-1.0.0.jar
	else
		cp $1 $2		
	fi		
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

	echo "copy android deps"
	target_path="../app/aars"
	reset_dir ${target_path}
	android_deps_path=${work_dir%*/}/third_party/android_deps
	find ${android_deps_path} -name "*.aar" -exec cp {} ${target_path} \;
	find ${work_dir%*/}/third_party/gvr-android-sdk/src/libraries/ -name "*.aar" -exec cp {} ${target_path} \;
	target_path="../app/android_deps"
	reset_dir ${target_path}
	find ${android_deps_path} -name "*.jar" -exec ./owncp.sh {} ${target_path} \;

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

    echo "copy gen srcjar"
	target_path="../app/src/main/gen_chrome"
	reset_dir ${target_path}
	gen_srcjar_path=${work_dir%*/}/out/Default/gen/chrome
    find ${gen_srcjar_path} -name "*.srcjar" -exec ./extractsrcjar.sh {} ${target_path} \;
    find ${gen_srcjar_path} -name "product_version_resources.srcjar" -exec unzip -o -d ${target_path} {} \;
	gen_srcjar_path=${work_dir%*/}/out/Default/gen/components
    find ${gen_srcjar_path} -name "*.srcjar" -exec ./extractsrcjar.sh {} ${target_path} \;

	echo "copy so"
	target_path="../app/src/main/jniLibs/armeabi-v7a"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/out/Default/libchrome.so ${target_path}
	cp -rf ${work_dir%*/}/out/Default/libchromium_android_linker.so ${target_path}

	echo "copy res"
	target_path="../app/src/main/res_browser"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/chrome/android/java/res/* ${target_path}

	echo "copy androidmedia_res"
	target_path="../libraries_res/androidmedia_res/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/third_party/android_media/java/res/* ${target_path}

	echo "copy autofill_res"
	target_path="../libraries_res/autofill_res/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/components/autofill/android/java/res/* ${target_path}
	cp -rf ${work_dir%*/}/out/Default/gen/components/autofill/android/autofill_strings_grd_grit_output/values* ${target_path}

	echo "copy chrome_res"
	target_path="../libraries_res/chrome_res/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/chrome/android/java/res/* ${target_path}
	target_path="../libraries_res/chrome_res/src/main/res_chromium"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/chrome/android/java/res_chromium/* ${target_path}
	target_path="../libraries_res/chrome_res/src/main/res_brave"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/chrome/android/java/res_brave/* ${target_path}
	cp -rf ${work_dir%*/}/chrome/android/java/res_chromium/mipmap-xhdpi/app_single_page_icon.png ${target_path}/mipmap-xhdpi/
	cp -rf ${work_dir%*/}/chrome/android/java/res_chromium/mipmap-xxxhdpi/app_single_page_icon.png ${target_path}/mipmap-xxxhdpi/
	cp -rf ${work_dir%*/}/chrome/android/java/res_chromium/mipmap-xxhdpi/app_single_page_icon.png ${target_path}/mipmap-xxhdpi/
	cp -rf ${work_dir%*/}/chrome/android/java/res_chromium/mipmap-hdpi/app_single_page_icon.png ${target_path}/mipmap-hdpi/
	cp -rf ${work_dir%*/}/chrome/android/java/res_chromium/mipmap-mdpi/app_single_page_icon.png ${target_path}/mipmap-mdpi/
	find ${target_path} -name app_icon.png -exec rm {} \;													  
	target_path="../libraries_res/chrome_res/src/main/res_vr"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/chrome/android/java/res_vr/* ${target_path}
	find ${target_path} -name OWNERS -exec rm {} \;												   
	target_path="../libraries_res/chrome_res/src/main/res_gen"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/out/Default/gen/chrome/android/chrome_strings_grd_grit_output/values* ${target_path}
	cp -rf ${work_dir%*/}/out/Default/gen/chrome/java/res/* ${target_path}
	cp -rf ${work_dir%*/}/out/Default/gen/chrome/app/policy/android/* ${target_path}
	unzip -o -d ${target_path} ${work_dir%*/}/out/Default/resource_zips/chrome/android/chrome_public_apk_template_resources.resources.zip																  

	echo "copy components_res"
	target_path="../libraries_res/components_res/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/out/Default/gen/components/strings/java/res/* ${target_path}

	echo "copy content_res"
	target_path="../libraries_res/content_res/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/content/public/android/java/res/* ${target_path}
	cp -rf ${work_dir%*/}/out/Default/gen/content/public/android/content_strings_grd_grit_output/values* ${target_path}

	echo "copy customtabs_res"
	target_path="../libraries_res/customtabs_res/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/third_party/custom_tabs_client/src/customtabs/res/* ${target_path}

	echo "copy datausagechart_res"
	target_path="../libraries_res/datausagechart_res/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/third_party/android_data_chart/java/res/* ${target_path}

	echo "copy gvr-android-sdk"
	target_path="../libraries_res/gvr-android-sdk/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/out/Default/gen/third_party/gvr-android-sdk/gvr_common_java/res/* ${target_path}

	echo "copy media_res"
	target_path="../libraries_res/media_res/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/media/base/android/java/res/* ${target_path}

	echo "copy ui_res"
	target_path="../libraries_res/ui_res/src/main/res"
	reset_dir ${target_path}
	cp -rf ${work_dir%*/}/ui/android/java/res/* ${target_path}
	cp -rf ${work_dir%*/}/out/Default/gen/ui/android/ui_strings_grd_grit_output/values* ${target_path}
	unzip -o -d ${target_path} ${work_dir%*/}/out/Default/resource_zips/ui/android/ui_locale_string_resources.zip																  
	
	echo "sync complete!!!"
fi

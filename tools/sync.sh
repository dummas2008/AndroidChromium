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

    echo "generate app module"
    target_path="../app"
    reset_dir ${target_path}
    reset_dir ${target_path}/src/main
    cp ${work_dir%*/}/out/Default/gen/chrome/android/chrome_public_apk/AndroidManifest.xml ${target_path}/src/main
    ./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro
    cp ./ftl/own/build.gradle ${target_path}
    cp ./ftl/own/browser.keystore ${target_path}

    echo "copy assets"
    target_path="../app/src/main/assets"
    reset_dir ${target_path}
    cp ${work_dir%*/}/out/Default/chrome_100_percent.pak ${target_path}
    cp ${work_dir%*/}/out/Default/resources.pak ${target_path}
    cp ${work_dir%*/}/out/Default/natives_blob.bin ${target_path}
    cp ${work_dir%*/}/net/data/ssl/certificates/urp.crt ${target_path}
    cp ${work_dir%*/}/net/data/ssl/certificates/urp_staging.crt ${target_path}
    cp ${work_dir%*/}/out/Default/gen/chrome/android/chrome_public_apk_unwind_assets/unwind_cfi_32 ${target_path}
    cp ${work_dir%*/}/out/Default/gen/chrome/android/webapk/libs/runtime_library/webapk_dex_version.txt ${target_path}
    cp ${work_dir%*/}/chrome/browser/net/blockers/data/regions.json ${target_path}
    cp ${work_dir%*/}/niceware/browser/niceware.js ${target_path}
    cp ${work_dir%*/}/braveSync/bundles/bundle.js ${target_path}
    cp ${work_dir%*/}/sync/android_sync.js ${target_path}
    cp ${work_dir%*/}/braveSync/bundles/bundle.js ${target_path}
    cp ${work_dir%*/}/sync/android_sync_words.js ${target_path}
    reset_dir ${target_path}/locales
    cp ${work_dir%*/}/out/Default/locales/*.pak ${target_path}/locales

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

	echo "generate androidmedia_res module"
	target_path="../libraries_res/androidmedia_res"
	reset_dir ${target_path}
	reset_dir ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/third_party/android_media/java/res/* ${target_path}/src/main/res
	./fmpp/fmpp -D'packageName:org.chromium.third_party.android.media,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:org.chromium.third_party.android.media,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro


	echo "generate autofill_res module"
	target_path="../libraries_res/autofill_res"
	reset_dir ${target_path}
	reset_dir ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/components/autofill/android/java/res/* ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/out/Default/gen/components/autofill/android/autofill_strings_grd_grit_output/values* ${target_path}/src/main/res
	./fmpp/fmpp -D'packageName:org.chromium.components.autofill,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:org.chromium.components.autofill,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro

	echo "generate chrome_res module"
	target_path="../libraries_res/chrome_res"
	reset_dir ${target_path}
	./fmpp/fmpp -D'packageName:org.chromium.chrome,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:org.chromium.chrome,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro
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

	echo "generate components_res module"
	target_path="../libraries_res/components_res"
	reset_dir ${target_path}
	reset_dir ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/out/Default/gen/components/strings/java/res/* ${target_path}/src/main/res
	./fmpp/fmpp -D'packageName:org.chromium.components,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:org.chromium.components,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro

	echo "generate content_res module"
	target_path="../libraries_res/content_res"
	reset_dir ${target_path}
	reset_dir ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/content/public/android/java/res/* ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/out/Default/gen/content/public/android/content_strings_grd_grit_output/values* ${target_path}
	./fmpp/fmpp -D'packageName:org.chromium.content,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:org.chromium.content,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro

	echo "generate customtabs_res module"
	target_path="../libraries_res/customtabs_res"
	reset_dir ${target_path}
	reset_dir ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/third_party/custom_tabs_client/src/customtabs/res/* ${target_path}/src/main/res
	./fmpp/fmpp -D'packageName:android.support.customtabs,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:android.support.customtabs,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro

	echo "generate datausagechart_res module"
	target_path="../libraries_res/datausagechart_res"
	reset_dir ${target_path}
	reset_dir ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/third_party/android_data_chart/java/res/* ${target_path}/src/main/res
	./fmpp/fmpp -D'packageName:org.chromium.third_party.android,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:org.chromium.third_party.android,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro

	echo "generate gvr-android-sdk module"
	target_path="../libraries_res/gvr-android-sdk"
	reset_dir ${target_path}
	reset_dir ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/out/Default/gen/third_party/gvr-android-sdk/gvr_common_java/res/* ${target_path}/src/main/res
	./fmpp/fmpp -D'packageName:com.google.vr.cardboard,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:com.google.vr.cardboard,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro

	echo "generate media_res module"
	target_path="../libraries_res/media_res"
	reset_dir ${target_path}
	reset_dir ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/media/base/android/java/res/* ${target_path}/src/main/res
	./fmpp/fmpp -D'packageName:org.chromium.media,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:org.chromium.media,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro

	echo "generate ui_res module"
	target_path="../libraries_res/ui_res"
	reset_dir ${target_path}
	reset_dir ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/ui/android/java/res/* ${target_path}/src/main/res
	cp -rf ${work_dir%*/}/out/Default/gen/ui/android/ui_strings_grd_grit_output/values* ${target_path}/src/main/res
	unzip -o -d ${target_path}/src/main/res ${work_dir%*/}/out/Default/resource_zips/ui/android/ui_locale_string_resources.zip
	./fmpp/fmpp -D'packageName:org.chromium.ui,isLibraryProject:true,isInstantApp:false,isBaseFeature:false' ftl/AndroidManifest.xml.ftl -o ${target_path}/src/main/AndroidManifest.xml
	./fmpp/fmpp -D'isInstantApp:false,isLibraryProject:false,isDynamicFeature:false,generateKotlin:false,isApplicationProject:true,packageName:org.chromium.ui,isBaseFeature:false,buildApiString:28,gradlePluginVersion:3.2.1,buildToolsVersion:28.0.3,minApi:21,targetApiString:28,postprocessingSupported:false,improvedTestDeps:true' ftl/build.gradle.ftl -o ${target_path}/build.gradle
	./fmpp/fmpp ftl/common/proguard-rules.txt.ftl -o ${target_path}/proguard-rules.pro
	echo "sync complete!!!"
fi

#! /bin/bash

if [ ! $1 ]; then
  echo "invalid parameter!"
else
  output_dir='../app/src/main/gen_chrome/'
  if [ $2 ]; then
    outdir=$2
  fi
  if [ ! -f $1 ]; then
    echo $1" is not exist!"
	exit -1
  fi
  if ! [[ $1 == *.srcjar ]]; then
    echo "non srcjar"
    exit -1
  fi

  if [[ $1 == */payments/mojom/mojom_java_sources.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */android/partner_location_descriptor_proto_java__protoc_java.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */android/document_tab_model_info_proto_java__protoc_java.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */services/filesystem/public/interfaces/* ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */dom_distiller/core/android/dom_distiller_core_font_family_javagen.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */dom_distiller/core/android/dom_distiller_core_theme_javagen.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */location/android/location_settings_dialog_enums_java.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */bookmarks/common/android/bookmark_type_javagen.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */autofill/android/autofill_core_browser_java_enums.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */autofill/android/autofill_java_resources.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */version_info/android/version_constants_srcjar.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */version_info/android/channel_enum_srcjar.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */download/public/background_service/jni_enums.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */download/public/common/jni_enums.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */invalidation/impl/proto_java__protoc_java.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */offline_items_collection/core/jni_enums.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */sync/android/java_enums.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == */feature_engagement/public/public_java_enums_srcjar.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == *.mojom.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 == *mojo_bindings_java_sources.srcjar ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 =~ "/webapk/" ]]; then
    echo "skip "$1
	exit 0
  fi
  if [[ $1 =~ '_resources.srcjar' ]] ; then
  	echo "skip "$1
    exit 0
  fi
    
  unzip -o -d ${output_dir} $1
fi

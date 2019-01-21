#!/bin/bash
if [[ $1 == */android_arch_core_common/common-1.0.0.jar ]]; then
	cp $1 $2/android_arch_core_common-1.0.0.jar
elif [[ $1 == */android_arch_lifecycle_common/common-1.0.0.jar ]]; then
	cp $1 $2/android_arch_lifecycle_common-1.0.0.jar
else
	cp $1 $2		
fi

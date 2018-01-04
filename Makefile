# NetworkBroadcastTest : For Android and JAVA desktop 
# 
#    JAVA desktop : 
#                     to build: make
#                     to run:   make run
#
#    Android:
#                     NEED TO DO:    configure signing storeFile and passwords in app/build.gradle
#                     to build:      make apk
#                     to install:    make install
#                     to run on all: make runa
#

all: build_networkbroadcasttest

build_networkbroadcasttest:
	mkdir -p ./ravn/networkbroadcastshared
	javac -d . ./app/src/main/java/ravn/networkbroadcastshared/*.java
	javac ./ravn/networkbroadcasttestawt/*.java

run:
	java -Djava.net.preferIPv4Stack=true ravn.networkbroadcasttestawt.BroadcastTest

ANDROID_SDK ?= $(HOME)/android-sdk-mac_x86

apk:
	 ./gradlew assembleRelease

list:
	$(ANDROID_SDK)/platform-tools/adb devices -l

install:
	$(ANDROID_SDK)/platform-tools/adb devices | tail -n +2 | cut -sf 1 | xargs -I {} $(ANDROID_SDK)/platform-tools/adb -s {} install -r ./app/build/outputs/apk/app-release.apk

uninstall:
	$(ANDROID_SDK)/platform-tools/adb devices | tail -n +2 | cut -sf 1 | xargs -I {} $(ANDROID_SDK)/platform-tools/adb -s {} uninstall ravn.networkbroadcasttest

#run android
runa:
	$(ANDROID_SDK)/platform-tools/adb devices | tail -n +2 | cut -sf 1 | xargs -I {} $(ANDROID_SDK)/platform-tools/adb -s {} shell am start -n ravn.networkbroadcasttest/ravn.networkbroadcasttest.BroadcastTestActivity

stop:
	$(ANDROID_SDK)/platform-tools/adb devices | tail -n +2 | cut -sf 1 | xargs -I {} $(ANDROID_SDK)/platform-tools/adb -s {} shell am force-stop ravn.networkbroadcasttest

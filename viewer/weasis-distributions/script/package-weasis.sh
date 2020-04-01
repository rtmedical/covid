#!/bin/bash
# This script makes the multiplatform build with jpackage
#
# Initial script by Nicolas Roduit

# Specify the required Java version.
# Only major version is checked. Minor version or any other version string info is left out.
REQUIRED_TEXT_VERSION=13

# Build Parameters
REVISON_INC="1"
PACKAGE=YES
# Package for Java 11 (remove in weasis 4)
SUBSTANCE_PKG="3.0.0-rc3"

# Options
# jdk.unsupported => sun.misc.Signal
# jdk.localedata => other locale (en_us) data are included in the jdk.localedata
# jdk.jdwp.agent => package for debugging agent
JDK_MODULES="java.base,java.compiler,java.datatransfer,java.desktop,java.logging,java.management,java.prefs,java.xml,jdk.localedata,jdk.charsets,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.unsupported,jdk.jdwp.agent"
NAME="Weasis"
IDENTIFIER="org.weasis.viewer"
JVM_ARGS="-Dgosh.port=17179 #-Daudit.log=true #-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8789"

# Aux functions:
die ( ) {
  echo
  echo -e "ERROR: $*"
  exit 1
}

# Get the machine architecture
arc=$(uname -m)
case "$arc" in
  x86)    arc="x86";;
i?86)   arc="x86";;
amd64)  arc="x86_64";;
x86_64) arc="x86_64";;
* ) die "The machine architecture '$arc' -> is not supported.";;
esac

# Get the system name
machine="$(uname -s)"
case "${machine}" in
  Linux*)     machine="linux";;
Darwin*)    machine="macosx";;
CYGWIN*)    machine="windows";;
*) die "The system '$machine' -> is not supported.";;
esac

ARC_OS="$machine-$arc"

POSITIONAL=()
while [[ $# -gt 0 ]]
do
  key="$1"

  case $key in
    -h|--help)
echo "Usage: package-weasis.sh <options>"
echo "Sample usages:"
echo "    Build an installer for the current platform with the minimal required parameters"
echo "        package-weasis.sh --input /home/user/weasis-portable --jdk /home/user/jdk-13"
echo ""
echo "Options:"
echo " --help -h
Print the usage text with a list and description of each valid
option the output stream, and exit"
echo " --input -i
Path of the weasis-portable directory"
echo " --output -o
Path of the base output directory.
Default value is the current directory"
echo " --jdk -j
Path of the jdk with the jpackage module (>= jdk-13)"
echo " --jdk-modules
List of modules to build the Java Runtime
If not set, a minimal default list is applied"
echo " --mac-signing-key-user-name
Key user name of the certificate to sign the bundle"
exit 0
;;
-j|--jdk)
JDK_PATH_UNIX="$2"
shift # past argument
shift # past value
;;
-i|--input)
INPUT_PATH="$2"
shift # past argument
shift # past value
;;
-o|--output)
OUTPUT_PATH="$2"
shift # past argument
shift # past value
;;
--mac-signing-key-user-name)
CERTIFICATE="$2"
shift # past argument
shift # past value
;;
*)    # unknown option
POSITIONAL+=("$1") # save it in an array for later
shift # past argument
;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters

RES="resources/$machine"

if [ "$machine" = "windows" ] ; then
  INPUT_PATH_UNIX=$(cygpath -u "$INPUT_PATH")
  OUTPUT_PATH_UNIX=$(cygpath -u "$OUTPUT_PATH")
else
  INPUT_PATH_UNIX="$INPUT_PATH"
  OUTPUT_PATH_UNIX="$OUTPUT_PATH"
fi

# Set custom JDK path (>= JDK 11)
export JAVA_HOME=$JDK_PATH_UNIX

WEASIS_VERSION=$(grep -i "weasis.version=" "$INPUT_PATH_UNIX/weasis/conf/config.properties" | sed 's/^.*=//')

echo System        = "${ARC_OS}"
echo JDK path        = "${JDK_PATH_UNIX}"
echo Weasis version  = "${WEASIS_VERSION}"
echo Input path      = "${INPUT_PATH}"
if [ "$machine" = "windows" ]
then
  echo Input unix path      = "${INPUT_PATH_UNIX}"
fi

# Extract major version number for comparisons from the required version string.
# In order to do that, remove leading "1." if exists, and minor and security versions.
REQUIRED_MAJOR_VERSION=$(echo $REQUIRED_TEXT_VERSION | sed -e 's/^1\.//' -e 's/\..*//')

# Check jlink command.
if [ -x "$JDK_PATH_UNIX/bin/jpackage" ] ; then
  JPKGCMD="$JDK_PATH_UNIX/bin/jpackage"
  JLINKCMD="$JDK_PATH_UNIX/bin/jlink"
  JDEPSCMD="$JDK_PATH_UNIX/bin/jdeps"
  JAVACMD="$JDK_PATH_UNIX/bin/java"
  JAVACCMD="$JDK_PATH_UNIX/bin/javac"
  JARCMD="$JDK_PATH_UNIX/bin/jar"
else
  die "JAVA_HOME is not set and no 'jpackage' command could be found in your PATH. Specify a jdk path >=$REQUIRED_TEXT_VERSION."
fi

# Then, get the installed version
INSTALLED_VERSION=$($JAVACMD -version 2>&1 | awk '/version [0-9]*/ {print $3;}')
echo "Found java version $INSTALLED_VERSION"
echo "Java command path: $JAVACMD"

# Remove double quotes, remove leading "1." if it exists and remove everything apart from the major version number.
INSTALLED_MAJOR_VERSION=$(echo $INSTALLED_VERSION | sed -e 's/"//g' -e 's/^1\.//' -e 's/\..*//' -e 's/-.*//')
echo "Java major version: $INSTALLED_MAJOR_VERSION"
if (( INSTALLED_MAJOR_VERSION < REQUIRED_MAJOR_VERSION )) ; then
  die "Your version of java is too low to run this script.\nPlease update to $REQUIRED_TEXT_VERSION or higher"
fi

if ( "$JAVACMD" -version 2>&1 | grep -q "64" ) ; then
  if [ "$arc" = "x86" ] ; then
    die "The 64-bit JDK is not compatible with the running architecture ($ARC_OS)"
  fi
  ARC_NAME="x86-64"
  ARC_OS="$machine-x86-64"
else
  ARC_NAME="x86"
  ARC_OS="$machine-x86"
fi

if [ -z "$OUTPUT_PATH" ] ; then
  OUTPUT_PATH="weasis-$ARC_OS-jdk$REQUIRED_TEXT_VERSION-$WEASIS_VERSION"
  OUTPUT_PATH_UNIX="$OUTPUT_PATH"
fi

echo Output path = "${OUTPUT_PATH}"
if [ "$machine" = "windows" ] ; then
  echo Output unix path = "${OUTPUT_PATH_UNIX}"
fi


if [ "$machine" = "windows" ] ; then
  INPUT_DIR="$INPUT_PATH\\weasis"
  IMAGE_PATH="$OUTPUT_PATH\\$NAME"
else
  IMAGE_PATH="$OUTPUT_PATH/$NAME"
  INPUT_DIR="$INPUT_PATH_UNIX/weasis"
fi

MAC_SIGN=""
WEASIS_CLEAN_VERSION=$(echo $WEASIS_VERSION | sed -e 's/"//g' -e 's/-.*//')
if [ "$machine" = "macosx" ] ; then
  WEASIS_VERSION="$WEASIS_CLEAN_VERSION"
  JVM_ARGS="-Dapple.laf.useScreenMenuBar=true $JVM_ARGS"
  if [[ ! -x "$CERTIFICATE" ]] ; then
    MAC_SIGN="--mac-sign"
  fi
fi

# Remove pack jar for launcher
rm -f "$INPUT_DIR"/*.jar.pack.gz

# Remove the unrelated native packages
find "$INPUT_DIR"/bundle/*-x86* -type f ! -name '*-'${ARC_OS}'-*'  -exec rm -f {} \;

# Special case with 32-bit architecture, remove 64-bit lib
if [ "$arc" = "x86" ] ; then
  find "$INPUT_DIR"/bundle/*-x86* -type f -name '*-${machine}-x86-64-*'  -exec rm -f {} \;
fi

# Replace substance available for Java 11
mvn dependency:get -Dartifact=org.pushing-pixels:radiance-substance-all:$SUBSTANCE_PKG -DremoteRepositories=https://raw.github.com/nroduit/mvn-repo/master/
MVN_REPO=$(mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]')
SUBSTANCE_FILE="${MVN_REPO//[$'\t\r\n']}/org/pushing-pixels/radiance-substance-all/$SUBSTANCE_PKG/radiance-substance-all-$SUBSTANCE_PKG.jar"
if [[ -r "$SUBSTANCE_FILE" ]] ; then
  cp -fv "${SUBSTANCE_FILE}" "${INPUT_DIR}/substance.jar"
else
  echo "Warning: cannot copy Substance file: ${SUBSTANCE_FILE}"
fi

# Remove previous package
if [ -d "${OUTPUT_PATH}" ] ; then
  rm -rf "${OUTPUT_PATH}"
fi
if [ -d "${OUTPUT_PATH}-debug" ] ; then
  rm -rf "${OUTPUT_PATH}-debug"
fi

# Build Java Runtime
$JLINKCMD --add-modules "$JDK_MODULES" --output "$OUTPUT_PATH/runtime"

$JPKGCMD --type app-image --input "$INPUT_DIR" --dest "$OUTPUT_PATH" --name "$NAME" \
--main-jar weasis-launcher.jar --main-class org.weasis.launcher.AppLauncher --runtime-image "$OUTPUT_PATH/runtime" \
--resource-dir "$RES" --java-options "$JVM_ARGS" --app-version "$WEASIS_VERSION" --verbose

# Build exe for debugging in the console and copy them into the debug folder
if [ "$machine" == "windows" ] ; then
  $JPKGCMD --type app-image --input "$INPUT_DIR" --dest "$OUTPUT_PATH-debug" --name "$NAME" \
  --main-jar weasis-launcher.jar --main-class org.weasis.launcher.AppLauncher --runtime-image "$OUTPUT_PATH/runtime" \
  --resource-dir "$RES" --java-options "$JVM_ARGS" --app-version "$WEASIS_VERSION" --win-console --verbose
  mkdir "$IMAGE_PATH\\debug"
  cp "$OUTPUT_PATH-debug\\$NAME\\$NAME.exe"  "$IMAGE_PATH\\debug\\$NAME.exe"
fi



if [ "$machine" = "macosx" ] ; then
  OUT_APP="$OUTPUT_PATH_UNIX/$NAME.app/Contents/app"
  APP_FOLDER_NAME="app"
elif [ "$machine" = "linux" ] ; then
  OUT_APP="$OUTPUT_PATH_UNIX/$NAME/lib/app"
  APP_FOLDER_NAME="lib\/app"
else
  OUT_APP="$OUTPUT_PATH_UNIX/$NAME/app"
  APP_FOLDER_NAME="app"
fi

if [ "$machine" = "windows" ] ; then
  LAUNCHER_CP=";\$ROOTDIR\\\\$APP_FOLDER_NAME\\\\"
else
  LAUNCHER_CP=":\$ROOTDIR\/$APP_FOLDER_NAME\/"
fi

match="app.name"
insertWeasis='app.splash=resources\/images\/about-round.png\
#app.memory=50%\
app.identifier='"$IDENTIFIER"'\
app.classpath='"${LAUNCHER_CP:1}"'felix.jar'"$LAUNCHER_CP"'substance.jar'"$LAUNCHER_CP"'weasis-launcher.jar\
'
sed -i.bck '/^app\.identifier/d' "$OUT_APP/$NAME.cfg"
sed -i.bck '/^app\.classpath/d' "$OUT_APP/$NAME.cfg"
sed -i.bck "s/$match/$insertWeasis$match/" "$OUT_APP/$NAME.cfg"
rm -f "$OUT_APP/$NAME.cfg.bck"


if [ "$machine" = "linux" ] ; then
  cp "$RES/Dicomizer.desktop" "$OUTPUT_PATH_UNIX/$NAME/lib/weasis-Dicomizer.desktop"
elif [ "$machine" = "windows" ] ; then
  # Fix icon of second launcher
  cp "$RES/Dicomizer.ico" "$OUTPUT_PATH_UNIX/$NAME/Dicomizer.ico"
elif [ "$machine" = "macosx" ] ; then
  cp -Rf "$RES/weasis-uri-handler.app" "$OUTPUT_PATH_UNIX/$NAME.app/Contents/MacOS/"
  cp -Rf "$RES/Dicomizer.app" "$OUTPUT_PATH_UNIX/$NAME.app/Contents/MacOS/"
fi

if [ "$PACKAGE" = "YES" ] ; then
  FILE_ASSOC="file-associations.properties"
  VENDOR="Weasis Team"
  COPYRIGHT="© 2009-2020 Weasis Team"
  if [ "$machine" = "windows" ] ; then
    [ "$ARC_NAME" = "x86" ]  && UPGRADE_UID="3aedc24e-48a8-4623-ab39-0c3c01c7383b" || UPGRADE_UID="3aedc24e-48a8-4623-ab39-0c3c01c7383a"
    $JPKGCMD --type "msi" --app-image "$IMAGE_PATH" --dest "$OUTPUT_PATH" --name "$NAME" --resource-dir "$RES/msi/$ARC_NAME" \
    --license-file "$INPUT_PATH\Licence.txt" --description "Weasis DICOM viewer" \
    --win-menu --win-menu-group "$NAME" --win-upgrade-uuid "$UPGRADE_UID" \
    --copyright "$COPYRIGHT" --app-version "$WEASIS_CLEAN_VERSION" \
    --vendor "$VENDOR" --file-associations "$FILE_ASSOC" --verbose
    mv "$OUTPUT_PATH_UNIX/$NAME-$WEASIS_CLEAN_VERSION.msi" "$OUTPUT_PATH_UNIX/$NAME-$WEASIS_CLEAN_VERSION-$ARC_NAME.msi"
  elif [ "$machine" = "linux" ] ; then
    declare -a installerTypes=("deb" "rpm")
    for installerType in ${installerTypes[@]}; do
      $JPKGCMD --type "$installerType" --app-image "$IMAGE_PATH" --dest "$OUTPUT_PATH"  --name "$NAME" --resource-dir "$RES/$installerType" \
      --license-file "$INPUT_PATH/Licence.txt" --description "Weasis DICOM viewer" --vendor "$VENDOR" \
      --copyright "$COPYRIGHT" --app-version "$WEASIS_CLEAN_VERSION" --file-associations "$FILE_ASSOC" \
      --linux-app-release "$REVISON_INC" --linux-package-name "weasis" --linux-deb-maintainer "Nicolas Roduit" --linux-rpm-license-type "EPL-2.0" \
      --linux-menu-group "Viewer;MedicalSoftware;Graphics;" --linux-app-category "science" --linux-shortcut --verbose
    done
  elif [ "$machine" = "macosx" ] ; then
    $JPKGCMD --type "pkg" --app-image "$IMAGE_PATH.app" --dest "$OUTPUT_PATH" --name "$NAME" --resource-dir "$RES" \
    --license-file "$INPUT_PATH/Licence.txt" --copyright "$COPYRIGHT" --app-version "$WEASIS_CLEAN_VERSION" --mac-package-identifier "$IDENTIFIER" \
    --mac-signing-key-user-name "$CERTIFICATE" --verbose "$MAC_SIGN"
  fi
fi


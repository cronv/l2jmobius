#!/bin/sh
# 
# Copyright (c) 2013 L2jMobius
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
# IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# 

# One click build tool.
# Run it and the project compiles, nothing has to be set up first.
# Neither Eclipse nor a working JDK installation is needed.
# A missing JDK is downloaded and properly installed,
# Apache Ant is downloaded into ~/.local/share/l2jmobius since only the build needs it.
# The finished zip is written to the build folder next to this script.
#
# Pass -y as the first argument to accept every download without being asked.

set -eu

PROJECT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_NAME=$(basename "$PROJECT_DIR")
BUILD_DIR="$PROJECT_DIR/build"
# Ant is shared by every project, so it lives outside the build folders any one of them may wipe.
TOOLS_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/l2jmobius"
BUILD_FILE="$PROJECT_DIR/build.xml"
ANT_VERSION=1.10.18
ASSUME_YES=0

[ "${1:-}" = "-y" ] && ASSUME_YES=1

if [ ! -f "$BUILD_FILE" ]; then
	echo "build.xml was not found next to this script." >&2
	exit 1
fi

# Runs on every exit, set -e included, so a failed run leaves no half download behind.
# build.xml only cleans dist, the loose classes in bin are of no use once the zip exists.
trap 'rm -rf "$TOOLS_DIR/jdk.deb" "$TOOLS_DIR/jdk.rpm" "$TOOLS_DIR/jdk.tar.gz" "$TOOLS_DIR/jdk.stage" "$TOOLS_DIR/ant.tar.gz" "$TOOLS_DIR/ant.stage" "$BUILD_DIR/bin"' EXIT

case "$(uname -s)" in
	Darwin) PLATFORM=macos ;;
	*) PLATFORM=linux ;;
esac

case "$(uname -m)" in
	aarch64|arm64) ARCHITECTURE=arm ;;
	*) ARCHITECTURE=x86 ;;
esac

# build.xml guards itself with a JDK<n>.present property, that n is the version to install.
# The fallback matches every project in the workspace, it only covers a reworded build.xml.
REQUIRED=$(sed -n 's/.*JDK\([0-9][0-9]*\)\.present.*/\1/p' "$BUILD_FILE" | head -1)
[ -n "$REQUIRED" ] || REQUIRED=25

confirm()
{
	[ "$ASSUME_YES" = 1 ] && return 0

	printf '%s [y/N] ' "$1"
	read -r answer
	case "$answer" in
		y|Y|yes|YES) return 0 ;;
		*) return 1 ;;
	esac
}

jdk_feature()
{
	[ -x "$1/bin/javac" ] || return 1
	"$1/bin/javac" -version 2>&1 | sed -n 's/[^0-9]*\([0-9][0-9]*\).*/\1/p' | head -1
}

find_jdk()
{
	for home in "$TOOLS_DIR/jdk" "$TOOLS_DIR/jdk/Contents/Home" "${JAVA_HOME:-}"; do
		[ -n "$home" ] || continue

		feature=$(jdk_feature "$home" 2>/dev/null || true)
		if [ -n "$feature" ] && [ "$feature" -ge "$REQUIRED" ]; then
			echo "$home"
			return 0
		fi
	done

	javac_path=$(command -v javac || true)
	if [ -n "$javac_path" ]; then
		home=$(dirname "$(dirname "$javac_path")")
		feature=$(jdk_feature "$home" 2>/dev/null || true)
		if [ -n "$feature" ] && [ "$feature" -ge "$REQUIRED" ]; then
			echo "$home"
			return 0
		fi
	fi

	return 1
}

find_ant()
{
	for home in "$TOOLS_DIR/ant" "${ANT_HOME:-}"; do
		[ -n "$home" ] || continue

		if [ -x "$home/bin/ant" ]; then
			echo "$home"
			return 0
		fi
	done

	ant_path=$(command -v ant || true)
	if [ -n "$ant_path" ]; then
		dirname "$(dirname "$ant_path")"
		return 0
	fi

	return 1
}

fetch()
{
	if command -v curl >/dev/null 2>&1; then
		curl -L --fail --progress-bar -o "$2" "$1"
	else
		wget -O "$2" "$1"
	fi
}

unpack()
{
	stage="$2.stage"
	rm -rf "$stage"
	mkdir -p "$stage"
	tar -xzf "$1" -C "$stage"

	# Both archives carry everything under one versioned top folder, that folder becomes the target.
	# The old target goes only once the extraction is through, a failed download keeps what was there.
	inner=$(find "$stage" -mindepth 1 -maxdepth 1 -type d | head -1)
	rm -rf "$2"
	if [ -n "$inner" ]; then
		mv "$inner" "$2"
		rm -rf "$stage"
	else
		mv "$stage" "$2"
	fi
}

# The Liberica release index answers in plain text, newest build first.
liberica_url()
{
	query="https://api.bell-sw.com/v1/liberica/releases?version-feature=$REQUIRED&version-modifier=latest&os=$PLATFORM&arch=$ARCHITECTURE&bitness=64&package-type=$1&bundle-type=jdk&output=text&fields=downloadUrl"

	if command -v curl >/dev/null 2>&1; then
		curl -sL --fail "$query" | head -1
	else
		wget -qO- "$query" | head -1
	fi
}

# A distribution package is a real install, it puts java on the PATH for every user and outlives this
# workspace. The tarball is the fallback for a machine without sudo, and for macOS, which has neither.
jdk_package_type()
{
	if [ "$PLATFORM" = linux ] && command -v sudo >/dev/null 2>&1; then
		command -v dpkg >/dev/null 2>&1 && { echo deb; return 0; }
		command -v rpm >/dev/null 2>&1 && { echo rpm; return 0; }
	fi

	echo tar.gz
}

install_jdk()
{
	package=$(jdk_package_type)
	archive="$TOOLS_DIR/jdk.$package"
	mkdir -p "$TOOLS_DIR"

	url=$(liberica_url "$package")
	if [ -z "$url" ]; then
		echo "No Liberica JDK $REQUIRED $package build is listed for $PLATFORM $ARCHITECTURE." >&2
		exit 1
	fi

	echo "Downloading JDK $REQUIRED ..."
	fetch "$url" "$archive"

	case "$package" in
		deb) sudo dpkg -i "$archive" ;;
		rpm) sudo rpm -Uvh "$archive" ;;
		*)
			unpack "$archive" "$TOOLS_DIR/jdk"
			# GameServerTask.sh and its siblings call a bare java, an unpacked JDK reaches no PATH.
			echo "The server launchers call java from the PATH. Add this to your profile to run them:"
			echo "  export JAVA_HOME=\"$TOOLS_DIR/jdk\""
			echo "  export PATH=\"\$JAVA_HOME/bin:\$PATH\""
			;;
	esac

	rm -f "$archive"
}

install_ant()
{
	archive="$TOOLS_DIR/ant.tar.gz"
	mkdir -p "$TOOLS_DIR"

	echo "Downloading Apache Ant $ANT_VERSION ..."
	# The dlcdn mirror only carries current releases, the archive mirror carries every one.
	fetch "https://dlcdn.apache.org/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz" "$archive" || fetch "https://archive.apache.org/dist/ant/binaries/apache-ant-$ANT_VERSION-bin.tar.gz" "$archive"
	unpack "$archive" "$TOOLS_DIR/ant"
	rm -f "$archive"
}

JDK_HOME=$(find_jdk || true)
if [ -z "$JDK_HOME" ]; then
	confirm "JDK $REQUIRED was not found. Download and install Liberica JDK $REQUIRED (about 250 MB)? A package install asks for your sudo password." || exit 1
	install_jdk

	JDK_HOME=$(find_jdk || true)
	if [ -z "$JDK_HOME" ]; then
		echo "The JDK was installed but no javac $REQUIRED turned up afterwards." >&2
		exit 1
	fi
fi

ANT_HOME=$(find_ant || true)
if [ -z "$ANT_HOME" ]; then
	confirm "Apache Ant was not found. Download Apache Ant $ANT_VERSION (about 9 MB) into $TOOLS_DIR/ant?" || exit 1
	install_ant
	ANT_HOME=$(find_ant)
fi

mkdir -p "$BUILD_DIR"
JAVA_HOME=$JDK_HOME
export JAVA_HOME ANT_HOME

echo "Building $PROJECT_NAME with JAVA_HOME=$JAVA_HOME"
# build.xml aims at ../build, the command line wins over the property it declares.
"$ANT_HOME/bin/ant" -Dbuild="$BUILD_DIR" -buildfile "$BUILD_FILE"

echo "Build finished: $BUILD_DIR/$PROJECT_NAME.zip"

#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
PROPS_FILE="$WRAPPER_DIR/maven-wrapper.properties"

if [ ! -f "$PROPS_FILE" ]; then
  echo "Missing wrapper properties: $PROPS_FILE" >&2
  exit 1
fi

DISTRIBUTION_URL=$(grep '^distributionUrl=' "$PROPS_FILE" | head -n 1 | cut -d'=' -f2-)
if [ -z "$DISTRIBUTION_URL" ]; then
  echo "Failed to read distributionUrl from $PROPS_FILE" >&2
  exit 1
fi

ARCHIVE_NAME=$(basename "$DISTRIBUTION_URL" .zip)
MAVEN_DIR="$WRAPPER_DIR/${ARCHIVE_NAME%-bin}"
MAVEN_CMD="$MAVEN_DIR/bin/mvn"
ZIP_FILE="$WRAPPER_DIR/$ARCHIVE_NAME.zip"

MAVEN_BOOT_JAR=$(find "$MAVEN_DIR/boot" -maxdepth 1 -name 'plexus-classworlds-*.jar' -print -quit 2>/dev/null)
if [ ! -x "$MAVEN_CMD" ] || [ -z "$MAVEN_BOOT_JAR" ]; then
  echo "Downloading Maven wrapper distribution..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DISTRIBUTION_URL" -o "$ZIP_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DISTRIBUTION_URL" -O "$ZIP_FILE"
  else
    echo "Please install curl or wget to use mvnw" >&2
    exit 1
  fi

  if command -v unzip >/dev/null 2>&1; then
    unzip -oq "$ZIP_FILE" -d "$WRAPPER_DIR"
  else
    echo "Please install unzip to use mvnw" >&2
    exit 1
  fi
fi

exec "$MAVEN_CMD" "$@"

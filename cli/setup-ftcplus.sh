#!/bin/bash

set -e

FTCPLUS_DIR="$HOME/.ftcplus"

echo "Setting up ~/.ftcplus..."

mkdir -p "$FTCPLUS_DIR/libs"
mkdir -p "$FTCPLUS_DIR/sdk"
mkdir -p "$FTCPLUS_DIR/template"

echo "Fetching latest FTC SDK tag..."
SDK_TAG=$(curl -s https://api.github.com/repos/FIRST-Tech-Challenge/FtcRobotController/releases/latest | grep '"tag_name"' | sed 's/.*"tag_name": "\(.*\)".*/\1/')
echo "Latest SDK: $SDK_TAG"

echo "Cloning FtcRobotController..."
git clone --depth 1 --branch "$SDK_TAG" \
    https://github.com/FIRST-Tech-Challenge/FtcRobotController.git \
    "$FTCPLUS_DIR/template/FtcRobotController"

echo "$SDK_TAG" > "$FTCPLUS_DIR/sdk_tag"

echo ""
echo "Done! ~/.ftcplus is ready."
#!/bin/bash

set -e

REPO="BeatMaker12345/ftcplus"
INSTALL_DIR="/usr/local/bin"
FTCPLUS_DIR="$HOME/.ftcplus"


OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

case "$OS" in
    darwin)
        case "$ARCH" in
            arm64)  BINARY="ftcplus-darwin-arm64" ;;
            x86_64) BINARY="ftcplus-darwin-amd64" ;;
            *)      echo "Unsupported architecture: $ARCH"; exit 1 ;;
        esac
        ;;
    linux)
        case "$ARCH" in
            x86_64) BINARY="ftcplus-linux-amd64" ;;
            *)      echo "Unsupported architecture: $ARCH"; exit 1 ;;
        esac
        ;;
    *)
        echo "Unsupported OS: $OS"
        echo "Windows support coming soon"
        exit 1
        ;;
esac


echo "Fetching latest FTC+ version..."
LATEST=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/latest" | grep '"tag_name"' | sed 's/.*"tag_name": "\(.*\)".*/\1/')
echo "Latest version: $LATEST"

DOWNLOAD_URL="https://github.com/$REPO/releases/latest/download/$BINARY"

echo "Downloading ftcplus ($BINARY)..."
TMP=$(mktemp)
curl -fsSL "$DOWNLOAD_URL" -o "$TMP"
chmod +x "$TMP"

echo "Installing to $INSTALL_DIR/ftcplus..."
if [ -w "$INSTALL_DIR" ]; then
    mv "$TMP" "$INSTALL_DIR/ftcplus"
else
    sudo mv "$TMP" "$INSTALL_DIR/ftcplus"
fi


echo "Setting up ~/.ftcplus..."
mkdir -p "$FTCPLUS_DIR/template"
mkdir -p "$FTCPLUS_DIR/android-sdk"
mkdir -p "$FTCPLUS_DIR/libs"


if [ -d "$FTCPLUS_DIR/template/FtcRobotController" ]; then
    echo "FtcRobotController already installed, updating..."
    git -C "$FTCPLUS_DIR/template/FtcRobotController" fetch --depth 1
else
    echo "Fetching latest FTC SDK tag..."
    SDK_TAG=$(curl -fsSL "https://api.github.com/repos/FIRST-Tech-Challenge/FtcRobotController/releases/latest" | grep '"tag_name"' | sed 's/.*"tag_name": "\(.*\)".*/\1/')
    echo "Cloning FtcRobotController $SDK_TAG..."
    git clone --depth 1 --branch "$SDK_TAG" \
        https://github.com/FIRST-Tech-Challenge/FtcRobotController.git \
        "$FTCPLUS_DIR/template/FtcRobotController"
fi


echo ""
echo "✓ FTC+ $LATEST installed successfully!"
echo ""
echo "  ftcplus new       — create a new project"
echo "  ftcplus build     — build your project"
echo "  ftcplus --help    — see all commands"
echo ""
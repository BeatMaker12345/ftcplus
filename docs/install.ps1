$ErrorActionPreference = "Stop"

$REPO     = "BeatMaker12345/ftcplus"
$INSTALL  = "$env:LOCALAPPDATA\ftcplus"
$BIN      = "$env:LOCALAPPDATA\Microsoft\WindowsApps"
$LIB      = "$env:USERPROFILE\.ftcplus"
$TOOLS    = "$LIB\tools"
$TEMPLATE = "$LIB\template"
$SDK      = "$LIB\android-sdk"

function Info  { param($msg) Write-Host $msg -ForegroundColor Cyan }
function Ok    { param($msg) Write-Host "✓ $msg" -ForegroundColor Green }
function Err   { param($msg) Write-Host "✗ $msg" -ForegroundColor Red; exit 1 }

Info "Fetching latest FTC+ version..."
try {
    $release = Invoke-RestMethod "https://api.github.com/repos/$REPO/releases/latest"
    $tag = $release.tag_name
} catch {
    Err "Failed to fetch latest release: $_"
}
Ok "Latest version: $tag"

Info "Downloading ftcplus.exe..."
New-Item -ItemType Directory -Force -Path $INSTALL | Out-Null
$url = "https://github.com/$REPO/releases/latest/download/ftcplus-windows-amd64.exe"
$exe = "$INSTALL\ftcplus.exe"

try {
    Invoke-WebRequest -Uri $url -OutFile $exe -UseBasicParsing
} catch {
    Err "Failed to download ftcplus.exe: $_"
}
Ok "Downloaded ftcplus.exe"

$userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($userPath -notlike "*$INSTALL*") {
    [Environment]::SetEnvironmentVariable("PATH", "$userPath;$INSTALL", "User")
    Ok "Added $INSTALL to PATH"
} else {
    Ok "PATH already configured"
}

Info "Setting up ~/.ftcplus..."
New-Item -ItemType Directory -Force -Path $LIB     | Out-Null
New-Item -ItemType Directory -Force -Path $TOOLS   | Out-Null
New-Item -ItemType Directory -Force -Path $TEMPLATE | Out-Null
New-Item -ItemType Directory -Force -Path $SDK     | Out-Null
Ok "Created ~/.ftcplus"

Info "Downloading AST tool..."
$astUrl = "https://github.com/$REPO/releases/latest/download/ftcplus-ast.jar"
$astJar = "$TOOLS\ftcplus-ast.jar"
try {
    Invoke-WebRequest -Uri $astUrl -OutFile $astJar -UseBasicParsing
    Ok "Downloaded ftcplus-ast.jar"
} catch {
    Write-Host "Warning: AST tool not available — modify commands will be unavailable" -ForegroundColor Yellow
}

Info "Fetching latest FTC SDK version..."
try {
    $sdkRelease = Invoke-RestMethod "https://api.github.com/repos/FIRST-Tech-Challenge/FtcRobotController/releases/latest"
    $sdkTag = $sdkRelease.tag_name
} catch {
    $sdkTag = "v10.1.1"
    Write-Host "Warning: could not fetch latest SDK tag, using $sdkTag" -ForegroundColor Yellow
}

$templateDir = "$TEMPLATE\FtcRobotController"
if (Test-Path $templateDir) {
    Info "FtcRobotController already installed, updating..."
    Set-Location $templateDir
    git fetch --depth 1 2>&1 | Out-Null
    Set-Location -
} else {
    Info "Cloning FtcRobotController $sdkTag..."
    git clone --depth 1 --branch $sdkTag `
        https://github.com/FIRST-Tech-Challenge/FtcRobotController.git `
        $templateDir
    Ok "Cloned FtcRobotController $sdkTag"
}

Write-Host ""
Write-Host "FTC+ $tag installed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "  ftcplus new       — create a new project"
Write-Host "  ftcplus build     — build your project"
Write-Host "  ftcplus --help    — see all commands"
Write-Host ""
Write-Host "Get started: https://github.com/BeatMaker12345/ftcplus"
Write-Host ""
Write-Host "NOTE: Restart your terminal for PATH changes to take effect." -ForegroundColor Yellow
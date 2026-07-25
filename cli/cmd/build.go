package cmd

import (
	"archive/zip"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"text/template"

	"github.com/spf13/cobra"
)

func buildCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "build",
		Short: "Build the FTC+ project and produce an APK",
		RunE: func(cmd *cobra.Command, args []string) error {
			release, _ := cmd.Flags().GetBool("release")
			return runBuild(release)
		},
	}
	cmd.Flags().Bool("release", false, "Build a release APK")
	return cmd
}

func runBuild(release bool) error {
	cfg, err := readProjectConfig()
	if err != nil {
		return err
	}

	home, _ := os.UserHomeDir()
	ftcPlusLib := filepath.Join(home, ".ftcplus")
	templateDir := filepath.Join(ftcPlusLib, "template", "FtcRobotController")

	if _, err := os.Stat(templateDir); os.IsNotExist(err) {
		return fmt.Errorf("~/.ftcplus not found — run the FTC+ installer first:\ncurl -fsSL https://ftc.plus/install.sh | sh")
	}

	if err := ensureAndroidSdk(ftcPlusLib); err != nil {
		return err
	}

	buildDir := filepath.Join("ftcplus-build")
	if err := os.RemoveAll(buildDir); err != nil {
		return fmt.Errorf("failed to clean build dir: %w", err)
	}

	fmt.Println("Preparing build environment...")
	if err := copyDir(templateDir, buildDir); err != nil {
		return fmt.Errorf("failed to copy FTC SDK template: %w", err)
	}

	sdkDir := filepath.Join(ftcPlusLib, "android-sdk")
	localProps := fmt.Sprintf("sdk.dir=%s\n", strings.ReplaceAll(sdkDir, `\`, `\\`))
	if err := os.WriteFile(filepath.Join(buildDir, "local.properties"), []byte(localProps), 0644); err != nil {
		return fmt.Errorf("failed to write local.properties: %w", err)
	}

	if err := generateTeamCodeBuild(buildDir, cfg); err != nil {
		return fmt.Errorf("failed to generate TeamCode build file: %w", err)
	}

	if err := patchSettings(buildDir); err != nil {
		return fmt.Errorf("failed to patch settings.gradle: %w", err)
	}

	teamCodeSrc := filepath.Join(buildDir, "TeamCode", "src", "main", "java",
		strings.ReplaceAll(cfg.Package, ".", "/"))
	if err := os.MkdirAll(teamCodeSrc, 0755); err != nil {
		return err
	}

	localSrc := filepath.Join("src", "main", "java", strings.ReplaceAll(cfg.Package, ".", "/"))
	if err := copyDir(localSrc, teamCodeSrc); err != nil {
		return fmt.Errorf("failed to copy team source: %w", err)
	}

	task := "assembleDebug"
	if release {
		task = "assembleRelease"
	}

	fmt.Printf("Building (%s)...\n", task)
	gradlew := "./gradlew"
	if runtime.GOOS == "windows" {
		gradlew = "gradlew.bat"
	}

	cmd := exec.Command(gradlew, ":TeamCode:"+task)
	cmd.Dir = buildDir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Env = append(os.Environ(),
		"GRADLE_USER_HOME="+filepath.Join(home, ".gradle"),
	)

	if err := cmd.Run(); err != nil {
		return fmt.Errorf("build failed: %w", err)
	}

	apkPattern := "debug"
	if release {
		apkPattern = "release"
	}

	apkSrc, err := findAPK(buildDir, apkPattern)
	if err != nil {
		return fmt.Errorf("build succeeded but APK not found: %w", err)
	}

	apkDst := filepath.Join("build", filepath.Base(apkSrc))
	if err := os.MkdirAll("build", 0755); err != nil {
		return err
	}
	if err := copyFile(apkSrc, apkDst); err != nil {
		return fmt.Errorf("failed to copy APK: %w", err)
	}

	fmt.Printf("\nBuild successful! APK: %s\n", apkDst)
	return nil
}

func readProjectConfig() (*projectConfig, error) {
	data, err := os.ReadFile("ftcplus.json")
	if err != nil {
		return nil, fmt.Errorf("ftcplus.json not found — run this command from your project root")
	}
	var cfg projectConfig
	if err := json.Unmarshal(data, &cfg); err != nil {
		return nil, fmt.Errorf("failed to parse ftcplus.json: %w", err)
	}
	return &cfg, nil
}

type teamCodeBuildData struct {
	Package        string
	FtcPlusVersion string
}

func generateTeamCodeBuild(buildDir string, cfg *projectConfig) error {
	teamCodeDir := filepath.Join(buildDir, "TeamCode")
	if err := os.MkdirAll(teamCodeDir, 0755); err != nil {
		return err
	}

	kotlinBuildPath := filepath.Join(teamCodeDir, "build.gradle.kts")
	if err := os.Remove(kotlinBuildPath); err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("failed to remove stale build.gradle.kts: %w", err)
	}

	data := teamCodeBuildData{
		Package:        cfg.Package,
		FtcPlusVersion: cfg.FtcPlus,
	}

	tmpl, err := template.New("TeamCode/build.gradle").Parse(teamCodeBuildTemplate)
	if err != nil {
		return fmt.Errorf("failed to parse TeamCode build template: %w", err)
	}

	buildPath := filepath.Join(teamCodeDir, "build.gradle")
	f, err := os.Create(buildPath)
	if err != nil {
		return fmt.Errorf("failed to create %s: %w", buildPath, err)
	}
	defer f.Close()

	if err := tmpl.Execute(f, data); err != nil {
		return fmt.Errorf("failed to write %s: %w", buildPath, err)
	}

	return nil
}

var teamCodeBuildTemplate = `apply from: '../build.common.gradle'
apply from: '../build.dependencies.gradle'

android {
    namespace = '{{.Package}}'

    packagingOptions {
        jniLibs.useLegacyPackaging true
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    implementation project(':FtcRobotController')
    implementation 'dev.ftcplus:core:1.0-SNAPSHOT'
}
`

func patchSettings(buildDir string) error {
	paths := []string{
		filepath.Join(buildDir, "settings.gradle.kts"),
		filepath.Join(buildDir, "settings.gradle"),
	}

	var settingsPath string
	var data []byte
	var err error

	for _, p := range paths {
		data, err = os.ReadFile(p)
		if err == nil {
			settingsPath = p
			break
		}
	}

	if settingsPath == "" {
		return fmt.Errorf("could not find settings.gradle in FTC SDK template")
	}

	content := string(data)

	if !strings.Contains(content, `":TeamCode"`) && !strings.Contains(content, `':TeamCode'`) {
		if strings.HasSuffix(settingsPath, ".kts") {
			content += "\ninclude(\":TeamCode\")\n"
		} else {
			content += "\ninclude ':TeamCode'\n"
		}
		return os.WriteFile(settingsPath, []byte(content), 0644)
	}

	return nil
}

func findAPK(buildDir, variant string) (string, error) {
	var found string
	err := filepath.Walk(buildDir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if !info.IsDir() && strings.HasSuffix(path, ".apk") && strings.Contains(path, variant) {
			found = path
		}
		return nil
	})
	if err != nil {
		return "", err
	}
	if found == "" {
		return "", fmt.Errorf("no %s APK found", variant)
	}
	return found, nil
}

func copyDir(src, dst string) error {
	return filepath.Walk(src, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		rel, err := filepath.Rel(src, path)
		if err != nil {
			return err
		}

		target := filepath.Join(dst, rel)

		if info.IsDir() {
			return os.MkdirAll(target, info.Mode())
		}

		if err := copyFile(path, target); err != nil {
			return err
		}
		return os.Chmod(target, info.Mode())
	})
}

func ensureAndroidSdk(ftcPlusLib string) error {
	sdkDir := filepath.Join(ftcPlusLib, "android-sdk")

	if _, err := os.Stat(filepath.Join(sdkDir, "platform-tools")); err == nil {
		return nil
	}

	fmt.Println("Android SDK not found — installing...")

	cmdlineToolsDir := filepath.Join(sdkDir, "cmdline-tools")
	if err := os.MkdirAll(cmdlineToolsDir, 0755); err != nil {
		return err
	}

	zipPath := filepath.Join(ftcPlusLib, "cmdline-tools.zip")
	url := androidSdkToolsURL()

	fmt.Printf("Downloading Android command-line tools from %s...\n", url)
	if err := downloadFile(zipPath, url); err != nil {
		return fmt.Errorf("failed to download Android SDK tools: %w", err)
	}

	fmt.Println("Extracting...")
	if err := unzip(zipPath, cmdlineToolsDir); err != nil {
		return fmt.Errorf("failed to extract SDK tools: %w", err)
	}
	os.Remove(zipPath)

	sdkmanager := filepath.Join(cmdlineToolsDir, "cmdline-tools", "bin", "sdkmanager")
	os.Chmod(sdkmanager, 0755)

	fmt.Println("Installing Android SDK packages...")
	packages := []string{
		"platform-tools",
		"platforms;android-34",
		"build-tools;34.0.0",
	}

	for _, pkg := range packages {
		fmt.Printf("  Installing %s...\n", pkg)
		cmd := exec.Command(sdkmanager, "--sdk_root="+sdkDir, pkg)
		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr
		cmd.Stdin = strings.NewReader(strings.Repeat("y\n", 10))

		if err := cmd.Run(); err != nil {
			return fmt.Errorf("failed to install %s: %w", pkg, err)
		}
	}

	fmt.Println("Android SDK installed.")
	return nil
}

func androidSdkToolsURL() string {
	switch runtime.GOOS {
	case "darwin":
		return "https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
	case "windows":
		return "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
	default:
		return "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
	}
}

func downloadFile(dst, url string) error {
	resp, err := http.Get(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	f, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer f.Close()

	_, err = io.Copy(f, resp.Body)
	return err
}

func unzip(src, dst string) error {
	r, err := zip.OpenReader(src)
	if err != nil {
		return err
	}
	defer r.Close()

	for _, f := range r.File {
		path := filepath.Join(dst, f.Name)

		if f.FileInfo().IsDir() {
			os.MkdirAll(path, f.Mode())
			continue
		}

		if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
			return err
		}

		out, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, f.Mode())
        if err != nil {
			return err
		}

		rc, err := f.Open()
		if err != nil {
			out.Close()
			return err
		}

		_, err = io.Copy(out, rc)
		rc.Close()
		out.Close()
		if err != nil {
			return err
		}
	}
	return nil
}
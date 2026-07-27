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
		Short: "Build and deploy the FTC+ project to a connected Control Hub",
		RunE: func(cmd *cobra.Command, args []string) error {
			release, _ := cmd.Flags().GetBool("release")
			buildOnly, _ := cmd.Flags().GetBool("build-only")
			return runBuild(release, buildOnly)
		},
	}
	cmd.Flags().Bool("release", false, "Build a release APK")
	cmd.Flags().Bool("build-only", false, "Build without deploying (produces AAR in build/)")
	return cmd
}

func runBuild(release bool, buildOnly bool) error {
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

    if !buildOnly {
        if err := ensureAdbConnected(cfg); err != nil {
            return fmt.Errorf("not connected to Control Hub — run 'ftcplus connect' first: %w", err)
        }
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

	commonGradlePath := filepath.Join(buildDir, "build.common.gradle")
	if data, err := os.ReadFile(commonGradlePath); err == nil {
		content := strings.Replace(string(data),
			"repositories {\n}",
			"repositories {\n    mavenLocal()\n    google()\n    mavenCentral()\n    maven { url \"https://mymaven.bylazar.com/releases\" }\n}",
			1,
		)
		os.WriteFile(commonGradlePath, []byte(content), 0644)
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

	var task string
	if buildOnly {
		if release {
			task = ":TeamCode:assembleRelease"
		} else {
			task = ":TeamCode:assembleDebug"
		}
	} else {
		if release {
			task = ":TeamCode:installRelease"
		} else {
			task = ":TeamCode:installDebug"
		}
	}

	fmt.Printf("Building (%s)...\n", task)
	gradlew := "./gradlew"
	if runtime.GOOS == "windows" {
		gradlew = "gradlew.bat"
	}

	cmd := exec.Command(gradlew, task)
	cmd.Dir = buildDir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Env = append(os.Environ(),
		"GRADLE_USER_HOME="+filepath.Join(home, ".gradle"),
	)

	if err := cmd.Run(); err != nil {
		if !buildOnly {
			return fmt.Errorf("build/deploy failed — is a Control Hub connected over WiFi?\n(use --build-only to build without deploying): %w", err)
		}
		return fmt.Errorf("build failed: %w", err)
	}

	if buildOnly {
		aarSrc, err := findOutput(buildDir, func(path string) bool {
			return strings.HasSuffix(path, ".aar")
		})
		if err != nil {
			return fmt.Errorf("build succeeded but output not found: %w", err)
		}
		dst := filepath.Join("build", filepath.Base(aarSrc))
		if err := os.MkdirAll("build", 0755); err != nil {
			return err
		}
		if err := copyFile(aarSrc, dst); err != nil {
			return fmt.Errorf("failed to copy output: %w", err)
		}
		fmt.Printf("\nBuild successful! Output: %s\n", dst)
	} else {
		fmt.Println("\nBuild and deploy successful! Your robot should be ready.")
	}

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

	data := teamCodeBuildData{
		Package:        cfg.Package,
		FtcPlusVersion: cfg.FtcPlus,
	}

	tmpl, err := template.New("").Parse(teamCodeBuildTemplate)
	if err != nil {
		return err
	}

	f, err := os.Create(filepath.Join(teamCodeDir, "build.gradle"))
	if err != nil {
		return err
	}
	defer f.Close()

	return tmpl.Execute(f, data)
}

var teamCodeBuildTemplate = `plugins {
    id("com.android.library")
}

android {
    namespace = "{{.Package}}"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven { url = uri("https://mymaven.bylazar.com/releases") }
}

dependencies {
    implementation(project(":FtcRobotController"))
    implementation("dev.ftcplus:core:{{.FtcPlusVersion}}")
    implementation("dev.ftcplus:ftc-runtime:{{.FtcPlusVersion}}")
    implementation("dev.ftcplus:catalog:{{.FtcPlusVersion}}")
    implementation("dev.ftcplus:drivetrains:{{.FtcPlusVersion}}")
    implementation("dev.ftcplus:auto:{{.FtcPlusVersion}}")
    implementation("com.pedropathing:ftc:2.1.2")
    implementation("com.pedropathing:telemetry:1.0.0")
    annotationProcessor("dev.ftcplus:annotation-processor:{{.FtcPlusVersion}}")
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

	if !strings.Contains(content, "pluginManagement") {
		prefix := `pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
}

`
		content = prefix + content
	}

	if !strings.Contains(content, `":TeamCode"`) && !strings.Contains(content, `':TeamCode'`) {
		content += "\ninclude ':TeamCode'\n"
	}

	return os.WriteFile(settingsPath, []byte(content), 0644)
}


func findOutput(buildDir string, match func(string) bool) (string, error) {
	var found string
	err := filepath.Walk(buildDir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if !info.IsDir() && match(path) {
			found = path
		}
		return nil
	})
	if err != nil {
		return "", err
	}
	if found == "" {
		return "", fmt.Errorf("output not found")
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

func ensureAdbConnected(cfg *projectConfig) error {
    target := cfg.ControlHub
    if target == "" {
        target = "192.168.43.1:5555"
    }
    if !strings.Contains(target, ":") {
        target += ":5555"
    }

    fmt.Printf("Connecting to Control Hub (%s)...\n", target)

    exec.Command("adb", "kill-server").Run()
    exec.Command("adb", "start-server").Run()

    if strings.HasPrefix(target, "192.") || strings.Contains(target, ".") {
        cmd := exec.Command("adb", "connect", target)
        out, err := cmd.CombinedOutput()
        if err != nil || strings.Contains(string(out), "failed") || strings.Contains(string(out), "unable") {
            return fmt.Errorf("could not connect to %s: %s", target, string(out))
        }
        fmt.Printf("Connected to %s\n", target)
    }

    return nil
}
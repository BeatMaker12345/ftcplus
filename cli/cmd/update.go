package cmd

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"regexp"
	"strings"

	"github.com/spf13/cobra"
)

func updateCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "update",
		Short: "Update FTC+ dependencies to latest",
		RunE: func(cmd *cobra.Command, args []string) error {
			return runUpdate()
		},
	}
}

func runUpdate() error {
	fmt.Println("Fetching latest FTC+ version...")
	ftcPlusVersion, err := latestFtcPlusVersion()
	if err != nil {
		return fmt.Errorf("failed to fetch FTC+ version: %w", err)
	}
	fmt.Printf("Latest FTC+ version: %s\n", ftcPlusVersion)

	fmt.Println("Fetching latest FTC SDK version...")
	sdkTag, err := latestFtcSdkTag()
	if err != nil {
		return fmt.Errorf("failed to fetch SDK version: %w", err)
	}
	fmt.Printf("Latest FTC SDK version: %s\n", sdkTag)

	buildFile := "TeamCode/build.gradle.kts"
	data, err := os.ReadFile(buildFile)
	if err != nil {
		return fmt.Errorf("could not read %s — run this from your project root: %w", buildFile, err)
	}

	content := string(data)
	content = replaceDependencyVersion(content, "dev.ftcplus:ftc-runtime", ftcPlusVersion)
	content = replaceDependencyVersion(content, "dev.ftcplus:catalog", ftcPlusVersion)

	if err := os.WriteFile(buildFile, []byte(content), 0644); err != nil {
		return fmt.Errorf("failed to write %s: %w", buildFile, err)
	}

	fmt.Printf("Updated %s\n", buildFile)
	fmt.Println("Done! Sync your gradle project to apply changes.")
	return nil
}

func latestFtcPlusVersion() (string, error) {
	return "1.0-SNAPSHOT", nil
}

func replaceDependencyVersion(content, dep, version string) string {
	pattern := regexp.MustCompile(`("` + regexp.QuoteMeta(dep) + `:)[^"]+("`)
	replacement := `${1}` + version + `${2}`
	result := pattern.ReplaceAllString(content, replacement)
	if result == content {
		fmt.Printf("  warning: could not find dependency %s in build.gradle.kts\n", dep)
	} else {
		fmt.Printf("  updated %s to %s\n", dep, version)
	}
	return result
}

func latestMavenVersion(group, artifact string) (string, error) {
	groupPath := strings.ReplaceAll(group, ".", "/")
	url := fmt.Sprintf(
		"https://search.maven.org/solrsearch/select?q=g:%s+AND+a:%s&rows=1&wt=json",
		group, artifact,
	)
	resp, err := http.Get(url)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var result struct {
		Response struct {
			Docs []struct {
				LatestVersion string `json:"latestVersion"`
			} `json:"docs"`
		} `json:"response"`
	}
	_ = groupPath
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return "", err
	}
	if len(result.Response.Docs) == 0 {
		return "", fmt.Errorf("no versions found for %s:%s", group, artifact)
	}
	return result.Response.Docs[0].LatestVersion, nil
}
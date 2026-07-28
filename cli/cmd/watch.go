package cmd

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"github.com/spf13/cobra"
)

func watchCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:    "watch",
		Short:  "Watch for calibration and settings changes from a connected Control Hub",
		Hidden: true,
		RunE: func(cmd *cobra.Command, args []string) error {
			daemon, _ := cmd.Flags().GetBool("daemon")
			return runWatch(daemon)
		},
	}
	cmd.Flags().Bool("daemon", false, "Run as background daemon")
	return cmd
}

func runWatch(daemon bool) error {
	fmt.Println("Watching for FTC+ events from Control Hub...")

	logcat := exec.Command("adb", "logcat", "-s",
		"FTCPLUS_CALIBRATION:I",
		"FTCPLUS_SETTINGS:I",
	)
	stdout, err := logcat.StdoutPipe()
	if err != nil {
		return fmt.Errorf("failed to start logcat: %w", err)
	}
	if err := logcat.Start(); err != nil {
		return fmt.Errorf("failed to start logcat: %w", err)
	}

	scanner := bufio.NewScanner(stdout)
	for scanner.Scan() {
		line := scanner.Text()

		if strings.Contains(line, "FTCPLUS_CALIBRATION") {
			handleCalibrationEvent(line)
		} else if strings.Contains(line, "FTCPLUS_SETTINGS") {
			handleSettingsEvent(line)
		}
	}

	return nil
}


type calibrationPayload struct {
	Class  string                 `json:"class"`
	Values map[string]interface{} `json:"values"`
}

func handleCalibrationEvent(line string) {
	start := strings.Index(line, "{")
	if start < 0 {
		return
	}
	raw := line[start:]

	var payload calibrationPayload
	if err := json.Unmarshal([]byte(raw), &payload); err != nil {
		fmt.Printf("warning: failed to parse calibration event: %v\n", err)
		return
	}

	pkg, err := detectPackage()
	if err != nil {
		return
	}

	srcFile := classToSourceFile(payload.Class, pkg)
	if srcFile == "" {
		fmt.Printf("warning: could not find source file for %s\n", payload.Class)
		return
	}

	for fieldName, value := range payload.Values {
		if err := writeCalibrationConstant(srcFile, fieldName, value); err != nil {
			fmt.Printf("warning: failed to write %s.%s: %v\n", payload.Class, fieldName, err)
		} else {
			fmt.Printf("✓ wrote %s = %v to %s\n", fieldName, value, srcFile)
		}
	}
}

func writeCalibrationConstant(srcFile, fieldName string, value interface{}) error {
	data, err := os.ReadFile(srcFile)
	if err != nil {
		return err
	}

	content := string(data)
	marker := "// AUTO-CALIBRATED"

	existingLine := fmt.Sprintf("public static final int %s =", fieldName)
	existingLineDouble := fmt.Sprintf("public static final double %s =", fieldName)

	valueStr := fmt.Sprintf("%v", value)
	isDouble := strings.Contains(valueStr, ".")

	var newLine string
	if isDouble {
		newLine = fmt.Sprintf("    %s\n    public static final double %s = %s;",
			marker, fieldName, valueStr)
	} else {
		newLine = fmt.Sprintf("    %s\n    public static final int %s = %s;",
			marker, fieldName, valueStr)
	}

	if strings.Contains(content, existingLine) || strings.Contains(content, existingLineDouble) {
		lines := strings.Split(content, "\n")
		var result []string
		skip := false
		for _, line := range lines {
			if strings.Contains(line, marker) {
				skip = true
				continue
			}
			if skip && (strings.Contains(line, existingLine) || strings.Contains(line, existingLineDouble)) {
				result = append(result, newLine)
				skip = false
				continue
			}
			skip = false
			result = append(result, line)
		}
		content = strings.Join(result, "\n")
	} else {
		idx := strings.Index(content, "{")
		if idx < 0 {
			return fmt.Errorf("could not find class body")
		}
		content = content[:idx+1] + "\n" + newLine + "\n" + content[idx+1:]
	}

	return os.WriteFile(srcFile, []byte(content), 0644)
}


type settingsPayload struct {
	Class string      `json:"class"`
	Field string      `json:"field"`
	Value interface{} `json:"value"`
}

func handleSettingsEvent(line string) {
	start := strings.Index(line, "{")
	if start < 0 {
		return
	}
	raw := line[start:]

	var payload settingsPayload
	if err := json.Unmarshal([]byte(raw), &payload); err != nil {
		fmt.Printf("warning: failed to parse settings event: %v\n", err)
		return
	}

	pkg, err := detectPackage()
	if err != nil {
		return
	}

	srcFile := classToSourceFile(payload.Class, pkg)
	if srcFile == "" {
		fmt.Printf("warning: could not find source file for %s\n", payload.Class)
		return
	}

	if err := writeSettingDefault(srcFile, payload.Field, payload.Value); err != nil {
		fmt.Printf("warning: failed to write setting %s.%s: %v\n", payload.Class, payload.Field, err)
	} else {
		fmt.Printf("✓ updated @Setting %s = %v in %s\n", payload.Field, payload.Value, srcFile)
	}
}

func writeSettingDefault(srcFile, fieldName string, value interface{}) error {
	data, err := os.ReadFile(srcFile)
	if err != nil {
		return err
	}

	lines := strings.Split(string(data), "\n")
	for i, line := range lines {
		trimmed := strings.TrimSpace(line)
		if strings.Contains(trimmed, fieldName+" =") || strings.Contains(trimmed, fieldName+"=") {
			eqIdx := strings.LastIndex(line, "=")
			semiIdx := strings.LastIndex(line, ";")
			if eqIdx < 0 || semiIdx < 0 || eqIdx >= semiIdx {
				continue
			}
			lines[i] = line[:eqIdx+1] + " " + fmt.Sprintf("%v", value) + ";" +
				"  // updated by FTC+ " + currentDate()
			break
		}
	}

	return os.WriteFile(srcFile, []byte(strings.Join(lines, "\n")), 0644)
}


func classToSourceFile(className, pkg string) string {
	parts := strings.Split(className, ".")
	if len(parts) == 0 {
		return ""
	}
	rel := filepath.Join(parts...)
	path := filepath.Join("src", "main", "java", rel+".java")
	if _, err := os.Stat(path); err == nil {
		return path
	}
	return ""
}

func currentDate() string {
	return "2026"
}
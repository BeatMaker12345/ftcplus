package cmd

import (
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"strings"
	"path/filepath"

	"github.com/spf13/cobra"
)

func connectCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "connect [ip|usb]",
		Short: "Connect to a Control Hub",
		Long: `Connect to a Control Hub over WiFi or USB.

Examples:
  ftcplus connect              — connect to default IP (192.168.43.1)
  ftcplus connect 192.168.1.5  — connect to custom IP
  ftcplus connect usb          — connect over USB`,
		Args: cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			if len(args) == 0 {
				return runConnect("192.168.43.1")
			}
			return runConnect(args[0])
		},
	}
	return cmd
}

func runConnect(target string) error {
	if target == "usb" {
		return connectUSB()
	}
	return connectWiFi(target)
}

func connectWiFi(ip string) error {
	if !strings.Contains(ip, ":") {
		ip = ip + ":5555"
	}

	fmt.Printf("Connecting to Control Hub at %s...\n", ip)

	if err := adb("connect", ip); err != nil {
		return fmt.Errorf("failed to connect: %w", err)
	}

	if err := saveConnectionTarget(ip); err != nil {
		fmt.Printf("warning: could not save connection target: %v\n", err)
	}

    startBackgroundWatcher()

	fmt.Printf("Connected! Run 'ftcplus build' to deploy.\n")
	return nil
}

func connectUSB() error {
	fmt.Println("Connecting over USB...")

	if err := adb("start-server"); err != nil {
		return fmt.Errorf("failed to start ADB server: %w", err)
	}

	out, err := exec.Command("adb", "devices").Output()
	if err != nil {
		return fmt.Errorf("failed to list ADB devices: %w", err)
	}

	lines := strings.Split(string(out), "\n")
	var devices []string
	for _, line := range lines[1:] {
		line = strings.TrimSpace(line)
		if line != "" && strings.Contains(line, "device") && !strings.Contains(line, "offline") {
			devices = append(devices, strings.Fields(line)[0])
		}
	}

	if len(devices) == 0 {
		return fmt.Errorf("no USB devices found — is the Control Hub plugged in with USB debugging enabled?")
	}

	target := "usb"
	if len(devices) == 1 {
		target = devices[0]
		fmt.Printf("Found device: %s\n", target)
	} else {
		fmt.Println("Multiple devices found:")
		for i, d := range devices {
			fmt.Printf("  %d. %s\n", i+1, d)
		}
		fmt.Print("Select device number: ")
		var choice int
		fmt.Scan(&choice)
		if choice < 1 || choice > len(devices) {
			return fmt.Errorf("invalid selection")
		}
		target = devices[choice-1]
	}

	if err := saveConnectionTarget(target); err != nil {
		fmt.Printf("warning: could not save connection target: %v\n", err)
	}

    startBackgroundWatcher()

	fmt.Printf("Connected to %s! Run 'ftcplus build' to deploy.\n", target)
	return nil
}

func adb(args ...string) error {
	cmd := exec.Command("adb", args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func saveConnectionTarget(target string) error {
	data, err := os.ReadFile("ftcplus.json")
	if err != nil {
		return err
	}

	var raw map[string]interface{}
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}

	raw["controlHub"] = target

	out, err := json.MarshalIndent(raw, "", "  ")
	if err != nil {
		return err
	}

	return os.WriteFile("ftcplus.json", out, 0644)
}

func startBackgroundWatcher() error {
    home, _ := os.UserHomeDir()
    logFile := filepath.Join(home, ".ftcplus", "watcher.log")
    pidFile := filepath.Join(home, ".ftcplus", "watcher.pid")

    if data, err := os.ReadFile(pidFile); err == nil {
        pid := strings.TrimSpace(string(data))
        exec.Command("kill", pid).Run()
    }

    cmd := exec.Command(os.Args[0], "watch", "--daemon")
    logF, _ := os.Create(logFile)
    cmd.Stdout = logF
    cmd.Stderr = logF

    if err := cmd.Start(); err != nil {
        return fmt.Errorf("failed to start watcher: %w", err)
    }

    os.WriteFile(pidFile, []byte(fmt.Sprintf("%d", cmd.Process.Pid)), 0644)
    fmt.Printf("Background watcher started (PID %d)\n", cmd.Process.Pid)
    return nil
}
package cmd

import (
    "encoding/json"
    "fmt"
    "os"
    "path/filepath"
    "strings"

    "github.com/spf13/cobra"
)

func hardwareListCmd() *cobra.Command {
    return &cobra.Command{
        Use:   "list",
        Short: "List hardware devices in this project",
        RunE: func(cmd *cobra.Command, args []string) error {
            return listJavaFiles("hardware", "hardware device")
        },
    }
}

func subsystemListCmd() *cobra.Command {
    return &cobra.Command{
        Use:   "list",
        Short: "List subsystems devices in this project",
        RunE: func(cmd *cobra.Command, args []string) error {
            return listJavaFiles("subsystems", "subsystem")
        },
    }
}

func signalListCmd() *cobra.Command {
    return &cobra.Command{
        Use:   "list",
        Short: "List signals in this project",
        RunE: func(cmd *cobra.Command, args []string) error {
            return listJavaFiles("signals", "signal")
        },
    }
}

func opModeListCmd() *cobra.Command {
    return &cobra.Command{
        Use:   "list",
        Short: "List opmodes in this project",
        RunE: func(cmd *cobra.Command, args []string) error {
            return listJavaFiles("opmodes", "opmode")
        },
    }
}

func robotListCmd() *cobra.Command {
    return &cobra.Command{
        Use:   "list",
        Short: "List robots in this project",
        RunE: func(cmd *cobra.Command, args []string) error {
            pkg, err := detectPackage()
            if err != nil {
                return err
            }

            srcBase := filepath.Join("src", "main", "java", strings.ReplaceAll(pkg, ".", "/"))

            var found []string

            if _, err := os.Stat(filepath.Join(srcBase, "Robot.java")); err == nil {
                found = append(found, "Robot (default)")
            }

            robotsDir := filepath.Join(srcBase, "robots")
            if entries, err := os.ReadDir(robotsDir); err == nil {
                for _, e := range entries {
                    if !e.IsDir() && strings.HasSuffix(e.Name(), ".java") &&
                        !strings.HasSuffix(e.Name(), "Config.java") &&
                        !strings.HasSuffix(e.Name(), "Hardware.java") &&
                        !strings.HasSuffix(e.Name(), "Globals.java") &&
                        !strings.HasSuffix(e.Name(), "Properties.java") {
                        found = append(found, strings.TrimSuffix(e.Name(), ".java"))
                    }
                }
            }

            if len(found) == 0 {
                fmt.Println("No robots found.")
                return nil
            }

            cfg, _ := readProjectConfig()
            active := ""
            if cfg != nil {
                active = cfg.ActiveRobot
            }

            fmt.Printf("Robots (%d):\n", len(found))
            for _, name := range found {
                marker := "  "
                if name == active || (active == "" && name == "Robot (default)") {
                    marker = "* "
                }
                fmt.Printf("%s%s\n", marker, name)
            }
            return nil
        },
    }
}

func robotSelectCmd() *cobra.Command {
    return &cobra.Command{
        Use:   "select <name>",
        Short: "Set the active robot",
        Args:  cobra.ExactArgs(1),
        RunE: func(cmd *cobra.Command, args []string) error {
            name := args[0]

            data, err := os.ReadFile("ftcplus.json")
            if err != nil {
                return fmt.Errorf("ftcplus.json not found — run from your project root")
            }

            var raw map[string]interface{}
            if err := json.Unmarshal(data, &raw); err != nil {
                return err
            }

            raw["activeRobot"] = name

            out, err := json.MarshalIndent(raw, "", "  ")
            if err != nil {
                return err
            }

            if err := os.WriteFile("ftcplus.json", out, 0644); err != nil {
                return err
            }

            fmt.Printf("Active robot set to: %s\n", name)
            return nil
        },
    }
}

func listJavaFiles(dir, kind string) error {
    pkg, err := detectPackage()
    if err != nil {
        return err
    }

    path := filepath.Join("src", "main", "java", strings.ReplaceAll(pkg, ".", "/"), dir)
    entries, err := os.ReadDir(path)
    if err != nil {
        fmt.Printf("No %ss found.\n", kind)
        return nil
    }

    var files []string
    for _, e := range entries {
        if !e.IsDir() && strings.HasSuffix(e.Name(), ".java") && e.Name() != ".gitkeep" {
            files = append(files, strings.TrimSuffix(e.Name(), ".java"))
        }
    }

    if len(files) == 0 {
        fmt.Printf("No %ss found.\n", kind)
        return nil
    }

    fmt.Printf("%s%ss (%d):\n", strings.ToUpper(kind[:1]), kind[1:], len(files))
    for _, f := range files {
        fmt.Printf("  %s\n", f)
    }
    return nil
}
package cmd

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"text/template"

	"github.com/charmbracelet/bubbles/textinput"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/spf13/cobra"
)

type newArgs struct {
	teamName   string
	teamNumber string
	pkg        string
}

type projectConfig struct {
    TeamName   string `json:"teamName"`
    TeamNumber string `json:"teamNumber"`
    Package    string `json:"package"`
    SdkTag     string `json:"sdkTag"`
    FtcPlus    string `json:"ftcplus"`
    ControlHub string `json:"controlHub,omitempty"`
    ActiveRobot string `json:"activeRobot"`
}

func newCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "new [team-name]",
		Short: "Create a new FTC+ project",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			a := newArgs{}
			if len(args) > 0 {
				a.teamName = args[0]
			}
			return runNewWizard(&a)
		},
	}
}


type newWizardModel struct {
	inputs  []textinput.Model
	focused int
	done    bool
}

var inputLabels = []string{"Team name", "Team number", "Java package (e.g. com.team1234)"}

func initialNewWizardModel(prefill *newArgs) newWizardModel {
	inputs := make([]textinput.Model, 3)
	for i := range inputs {
		t := textinput.New()
		t.CharLimit = 64
		inputs[i] = t
	}

	inputs[0].Placeholder = "My Team"
	inputs[1].Placeholder = "12345"
	inputs[2].Placeholder = "com.team12345"

	if prefill != nil {
		inputs[0].SetValue(prefill.teamName)
		inputs[1].SetValue(prefill.teamNumber)
		inputs[2].SetValue(prefill.pkg)
	}

	inputs[0].Focus()
	return newWizardModel{inputs: inputs, focused: 0}
}

func (m newWizardModel) Init() tea.Cmd { return textinput.Blink }

func (m newWizardModel) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "esc":
			return m, tea.Quit
		case "tab", "enter":
			if m.focused == len(m.inputs)-1 {
				m.done = true
				return m, tea.Quit
			}
			m.inputs[m.focused].Blur()
			m.focused++
			m.inputs[m.focused].Focus()
			return m, textinput.Blink
		}
	}

	var cmd tea.Cmd
	m.inputs[m.focused], cmd = m.inputs[m.focused].Update(msg)
	return m, cmd
}

func (m newWizardModel) View() string {
	var b strings.Builder
	b.WriteString(activeStyle.Render("FTC+ — New Project") + "\n\n")

	for i, input := range m.inputs {
		label := inputLabels[i]
		if i == m.focused {
			b.WriteString(activeStyle.Render("> "+label) + "\n")
		} else {
			b.WriteString(labelStyle.Render("  "+label) + "\n")
		}
		b.WriteString("  " + input.View() + "\n\n")
	}

	b.WriteString(labelStyle.Render("tab/enter to advance • esc to quit"))
	return b.String()
}

func runNewWizard(prefill *newArgs) error {
	m, err := tea.NewProgram(initialNewWizardModel(prefill)).Run()
	if err != nil {
		return err
	}

	result := m.(newWizardModel)
	if !result.done {
		return nil
	}

	args := newArgs{
		teamName:   result.inputs[0].Value(),
		teamNumber: result.inputs[1].Value(),
		pkg:        result.inputs[2].Value(),
	}

	if args.teamName == "" || args.teamNumber == "" || args.pkg == "" {
		return fmt.Errorf("all fields are required")
	}

	return scaffoldProject(args)
}


func scaffoldProject(args newArgs) error {
	dirName := strings.ReplaceAll(strings.ToLower(args.teamName), " ", "-")
	fmt.Printf("Creating FTC+ project in ./%s\n", dirName)

	if err := os.MkdirAll(dirName, 0755); err != nil {
		return fmt.Errorf("failed to create project directory: %w", err)
	}

	fmt.Println("Fetching latest FTC SDK version...")
	sdkTag, err := latestFtcSdkTag()
	if err != nil {
		return fmt.Errorf("failed to fetch FTC SDK version: %w", err)
	}
	fmt.Printf("Using FTC SDK %s\n", sdkTag)

	ftcPlusVersion := "1.0-SNAPSHOT"

	fmt.Println("Generating project files...")
	if err := generateProjectFiles(dirName, args, sdkTag, ftcPlusVersion); err != nil {
		return fmt.Errorf("failed to generate project files: %w", err)
	}

	home, _ := os.UserHomeDir()
	ftcPlusLib := filepath.Join(home, ".ftcplus")
	if _, err := os.Stat(ftcPlusLib); os.IsNotExist(err) {
		fmt.Println()
		fmt.Println("⚠  ~/.ftcplus not found — FTC+ library not installed.")
		fmt.Println("   Run the FTC+ installer to enable building:")
		fmt.Println("   curl -fsSL https://ftc.plus/install.sh | sh")
		fmt.Println("   (project files have been generated and are ready)")
	}

	fmt.Printf("\nDone! Open ./%s in your IDE.\n", dirName)
	return nil
}

func latestFtcSdkTag() (string, error) {
	resp, err := http.Get("https://api.github.com/repos/FIRST-Tech-Challenge/FtcRobotController/releases/latest")
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var release struct {
		TagName string `json:"tag_name"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&release); err != nil {
		return "", err
	}
	return release.TagName, nil
}


func generateProjectFiles(dir string, args newArgs, sdkTag, ftcPlusVersion string) error {
	data := map[string]string{
		"TeamName":      args.teamName,
		"TeamNumber":    args.teamNumber,
		"Package":       args.pkg,
		"SdkTag":        sdkTag,
		"FtcPlusVersion": ftcPlusVersion,
		"PackagePath":   strings.ReplaceAll(args.pkg, ".", "/"),
	}

	cfg := projectConfig{
		TeamName:   args.teamName,
		TeamNumber: args.teamNumber,
		Package:    args.pkg,
		SdkTag:     sdkTag,
		FtcPlus:    ftcPlusVersion,
	}
	b, err := json.MarshalIndent(cfg, "", "  ")
	if err != nil {
		return err
	}
	if err := os.WriteFile(filepath.Join(dir, "ftcplus.json"), b, 0644); err != nil {
		return err
	}

	srcBase := filepath.Join(dir, "src", "main", "java", data["PackagePath"])

	files := map[string]string{
		filepath.Join(dir, ".gitignore"):                     gitignoreTemplate,
		filepath.Join(srcBase, "Robot.java"):                      robotTemplate,
		filepath.Join(srcBase, "config", "Hardware.java"):         hardwareTemplate,
		filepath.Join(srcBase, "config", "Globals.java"):          globalsTemplate,
		filepath.Join(srcBase, "config", "Properties.java"):       propertiesTemplate,
	}

	for path, tmplStr := range files {
		if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
			return err
		}

		if path == filepath.Join(dir, "ftcplus.json") {
			cfg := projectConfig{
				TeamName:   args.teamName,
				TeamNumber: args.teamNumber,
				Package:    args.pkg,
				SdkTag:     sdkTag,
				FtcPlus:    ftcPlusVersion,
			}
			b, err := json.MarshalIndent(cfg, "", "  ")
			if err != nil {
				return err
			}
			if err := os.WriteFile(path, b, 0644); err != nil {
				return err
			}
			continue
		}

		tmpl, err := template.New("").Parse(tmplStr)
		if err != nil {
			return err
		}

		f, err := os.Create(path)
		if err != nil {
			return err
		}

		if err := tmpl.Execute(f, data); err != nil {
			f.Close()
			return err
		}
		f.Close()
	}

	emptyDirs := []string{"hardware", "subsystems", "signals", "opmodes"}
	for _, d := range emptyDirs {
		path := filepath.Join(srcBase, d)
		if err := os.MkdirAll(path, 0755); err != nil {
			return err
		}
		f, _ := os.Create(filepath.Join(path, ".gitkeep"))
		f.Close()
	}

	return nil
}


var gitignoreTemplate = `*.iml
.gradle
.DS_Store
/build
.idea
`

var robotTemplate = `package {{.Package}};

import dev.ftcplus.core.TeamRobot;
import {{.Package}}.config.Hardware;
import {{.Package}}.config.Globals;
import {{.Package}}.config.Properties;

@TeamRobot(name = "{{.TeamName}}")
public class Robot extends dev.ftcplus.core.Robot<Hardware, Globals, Properties> {
    public Robot() {
        super(Hardware.class, new Globals(), new Properties());
    }
}
`

var hardwareTemplate = `package {{.Package}}.config;

import dev.ftcplus.core.HardwareEntry;

public enum Hardware implements HardwareEntry {
    ;  // add hardware entries here, e.g: INTAKE_MOTOR("intake_motor")

    private final String name;

    Hardware(String name) {
        this.name = name;
    }

    @Override
    public String hardwareName() {
        return name;
    }
}
`

var globalsTemplate = `package {{.Package}}.config;

public class Globals {
    // Add global variables here
}
`

var propertiesTemplate = `package {{.Package}}.config;

import dev.ftcplus.core.RobotProperties;

public class Properties extends RobotProperties {
    // override to provide accurate values for your robot — all have sensible defaults

    // @Override public double trackWidthInches()                    { return 12.0; }
    // @Override public double wheelDiameterInches()                 { return 3.78; }
    // @Override public double maxLinearVelocityCmPerSecond()        { return 45.0; }
    // @Override public double maxAngularVelocityDegreesPerSecond()  { return 180.0; }
}
`
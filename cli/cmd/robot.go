package cmd

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"text/template"

	"github.com/charmbracelet/bubbles/textinput"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/spf13/cobra"
)

func robotCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "robot",
		Short: "Manage robots",
		RunE: func(cmd *cobra.Command, args []string) error {
			return runRobotAddWizard()
		},
	}
	cmd.AddCommand(robotAddCmd())
    cmd.AddCommand(robotListCmd())
    cmd.AddCommand(robotSelectCmd())
    cmd.AddCommand(robotModifyCmd())
	return cmd
}

type robotWizardModel struct {
	nameInput    textinput.Model
	displayInput textinput.Model
	state        int
	className    string
	displayName  string
	done         bool
}

func initialRobotWizardModel() robotWizardModel {
	name := textinput.New()
	name.Placeholder = "CompRobot"
	name.Focus()

	display := textinput.New()
	display.Placeholder = "Competition Robot"

	return robotWizardModel{nameInput: name, displayInput: display}
}

func (m robotWizardModel) Init() tea.Cmd { return textinput.Blink }

func (m robotWizardModel) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "esc":
			return m, tea.Quit
		case "enter":
			if m.state == 0 {
				if m.nameInput.Value() == "" {
					return m, nil
				}
				m.className = m.nameInput.Value()
				m.nameInput.Blur()
				m.displayInput.Focus()
				m.state = 1
				return m, textinput.Blink
			} else {
				m.displayName = m.displayInput.Value()
				if m.displayName == "" {
					m.displayName = m.className
				}
				m.done = true
				return m, tea.Quit
			}
		}
	}

	var cmd tea.Cmd
	if m.state == 0 {
		m.nameInput, cmd = m.nameInput.Update(msg)
	} else {
		m.displayInput, cmd = m.displayInput.Update(msg)
	}
	return m, cmd
}

func (m robotWizardModel) View() string {
	var b strings.Builder
	b.WriteString(activeStyle.Render("FTC+ — Add Robot") + "\n\n")
	if m.state == 0 {
		b.WriteString(activeStyle.Render("> Class name") + "\n")
		b.WriteString("  " + m.nameInput.View() + "\n")
	} else {
		b.WriteString(labelStyle.Render("  Class name: "+m.className) + "\n\n")
		b.WriteString(activeStyle.Render("> Display name") + "\n")
		b.WriteString("  " + m.displayInput.View() + "\n")
	}
	b.WriteString("\n" + labelStyle.Render("enter to advance • esc to quit"))
	return b.String()
}

func runRobotAddWizard() error {
	m, err := tea.NewProgram(initialRobotWizardModel()).Run()
	if err != nil {
		return err
	}

	result := m.(robotWizardModel)
	if !result.done {
		return nil
	}

	return generateRobotFile(result.className, result.displayName)
}

type robotTemplateData struct {
	Package     string
	ClassName   string
	DisplayName string
}

func generateRobotFile(className, displayName string) error {
	pkg, err := detectPackage()
	if err != nil {
		return err
	}

	data := robotTemplateData{
		Package:     pkg,
		ClassName:   className,
		DisplayName: displayName,
	}

	outputPath := filepath.Join(
		"src", "main", "java",
		strings.ReplaceAll(pkg, ".", "/"),
		"robots",
		className+".java",
	)

	configPath := filepath.Join(
		"src", "main", "java",
		strings.ReplaceAll(pkg, ".", "/"),
		"robots",
		className+"Config.java",
	)

	hardwarePath := filepath.Join(
		"src", "main", "java",
		strings.ReplaceAll(pkg, ".", "/"),
		"robots",
		className+"Hardware.java",
	)

	globalsPath := filepath.Join(
		"src", "main", "java",
		strings.ReplaceAll(pkg, ".", "/"),
		"robots",
		className+"Globals.java",
	)

	files := map[string]string{
		outputPath:   robotFileTemplate,
		configPath:   robotConfigTemplate,
		hardwarePath: robotHardwareTemplate,
		globalsPath:  robotGlobalsTemplate,
	}

	for path, tmplStr := range files {
		if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
			return err
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
		fmt.Printf("Created %s\n", path)
	}

	return nil
}

var robotFileTemplate = `package {{.Package}}.robots;

import dev.ftcplus.core.TeamRobot;
import dev.ftcplus.core.Robot;

@TeamRobot(name = "{{.DisplayName}}")
public class {{.ClassName}} extends Robot<{{.ClassName}}Hardware, {{.ClassName}}Globals> {
    public {{.ClassName}}() {
        super(new {{.ClassName}}Config());
    }
}
`

var robotConfigTemplate = `package {{.Package}}.robots;

import dev.ftcplus.core.RobotConfiguration;

public class {{.ClassName}}Config extends RobotConfiguration<{{.ClassName}}Hardware, {{.ClassName}}Globals> {
    public {{.ClassName}}Config() {
        super({{.ClassName}}Hardware.class, new {{.ClassName}}Globals());
    }
}
`

var robotHardwareTemplate = `package {{.Package}}.robots;

import dev.ftcplus.core.HardwareEntry;

public enum {{.ClassName}}Hardware implements HardwareEntry {
    // EXAMPLE_MOTOR("example_motor");
    ;

    private final String hardwareName;

    {{.ClassName}}Hardware(String hardwareName) {
        this.hardwareName = hardwareName;
    }

    @Override
    public String hardwareName() {
        return hardwareName;
    }
}
`

var robotGlobalsTemplate = `package {{.Package}}.robots;

public class {{.ClassName}}Globals {
    // Add global variables here
}
`
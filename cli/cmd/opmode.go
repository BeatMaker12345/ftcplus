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

func opModeCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "opmode",
		Short: "Manage opmodes",
		RunE: func(cmd *cobra.Command, args []string) error {
			return runOpModeAddWizard()
		},
	}
	cmd.AddCommand(&cobra.Command{
		Use:   "add [name]",
		Short: "Add an opmode",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			return runOpModeAddWizard()
		},
	})
    cmd.AddCommand(opModeListCmd())
    cmd.AddCommand(opModeModifyCmd())
	return cmd
}

type opModeWizardModel struct {
	nameInput textinput.Model
	dsName    textinput.Model
	state     int
	className string
	dsNameStr string
	done      bool
}

func initialOpModeWizardModel() opModeWizardModel {
	name := textinput.New()
	name.Placeholder = "MainTeleOp"
	name.Focus()

	ds := textinput.New()
	ds.Placeholder = "TeleOp"

	return opModeWizardModel{nameInput: name, dsName: ds}
}

func (m opModeWizardModel) Init() tea.Cmd { return textinput.Blink }

func (m opModeWizardModel) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
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
				m.dsName.Focus()
				m.state = 1
				return m, textinput.Blink
			} else {
				m.dsNameStr = m.dsName.Value()
				if m.dsNameStr == "" {
					m.dsNameStr = m.className
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
		m.dsName, cmd = m.dsName.Update(msg)
	}
	return m, cmd
}

func (m opModeWizardModel) View() string {
	var b strings.Builder
	b.WriteString(activeStyle.Render("FTC+ — Add OpMode") + "\n\n")
	if m.state == 0 {
		b.WriteString(activeStyle.Render("> Class name") + "\n")
		b.WriteString("  " + m.nameInput.View() + "\n")
	} else {
		b.WriteString(labelStyle.Render("  Class name: "+m.className) + "\n\n")
		b.WriteString(activeStyle.Render("> Driver Station name") + "\n")
		b.WriteString("  " + m.dsName.View() + "\n")
	}
	b.WriteString("\n" + labelStyle.Render("enter to advance • esc to quit"))
	return b.String()
}

func runOpModeAddWizard() error {
	m, err := tea.NewProgram(initialOpModeWizardModel()).Run()
	if err != nil {
		return err
	}

	result := m.(opModeWizardModel)
	if !result.done {
		return nil
	}

	return generateOpModeFile(result.className, result.dsNameStr)
}

type opModeTemplateData struct {
	Package   string
	ClassName string
	DSName    string
}

func generateOpModeFile(className, dsName string) error {
	pkg, err := detectPackage()
	if err != nil {
		return err
	}

	data := opModeTemplateData{
		Package:   pkg,
		ClassName: className,
		DSName:    dsName,
	}

	outputPath := filepath.Join(
		"src", "main", "java",
		strings.ReplaceAll(pkg, ".", "/"),
		"opmodes",
		className+".java",
	)

	if err := os.MkdirAll(filepath.Dir(outputPath), 0755); err != nil {
		return err
	}

	tmpl, err := template.New("").Parse(opModeTemplate)
	if err != nil {
		return err
	}

	f, err := os.Create(outputPath)
	if err != nil {
		return err
	}
	defer f.Close()

	if err := tmpl.Execute(f, data); err != nil {
		return err
	}

	fmt.Printf("Created %s\n", outputPath)
	return nil
}

var opModeTemplate = `package {{.Package}}.opmodes;

import dev.ftcplus.runtime.OpMode;
import dev.ftcplus.core.input.GamepadButton;

@OpMode.Register("TeleOp")
public class {{.ClassName}} extends OpMode {

    @Override
    protected void configure() {
        // TODO: configure controls
    }
}
`
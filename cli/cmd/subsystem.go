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

func subsystemCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "subsystem",
		Short: "Manage subsystems",
		RunE: func(cmd *cobra.Command, args []string) error {
			return runSubsystemAddWizard()
		},
	}
	cmd.AddCommand(&cobra.Command{
		Use:   "add [name]",
		Short: "Add a subsystem",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			return runSubsystemAddWizard()
		},
	})
	return cmd
}


type subsystemWizardState int

const (
	subsystemStateName subsystemWizardState = iota
	subsystemStateStates
	subsystemStateHardware
	subsystemStateDone
)

type subsystemWizardModel struct {
	state        subsystemWizardState
	nameInput    textinput.Model
	statesInput  textinput.Model
	hardwareInput textinput.Model
	className    string
	states       []string
	hardware     []string
	done         bool
}

func initialSubsystemWizardModel() subsystemWizardModel {
	name := textinput.New()
	name.Placeholder = "Intake"
	name.Focus()

	states := textinput.New()
	states.Placeholder = "IDLE, INTAKING, HOLDING"

	hw := textinput.New()
	hw.Placeholder = "IntakeMotor, IntakeSensor (comma separated, leave blank to skip)"

	return subsystemWizardModel{
		state:         subsystemStateName,
		nameInput:     name,
		statesInput:   states,
		hardwareInput: hw,
	}
}

func (m subsystemWizardModel) Init() tea.Cmd { return textinput.Blink }

func (m subsystemWizardModel) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "esc":
			return m, tea.Quit
		case "enter":
			switch m.state {
			case subsystemStateName:
				if m.nameInput.Value() == "" {
					return m, nil
				}
				m.className = m.nameInput.Value()
				m.nameInput.Blur()
				m.statesInput.Focus()
				m.state = subsystemStateStates
				return m, textinput.Blink
			case subsystemStateStates:
				raw := m.statesInput.Value()
				for _, s := range strings.Split(raw, ",") {
					s = strings.TrimSpace(s)
					if s != "" {
						m.states = append(m.states, strings.ToUpper(s))
					}
				}
				m.statesInput.Blur()
				m.hardwareInput.Focus()
				m.state = subsystemStateHardware
				return m, textinput.Blink
			case subsystemStateHardware:
				raw := m.hardwareInput.Value()
				for _, h := range strings.Split(raw, ",") {
					h = strings.TrimSpace(h)
					if h != "" {
						m.hardware = append(m.hardware, h)
					}
				}
				m.done = true
				return m, tea.Quit
			}
		}
	}

	var cmd tea.Cmd
	switch m.state {
	case subsystemStateName:
		m.nameInput, cmd = m.nameInput.Update(msg)
	case subsystemStateStates:
		m.statesInput, cmd = m.statesInput.Update(msg)
	case subsystemStateHardware:
		m.hardwareInput, cmd = m.hardwareInput.Update(msg)
	}
	return m, cmd
}

func (m subsystemWizardModel) View() string {
	var b strings.Builder
	b.WriteString(activeStyle.Render("FTC+ — Add Subsystem") + "\n\n")

	switch m.state {
	case subsystemStateName:
		b.WriteString(activeStyle.Render("> Class name") + "\n")
		b.WriteString("  " + m.nameInput.View() + "\n")
	case subsystemStateStates:
		b.WriteString(labelStyle.Render("  Class name: " + m.className) + "\n\n")
		b.WriteString(activeStyle.Render("> States (comma separated)") + "\n")
		b.WriteString("  " + m.statesInput.View() + "\n")
	case subsystemStateHardware:
		b.WriteString(labelStyle.Render("  Class name: "+m.className) + "\n")
		b.WriteString(labelStyle.Render("  States: "+strings.Join(m.states, ", ")) + "\n\n")
		b.WriteString(activeStyle.Render("> Hardware devices (comma separated)") + "\n")
		b.WriteString("  " + m.hardwareInput.View() + "\n")
	}

	b.WriteString("\n" + labelStyle.Render("enter to advance • esc to quit"))
	return b.String()
}

func runSubsystemAddWizard() error {
	m, err := tea.NewProgram(initialSubsystemWizardModel()).Run()
	if err != nil {
		return err
	}

	result := m.(subsystemWizardModel)
	if !result.done {
		return nil
	}

	return generateSubsystemFile(result.className, result.states, result.hardware)
}


type subsystemTemplateData struct {
	Package   string
	ClassName string
	States    []string
	Hardware  []subsystemHardwareEntry
}

type subsystemHardwareEntry struct {
	ClassName string
	FieldName string
}

func generateSubsystemFile(className string, states []string, hardware []string) error {
	pkg, err := detectPackage()
	if err != nil {
		return err
	}

	if len(states) == 0 {
		states = []string{"IDLE"}
	}

	hwEntries := make([]subsystemHardwareEntry, len(hardware))
	for i, h := range hardware {
		hwEntries[i] = subsystemHardwareEntry{
			ClassName: h,
			FieldName: lcFirst(h),
		}
	}

	data := subsystemTemplateData{
		Package:   pkg,
		ClassName: className,
		States:    states,
		Hardware:  hwEntries,
	}

	outputPath := filepath.Join(
		"TeamCode/src/main/java",
		strings.ReplaceAll(pkg, ".", "/"),
		"subsystems",
		className+".java",
	)

	if err := os.MkdirAll(filepath.Dir(outputPath), 0755); err != nil {
		return err
	}

	tmpl, err := template.New("").Parse(subsystemTemplate)
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

func lcFirst(s string) string {
	if s == "" {
		return s
	}
	return strings.ToLower(s[:1]) + s[1:]
}

var subsystemTemplate = `package {{.Package}}.subsystems;

import dev.ftcplus.core.Subsystem;
import dev.ftcplus.core.statemachine.StateMachine;
{{range .Hardware}}import {{$.Package}}.hardware.{{.ClassName}};
{{end}}
public class {{.ClassName}} extends Subsystem<{{.ClassName}}.State> {

    public enum State {
        {{range $i, $s := .States}}{{if $i}}, {{end}}{{$s}}{{end}}
    }
{{range .Hardware}}
    private final {{.ClassName}} {{.FieldName}} = register(new {{.ClassName}}());
{{end}}
    @Override
    protected State initialState() {
        return State.{{index .States 0}};
    }

    @Override
    protected void defineStates(StateMachine<State> states) {
        {{range .States}}states.state(State.{{.}})
            .onEnter(() -> { /* TODO */ });

        {{end}}
    }
}
`
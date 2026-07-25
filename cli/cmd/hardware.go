package cmd

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"text/template"

	"dev.ftcplus/cli/internal/catalog"
	"github.com/charmbracelet/bubbles/list"
	"github.com/charmbracelet/bubbles/textinput"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/spf13/cobra"
)

func hardwareCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "hardware",
		Short: "Manage hardware devices",
		RunE: func(cmd *cobra.Command, args []string) error {
			return runHardwareAddWizard()
		},
	}
	cmd.AddCommand(hardwareAddCmd())
	return cmd
}

func hardwareAddCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "add [name]",
		Short: "Add a hardware device",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			return runHardwareAddWizard()
		},
	}
}


type hwType string

const (
	hwMotor   hwType = "motor"
	hwServo   hwType = "servo"
	hwCRServo hwType = "crservo"
)

type hwTypeItem struct{ t hwType }

func (i hwTypeItem) Title() string       { return string(i.t) }
func (i hwTypeItem) Description() string { return "" }
func (i hwTypeItem) FilterValue() string { return string(i.t) }

type hwTypePicker struct {
	list   list.Model
	choice hwType
	done   bool
}

func (m hwTypePicker) Init() tea.Cmd { return nil }

func (m hwTypePicker) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "esc":
			return m, tea.Quit
		case "enter":
			item, ok := m.list.SelectedItem().(hwTypeItem)
			if ok {
				m.choice = item.t
				m.done = true
			}
			return m, tea.Quit
		}
	case tea.WindowSizeMsg:
		h, v := docStyle.GetFrameSize()
		m.list.SetSize(msg.Width-h, msg.Height-v)
	}
	var cmd tea.Cmd
	m.list, cmd = m.list.Update(msg)
	return m, cmd
}

func (m hwTypePicker) View() string {
	return docStyle.Render(m.list.View())
}


type partItem struct {
	name     string
	constant string
}

func (i partItem) Title() string       { return i.name }
func (i partItem) Description() string { return i.constant }
func (i partItem) FilterValue() string { return i.name }

type partPicker struct {
	list     list.Model
	choice   partItem
	done     bool
	noSpec   bool
}

func (m partPicker) Init() tea.Cmd { return nil }

func (m partPicker) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "esc":
			return m, tea.Quit
		case "enter":
			item, ok := m.list.SelectedItem().(partItem)
			if ok {
				m.choice = item
				m.done = true
			}
			return m, tea.Quit
		case "tab":
			// skip spec selection
			m.noSpec = true
			m.done = true
			return m, tea.Quit
		}
	case tea.WindowSizeMsg:
		h, v := docStyle.GetFrameSize()
		m.list.SetSize(msg.Width-h, msg.Height-v)
	}
	var cmd tea.Cmd
	m.list, cmd = m.list.Update(msg)
	return m, cmd
}

func (m partPicker) View() string {
	return docStyle.Render(m.list.View() + "\n\ntab to skip part selection")
}


type nameInputModel struct {
	input textinput.Model
	done  bool
}

func (m nameInputModel) Init() tea.Cmd { return textinput.Blink }

func (m nameInputModel) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "esc":
			return m, tea.Quit
		case "enter":
			if m.input.Value() != "" {
				m.done = true
				return m, tea.Quit
			}
		}
	}
	var cmd tea.Cmd
	m.input, cmd = m.input.Update(msg)
	return m, cmd
}

func (m nameInputModel) View() string {
	return fmt.Sprintf("\n  Class name (e.g. IntakeMotor)\n  %s\n\n%s",
		m.input.View(),
		labelStyle.Render("enter to confirm • esc to quit"))
}


func runHardwareAddWizard() error {
	typeItems := []list.Item{
		hwTypeItem{hwMotor},
		hwTypeItem{hwServo},
		hwTypeItem{hwCRServo},
	}
	typeList := list.New(typeItems, list.NewDefaultDelegate(), 40, 10)
	typeList.Title = "Hardware type"
	typeList.SetShowStatusBar(false)
	typeList.SetFilteringEnabled(false)

	tm, err := tea.NewProgram(hwTypePicker{list: typeList}).Run()
	if err != nil {
		return err
	}
	typePicked := tm.(hwTypePicker)
	if !typePicked.done {
		return nil
	}
	hwt := typePicked.choice

	cat, err := catalog.Load()
	if err != nil {
		return fmt.Errorf("failed to load catalog: %w", err)
	}

	var partItems []list.Item
	switch hwt {
	case hwMotor:
		for _, m := range cat.Motors {
			partItems = append(partItems, partItem{name: m.Name, constant: m.Constant})
		}
	case hwServo:
		for _, s := range cat.Servos {
			partItems = append(partItems, partItem{name: s.Name, constant: s.Constant})
		}
	case hwCRServo:
		for _, c := range cat.CRServos {
			partItems = append(partItems, partItem{name: c.Name, constant: c.Constant})
		}
	}

	partList := list.New(partItems, list.NewDefaultDelegate(), 60, 20)
	partList.Title = fmt.Sprintf("Select %s model (tab to skip)", hwt)

	pm, err := tea.NewProgram(partPicker{list: partList}, tea.WithAltScreen()).Run()
	if err != nil {
		return err
	}
	partPicked := pm.(partPicker)

	var selectedConstant string
	if !partPicked.noSpec && partPicked.done {
		selectedConstant = partPicked.choice.constant
	}

	ti := textinput.New()
	ti.Placeholder = "IntakeMotor"
	ti.Focus()

	nm, err := tea.NewProgram(nameInputModel{input: ti}).Run()
	if err != nil {
		return err
	}
	namePicked := nm.(nameInputModel)
	if !namePicked.done {
		return nil
	}
	className := namePicked.input.Value()

	return generateHardwareFile(className, hwt, selectedConstant)
}


type hardwareTemplateData struct {
	Package   string
	ClassName string
	BaseClass string
	SpecImport string
	SpecConst string
	HasSpec   bool
}

func generateHardwareFile(className string, hwt hwType, specConstant string) error {
	pkg, err := detectPackage()
	if err != nil {
		return err
	}

	var baseClass, specImport string
	switch hwt {
	case hwMotor:
		baseClass = "Motor"
		specImport = "dev.ftcplus.core.motor.Motor"
	case hwServo:
		baseClass = "Servo"
		specImport = "dev.ftcplus.core.servo.Servo"
	case hwCRServo:
		baseClass = "CRServo"
		specImport = "dev.ftcplus.core.servo.CRServo"
	}

	data := hardwareTemplateData{
		Package:   pkg,
		ClassName: className,
		BaseClass: baseClass,
		SpecImport: specImport,
		SpecConst: specConstant,
		HasSpec:   specConstant != "",
	}

	outputPath := filepath.Join(
		"TeamCode/src/main/java",
		strings.ReplaceAll(pkg, ".", "/"),
		"hardware",
		className+".java",
	)

	if err := os.MkdirAll(filepath.Dir(outputPath), 0755); err != nil {
		return err
	}

	tmpl, err := template.New("").Parse(hardwareFileTemplate)
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

var hardwareFileTemplate = `package {{.Package}}.hardware;

import {{.SpecImport}};
{{- if .HasSpec}}
import dev.ftcplus.catalog.GoBILDA;
{{- end}}
import {{.Package}}.config.Hardware;

public class {{.ClassName}} extends {{.BaseClass}} {

    public {{.ClassName}}() {
        super(Hardware./* TODO: set hardware entry */{{if .HasSpec}}, {{.SpecConst}}{{end}});
    }

    // Add methods here
}
`


func detectPackage() (string, error) {
	var pkg string
	err := filepath.Walk("TeamCode/src/main", func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || info.Name() != "AndroidManifest.xml" {
			return err
		}
		data, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		content := string(data)
		idx := strings.Index(content, `package="`)
		if idx == -1 {
			return nil
		}
		start := idx + len(`package="`)
		end := strings.Index(content[start:], `"`)
		if end == -1 {
			return nil
		}
		pkg = content[start : start+end]
		return nil
	})
	if err != nil {
		return "", err
	}
	if pkg == "" {
		return "", fmt.Errorf("could not detect package — run this command from your project root")
	}
	return pkg, nil
}
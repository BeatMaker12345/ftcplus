package cmd

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"text/template"

	"github.com/charmbracelet/bubbles/list"
	"github.com/charmbracelet/bubbles/textinput"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/spf13/cobra"
)

func signalCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "signal",
		Short: "Manage signals",
		RunE: func(cmd *cobra.Command, args []string) error {
			return runSignalAddWizard()
		},
	}
	cmd.AddCommand(&cobra.Command{
		Use:   "add [name]",
		Short: "Add a signal",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			return runSignalAddWizard()
		},
	})
	return cmd
}


type signalKind string

const (
	signalEvent   signalKind = "Event"
	signalMessage signalKind = "Message"
)

type signalKindItem struct{ k signalKind }

func (i signalKindItem) Title() string       { return string(i.k) }
func (i signalKindItem) Description() string {
	if i.k == signalEvent {
		return "Something that happened at a moment"
	}
	return "Data, a reading, or a command-like intent"
}
func (i signalKindItem) FilterValue() string { return string(i.k) }

type signalKindPicker struct {
	list   list.Model
	choice signalKind
	done   bool
}

func (m signalKindPicker) Init() tea.Cmd { return nil }

func (m signalKindPicker) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "esc":
			return m, tea.Quit
		case "enter":
			item, ok := m.list.SelectedItem().(signalKindItem)
			if ok {
				m.choice = item.k
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

func (m signalKindPicker) View() string {
	return docStyle.Render(m.list.View())
}

type signalWizardState int

const (
	signalStateName signalWizardState = iota
	signalStateParams
	signalStateDone
)

type signalParam struct {
	Type string
	Name string
}

type signalWizardModel struct {
	state      signalWizardState
	nameInput  textinput.Model
	paramInput textinput.Model
	className  string
	kind       signalKind
	params     []signalParam
	done       bool
}

func initialSignalWizardModel(kind signalKind) signalWizardModel {
	name := textinput.New()
	name.Placeholder = "GamePieceDetected"
	name.Focus()

	param := textinput.New()
	param.Placeholder = "double confidence, int id (leave blank for no params)"

	return signalWizardModel{
		kind:       kind,
		nameInput:  name,
		paramInput: param,
	}
}

func (m signalWizardModel) Init() tea.Cmd { return textinput.Blink }

func (m signalWizardModel) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "esc":
			return m, tea.Quit
		case "enter":
			switch m.state {
			case signalStateName:
				if m.nameInput.Value() == "" {
					return m, nil
				}
				m.className = m.nameInput.Value()
				m.nameInput.Blur()
				m.paramInput.Focus()
				m.state = signalStateParams
				return m, textinput.Blink
			case signalStateParams:
				raw := m.paramInput.Value()
				if raw != "" {
					for _, p := range strings.Split(raw, ",") {
						parts := strings.Fields(strings.TrimSpace(p))
						if len(parts) == 2 {
							m.params = append(m.params, signalParam{Type: parts[0], Name: parts[1]})
						}
					}
				}
				m.done = true
				return m, tea.Quit
			}
		}
	}

	var cmd tea.Cmd
	switch m.state {
	case signalStateName:
		m.nameInput, cmd = m.nameInput.Update(msg)
	case signalStateParams:
		m.paramInput, cmd = m.paramInput.Update(msg)
	}
	return m, cmd
}

func (m signalWizardModel) View() string {
	var b strings.Builder
	b.WriteString(activeStyle.Render("FTC+ — Add "+string(m.kind)) + "\n\n")

	switch m.state {
	case signalStateName:
		b.WriteString(activeStyle.Render("> Class name") + "\n")
		b.WriteString("  " + m.nameInput.View() + "\n")
	case signalStateParams:
		b.WriteString(labelStyle.Render("  Class name: "+m.className) + "\n\n")
		b.WriteString(activeStyle.Render("> Parameters (e.g. double confidence, int id)") + "\n")
		b.WriteString("  " + m.paramInput.View() + "\n")
	}

	b.WriteString("\n" + labelStyle.Render("enter to advance • esc to quit"))
	return b.String()
}

func runSignalAddWizard() error {
	kindItems := []list.Item{
		signalKindItem{signalEvent},
		signalKindItem{signalMessage},
	}
	kindList := list.New(kindItems, list.NewDefaultDelegate(), 50, 8)
	kindList.Title = "Signal type"
	kindList.SetShowStatusBar(false)
	kindList.SetFilteringEnabled(false)

	km, err := tea.NewProgram(signalKindPicker{list: kindList}).Run()
	if err != nil {
		return err
	}
	kindPicked := km.(signalKindPicker)
	if !kindPicked.done {
		return nil
	}

	m, err := tea.NewProgram(initialSignalWizardModel(kindPicked.choice)).Run()
	if err != nil {
		return err
	}

	result := m.(signalWizardModel)
	if !result.done {
		return nil
	}

	return generateSignalFile(result.className, result.kind, result.params)
}


type signalTemplateData struct {
	Package   string
	ClassName string
	BaseClass string
	Params    []signalParam
}

func generateSignalFile(className string, kind signalKind, params []signalParam) error {
	pkg, err := detectPackage()
	if err != nil {
		return err
	}

	data := signalTemplateData{
		Package:   pkg,
		ClassName: className,
		BaseClass: string(kind),
		Params:    params,
	}

	outputPath := filepath.Join(
		"TeamCode/src/main/java",
		strings.ReplaceAll(pkg, ".", "/"),
		"signals",
		className+".java",
	)

	if err := os.MkdirAll(filepath.Dir(outputPath), 0755); err != nil {
		return err
	}

	tmpl, err := template.New("").Parse(signalTemplate)
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

var signalTemplate = `package {{.Package}}.signals;

import dev.ftcplus.core.signal.{{.BaseClass}};

public final class {{.ClassName}} extends {{.BaseClass}} {
{{if .Params}}
{{range .Params}}    public final {{.Type}} {{.Name}};
{{end}}
    public {{.ClassName}}({{range $i, $p := .Params}}{{if $i}}, {{end}}{{$p.Type}} {{$p.Name}}{{end}}) {
{{range .Params}}        this.{{.Name}} = {{.Name}};
{{end}}    }
{{else}}
    public {{.ClassName}}() {}
{{end}}}
`
package cmd

import (
    tea "github.com/charmbracelet/bubbletea"
    "github.com/charmbracelet/bubbles/list"
    "github.com/charmbracelet/lipgloss"
)

var docStyle = lipgloss.NewStyle().Margin(1, 2)

type wizardItem struct {
    title, desc string
}

func (i wizardItem) Title() string       { return i.title }
func (i wizardItem) Description() string { return i.desc }
func (i wizardItem) FilterValue() string { return i.title }

type mainWizardModel struct {
    list     list.Model
    choice   string
    quitting bool
}

func (m mainWizardModel) Init() tea.Cmd {
    return nil
}

func (m mainWizardModel) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
    switch msg := msg.(type) {
    case tea.KeyMsg:
        switch msg.String() {
        case "ctrl+c", "q":
            m.quitting = true
            return m, tea.Quit
        case "enter":
            item, ok := m.list.SelectedItem().(wizardItem)
            if ok {
                m.choice = item.title
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

func (m mainWizardModel) View() string {
    if m.quitting {
        return ""
    }
    return docStyle.Render(m.list.View())
}

func runMainWizard() error {
    items := []list.Item{
        wizardItem{"new", "Create a new FTC+ project"},
        wizardItem{"hardware add", "Add a hardware device"},
        wizardItem{"subsystem add", "Add a subsystem"},
        wizardItem{"signal add", "Add a signal"},
        wizardItem{"opmode add", "Add an OpMode"},
        wizardItem{"robot add", "Add a robot"},
        wizardItem{"update", "Update FTC+ dependencies"},
    }

    l := list.New(items, list.NewDefaultDelegate(), 0, 0)
    l.Title = "FTC+"
    l.SetShowStatusBar(false)
    l.SetFilteringEnabled(false)

    m, err := tea.NewProgram(mainWizardModel{list: l}, tea.WithAltScreen()).Run()
    if err != nil {
        return err
    }

    result := m.(mainWizardModel)
    if result.quitting || result.choice == "" {
        return nil
    }

    switch result.choice {
    case "new":
        return runNewWizard(nil)
    case "hardware add":
        return runHardwareAddWizard()
    case "subsystem add":
        return runSubsystemAddWizard()
    case "signal add":
        return runSignalAddWizard()
    case "opmode add":
        return runOpModeAddWizard()
    case "robot add":
        return runRobotAddWizard()
    case "update":
        return runUpdate()
    }

    return nil
}
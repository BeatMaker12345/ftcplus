package cmd

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/charmbracelet/bubbles/list"
	"github.com/charmbracelet/bubbles/textinput"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/spf13/cobra"
)


func findSourceFile(kind, name, pkg string) string {
	dirs := map[string]string{
		"subsystem": "subsystems",
		"hardware":  "hardware",
		"opmode":    "opmodes",
		"signal":    "signals",
		"robot":     "",
	}
	base := filepath.Join("src", "main", "java", strings.ReplaceAll(pkg, ".", "/"))
	dir := dirs[kind]
	if dir == "" {
		return filepath.Join(base, name+".java")
	}
	return filepath.Join(base, dir, name+".java")
}

func listNamesInDir(pkg, dir string) ([]string, error) {
	path := filepath.Join("src", "main", "java", strings.ReplaceAll(pkg, ".", "/"), dir)
	entries, err := os.ReadDir(path)
	if err != nil {
		return nil, err
	}
	var names []string
	for _, e := range entries {
		if !e.IsDir() && strings.HasSuffix(e.Name(), ".java") {
			names = append(names, strings.TrimSuffix(e.Name(), ".java"))
		}
	}
	return names, nil
}

func pickFromList(title string, names []string) (string, error) {
	items := make([]list.Item, len(names))
	for i, n := range names {
		items[i] = simpleItem(n)
	}
	l := list.New(items, list.NewDefaultDelegate(), 50, 20)
	l.Title = title
	l.SetShowStatusBar(false)
	l.SetFilteringEnabled(true)

	m, err := tea.NewProgram(simplePicker{list: l}).Run()
	if err != nil {
		return "", err
	}
	picked := m.(simplePicker)
	if !picked.done {
		return "", nil
	}
	return picked.choice, nil
}


type simpleItem string

func (i simpleItem) Title() string       { return string(i) }
func (i simpleItem) Description() string { return "" }
func (i simpleItem) FilterValue() string { return string(i) }

type simplePicker struct {
	list   list.Model
	choice string
	done   bool
}

func (m simplePicker) Init() tea.Cmd { return nil }

func (m simplePicker) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "esc":
			return m, tea.Quit
		case "enter":
			if item, ok := m.list.SelectedItem().(simpleItem); ok {
				m.choice = string(item)
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

func (m simplePicker) View() string { return docStyle.Render(m.list.View()) }


func promptText(label, placeholder string) (string, error) {
	ti := textinput.New()
	ti.Placeholder = placeholder
	ti.Focus()

	m, err := tea.NewProgram(nameInputModel{input: ti}).Run()
	if err != nil {
		return "", err
	}
	picked := m.(nameInputModel)
	if !picked.done {
		return "", nil
	}
	return picked.input.Value(), nil
}


func subsystemModifyCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "modify [Name]",
		Short: "Modify a subsystem",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			pkg, err := detectPackage()
			if err != nil {
				return err
			}

			var name string
			if len(args) > 0 {
				name = args[0]
			} else {
				names, err := listNamesInDir(pkg, "subsystems")
				if err != nil || len(names) == 0 {
					return fmt.Errorf("no subsystems found")
				}
				name, err = pickFromList("Select subsystem", names)
				if err != nil || name == "" {
					return err
				}
			}

			return runSubsystemModify(name, pkg)
		},
	}
}

func runSubsystemModify(name, pkg string) error {
	ast, err := getAstClient()
	if err != nil {
		return err
	}

	filePath := findSourceFile("subsystem", name, pkg)
	parsed, err := ast.parse(filePath)
	if err != nil {
		return fmt.Errorf("failed to parse %s: %w", name, err)
	}

	for {
		action, err := pickFromList("Modify "+name, []string{
			"Add state",
			"Remove state",
			"Add hardware device",
			"Remove hardware device",
			"Add telemetry",
			"Add @Setting",
			"Add @Diagnostic",
			"Done",
		})
		if err != nil || action == "" || action == "Done" {
			return err
		}

		switch action {
		case "Add state":
			stateName, err := promptText("State name", "EJECTING")
			if err != nil || stateName == "" {
				continue
			}
			if err := ast.addState(filePath, stateName); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added state %s\n", stateName)
			}

		case "Remove state":
			states := toStringSlice(parsed["states"])
			if len(states) == 0 {
				fmt.Println("No states found.")
				continue
			}
			state, err := pickFromList("Remove state", states)
			if err != nil || state == "" {
				continue
			}
			if err := ast.removeState(filePath, state); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Removed state %s\n", state)
			}

		case "Add hardware device":
			hwNames, _ := listNamesInDir(pkg, "hardware")
			if len(hwNames) == 0 {
				fmt.Println("No hardware devices found — run 'ftcplus hardware add' first.")
				continue
			}
			hwName, err := pickFromList("Select hardware device", hwNames)
			if err != nil || hwName == "" {
				continue
			}
			fieldName := strings.ToLower(hwName[:1]) + hwName[1:]
			init := fmt.Sprintf("register(new %s())", hwName)
			if err := ast.addField(filePath, hwName, fieldName, init); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added %s %s\n", hwName, fieldName)
			}

		case "Remove hardware device":
			fields := fieldsOfKind(parsed, pkg)
			if len(fields) == 0 {
				fmt.Println("No hardware fields found.")
				continue
			}
			field, err := pickFromList("Remove hardware device", fields)
			if err != nil || field == "" {
				continue
			}
			if err := ast.removeField(filePath, field); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Removed field %s\n", field)
			}

		case "Add telemetry":
			key, err := promptText("Telemetry key", "intakeState")
			if err != nil || key == "" {
				continue
			}
			expr, err := promptText("Expression", "currentState().toString()")
			if err != nil || expr == "" {
				continue
			}
			stmt := fmt.Sprintf(`telemetry().panel("%s").line(() -> %s)`, name, expr)
			if err := ast.addField(filePath, "/* telemetry */", "_tel_"+key, stmt); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added telemetry for %s\n", key)
			}

		case "Add @Setting":
			fieldType, err := pickFromList("Setting type", []string{"double", "int", "boolean", "String"})
			if err != nil || fieldType == "" {
				continue
			}
			fieldName, err := promptText("Field name", "speed")
			if err != nil || fieldName == "" {
				continue
			}
			defaultVal, err := promptText("Default value", "1.0")
			if err != nil || defaultVal == "" {
				continue
			}
			label, err := promptText("Display label", fieldName)
			if err != nil {
				continue
			}
			if label == "" {
				label = fieldName
			}
			if err := ast.addField(filePath, "@Setting(\""+label+"\") "+fieldType, fieldName, defaultVal); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added @Setting %s\n", fieldName)
			}

		case "Add @Diagnostic":
			methodName, err := promptText("Method name", "checkTravel")
			if err != nil || methodName == "" {
				continue
			}
			label, err := promptText("Display label", methodName)
			if err != nil {
				continue
			}
			if label == "" {
				label = methodName
			}
			stub := fmt.Sprintf(`@Diagnostic("%s")
    public DiagnosticResult %s() throws InterruptedException {
        // TODO: implement diagnostic
        return DiagnosticResult.pass("OK");
    }`, label, methodName)
			if err := ast.addField(filePath, "/* diagnostic */", "_diag_"+methodName, stub); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added @Diagnostic %s\n", methodName)
			}
		}
	}
}


func hardwareModifyCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "modify [Name]",
		Short: "Modify a hardware device",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			pkg, err := detectPackage()
			if err != nil {
				return err
			}

			var name string
			if len(args) > 0 {
				name = args[0]
			} else {
				names, _ := listNamesInDir(pkg, "hardware")
				if len(names) == 0 {
					return fmt.Errorf("no hardware devices found")
				}
				name, err = pickFromList("Select hardware device", names)
				if err != nil || name == "" {
					return err
				}
			}

			return runHardwareModify(name, pkg)
		},
	}
}

func runHardwareModify(name, pkg string) error {
	ast, err := getAstClient()
	if err != nil {
		return err
	}

	filePath := findSourceFile("hardware", name, pkg)

	for {
		action, err := pickFromList("Modify "+name, []string{
			"Change hardware entry",
			"Add @Setting",
			"Add @Diagnostic",
			"Add @Calibration",
			"Done",
		})
		if err != nil || action == "" || action == "Done" {
			return err
		}

		switch action {
		case "Change hardware entry":
			hwFile := findSourceFile("hardware", "../config/Hardware", pkg)
			parsed, err := ast.parse(hwFile)
			if err != nil {
				fmt.Println("Could not parse Hardware.java")
				continue
			}
			entries := toStringSlice(parsed["states"]) // enum entries
			if len(entries) == 0 {
				fmt.Println("No hardware entries found in Hardware.java")
				continue
			}
			entry, err := pickFromList("Select hardware entry", entries)
			if err != nil || entry == "" {
				continue
			}
			if err := ast.setHardwareEntry(filePath, entry); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Set hardware entry to Hardware.%s\n", entry)
			}

		case "Add @Setting":
			fieldType, err := pickFromList("Type", []string{"double", "int", "boolean", "String"})
			if err != nil || fieldType == "" {
				continue
			}
			fieldName, err := promptText("Field name", "power")
			if err != nil || fieldName == "" {
				continue
			}
			defaultVal, err := promptText("Default value", "1.0")
			if err != nil || defaultVal == "" {
				continue
			}
			label, err := promptText("Display label", fieldName)
			if err != nil {
				continue
			}
			if label == "" {
				label = fieldName
			}
			if err := ast.addField(filePath, "@Setting(\""+label+"\") "+fieldType, fieldName, defaultVal); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added @Setting %s\n", fieldName)
			}

		case "Add @Diagnostic":
			methodName, err := promptText("Method name", "checkResponds")
			if err != nil || methodName == "" {
				continue
			}
			label, err := promptText("Display label", methodName)
			if err != nil {
				continue
			}
			if label == "" {
				label = methodName
			}
			fmt.Printf("Added @Diagnostic stub %s — implement in %s\n", methodName, filePath)
			_ = label

		case "Add @Calibration":
			methodName, err := promptText("Method name", "calibrateZero")
			if err != nil || methodName == "" {
				continue
			}
			label, err := promptText("Display label", methodName)
			if err != nil {
				continue
			}
			if label == "" {
				label = methodName
			}
			fmt.Printf("Added @Calibration stub %s — implement in %s\n", methodName, filePath)
			_ = label
		}
	}
}


func opModeModifyCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "modify [Name]",
		Short: "Modify an opmode",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			pkg, err := detectPackage()
			if err != nil {
				return err
			}

			var name string
			if len(args) > 0 {
				name = args[0]
			} else {
				names, _ := listNamesInDir(pkg, "opmodes")
				if len(names) == 0 {
					return fmt.Errorf("no opmodes found")
				}
				name, err = pickFromList("Select opmode", names)
				if err != nil || name == "" {
					return err
				}
			}

			return runOpModeModify(name, pkg)
		},
	}
}

func runOpModeModify(name, pkg string) error {
	ast, err := getAstClient()
	if err != nil {
		return err
	}

	filePath := findSourceFile("opmode", name, pkg)

	for {
		action, err := pickFromList("Modify "+name, []string{
			"Add control binding",
			"Remove control binding",
			"Add telemetry line",
			"Done",
		})
		if err != nil || action == "" || action == "Done" {
			return err
		}

		switch action {
		case "Add control binding":
			trigger, err := pickFromList("Trigger", []string{
				"whenPressed", "whenReleased", "whileHeld",
			})
			if err != nil || trigger == "" {
				continue
			}

			button, err := promptText("Button (e.g. G1_A)", "G1_A")
			if err != nil || button == "" {
				continue
			}

			signalNames, _ := listNamesInDir(pkg, "signals")
			if len(signalNames) == 0 {
				fmt.Println("No signals found — run 'ftcplus signal add' first.")
				continue
			}
			signal, err := pickFromList("Signal to send", signalNames)
			if err != nil || signal == "" {
				continue
			}

			if err := ast.addControl(filePath, button, signal, trigger); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added: controls().%s(GamepadButton.%s).send(%s::new)\n", trigger, button, signal)
			}

		case "Remove control binding":
			button, err := promptText("Button to remove (e.g. G1_A)", "G1_A")
			if err != nil || button == "" {
				continue
			}
			if err := ast.removeControl(filePath, button); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Removed binding for GamepadButton.%s\n", button)
			}

		case "Add telemetry line":
			key, err := promptText("Label", "state")
			if err != nil || key == "" {
				continue
			}
			expr, err := promptText("Expression", "\"running\"")
			if err != nil || expr == "" {
				continue
			}
			fmt.Printf("Add to configure(): telemetry().line(() -> \"%s: \" + %s)\n", key, expr)
		}
	}
}


func signalModifyCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "modify [Name]",
		Short: "Modify a signal",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			pkg, err := detectPackage()
			if err != nil {
				return err
			}

			var name string
			if len(args) > 0 {
				name = args[0]
			} else {
				names, _ := listNamesInDir(pkg, "signals")
				if len(names) == 0 {
					return fmt.Errorf("no signals found")
				}
				var err error
				name, err = pickFromList("Select signal", names)
				if err != nil || name == "" {
					return err
				}
			}

			return runSignalModify(name, pkg)
		},
	}
}

func runSignalModify(name, pkg string) error {
	ast, err := getAstClient()
	if err != nil {
		return err
	}

	filePath := findSourceFile("signal", name, pkg)
	parsed, err := ast.parse(filePath)
	if err != nil {
		return fmt.Errorf("failed to parse %s: %w", name, err)
	}

	for {
		action, err := pickFromList("Modify "+name, []string{
			"Add parameter",
			"Remove parameter",
			"Done",
		})
		if err != nil || action == "" || action == "Done" {
			return err
		}

		switch action {
		case "Add parameter":
			paramType, err := pickFromList("Parameter type", []string{
				"double", "int", "boolean", "String", "float", "long",
			})
			if err != nil || paramType == "" {
				continue
			}
			paramName, err := promptText("Parameter name", "value")
			if err != nil || paramName == "" {
				continue
			}
			if err := ast.addParam(filePath, paramType, paramName); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added parameter %s %s\n", paramType, paramName)
			}

		case "Remove parameter":
			fields := toStringSlice(parsed["fields"])
			if len(fields) == 0 {
				fmt.Println("No parameters found.")
				continue
			}
			param, err := pickFromList("Remove parameter", fields)
			if err != nil || param == "" {
				continue
			}
			if err := ast.removeParam(filePath, param); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Removed parameter %s\n", param)
			}
		}
	}
}


func robotModifyCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "modify [Name]",
		Short: "Modify a robot",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			pkg, err := detectPackage()
			if err != nil {
				return err
			}

			var name string
			if len(args) > 0 {
				name = args[0]
			} else {
				var robots []string
				base := filepath.Join("src", "main", "java", strings.ReplaceAll(pkg, ".", "/"))
				if _, err := os.Stat(filepath.Join(base, "Robot.java")); err == nil {
					robots = append(robots, "Robot")
				}
				if entries, err := os.ReadDir(filepath.Join(base, "robots")); err == nil {
					for _, e := range entries {
						if !e.IsDir() && strings.HasSuffix(e.Name(), ".java") {
							robots = append(robots, strings.TrimSuffix(e.Name(), ".java"))
						}
					}
				}
				if len(robots) == 0 {
					return fmt.Errorf("no robots found")
				}
				name, err = pickFromList("Select robot", robots)
				if err != nil || name == "" {
					return err
				}
			}

			return runRobotModify(name, pkg)
		},
	}
}

func runRobotModify(name, pkg string) error {
	ast, err := getAstClient()
	if err != nil {
		return err
	}

	filePath := findSourceFile("robot", name, pkg)

	for {
		action, err := pickFromList("Modify "+name, []string{
			"Add subsystem",
			"Remove subsystem",
			"Add hardware device",
			"Remove hardware device",
			"Modify Globals",
			"Modify Hardware entries",
			"Modify Properties",
			"Add telemetry",
			"Done",
		})
		if err != nil || action == "" || action == "Done" {
			return err
		}

		switch action {
		case "Add subsystem":
			subsystems, _ := listNamesInDir(pkg, "subsystems")
			if len(subsystems) == 0 {
				fmt.Println("No subsystems found.")
				continue
			}
			sub, err := pickFromList("Select subsystem", subsystems)
			if err != nil || sub == "" {
				continue
			}
			fieldName := strings.ToLower(sub[:1]) + sub[1:]
			init := fmt.Sprintf("register(new %s())", sub)
			if err := ast.addField(filePath, sub, fieldName, init); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added %s %s\n", sub, fieldName)
			}

		case "Remove subsystem":
			parsed, _ := ast.parse(filePath)
			fields := toStringSlice(parsed["fields"])
			if len(fields) == 0 {
				fmt.Println("No fields found.")
				continue
			}
			field, err := pickFromList("Remove subsystem", fields)
			if err != nil || field == "" {
				continue
			}
			if err := ast.removeField(filePath, field); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Removed %s\n", field)
			}

		case "Add hardware device":
			hwNames, _ := listNamesInDir(pkg, "hardware")
			if len(hwNames) == 0 {
				fmt.Println("No hardware devices found.")
				continue
			}
			hw, err := pickFromList("Select hardware device", hwNames)
			if err != nil || hw == "" {
				continue
			}
			fieldName := strings.ToLower(hw[:1]) + hw[1:]
			init := fmt.Sprintf("register(new %s())", hw)
			if err := ast.addField(filePath, hw, fieldName, init); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added %s %s\n", hw, fieldName)
			}

		case "Remove hardware device":
			parsed, _ := ast.parse(filePath)
			fields := toStringSlice(parsed["fields"])
			if len(fields) == 0 {
				fmt.Println("No fields found.")
				continue
			}
			field, err := pickFromList("Remove hardware device", fields)
			if err != nil || field == "" {
				continue
			}
			if err := ast.removeField(filePath, field); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Removed %s\n", field)
			}

		case "Modify Globals":
			globalsFile := filepath.Join(
				"src", "main", "java",
				strings.ReplaceAll(pkg, ".", "/"),
				"config", "Globals.java",
			)
			if err := runModifySettings(globalsFile, ast); err != nil {
				fmt.Printf("Error: %v\n", err)
			}

		case "Modify Hardware entries":
			hwFile := filepath.Join(
				"src", "main", "java",
				strings.ReplaceAll(pkg, ".", "/"),
				"config", "Hardware.java",
			)
			for {
				action, err := pickFromList("Hardware entries", []string{
					"Add entry",
					"Done",
				})
				if err != nil || action == "" || action == "Done" {
					break
				}
				entryName, err := promptText("Entry name (e.g. INTAKE_MOTOR)", "INTAKE_MOTOR")
				if err != nil || entryName == "" {
					continue
				}
				hwName, err := promptText("Hardware map name (e.g. intake_motor)", "intake_motor")
				if err != nil || hwName == "" {
					continue
				}
				if err := ast.addField(hwFile, "/* entry */", entryName, "\""+hwName+"\""); err != nil {
					fmt.Printf("Error: %v\n", err)
				} else {
					fmt.Printf("Added Hardware.%s(\"%s\")\n", entryName, hwName)
				}
			}

		case "Modify Properties":
            propOptions := []string{
                "trackWidthInches",
                "wheelDiameterInches",
                "maxLinearVelocityCmPerSecond",
                "maxAngularVelocityDegreesPerSecond",
                "massKg",
                "nominalVoltage",
                "maxCurrentAmps",
            }
            prop, err := pickFromList("Select property to override", propOptions)
            if err != nil || prop == "" {
                continue
            }
            value, err := promptText("Value", "12.0")
            if err != nil || value == "" {
                continue
            }
            fmt.Printf("Add to Properties.java:\n  @Override public double %s() { return %s; }\n", prop, value)

		case "Add telemetry":
			key, err := promptText("Panel name", name)
			if err != nil || key == "" {
				continue
			}
			expr, err := promptText("Expression", "\"running\"")
			if err != nil || expr == "" {
				continue
			}
			fmt.Printf("Add to defineTelemetry():\n  telemetry().panel(\"%s\").line(() -> %s);\n", key, expr)
		}
	}
}

func runModifySettings(filePath string, ast *AstClient) error {
	for {
		action, err := pickFromList("Globals", []string{
			"Add @Setting",
			"Remove @Setting",
			"Done",
		})
		if err != nil || action == "" || action == "Done" {
			return err
		}

		switch action {
		case "Add @Setting":
			fieldType, err := pickFromList("Type", []string{"double", "int", "boolean", "String"})
			if err != nil || fieldType == "" {
				continue
			}
			fieldName, err := promptText("Field name", "speed")
			if err != nil || fieldName == "" {
				continue
			}
			defaultVal, err := promptText("Default value", "1.0")
			if err != nil || defaultVal == "" {
				continue
			}
			label, err := promptText("Display label", fieldName)
			if err != nil {
				continue
			}
			if label == "" {
				label = fieldName
			}
			if err := ast.addField(filePath, "@Setting(\""+label+"\") "+fieldType, fieldName, defaultVal); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Added @Setting %s\n", fieldName)
			}

		case "Remove @Setting":
			parsed, err := ast.parse(filePath)
			if err != nil {
				fmt.Printf("Error: %v\n", err)
				continue
			}
			fields := toStringSlice(parsed["fields"])
			if len(fields) == 0 {
				fmt.Println("No settings found.")
				continue
			}
			field, err := pickFromList("Remove setting", fields)
			if err != nil || field == "" {
				continue
			}
			if err := ast.removeField(filePath, field); err != nil {
				fmt.Printf("Error: %v\n", err)
			} else {
				fmt.Printf("Removed @Setting %s\n", field)
			}
		}
	}
}


func robotAddCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "add [Name]",
		Short: "Add a new robot configuration",
		Args:  cobra.MaximumNArgs(1),
		RunE: func(cmd *cobra.Command, args []string) error {
			pkg, err := detectPackage()
			if err != nil {
				return err
			}

			var name string
			if len(args) > 0 {
				name = args[0]
			} else {
				name, err = promptText("Robot class name", "CompetitionRobot")
				if err != nil || name == "" {
					return err
				}
			}

			return runRobotAdd(name, pkg)
		},
	}
}

func runRobotAdd(name, pkg string) error {
	base := filepath.Join("src", "main", "java", strings.ReplaceAll(pkg, ".", "/"))
	robotsDir := filepath.Join(base, "robots")
	existingRobot := filepath.Join(base, "Robot.java")

	if _, err := os.Stat(existingRobot); err == nil {
		if err := os.MkdirAll(robotsDir, 0755); err != nil {
			return err
		}
		dest := filepath.Join(robotsDir, "Robot.java")
		if err := os.Rename(existingRobot, dest); err != nil {
			return fmt.Errorf("failed to move Robot.java to robots/: %w", err)
		}
		fmt.Printf("Moved Robot.java → robots/Robot.java\n")
	}

	if err := os.MkdirAll(robotsDir, 0755); err != nil {
		return err
	}

	outputPath := filepath.Join(robotsDir, name+".java")

	content := fmt.Sprintf(`package %s.robots;

import dev.ftcplus.core.TeamRobot;
import %s.config.Hardware;
import %s.config.Globals;
import %s.config.Properties;

@TeamRobot(name = "%s")
public class %s extends dev.ftcplus.core.Robot<Hardware, Globals, Properties> {

    public %s() {
        super(Hardware.class, new Globals(), new Properties());
    }
}
`, pkg, pkg, pkg, pkg, name, name, name)

	if err := os.WriteFile(outputPath, []byte(content), 0644); err != nil {
		return err
	}

	fmt.Printf("Created %s\n", outputPath)
	return nil
}


func treeCmd() *cobra.Command {
	return &cobra.Command{
		Use:   "tree",
		Short: "Show the component tree of your robot",
		RunE: func(cmd *cobra.Command, args []string) error {
			pkg, err := detectPackage()
			if err != nil {
				return err
			}

			return runTree(pkg)
		},
	}
}

func runTree(pkg string) error {
	ast, err := getAstClient()
	if err != nil {
		return err
	}

	base := filepath.Join("src", "main", "java", strings.ReplaceAll(pkg, ".", "/"))

	robotFile := filepath.Join(base, "Robot.java")
	if _, err := os.Stat(robotFile); os.IsNotExist(err) {
		robotsDir := filepath.Join(base, "robots")
		entries, err := os.ReadDir(robotsDir)
		if err != nil || len(entries) == 0 {
			return fmt.Errorf("no Robot.java found")
		}
		cfg, _ := readProjectConfig()
		if cfg != nil && cfg.ActiveRobot != "" {
			robotFile = filepath.Join(robotsDir, cfg.ActiveRobot+".java")
		} else {
			robotFile = filepath.Join(robotsDir, entries[0].Name())
		}
	}

	parsed, err := ast.parse(robotFile)
	if err != nil {
		return fmt.Errorf("failed to parse robot: %w", err)
	}

	className := "Robot"
	if v, ok := parsed["className"].(string); ok {
		className = v
	}

	fmt.Printf("Robot: %s\n", className)

	fields := toFieldSlice(parsed["fields"])
	for _, f := range fields {
		if strings.Contains(f.init, "register(") {
			fmt.Printf("  ├─ %s (%s)\n", f.name, f.typ)
			printComponentChildren(ast, f.typ, pkg, "  │  ")
		}
	}

	return nil
}

func printComponentChildren(ast *AstClient, typeName, pkg, indent string) {
	dirs := []string{"subsystems", "hardware"}
	for _, dir := range dirs {
		filePath := filepath.Join(
			"src", "main", "java",
			strings.ReplaceAll(pkg, ".", "/"),
			dir, typeName+".java",
		)
		if _, err := os.Stat(filePath); err != nil {
			continue
		}
		parsed, err := ast.parse(filePath)
		if err != nil {
			continue
		}
		fields := toFieldSlice(parsed["fields"])
		for _, f := range fields {
			if strings.Contains(f.init, "register(") {
				fmt.Printf("%s├─ %s (%s)\n", indent, f.name, f.typ)
			}
		}
		states := toStringSlice(parsed["states"])
		if len(states) > 0 {
			fmt.Printf("%s└─ states: %s\n", indent, strings.Join(states, ", "))
		}
		return
	}
}


func toStringSlice(v interface{}) []string {
	if v == nil {
		return nil
	}
	raw, ok := v.([]interface{})
	if !ok {
		return nil
	}
	var result []string
	for _, item := range raw {
		if s, ok := item.(string); ok {
			result = append(result, s)
		} else if m, ok := item.(map[string]interface{}); ok {
			if name, ok := m["name"].(string); ok {
				result = append(result, name)
			}
		}
	}
	return result
}

type fieldInfo struct {
	name string
	typ  string
	init string
}

func toFieldSlice(v interface{}) []fieldInfo {
	if v == nil {
		return nil
	}
	raw, ok := v.([]interface{})
	if !ok {
		return nil
	}
	var result []fieldInfo
	for _, item := range raw {
		if m, ok := item.(map[string]interface{}); ok {
			f := fieldInfo{}
			if n, ok := m["name"].(string); ok {
				f.name = n
			}
			if t, ok := m["type"].(string); ok {
				f.typ = t
			}
			if i, ok := m["init"].(string); ok {
				f.init = i
			}
			result = append(result, f)
		}
	}
	return result
}

func fieldsOfKind(parsed map[string]interface{}, pkg string) []string {
	fields := toFieldSlice(parsed["fields"])
	var names []string
	for _, f := range fields {
		if strings.Contains(f.init, "register(") {
			names = append(names, f.name)
		}
	}
	return names
}

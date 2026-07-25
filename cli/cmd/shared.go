package cmd

import (
	"github.com/charmbracelet/lipgloss"
	"os"
)

var labelStyle = lipgloss.NewStyle().Foreground(lipgloss.Color("241"))
var activeStyle = lipgloss.NewStyle().Foreground(lipgloss.Color("205"))

func copyFile(src, dst string) error {
	data, err := os.ReadFile(src)
	if err != nil {
		return err
	}
	return os.WriteFile(dst, data, 0644)
}
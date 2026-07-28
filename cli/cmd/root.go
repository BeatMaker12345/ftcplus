package cmd

import (
    "github.com/spf13/cobra"
)

var rootCmd *cobra.Command

func Root() *cobra.Command {
    if rootCmd != nil {
        return rootCmd
    }

    rootCmd = &cobra.Command{
        Use:   "ftcplus",
        Short: "FTC+ project management CLI",
        Long:  "Create and manage FTC+ robot projects.",
        RunE: func(cmd *cobra.Command, args []string) error {
            return runMainWizard()
        },
    }

    rootCmd.AddCommand(newCmd())
    rootCmd.AddCommand(hardwareCmd())
    rootCmd.AddCommand(subsystemCmd())
    rootCmd.AddCommand(signalCmd())
    rootCmd.AddCommand(opModeCmd())
    rootCmd.AddCommand(robotCmd())
    rootCmd.AddCommand(updateCmd())
    rootCmd.AddCommand(buildCmd())
    rootCmd.AddCommand(connectCmd())
    rootCmd.AddCommand(watchCmd())

    return rootCmd
}
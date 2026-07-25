package main

import (
    "os"

    "dev.ftcplus/cli/cmd"
)

func main() {
    if err := cmd.Root().Execute(); err != nil {
        os.Exit(1)
    }
}
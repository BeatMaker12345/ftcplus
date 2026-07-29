// cli/cmd/ast.go
package cmd

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"sync"
)

type AstClient struct {
	cmd    *exec.Cmd
	stdin  *bufio.Writer
	stdout *bufio.Scanner
	mu     sync.Mutex
}

var astClient *AstClient

func getAstClient() (*AstClient, error) {
	if astClient != nil {
		return astClient, nil
	}

	home, _ := os.UserHomeDir()
	jarPath := filepath.Join(home, ".ftcplus", "tools", "ftcplus-ast.jar")

	if _, err := os.Stat(jarPath); os.IsNotExist(err) {
		return nil, fmt.Errorf("AST tool not found at %s — run the FTC+ installer first", jarPath)
	}

	cmd := exec.Command("java", "-jar", jarPath)
	stdin, err := cmd.StdinPipe()
	if err != nil {
		return nil, fmt.Errorf("failed to create AST tool stdin: %w", err)
	}
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return nil, fmt.Errorf("failed to create AST tool stdout: %w", err)
	}
	cmd.Stderr = os.Stderr

	if err := cmd.Start(); err != nil {
		return nil, fmt.Errorf("failed to start AST tool: %w", err)
	}

	astClient = &AstClient{
		cmd:    cmd,
		stdin:  bufio.NewWriter(stdin),
		stdout: bufio.NewScanner(stdout),
	}

	return astClient, nil
}

func (c *AstClient) call(req map[string]interface{}) (map[string]interface{}, error) {
	c.mu.Lock()
	defer c.mu.Unlock()

	data, err := json.Marshal(req)
	if err != nil {
		return nil, err
	}

	if _, err := c.stdin.WriteString(string(data) + "\n"); err != nil {
		return nil, err
	}
	if err := c.stdin.Flush(); err != nil {
		return nil, err
	}

	if !c.stdout.Scan() {
		return nil, fmt.Errorf("AST tool closed unexpectedly")
	}

	var result map[string]interface{}
	if err := json.Unmarshal([]byte(c.stdout.Text()), &result); err != nil {
		return nil, err
	}

	if success, ok := result["success"].(bool); ok && !success {
		if errMsg, ok := result["error"].(string); ok {
			return nil, fmt.Errorf("%s", errMsg)
		}
	}

	return result, nil
}

func (c *AstClient) parse(filePath string) (map[string]interface{}, error) {
	return c.call(map[string]interface{}{"op": "parse", "file": filePath})
}

func (c *AstClient) addState(filePath, state string) error {
	_, err := c.call(map[string]interface{}{"op": "add-state", "file": filePath, "state": state})
	return err
}

func (c *AstClient) removeState(filePath, state string) error {
	_, err := c.call(map[string]interface{}{"op": "remove-state", "file": filePath, "state": state})
	return err
}

func (c *AstClient) addField(filePath, typ, name, init string) error {
	req := map[string]interface{}{"op": "add-field", "file": filePath, "type": typ, "name": name}
	if init != "" {
		req["init"] = init
	}
	_, err := c.call(req)
	return err
}

func (c *AstClient) removeField(filePath, name string) error {
	_, err := c.call(map[string]interface{}{"op": "remove-field", "file": filePath, "name": name})
	return err
}

func (c *AstClient) addControl(filePath, button, signal, trigger string) error {
	_, err := c.call(map[string]interface{}{
		"op": "add-control", "file": filePath,
		"button": button, "signal": signal, "trigger": trigger,
	})
	return err
}

func (c *AstClient) removeControl(filePath, button string) error {
	_, err := c.call(map[string]interface{}{"op": "remove-control", "file": filePath, "button": button})
	return err
}

func (c *AstClient) addParam(filePath, typ, name string) error {
	_, err := c.call(map[string]interface{}{"op": "add-param", "file": filePath, "type": typ, "name": name})
	return err
}

func (c *AstClient) removeParam(filePath, name string) error {
	_, err := c.call(map[string]interface{}{"op": "remove-param", "file": filePath, "name": name})
	return err
}

func (c *AstClient) setHardwareEntry(filePath, entry string) error {
	_, err := c.call(map[string]interface{}{"op": "set-hardware-entry", "file": filePath, "entry": entry})
	return err
}

func closeAstClient() {
	if astClient != nil {
		astClient.cmd.Process.Kill()
		astClient = nil
	}
}
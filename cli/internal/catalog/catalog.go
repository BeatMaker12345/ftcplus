package catalog

import (
	_ "embed"
	"encoding/json"
)

//go:embed catalog.json
var catalogJSON []byte

type Motor struct {
	ID           string  `json:"id"`
	Name         string  `json:"name"`
	Brand        string  `json:"brand"`
	Series       string  `json:"series"`
	Constant     string  `json:"constant"`
	TPR          float64 `json:"tpr"`
	FreeSpeedRPM float64 `json:"freeSpeedRpm"`
	StallTorqueNm float64 `json:"stallTorqueNm"`
}

type Servo struct {
	ID            string  `json:"id"`
	Name          string  `json:"name"`
	Brand         string  `json:"brand"`
	Series        string  `json:"series"`
	Constant      string  `json:"constant"`
	TravelDegrees float64 `json:"travelDegrees"`
	StallTorqueNm float64 `json:"stallTorqueNm"`
}

type CRServo struct {
	ID            string  `json:"id"`
	Name          string  `json:"name"`
	Brand         string  `json:"brand"`
	Series        string  `json:"series"`
	Constant      string  `json:"constant"`
	FreeSpeedRPM  float64 `json:"freeSpeedRpm"`
	StallTorqueNm float64 `json:"stallTorqueNm"`
}

type Catalog struct {
	Motors   []Motor   `json:"motors"`
	Servos   []Servo   `json:"servos"`
	CRServos []CRServo `json:"crservos"`
}

var loaded *Catalog

func Load() (*Catalog, error) {
	if loaded != nil {
		return loaded, nil
	}
	var c Catalog
	if err := json.Unmarshal(catalogJSON, &c); err != nil {
		return nil, err
	}
	loaded = &c
	return loaded, nil
}
// This example scans and then connects to a specific Bluetooth peripheral
// and then displays all the services and characteristics.
//
// To run this on a desktop system:
//
//	go run ./examples/discover

package main

import (
	"fmt"
	"github.com/atotto/clipboard"
	"time"
	"tinygo.org/x/bluetooth"
)

var adapter = bluetooth.DefaultAdapter

const (
	deviceServiceUUID = "a32edd59-0f12-659f-7114-6bc6853f7b76"
	charUUID          = "6a33f2de-f227-11ed-a05b-0242ac120004"
)

// wait on bare-metal, proceed immediately on desktop OS.
func wait() {
	time.Sleep(3 * time.Second)
}

func main() {
	wait()

	println("enabling...")

	// Enable BLE interface.
	err := adapter.Enable()

	if err != nil {
		panic(fmt.Sprintf("an error occurred on bluetooth enable: %s", err.Error()))
	}

	ch := make(chan bluetooth.ScanResult, 1)

	parsedUUID, err := bluetooth.ParseUUID(deviceServiceUUID)

	if err != nil {
		panic(fmt.Sprintf("UUID parsing failed: %s", err.Error()))
	}

	// Start scanning.
	println("scanning...")

	err = adapter.Scan(func(adapter *bluetooth.Adapter, result bluetooth.ScanResult) {
		println("device found:", result.Address.String(), result.RSSI, result.LocalName())
		if result.AdvertisementPayload.HasServiceUUID(parsedUUID) {
			err := adapter.StopScan()
			if err == nil {
				ch <- result
			}
		}
	})

	var device *bluetooth.Device
	select {
	case result := <-ch:
		device, err = adapter.Connect(result.Address, bluetooth.ConnectionParams{})
		if err != nil {
			println(err.Error())
			return
		}

		println("connected to ", result.Address.String())
	}

	defer func(device *bluetooth.Device) {
		err := device.Disconnect()
		if err != nil {
			println(err)
		}
	}(device)

	// get services
	println("discovering services/characteristics")
	services, err := device.DiscoverServices(nil)

	if err != nil {
		panic(fmt.Sprintf("An error occurred on service discovery: %s", err.Error()))
	}

	var service *bluetooth.DeviceService = nil

	for _, s := range services {

		if s.UUID().String() == deviceServiceUUID {
			service = &s
			println("- found required service", s.UUID().String())
			break
		}
	}

	if service == nil {
		panic(fmt.Sprintf("Required service not found"))
	}

	chars, err := service.DiscoverCharacteristics(nil)

	if err != nil {
		panic(fmt.Sprintf(" something went wrong on characteristic discovery: %s", err.Error()))
	}

	var selectedChar *bluetooth.DeviceCharacteristic = nil

	for _, char := range chars {
		println("-- characteristic", char.UUID().String())
		if char.UUID().String() == charUUID {
			selectedChar = &char
		}
	}

	if selectedChar == nil {
		panic(fmt.Sprintf(" required char not found"))
	}

	err = selectedChar.EnableNotifications(func(buf []byte) {
		println("received data, copying")
		err := clipboard.WriteAll(string(buf))
		if err != nil {
			println("copy error", err.Error())
		}
	})

	if err != nil {
		panic(fmt.Sprintf(" an error occurred when enabling notification: %s", err.Error()))
	}

	select {}

}

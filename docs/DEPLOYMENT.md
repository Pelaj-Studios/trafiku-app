# Deployment and operation

## Device setup

Use an Android TV device that supports API level 36 or later. Keep the device power supply stable. Keep enough free storage for the database, application updates, and advertisements.

Install the application package. Then grant the requested location and media permissions. Connect the device to the server network when you must download a network snapshot.

For development, use the `Television_720p` Android Virtual Device (AVD). Do not use the `Small_Phone` AVD.

## Initial application setup

1. Start the application.
2. Download the Trafiku Urban network snapshot.
3. Select one bus line.
4. Select one direction.
5. Confirm that the route shows an ordered stop list.
6. Confirm that the application restores the same selection after a restart.

The application stores the downloaded network snapshot for offline use. The normal refresh interval is 24 hours. Use the forced refresh action when the stored data must be replaced before this interval ends.

## Global Positioning System setup

The application first uses the device Global Positioning System (GPS). Grant precise location access when the device asks for it.

Use a generic Universal Serial Bus (USB) serial National Marine Electronics Association (NMEA) receiver when device GPS is unavailable. Connect the receiver through a USB host port. Grant USB access when Android shows the permission request.

The receiver must send valid NMEA location sentences. The application probes common baud rates. These rates are 4800, 9600, 38400, and 115200 baud.

Use the debug simulator when a development device has no position source. The simulator moves through the selected stop sequence. Do not use the simulator to validate real receiver accuracy or off-route behavior.

## Advertisement setup

Create this shared media directory on the Android TV device:

```text
/sdcard/DCIM/TrafikuAds/
```

Copy JPEG, PNG, WebP, MP4, or WebM files into this directory. Give each file a name that sets its playback order. The application sorts files by name.

The application shows route information for 30 seconds before each advertisement. A still image stays visible for 20 seconds. A video plays to its end without audio.

Run a media scan after you copy files with Android Debug Bridge (ADB). See [Example advertisements](../sample-ads/README.md) for commands and supplied media.

## Remote-control policy

Use one paired Bluetooth remote for application control. Keep this remote with the operator or driver. Treat D-pad input from the paired remote as authorized input.

The application does not implement a personal identification number (PIN), a phone pairing flow, a Quick Response (QR) code, a local Wi-Fi hotspot, or application-level remote authorization.

This policy has an operating-system boundary. The application cannot identify the person who presses a key. It also cannot prevent Android from delivering input from another accepted input device.

The device owner must control Bluetooth pairing. The device owner must also disable or prevent infrared and other unwanted input paths when the device supports them. Kiosk policy, remote replacement, and television maintenance are outside the application scope.

If this deployment assumption is not acceptable, add a separate product requirement for application-level authorization. Do not describe the current policy as protection against physical device access.

## Acceptance checks

Before service starts, do these checks on the target Android TV device:

1. Move focus through all interactive controls with the paired remote.
2. Confirm selection and Back behavior.
3. Restart the application and confirm selection restoration.
4. Disconnect the network and confirm that the stored route remains available.
5. Confirm system GPS or USB NMEA updates.
6. Pass a stop and confirm the previous and next stop changes.
7. Move more than 60 metres from the route and confirm the off-route state.
8. Confirm that advertisements start, end, and return to the route screen.
9. Confirm that video advertisements have no audio.
10. Confirm that an advertisement closes when the operator presses Back.

## Release build

Store the Base64-encoded release keystore and its passwords in GitHub Actions secrets. Store the server base URL in the `TRAFIKU_SERVER_BASE_URL` repository variable. Do not commit signing material.

Push a tag that starts with `v` to start the signed release workflow. The workflow uses the tag as the application version name. It uses the GitHub run number as the application version code. Download the signed APK from the completed workflow artifacts.

## Troubleshooting

If no route data appears, check the server address and force a refresh. If the refresh fails, check the server connection and the response for `trafiku_urban`.

If the location does not update, grant precise location access. Then check the system location setting. If you use USB NMEA, reconnect the receiver and grant USB permission.

If advertisements do not appear, check the `DCIM/TrafikuAds` directory, file format, media permission, and media scan. Remove media that the device cannot decode.

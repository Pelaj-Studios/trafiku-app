# Example advertisements

Copy these files to `DCIM/TrafikuAds` on the Android TV device.

The application sorts the files by file name. It supports JPEG, PNG, WebP, MP4, and WebM files.

The example pack contains these files:

- `01-prishtina-coffee.png`: A still image advertisement.
- `02-prishtina-bakery.png`: A still image advertisement.
- `03-prishtina-coffee-video.mp4`: A 10-second muted H.264 video advertisement.

After you use Android Debug Bridge (ADB) to copy a file, request a media scan. Example:

```text
adb push sample-ads/01-prishtina-coffee.png /sdcard/DCIM/TrafikuAds/
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
    -d file:///sdcard/DCIM/TrafikuAds/01-prishtina-coffee.png
```

Repeat the media scan for each copied file. The application reads the folder again before each advertisement period.

See [Deployment and operation](../docs/DEPLOYMENT.md#advertisement-setup) for the complete advertisement behavior.

# Architecture

## Design goals

Trafiku runs on constrained Android TV hardware. The application limits work during composition and playback. It keeps passenger information available when the network connection is poor.

The application uses Jetpack Compose and unidirectional data flow. A screen-level state holder owns screen state and business actions. Reusable composables receive immutable data and event functions.

## Main layers

### User interface layer

The user interface layer shows route selection, route progress, status panels, and advertisements. `TrafikuViewModel` owns the main application state. `AdvertisementViewModel` supplies advertisement data. `AdvertisementScheduler` controls the route and advertisement periods.

Static passenger text uses separate Albanian, Serbian Latin, and English locale resources. The trilingual resolver reads all three resources. It joins them in that order with newline characters. Server-supplied line, direction, and stop data stays unchanged.

The user interface layer uses these packages:

- `ui/app` owns application state and screen coordination.
- `ui/advertisement` owns advertisement scheduling, state, and playback UI.
- `ui/route` owns route selection, route tracking, status panels, and stop presentation.
- `ui/theme` owns shared colors, typography, and theme configuration.

Keep feature-specific UI in its feature package. Put shared application coordination in `ui/app`. Put a shared visual token in `ui/theme`.

### Data layer

The data layer has these parts:

- `data/remote` defines the Retrofit server contract and transfer objects.
- `data/local` defines the Room database, entities, and data access object.
- `data/repository` coordinates remote and local network snapshot data.
- `data/preferences` stores the selected line and direction with DataStore.
- `data/location` supplies positions and calculates route progress.
- `data/advertisement` reads local advertisement media through MediaStore.

`TrafikuAppContainer` creates the application dependencies. Composables do not access a data source directly.

## Bus network data flow

Room is the source of truth for the bus network snapshot. The repository exposes stored data as a `Flow`.

The normal refresh flow is:

1. Read the stored snapshot.
2. Keep the snapshot if it is less than 24 hours old.
3. Download a new snapshot if the stored snapshot is absent or stale.
4. Validate and normalize the response.
5. Replace the Room data in one database transaction.
6. Continue to expose the Room data.

A forced refresh does not use the TTL check. If a download fails, the repository keeps the stored snapshot.

The server endpoint is `GET api/bus-network/{operator}/snapshot`. The current operator value is `trafiku_urban`.

The operator selects one bus line and one direction. The application stores this selection in DataStore. A route contains an ordered sequence of stops. The direction labels are `Dalje` and `Kthim`.

## Location data flow

`DefaultLocationRepository` selects the active location source.

1. Use system GPS when it supplies an acceptable position.
2. Use a USB serial NMEA source if system GPS is unavailable or stale.
3. Recheck the preferred source while the application runs.

The maximum accepted accuracy is 25 metres. The fallback timeout is 15 seconds. The USB source probes 4800, 9600, 38400, and 115200 baud. Each probe can run for three seconds.

The debug simulator moves through the ordered route stops. It supplies a position each second. It uses a speed of 8 metres per second and an accuracy of 5 metres.

## Route progress

`RouteProgressTracker` projects each accepted position onto the selected route. It uses the ordered stops to find the previous, current, and next stop.

The stop arrival radius is 20 metres. The tracker enters the off-route state after three qualifying fixes beyond 60 metres. It returns to the route after three qualifying fixes within 40 metres. This hysteresis prevents frequent state changes near one threshold.

The estimated time of arrival uses accepted speed samples from a 60-second window. It rejects implausible speed and acceleration values.

## Advertisement flow

The MediaStore data source reads files from `DCIM/TrafikuAds/`. It supports JPEG, PNG, WebP, MP4, and WebM media. It sorts the media by file name.

The scheduler shows route information for 30 seconds. It then shows one advertisement. A still image stays visible for 20 seconds. A video stays visible until playback ends. Video playback is muted.

The scheduler only runs on the route screen. It pauses while a dialog, permission request, or action panel blocks the route screen. The Back action closes the current advertisement. The next route period then starts.

The image loader limits decoded images to 1920 by 1080 pixels. The advertisement screen uses fit scaling. Invalid files are skipped. The application scans the folder again before each advertisement period.

The compact advertisement header shows the line, direction, current or next stop, and time. It does not show GPS status.

## Platform boundaries

The application requests location, network, image, and video permissions. Device GPS and USB host support are optional. This design lets the same application use system GPS or a USB receiver.

The application does not authorize physical remotes. Android delivers D-pad input after the operating system accepts the input device. See [Remote-control policy](DEPLOYMENT.md#remote-control-policy) for the deployment security boundary.

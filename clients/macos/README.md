# Stoplight Classroom — macOS Client

A native macOS client for Stoplight Classroom, built with Swift and SwiftUI.

## Status

**Scaffold only** — contains the REST API client (`StoplightAPI`) with models. Needs SwiftUI views.

## Building

Requires Xcode 15+ / Swift 5.9+.

```bash
cd clients/macos/StoplightClassroom
swift build
```

## API Client

`StoplightAPI` provides:
- `login(email:password:)` — student account login
- `joinSession(joinCode:displayName:)` — join a session
- `submitStoplight(sessionId:value:)` — submit GREEN/YELLOW/RED

## Next Steps

1. Add SwiftUI `App` entry point
2. Join session view with code entry
3. Stoplight button view (three large colored circles)
4. WebSocket integration for activity mode updates (use URLSessionWebSocketTask or Starscream)

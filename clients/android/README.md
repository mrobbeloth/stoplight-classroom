# Stoplight Classroom — Android Client

A native Android client for Stoplight Classroom, built with Kotlin.

## Status

**Scaffold only** — contains the REST API client (`StoplightAPI.kt`). Needs Android project setup and UI.

## Setup

1. Create a new Android project in Android Studio (min SDK 26)
2. Copy `StoplightAPI.kt` into your project
3. Replace the minimal HTTP client with Retrofit + OkHttp
4. Add Gson/Moshi for JSON parsing

## API Client

`StoplightAPI` provides:
- `login(email, password)` — student account login
- `joinSession(joinCode, displayName)` — join a session
- `submitStoplight(sessionId, value)` — submit GREEN/YELLOW/RED

## Next Steps

1. Full Android Studio project with Gradle build
2. Jetpack Compose or XML layouts for:
   - Join session screen (code + name entry)
   - Stoplight button screen (three large colored circles)
   - Activity mode display
3. WebSocket integration for real-time activity mode updates (use OkHttp WebSocket)
4. Replace scaffold HTTP client with Retrofit

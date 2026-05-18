# Contributing to MagniZoom

Thanks for taking time to improve MagniZoom.

## Development Setup

1. Install Android Studio and Android SDK 36.
2. Clone this repository.
3. Open the project in Android Studio or build from the terminal:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Before Opening a Pull Request

- Keep changes focused and scoped.
- Run the debug build:

```powershell
.\gradlew.bat :app:assembleDebug
```

- Run unit tests when touching logic:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

- For overlay changes, test on a physical Android device when possible.
- Include screenshots or a short screen recording for UI changes.

## Coding Guidelines

- Follow existing Kotlin and Compose patterns in the project.
- Prefer small composables and local helpers over broad rewrites.
- Keep overlay, capture, and permission changes conservative.
- Do not commit local machine files such as `local.properties`, build outputs, or IDE workspace files.
- Follow the project [Code of Conduct](CODE_OF_CONDUCT.md) in issues, pull requests, and discussions.

## Reporting Bugs

Please include:

- Android version and device model.
- Steps to reproduce.
- Expected behavior.
- Actual behavior.
- Screenshots or logs when available.

## Security Issues

Do not open a public issue for sensitive security reports. See [SECURITY.md](SECURITY.md).

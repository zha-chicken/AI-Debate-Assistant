# Rhetorix

Rhetorix is a free Android app for debate practice, rebuttal training, logic checks, and AI-assisted reasoning.

It was originally built as AiDebate and later renamed to Rhetorix. The project is currently an independent interest project, with the long-term goal of helping more students practice argumentation and critical thinking.

> Use it to prepare wisely, not to cheat.

## Download

Android is available now. iOS is planned next.

- GitHub APK: https://github.com/zha-chicken/AI-Debate-Assistant/releases/download/rhetorix-v1.0/app-debug.apk
- Lanzou Netdisk: https://wwari.lanzouq.com/izXfA3p1aaqb
- Lanzou password: `9yn6`
- Landing page repository: https://github.com/zha-chicken/Rhetorix-Landing

## What It Does

- Debate against AI in structured or free-flow formats.
- Watch AI-vs-AI debates from both sides of an issue.
- Practice face-to-face debate on one device.
- Build argument maps for pro and con positions.
- Train rebuttals under a timer and receive AI scoring.
- Detect logical fallacies in pasted arguments.
- Open an external AI hallucination detector from the tools page.
- Review debate history, scores, turns, and results.

All app features are free. There is no paywall. The app uses a support/donation QR code instead of premium gating.

## AI Providers

Rhetorix supports user-configured provider keys and models:

- OpenAI
- Anthropic Claude
- Google Gemini

Provider settings are stored locally through DataStore and Room. Users bring their own API keys.

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Dependency injection | Hilt |
| Local storage | Room + DataStore Preferences |
| Networking | Retrofit + OkHttp + Moshi |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |

Minimum SDK: 26
Target SDK: 34

## Project Structure

```text
app/src/main/java/com/aidebate/
|-- data/            # Room, Retrofit services, repositories, debate orchestration
|-- di/              # Hilt modules
|-- domain/          # Models, repository interfaces, debate interfaces
`-- presentation/    # Compose screens, navigation, theme, localization
```

Key screens include:

- Home
- Topic selection
- Debate setup
- Live debate
- Debate result
- Debate history
- Settings
- Provider configuration
- Argument map
- Rebuttal trainer
- Fallacy detector
- Tools page
- Donation/support page

## Building

```bash
./gradlew assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Getting Started

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the app on an Android device or emulator with API 26+.
4. Add at least one AI provider API key in Settings.
5. Start a debate, open the tools page, or try rebuttal training.

## Notes

- The current public release is `rhetorix-v1.0`.
- The app name shown to users is Rhetorix.
- Previous VIP, premium, paywall, and usage-limit code was removed.
- The landing page is maintained separately in `zha-chicken/Rhetorix-Landing`.

## License

MIT

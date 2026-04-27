# AI Debate
made particularly for those struggling to debate —— including me
 
An Android app for structured, AI-powered debates. Challenge an AI opponent or sit back and watch two AIs debate each other. Built with Jetpack Compose and Material 3.

!!! IMPORTANT NOTICE - DON'T USE IT TO CHEAT, USE IT TO PREPARE WISELY!!!

## Features
### Debate Modes
- **User vs AI** — Take a position and debate against an AI opponent
- **AI vs AI** — Watch two AI models debate in real-time

### Formats
- **Structured** — Opening statements, rebuttals, and closing arguments
- **Free Flow** — Natural back-and-forth discussion

### AI Providers
OpenAI, Anthropic (Claude), Gemini, DeepSeek, Groq, and Ollama — configure API keys and models in Settings.

### Prep Tools
- **Argument Map** — Visual mindmap of pro and con arguments for any topic
- **Rebuttal Trainer** — Practice rebuttals under timed pressure with AI scoring across logic, clarity, persuasiveness, and evidence
- **Fallacy Detector** — Paste or type an argument to detect logical fallacies, with a built-in reference guide

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Local storage | Room + DataStore Preferences |
| Networking | Retrofit + OkHttp + Moshi |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |

Minimum SDK 26, target SDK 34.

## Design System

Role-based design tokens that adapt UI to debate context:

- **PRO** (indigo) — Proposition side
- **CON** (amber) — Opposition side
- **USER** (teal) — User's own messages
- **MODERATOR** (gray) — System messages and phase transitions

Includes reusable components: glow wrappers, role-aware conversation bubbles, animated phase dividers, and smart typing indicators.

## Screens

| Screen | Route |
|---|---|
| Home | `home` |
| Topic Selection | `topic_selection` |
| Debate Setup | `debate_setup/{topicId}` |
| Live Debate | `debate/{sessionId}` |
| Debate Result | `debate_result/{sessionId}` |
| Debate History | `history` |
| Settings | `settings` |
| Provider Config | `provider_config/{providerName}` |
| Argument Map | `argument_map/{topicId}` |
| Rebuttal Trainer | `rebuttal_trainer` |
| Fallacy Detector | `fallacy_detector` |

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Output at:
# app/build/outputs/apk/debug/app-debug.apk
```

## Getting Started

1. Open the project in Android Studio
2. Sync Gradle
3. Run on a device or emulator (API 26+)
4. Add at least one AI provider API key in Settings

## License

MIT

# Rhetorix

Rhetorix is a free Android app for debate practice, rebuttal training, logic checks, and AI-assisted reasoning.

It was originally built as AiDebate and later renamed to Rhetorix. The project is currently an independent interest project, with the long-term goal of helping more students practice argumentation and critical thinking.

> Use it to prepare wisely, not to cheat.

## Download

Android is available now. iOS is planned next.

- Latest release: https://github.com/zha-chicken/AI-Debate-Assistant/releases/tag/rhetorix-v1.4
- GitHub APK: https://github.com/zha-chicken/AI-Debate-Assistant/releases/download/rhetorix-v1.4/Rhetorix-v1.4-debug.apk
- Lanzou Netdisk: https://wwari.lanzouq.com/izXfA3p1aaqb
- Lanzou password: `9yn6`
- Landing page repository: https://github.com/zha-chicken/Rhetorix-Landing

## What It Does

- Debate against AI in structured or free-flow formats.
- Watch AI-vs-AI debates from both sides of an issue.
- Practice face-to-face debate on one device.
- Analyze an opponent's constructive speech and reveal claim-level rebuttal points.
- Train rebuttals under a timer and receive AI scoring.
- Detect logical fallacies in pasted arguments.
- Open an external AI hallucination detector from the tools page.
- Review debate history, scores, turns, and results.
- See clear AI-generated content disclaimers under generated content.

All app features are free. There is no paywall. The app uses a support/donation QR code instead of premium gating.

## Current Android Parity Update

This Android branch has been moved closer to the current iOS Rhetorix direction:

- The user-facing Argument Graph entry has been replaced with Constructive Analysis.
- Constructive Analysis uses the same configured AI Provider and fail-closed safety layer as debate generation.
- The analysis UI first shows extracted claims; tapping a claim expands original quote, challenge explanation, and rebuttal points.
- The local preset topic library now seeds 300 debate topics adapted from the iOS topic library.
- Existing installs are not reset: missing default topics are added by title when the app starts.
- Topic rows show the current user's local debate count for that topic instead of fake global popularity.
- Custom topic creation now runs through content safety before saving.
- Topic Recommendation 2.0 has been ported from the iOS direction to Android. It uses local debate history, judging summaries, training weakness signals, optional MBTI, and result-page like/dislike feedback to recommend the next debate topic.

Live recording analysis from the iOS version is not yet implemented on Android. The Android MVP currently provides the reliable paste-and-analyze mode.

## Topic Recommendation 2.0

Rhetorix recommends topics from the local preset topic library instead of generating random topics with a model. The goal is to suggest topics the user is likely to find interesting while still targeting a concrete debate skill.

The recommendation engine is local-first and uses only data stored on the device:

- Completed or engaged debate sessions, excluding AI-vs-AI for preference learning.
- AI-vs-AI sessions only for recent-topic repetition penalty.
- Topic category history and per-topic debate counts.
- AI judging summaries, used to infer recurring training weaknesses such as weak evidence, unclear definitions, weak structure, insufficient direct clash, or weak impact weighing.
- Optional MBTI, with intentionally low weight.
- Result-page feedback. Liking or disliking a topic's category changes future category preference. Technique feedback is recorded but does not change topic ranking.

The baseline scoring formula is:

```text
score =
  favoriteCategoryMatch * 16
  + weaknessTrainingTagMatches * 52
  + weaknessKeywordMatches * 18
  + mbtiKeywordMatches * 5
  + categoryFeedbackScore
  - previousDebateCountForSameTopic * 9
  - recentRepeatPenalty
```

A same-batch category diversity penalty is also applied while selecting multiple recommendations, so the app does not repeatedly show the same type of topic. Recommendation cards appear after the user has at least two non-AI-vs-AI engaged debate sessions, which prevents early recommendations from pretending to know the user.

## AI Providers

Rhetorix supports user-configured provider keys and models:

- OpenAI
- Anthropic Claude
- Google Gemini
- DeepSeek
- Groq
- Ollama / OpenAI-compatible local endpoints

Provider settings are stored locally through DataStore and Room. Users bring their own API keys.

## Safety and Transparency

Rhetorix includes a fail-closed content safety layer before user prompts are sent to a model and before AI output is displayed or stored. If a safety check fails because of a timeout, invalid API key, invalid response, or provider error, the related content is blocked and the app asks the user to retry later.

Safety checks use the selected provider when possible. For example, if the user configures DeepSeek for a debate, DeepSeek is used for the safety classification step. OpenAI uses the moderation endpoint when available.

Visible AI-generated content includes the disclaimer:

```text
内容由AI生成，仅供参考 AI-generated, for reference only
```

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
- Constructive analysis
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

- The current public release is `rhetorix-v1.4`.
- The app name shown to users is Rhetorix.
- Previous VIP, premium, paywall, and usage-limit code was removed.
- The landing page is maintained separately in `zha-chicken/Rhetorix-Landing`.

## License

MIT

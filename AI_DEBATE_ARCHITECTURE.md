# AI Debate — Architecture Reference

## Overview

AI Debate is a Jetpack Compose Android app that enables users to debate AI opponents, watch two AIs debate each other, or play face-to-face pass-and-play with AI judging. It also provides a suite of preparation tools: argument mapping, rebuttal training with live scoring, and a logical fallacy detector.

**Tech stack:** Kotlin, Jetpack Compose + Material 3, MVVM + Clean Architecture, Hilt DI, Room (local DB), Moshi (JSON), Retrofit (HTTP), OkHttp.

---

## 1. Architecture Layers

```
┌────────────────────────────────────────────────────────┐
│  Presentation Layer (Compose Screens + ViewModels)     │
│  ── HomeScreen / DebateScreen / FaceToFaceScreen /    │
│     RebuttalTrainerScreen / ArgumentMapScreen /        │
│     FallacyDetectorScreen / SettingsScreen             │
├────────────────────────────────────────────────────────┤
│  Domain Layer (Models, Repositories, Orchestrators)    │
│  ── DebateModels, PrepModels, ChatModels              │
│  ── DebateOrchestrator, DebateStateMachine            │
│  ── Repository interfaces                             │
├────────────────────────────────────────────────────────┤
│  Data Layer (Implementations, DB, Remote APIs)         │
│  ── Room DAOs + Entities + Mappers                    │
│  ── Retrofit API services (OpenAI, Anthropic, Gemini) │
│  ── AiProviderAdapter implementations                 │
│  ── Repository impls                                  │
└────────────────────────────────────────────────────────┘
```

### Dependency Injection (Hilt)

- **`AppModule`** — Room DB, DAOs, Moshi
- **`NetworkModule`** — OkHttpClient, Retrofit services for OpenAI, Anthropic, Gemini
- **`RepositoryModule`** — binds repository interfaces to implementations
- **`OrchestratorModule`** — binds `DebateOrchestrator` to `DebateOrchestratorImpl`

---

## 2. Domain Models

### `domain/model/DebateModels.kt` — Core debate entities

| Model | Purpose |
|---|---|
| `DebateMode` | `USER_VS_AI` \| `AI_VS_AI` \| `USER_VS_USER` |
| `DebateFormat` | `STRUCTURED` (Opening/Rebuttal/Closing) \| `FREE_FLOW` |
| `SpeakerRole` | `USER` \| `AI_PROPOSITION` \| `AI_OPPOSITION` \| `MODERATOR` |
| `StructuredPhase` | `OPENING` \| `REBUTTAL` \| `CLOSING` |
| `DebateDifficulty` | `EASY` \| `MEDIUM` \| `HARD` |
| `DebateSession` | A single debate instance. Holds topicId, mode, format, providers, models, status, difficulty |
| `DebateTurn` | A single turn in a debate. Has speakerRole, content, phase, turnIndex, optional score + highlights |
| `DebateResult` | AI-judged outcome. winner (SpeakerRole), summary, judgedByProvider, judgedByModel |
| `DebateSessionSummary` | Lightweight summary for history list (topicTitle, mode, turnCount, etc.) |
| `DebateTopic` | A debate topic/question with title, category, isPredefined flag |
| `TurnScore` | Per-turn AI score (overall 0-100, rationale) |
| `ArgumentHighlight` | Highlight on a turn: STRONG_ARGUMENT, WEAK_EVIDENCE, LOGICAL_FALLACY, etc. |
| `ProviderConfig` | User-configured AI provider (provider enum, apiKey, modelName, baseUrl, isEnabled) |
| `AiProvider` | `OPENAI` \| `ANTHROPIC` \| `GEMINI` \| `DEEPSEEK` \| `GROQ` \| `OLLAMA` |
| `SessionStatus` | `ACTIVE` \| `COMPLETED` \| `ABANDONED` |
| `HistoryItem` | Sealed interface — `Debate(summary)` or `Rebuttal(session, bestScore, attemptCount)` |

### `domain/model/PrepModels.kt` — Preparation tool entities

| Model | Purpose |
|---|---|
| `ArgumentNode` | Node in argument mindmap (TOPIC, PRO, CON, EVIDENCE). Has position (x, y) |
| `ArgumentEdge` | Edge between nodes with relation (SUPPORTS, REFUTES, RELATES) |
| `RebuttalSession` | A rebuttal practice session (topicId, userSide, difficulty) |
| `RebuttalAttempt` | A single rebuttal practice attempt (4 scored criteria: logic, clarity, persuasion, evidence — each /25) |
| `ScoreBreakdown` | Per-category breakdown for explain-it feature |
| `RebuttalExplanation` | Full explanation from AI coach (breakdown list, overallAdvice, keyTakeaway) |
| `RebuttalChatMessage` | Chat message in the explain-it Q&A |
| `FallacyResult` | Detected fallacy (name, quotedText, explanation) |
| `FallacyReference` | Reference guide entry for a logical fallacy |

### `domain/model/ChatModels.kt` — AI communication

| Model | Purpose |
|---|---|
| `ChatMessage` | Generic role + content message for AI API calls |
| `ChatConfig` | Model settings (model name, temperature, maxTokens) |
| `ChatResult` | AI response (content, optional tokenUsage, finishReason) |
| `DebateContextualState` | Sealed interface tracking debate state machine status |

**DebateContextualState** states:
- `WaitingForUserInput` — user's turn to type
- `WaitingForAiTurn` — AI is generating a response (shows loading)
- `WaitingForTap` — AI turn done, user taps to let the next speaker respond (AI vs AI)
- `DebateCompleted` — debate finished
- `Judging` — AI is judging the debate
- `Error` — something went wrong

---

## 3. Data Layer

### Room Database (`AppDatabase`)

**Tables:**
1. `debate_sessions` — debate session records
2. `debate_turns` — individual turns per session
3. `debate_topics` — predefined + custom topics
4. `provider_configs` — user's AI provider API keys
5. `debate_results` — AI judgment results
6. `argument_nodes` — mindmap nodes
7. `argument_edges` — mindmap edges
8. `rebuttal_sessions` — rebuttal training sessions
9. `rebuttal_attempts` — practice attempts with scores

**Entity ↔ Domain mapping** is done in `EntityMappers.kt` via extension functions (`.toEntity()`, `.toDomain()`). Enums are stored as strings.

### Remote API Services

| Service | Endpoint | Provider |
|---|---|---|
| `OpenAiApiService` | `POST v1/chat/completions` | OpenAI |
| `AnthropicApiService` | `POST v1/messages` | Anthropic (with x-api-key header) |
| `GeminiApiService` | `POST v1beta/models/{model}:generateContent` | Google Gemini (with key query param) |
| `OpenAiCompatibleApiService` | `POST v1/chat/completions` | DeepSeek, Groq, Ollama |

### Adapter Pattern (`AiProviderAdapter`)

Each provider has an adapter (`OpenAiAdapter`, `AnthropicAdapter`, `GeminiAdapter`, `OpenAiCompatibleAdapter`) implementing:
- `chat()` — sends a prompt + conversation history, returns ChatResult
- `validate()` — tests the API key with a minimal request

The `ProviderAdapterFactory` maps `AiProvider` enum values to the correct adapter. OpenAI-compatible providers (DeepSeek, Groq, Ollama) all share `OpenAiCompatibleAdapter` which creates per-baseUrl Retrofit instances dynamically.

---

## 4. Debate Flow

### State Machine (`DebateStateMachine`)

For **STRUCTURED** format, 6 turns in alternating order:

```
OPENING:  AI_PROPOSITION  →  AI_OPPOSITION
REBUTTAL: AI_PROPOSITION  →  AI_OPPOSITION
CLOSING:  AI_PROPOSITION  →  AI_OPPOSITION
```

`advance()` returns `NextTurn(speaker, phase)` or `Completed`.

### Orchestrator (`DebateOrchestratorImpl`)

Singleton that manages debate execution:

1. **`initialize(session, initialTurns)`** — loads session, sets up state machine, sets contextual state based on mode
2. **`submitUserTurn(content)`** — saves user turn, generates AI response for OPPOSITION (USER_VS_AI only), advances state
3. **`advanceAiTurn()`** — generates response for the current state machine speaker (AI_VS_AI), then advances
4. **`requestJudgment()`** — builds transcript, calls OpenAI to judge winner, saves result
5. **`buildConversationContext()`** — assembles ChatMessage list from turns for context window

Live scoring is done asynchronously via `scoreTurnAsync()` after each AI response — best-effort, failures don't block.

### Mode-Specific Behavior

| Mode | User submits | AI generates | Judging |
|---|---|---|---|
| USER_VS_AI | User types turn | AI responds as OPPOSITION | Optional |
| AI_VS_AI | No user input | Both sides via tap-to-advance | Optional |
| USER_VS_USER | Two players alternate on one device | None (manual turns) | AI judges at end |

---

## 5. Presentation Layer

### Navigation (`Navigation.kt`)

Compose NavHost with animated transitions. Routes:

| Route | Screen |
|---|---|
| `home` | HomeScreen |
| `topic_selection` | TopicSelectionScreen |
| `debate_setup/{topicId}` | DebateSetupScreen |
| `debate/{sessionId}` | DebateScreen |
| `debate_result/{sessionId}` | DebateResultScreen |
| `history` | DebateHistoryScreen |
| `settings` | SettingsScreen |
| `provider_config/{providerName}` | ProviderConfigScreen |
| `argument_map/{topicId}` | ArgumentMapScreen |
| `rebuttal_trainer?sessionId={sessionId}` | RebuttalTrainerScreen |
| `fallacy_detector` | FallacyDetectorScreen |
| `facetoface/{sessionId}` | FaceToFaceScreen |

### Screen Architecture

Every screen follows the pattern:
1. **State data class** — holds all UI state
2. **HiltViewModel** — manages state via `MutableStateFlow`, exposes `StateFlow`
3. **Composable function** — observes state with `collectAsState()`, renders UI

### HomeScreen

Features:
- Animated hero section with gradient ring
- Staggered card entries for all features
- Sections: Debate Actions (New Debate, Face-to-Face, History), Preparation Tools (Argument Map, Rebuttal Trainer, Fallacy Detector), Settings

### DebateSetupScreen

Configuration flow:
1. Topic card (read-only from selected topic)
2. Mode selection (USER_VS_AI / AI_VS_AI / USER_VS_USER)
3. Format selection (hidden for F2F — always STRUCTURED)
4. Difficulty selection (hidden for F2F)
5. Position selection (USER_VS_AI only — For/Against)
6. AI provider selectors (hidden for F2F)
7. Start button

When F2F mode is selected, a description card shows: "Two players, one device, AI judges" and all AI configuration is hidden.

### DebateScreen

AI debate viewer with:
- Timeline with phase dividers (Opening/Rebuttal/Closing)
- Turn cards with role-specific colors (PRO = blue/indigo, CON = amber/orange)
- User input bar (USER_VS_AI only)
- Tap-to-advance overlay (AI_VS_AI)
- Typing indicator with AI provider name
- End card with AI judgment prompt

### FaceToFaceScreen

Pass-and-play UI:
- **Progress dots** — 6 dots with connector lines showing current position
- **Phase label** — Opening/Rebuttal/Closing
- **Player turn card** — big colored card showing who speaks (P1 = PRO blue, P2 = CON amber)
- **Text input** — multiline argument field
- **Pass overlay** — full-screen overlay after each turn: "Pass to Player X" with tap-to-continue
- **Judge prompt** — after all 6 turns, shows "Face-to-Face Complete!" with "Judge Results" button
- **Result display** — trophy icon with winner name, summary, and "View Full Result" button

### RebuttalTrainerScreen

Timed practice flow:
- TOPIC_SELECT → SETUP → READY (shows AI-generated argument) → RESPONDING (timer counting down) → SCORING → RESULT
- Score card with 4 criteria (Logic, Clarity, Persuasion, Evidence — each /25 = /100 total)
- Explain-it feature: detailed AI breakdown with Q&A chat
- Grading: A (90+), B (75+), C (50+), D (<50)

### ArgumentMapScreen

Interactive mindmap:
- AI-generated argument nodes (PRO/CON with evidence)
- Canvas-based node graph with edges
- Add/edit/delete nodes and edges
- Regenerate with AI

### FallacyDetectorScreen

- Text input → AI analyzes for logical fallacies
- Results with severity (High/Medium/Low) and quotes
- Reference guide of common fallacies

---

## 6. Localization

### Translation System

Uses `staticCompositionLocalOf` for reactive language switching (no app restart needed). The `Translation` data class holds all UI strings (~150+ fields). Two instances: `EnglishTranslation` and `ChineseTranslation`.

Language preference is stored in DataStore via `SettingsRepository`. The `MainActivity` observes the language flow, applies it to `CompositionLocalProvider`.

### Topic Translations

`TopicTranslations.kt` maps English topic titles to Chinese translations (title, category, description). The `DebateTopic.translate(languageCode)` extension function applies translations reactively when language changes, using `combine` with the topic Flow + language Flow in ViewModels.

---

## 7. Theme System

### Role Tokens (`RoleTokens.kt`)

Each role (PRO, CON, USER, MODERATOR) has a complete token system:
- **Color tokens** — primary, gradient (start/end), glow, surface, onSurface, container, onContainer, dim, accent
- **Depth tokens** — content/focus elevation, shadow color, glow radius/alpha
- **Motion tokens** — entry duration, easing, delay, spring stiffness/damping
- **Interaction tokens** — press scale, focus glow, active elevation, ripple alpha

PRO = blue/indigo theme, CON = amber/orange theme, USER = teal, MODERATOR = gray.

### Spacing & Radius

Fixed system: xs(4dp), sm(8dp), md(12dp), lg(16dp), xl(24dp), xxl(32dp), xxxl(48dp). Radius: small(12dp), medium(16dp), large(20dp).

---

## 8. Key Flows

### Starting a debate (USER_VS_AI)

```
Home → TopicSelection → DebateSetup → DebateScreen
```
1. User picks topic
2. Configures mode, format, difficulty, position, AI providers
3. Clicks "Start Debate" → ViewModel creates DebateSession via DebateRepository
4. Navigation routes to DebateScreen with sessionId
5. DebateViewModel.initialize() observes session + turns flows
6. DebateOrchestrator.initialize() sets up state machine
7. User types → submitUserTurn() → AI responds → advance → repeat
8. At end: requestJudgment() → AI picks winner → save result

### Face-to-Face debate (USER_VS_USER)

```
Home → TopicSelection → DebateSetup → FaceToFaceScreen
```
1. Same topic selection + setup (F2F mode selected)
2. FaceToFaceViewModel creates session with USER_VS_USER mode
3. 6 turns: P1 Opening → P2 Opening → P1 Rebuttal → P2 Rebuttal → P1 Closing → P2 Closing
4. After each turn, "Pass to Player X" overlay
5. After turn 6: "Judge Results" button
6. FaceToFaceViewModel.requestJudgment() initializes orchestrator with turns, calls requestJudgment()
7. Result displayed in FaceToFaceScreen or navigated to DebateResultScreen

### Rebuttal training

```
Home → RebuttalTrainer → Setup → Ready → Responding → Result
```
1. Select topic → configure side/difficulty/time limit
2. AI generates a practice argument
3. Timer starts → user writes rebuttal → submit
4. AI scores on 4 criteria (each /25 = /100)
5. Optional: "Explain Score" → AI coach gives detailed breakdown with Q&A chat

---

## 9. File Inventory

```
com.aidebate/
├── App.kt                          # Application class
├── MainActivity.kt                 # Single activity, sets up translations + nav
├── data/
│   ├── debate/
│   │   └── DebateOrchestratorImpl.kt   # Stateful debate execution + judgment
│   ├── local/
│   │   ├── AppDatabase.kt              # Room DB (9 tables)
│   │   ├── dao/  (Daos.kt, PrepDaos.kt)
│   │   ├── entity/ (Entities.kt, PrepEntities.kt)
│   │   └── mapper/EntityMappers.kt     # Entity ↔ Domain mapping
│   ├── remote/
│   │   ├── adapter/AiProviderAdapter.kt    # Provider adapters + factory
│   │   ├── dto/ (OpenAiDtos, AnthropicDtos, GeminiDtos)
│   │   └── service/ApiServices.kt         # Retrofit interfaces
│   └── repository/ (6 implementations)
├── di/
│   ├── AppModule.kt                 # Room, Moshi, DAOs
│   ├── NetworkModule.kt             # OkHttp, Retrofit services
│   └── RepositoryModule.kt          # Binds repos + orchestrator
├── domain/
│   ├── debate/
│   │   ├── DebateOrchestrator.kt        # Interface
│   │   └── DebateStateMachine.kt        # Turn progression logic
│   ├── model/
│   │   ├── DebateModels.kt              # Core entities
│   │   ├── PrepModels.kt                # Prep tool entities
│   │   └── ChatModels.kt                # AI communication + state
│   └── repository/ (interfaces)
└── presentation/
    ├── argumentmap/                    # Mindmap screen + VM
    ├── common/                         # Shared composables (RoleSelection, ConversationUnit, etc.)
    ├── debate/                         # AI debate screen + VM
    ├── facetoface/                     # F2F pass-and-play screen + VM
    ├── fallacy/                        # Fallacy detector screen + VM
    ├── history/                        # Unified history screen + VM
    ├── home/                           # Home screen
    ├── localization/                   # Translation data + CompositionLocal
    ├── navigation/                     # NavHost + route definitions
    ├── rebuttal/                       # Rebuttal trainer screen + VM
    ├── result/                         # Debate result screen + VM
    ├── settings/                       # Settings + provider config screens + VMs
    ├── setup/                          # Debate setup screen + VM
    ├── theme/                          # Role tokens, dimensions, depth, motion
    └── topic/                          # Topic selection screen + VM
```

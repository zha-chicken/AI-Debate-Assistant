# AI Debate — Android App Design Specification

## Overview

AI Debate is a Kotlin/Jetpack Compose + Material Design 3 Android app that lets users debate against AI or watch two AIs debate each other. It also includes three debate preparation tools: an argument mindmap, a rebuttal trainer, and a logical fallacy detector. The app ships with multi-provider AI support (OpenAI, Anthropic, Gemini, DeepSeek, Groq, Ollama), each configurable with custom API key, model, and base URL.

---

## Screen Inventory (10 screens + dialogs)

### 1. HomeScreen
**Role:** App entry point.

- **Top bar:** `CenterAlignedTopAppBar` with "AI Debate" title, no actions.
- **Hero section (centered):** A 72dp circular icon container with a diagonal gradient fill (primary → tertiary), a Forum icon inside, infinitely pulsing scale 1.0→1.06 over 1200ms with `EaseInOutCubic`. Below: "Welcome to AI Debate" in `headlineSmall` + a subtitle line.
- **Card list (scrollable column, 20dp horizontal padding):**
  - Section header "Debate Actions" in `titleSmall`, 50% alpha.
  - 2 primary cards: "New Debate" (primaryContainer) and "Debate History" (secondaryContainer).
  - Section header "Preparation Tools".
  - 3 tools cards: "Argument Map" (tertiaryContainer), "Rebuttal Trainer" (primaryContainer at 55% alpha), "Fallacy Detector" (secondaryContainer at 55% alpha).
  - Final card: "Settings" (surfaceVariant).
- **Card anatomy:** `RoundedCornerShape(16.dp)`, zero elevation. Row layout: 44dp circular icon background (accent color at 15% alpha) → title + subtitle column → ChevronRight icon.
- **Animation:** Cards enter staggered — each card delays 70ms × its index, then fades in (tween 400ms) + slides up from 1/3 height (tween 500ms, EaseOutCubic).

### 2. TopicSelectionScreen
**Role:** Choose a debate topic from predefined list or enter a custom one.

- **Layout:** `LazyColumn` inside Scaffold.
- **Top section:** A prominent outlined card with 2dp primary border "Write your own topic", with ChatBubbleOutline icon.
- **Topic list:** Grouped by category. Each category has a section header (`labelSmall`, 60% alpha). Topic cards use `surfaceVariant.copy(alpha=0.5f)` background, `RoundedCornerShape(12.dp)`, showing ChatBubbleOutline icon + title + optional description + ChevronRight.
- **Top bar action:** "+" icon button opens an `AlertDialog` with `OutlinedTextField` for custom topic name and description. Confirm button disabled when title is blank.

### 3. DebateSetupScreen
**Role:** Configure debate session parameters.

- **Layout:** Scrollable column, 24dp padding.
- **Sections (top to bottom):**
  - Topic info card (surfaceVariant, 8dp rounded).
  - "Debate Mode" — two selectable cards (User vs AI / AI vs AI). Selected card gets `primaryContainer` background + 2dp primary border.
  - "Debate Format" — two selectable cards (Structured / Free Flow). Same selection pattern.
  - "Your Position" (conditional, `AnimatedVisibility`): shown only for User vs AI mode. Two side cards: "For" (tertiary theme) and "Against" (error theme).
  - Proposition/Opposition provider selectors: `ExposedDropdownMenuBox` with provider name + model text field.
  - "Start Debate" button — full width, 48dp height.
- **States:** Shows `CircularProgressIndicator` centered when loading topic data.

### 4. DebateScreen
**Role:** Core debate experience — the most complex screen.

- **Top bar:** Topic title (`titleMedium`, bold) + phase label (`bodySmall`, primary color) for structured format. Back button triggers `endDebate()` before navigating away.
- **Content area:**
  - `LazyColumn` of chat bubbles with `animateScrollToItem` on new turns.
  - Each **bubble** (`DebateBubble`) enters with fade-in (300ms) + slide-up (400ms, EaseOutCubic):
    - Role label row: 24dp circular avatar with first letter of role (P/C/Y/J) + label ("PRO"/"CON"/"YOU"/"JUDGE"). Positioned on the start side for AI/Moderator, end side for User.
    - Speech bubble: `Surface` with asymmetric corners — bottom-start or bottom-end is 4dp sharp, all others 16dp rounded. Width capped at 320dp. Color-coded by role:
      - PRO → primaryContainer + onPrimaryContainer
      - CON → secondaryContainer + onSecondaryContainer
      - USER → tertiaryContainer + onTertiaryContainer
      - MODERATOR → surfaceVariant + onSurfaceVariant
    - Provider attribution line (`labelSmall`, 40% alpha) below AI bubbles.
  - **Typing indicator:** 72dp-wide surface with 3 dots. Each dot scales and fades (0.3→1.0) with staggered timing (0ms / 150ms / 300ms offset), 500ms tween, infinite repeatable.
  - **Tap-to-advance overlay** (AI vs AI mode): Full-width card with pulsing alpha (0.6→1.0, 1000ms tween), TouchApp icon, "Tap to see [provider]'s response" text.
  - **End card** (`DebateEndCard`): 16dp rounded card. If result exists: trophy icon (48dp), "Debate Complete" title, winner name (tertiary), summary text, "View Full Result" outlined button. If no result yet: checkbox icon, "Would you like an AI to judge?", Skip/Judge buttons.
  - **Error card:** `errorContainer` background, ErrorOutline icon, message, Dismiss/Retry buttons.
- **Bottom bar (`UserInputBar`):** Visible only when waiting for user input in User vs AI mode. `Surface` with 3dp tonalElevation, 8dp shadowElevation. Contains `OutlinedTextField` (pill-shaped 24dp corners, 1-4 lines) + 48dp `FilledIconButton` with Send icon. Button disabled when text is blank.

### 5. DebateResultScreen
**Role:** Show winner and full debate transcript.

- **Layout:** `LazyColumn` inside Scaffold.
- **Top bar:** "Result" title + Share action button (builds plain-text summary, fires `ACTION_SEND` intent).
- **Winner card:** `tertiaryContainer` background, `RoundedCornerShape(16.dp)`. Trophy icon (56dp), "Debate Complete" title, winner name (`headlineMedium`, tertiary), summary text.
- **Transcript section:** Header "Transcript" (`titleMedium`, bold). Each turn in a `surfaceVariant.copy(0.5f)` card: speaker role label (`labelMedium`, primary) + content (`bodyMedium`). No visual distinction between debate phases.
- **Bottom:** "Back to Home" outlined button.

### 6. DebateHistoryScreen
**Role:** Browse past debate sessions.

- **Layout:** `LazyColumn`.
- **History cards:** Topic title, mode badge (User vs AI / AI vs AI in `primaryContainer` chip), turn count badge (`secondaryContainer` at 50% alpha), formatted date, delete icon button (error color at 60% alpha).
- **Delete flow:** Icon tap opens confirmation `AlertDialog` with Delete/Cancel buttons. No swipe-to-delete.
- **Empty state:** Large History icon (64dp, 30% alpha), "No debate history yet" text.

### 7. SettingsScreen
**Role:** View and navigate to provider configurations.

- **Layout:** `LazyColumn`.
- **Provider cards:** Generic Adb icon in a `primaryContainer` circular area + provider name + enabled/disabled badge (tertiary 15% for enabled, error 15% for disabled) + model name or "Tap to configure". ChevronRight indicator.

### 8. ProviderConfigScreen
**Role:** Edit a single AI provider's configuration.

- **Layout:** Scrollable form.
- **Fields:**
  - Enable/disable `Switch` in a card row.
  - API Key: `OutlinedTextField` with `PasswordVisualTransformation` + visibility toggle icon.
  - Model name: plain `OutlinedTextField`.
  - Base URL: `AnimatedVisibility`-wrapped `OutlinedTextField` (hidden for some providers).
  - Save button (shows spinner while saving).
  - "Test Connection" button with inline result: success shows green CheckCircle + "Connection successful"; failure shows red ErrorOutline + error message.
- **Shape:** All inputs use `RoundedCornerShape(12.dp)`.

### 9. ArgumentMapScreen
**Role:** Visual mindmap of pro/con/evidence arguments for a topic.

- **Top bar:** Topic title + node count subtitle. Actions: "Regenerate" button (AutoAwesome icon, spinner when loading), "+" dropdown menu (Add Pro / Add Con / Add Evidence items, each with colored circle indicator).
- **Main canvas area:**
  - `Canvas` with pinch-to-zoom (`rememberTransformableState`, 0.25x–3.5x) and pan.
  - **Node rendering:** Custom `android.graphics.Paint`-based circles drawn on `nativeCanvas`:
    - Selected nodes: colored ring shadow (25% alpha, 8dp spread) + outer circle at 92% alpha + inner white highlight at 25% alpha + white centered title text (24sp, fake bold, scaled with zoom). Radius: 36f base, 1.15× when selected.
    - Colors are theme-derived: PRO → SuccessGreen (`#2E7D32`), CON → error red, EVIDENCE → WarningAmber (`#E65100`), TOPIC → primary indigo.
  - **Edge rendering:** Quadratic bezier curves with a control point offset 40f above midpoint, drawn with `outline` color at 35% alpha, stroke width clipped to 0.5f–2f.
  - **Hit detection:** Euclidean distance check — tap must be within 46f × zoom of a node center. Selected node ID set on tap.
- **FAB:** Animated `ExtendedFloatingActionButton` (scale+fade in/out), visible when a node is selected. Tapping opens edit dialog.
- **Empty state:** Large AccountTree icon with pulsing scale (0.85→1.0, 1500ms), "No argument map yet" text, "Generate with AI" button.
- **Loading overlay:** Centered card with spinner + "Generating argument map..." text, fades in/out.
- **Add/Edit dialog:** `AlertDialog` with `RoundedCornerShape(20.dp)`. FilterChip row for type selection (Pro/Con/Evidence), each with a colored dot. Title + Details `OutlinedTextField`s. Save/Delete/Cancel buttons.

### 10. RebuttalTrainerScreen
**Role:** Practice rebuttals under timed pressure with AI scoring.

- **Top bar:** "Rebuttal Trainer" title. Back button returns to topic select if in later phases, otherwise navigates back.
- **5 phases, managed via `AnimatedContent` with horizontal slide+fade transitions:**
  - **Phase 1 — Topic Select:** Same topic card list as other screens, plus "Past Sessions" section (5 most recent, surfaceVariant cards, FOR/AGAINST colored text).
  - **Phase 2 — Setup:** Topic confirmation chip, side picker (FOR in success green / AGAINST in error red, FilterChips with ThumbUp/ThumbDown icons), difficulty (Easy/Medium/Hard FilterChips), time limit (30s/60s/90s FilterChips with Timer icon), "Start Training" button.
  - **Phase 3 — Ready:** Shows the AI-generated argument in a secondaryContainer card with "THE ARGUMENT" label. Non-interactive AssistChips showing selected settings. "Start Timer & Write Rebuttal" button.
  - **Phase 4 — Responding:** Large live countdown timer card — `headlineMedium` time display, red when ≤10s, primary otherwise. Prompt summary card (2-line max). `OutlinedTextField` for rebuttal (weight 1f, 5-line min). "Submit Rebuttal" button (disabled when blank).
  - **Phase 5 — Result:** Animated score counter (counts 0→totalScore over 800ms, `animateIntAsState`). Total displayed as "X / 100" in `displaySmall`. Four sub-score chips (Logic, Clarity, Persuasion, Evidence) each with independent animated counters (600ms, 400ms delay). Color-coded: ≥20 green, ≥15 amber, <15 red. Feedback card (secondaryContainer). Time stats row. "New Round" / "Retry Same" buttons.
- **Error:** Animated error card at bottom center with dismiss.
- **Timer format:** Shows "M:SS" when ≥60s, else "Xs".

### 11. FallacyDetectorScreen
**Role:** AI-powered logical fallacy detection with built-in reference guide.

- **Top bar:** "Fallacy Detector" title + MenuBook icon to toggle reference.
- **Main panel (default):**
  - Title + description.
  - `OutlinedTextField` for argument text (min 150dp, max 300dp height, 6-line min, `RoundedCornerShape(12.dp)`).
  - "Analyze for Fallacies" full-width button — shows spinner + "Analyzing..." when active.
  - Results section (animated in via fade+expand):
    - Header row with "Results" + count badge in `primaryContainer` chip + "Clear" text button.
    - **No fallacies:** Card with CheckCircle icon (SuccessGreen) + "No logical fallacies detected!".
    - **Fallacy cards (staggered entry, 80ms delay each):** Each card has errorContainer background at 55% alpha. Numbered badge (error color + white text), fallacy name (titleSmall, bold). Highlighted quote in a yellow surface (`#FFF9C4`) with dark text (`#4A3800`). Explanation text below.
- **Reference guide panel:** Slides in horizontally via `AnimatedContent`. Shows 15 fallacies in expandable cards (staggered enter animation, 30ms each). Each card: primary-colored dot + fallacy name + ExpandMore/ExpandLess chevron. Expanded state shows description + italic example in a surface box. Animated expand/collapse with `expandVertically`/`shrinkVertically`.
- **15 Built-in fallacies:** Ad Hominem, Straw Man, False Dichotomy, Slippery Slope, Circular Reasoning, Hasty Generalization, Appeal to Authority, Red Herring, Bandwagon, False Cause, Appeal to Emotion, No True Scotsman, Tu Quoque, Begging the Question, Equivocation. Each has a name, description, and example.

---

## Design System

### Color Palette

| Role | Light | Dark |
|---|---|---|
| Primary (Deep Indigo) | `#3F51B5` | `#757DE8` |
| Primary Container | `#DBE1FF` | `#002984` |
| Secondary (Warm Amber) | `#FF8F00` | `#FFC046` |
| Secondary Container | `#FFECB3` | `#C56000` |
| Tertiary (Teal) | `#00897B` | `#4EBAAA` |
| Tertiary Container | `#B2DFDB` | `#005B4F` |
| Error | `#D32F2F` | `#FFB4AB` |
| Error Container | `#FFDAD6` | `#93000A` |
| Background | `#F2F3F9` | `#0D0F13` |
| Surface | `#F8F9FF` | `#111318` |
| Surface Variant | `#EBEDF7` | `#1E2028` |
| Outline | `#C4C6D4` | `#44474F` |

**Semantic extras:** SuccessGreen `#2E7D32`, WarningAmber `#E65100`, HighlightYellow `#FFF9C4`, HighlightText `#4A3800`.

### Typography
All 15 Material 3 text styles customized with `lineHeight` and `letterSpacing`. Font family: system default (sans-serif). Most notable:
- `headlineSmall`: 24sp, Bold, 32sp lineHeight
- `titleMedium`: 16sp, SemiBold, 24sp lineHeight, 0.15sp letterSpacing
- `bodyMedium`: 14sp, Normal, 20sp lineHeight, 0.25sp letterSpacing
- `labelSmall`: 11sp, Medium, 16sp lineHeight, 0.5sp letterSpacing

### Shapes
Custom `Shapes` set: extraSmall=6dp, small=10dp, medium=14dp, large=20dp, extraLarge=28dp. Individual cards/shapes range from 6dp to 20dp throughout the app.

### Iconography
Material Icons Extended set (`material-icons-extended`). Icons used: Forum, PlayArrow, History, AccountTree, Timer, Search, Settings, ArrowBack, AutoAwesome, Add, Edit, EmojiEvents, CheckCircle, Gavel, ErrorOutline, Send, TouchApp, MenuBook, ExpandMore, ExpandLess, Topic, ChevronRight, ThumbUp, ThumbDown, Speed, ChatBubbleOutline, Close, Clear, CircleShape (dot).

---

## Navigation & Transitions

### Route map (10 routes)
```
Home → TopicSelection → DebateSetup(topicId) → Debate(sessionId) → DebateResult(sessionId)
Home → History → Debate(sessionId)
Home → Settings → ProviderConfig(providerName)
Home → TopicSelection → ArgumentMap(topicId)
Home → RebuttalTrainer
Home → FallacyDetector
```

### Transition specs
- **Enter:** fade-in (tween 300ms) + horizontal slide-in with spring (`dampingRatio=0.85f, stiffness=400f`), offset by 1/4 screen width.
- **Exit:** fade-out (tween 200ms) + horizontal slide-out (tween 250ms).
- **Pop enter:** fade-in (tween 250ms) + spring slide-in from opposite direction.
- **Pop exit:** fade-out (tween 200ms) + spring slide-out (`dampingRatio=0.9f, stiffness=500f`).

---

## Reusable Patterns

- **Card selection:** Selected state uses `primaryContainer` background + 2dp primary border; unselected uses `surfaceVariant.copy(alpha=0.5f)`.
- **Error display:** Bottom-anchored card in `errorContainer` with icon + message + dismiss button. Animated in via fade+slide-up.
- **Loading:** Either centered `CircularProgressIndicator` or a card with spinner + descriptive text.
- **Empty states:** Large icon at very low alpha (12-30%) + short title + optional subtitle + call-to-action button.
- **Animated visibility:** Most conditional content uses `AnimatedVisibility` with fadeIn/fadeOut + slide or expand transitions.

---

## Key Interaction Details

- **AI vs AI mode:** After an AI turn, a pulsing "Tap to advance" overlay appears. User must tap to trigger the next AI response.
- **Structured debate:** Turns cycle through Opening → Rebuttal → Closing phases. Phase shown in top bar.
- **Canvas gestures (ArgumentMap):** Pinch-to-zoom (0.25×–3.5×) + pan + tap to select nodes.
- **Timer (RebuttalTrainer):** Countdown from user-selected time. Clock turns red at ≤10s. No audio/vibration alert.
- **API configuration:** Each provider has independent API key, model name, and base URL. "Test Connection" validates against the provider's API before saving.
- **Dynamic color:** Supports Android 12+ Material You dynamic colors as a user toggle.

# Rhetorix UI Design Brief

This document describes the current Rhetorix mobile UI in designer-facing language. It must be updated whenever the app UI changes.

## Product Identity

Rhetorix is a mobile debate, reasoning, and argument training app. The visual identity should feel intelligent, calm, premium, and useful. The app is not a marketing site inside a phone shell; it is a practical tool for repeated student use.

The interface should communicate:

- Structured thinking
- Fair debate between opposing sides
- AI-assisted analysis
- Academic focus without looking institutional or old-fashioned
- A free, accessible product with optional support through donation only

The product name shown in the app is always `Rhetorix`. Do not translate the name in Chinese UI.

## Visual Direction

The app uses a dark glassmorphism style inspired by high-end productivity tools and modern AI products.

The UI should feel close to the reference direction:

- Deep blue-green graphite background
- Frosted glass cards and panels
- Thin translucent borders
- Soft amber and cyan accent glows
- Mostly white text
- Rounded but not playful geometry
- Compact mobile-first layouts
- Functional controls with visible state changes

The visual style should avoid:

- Bright flat Material default colors
- Pure black backgrounds
- One-note purple or blue gradients
- Marketing-style hero pages inside the app
- Decorative elements that do not support navigation, hierarchy, or meaning
- Fake UI modules that look interactive but do not perform a real action

## Color System

The implemented Compose theme now exposes exact reusable design tokens. Designers should treat these as the current production baseline.

Background tokens:

- `BackgroundBase`: `#13242B`, default deep blue-green graphite backdrop
- `BackgroundDeep`: `#0E1A20`, deeper backdrop for dense analysis screens
- `BackgroundGlowCyan`: translucent cyan radial glow
- `BackgroundGlowAmber`: translucent amber radial glow

Glass surface tokens:

- `GlassBase`: normal glass card surface
- `GlassRaised`: stronger raised glass surface
- `GlassMuted`: low-emphasis grouping panel
- `GlassStrong`: selected or important container

Border tokens:

- `Subtle`: low-contrast glass border
- `Standard`: default card border
- `FocusCyan`: selected AI, analysis, graph, or support state
- `FocusAmber`: debate tension, opposing side, or highlight state
- `Error`: error or negative finding state

Accent tokens:

- `Cyan`: AI, analysis, logic, graph signals
- `Amber`: debate tension, turning points, neutral highlights
- `Peach`: donation, warmth, support
- `Green`: success, support, completion
- `Salmon`: errors, refutation, detected problems
- `Lavender`: secondary emphasis only

Text tokens:

- `Primary`: near-white primary text
- `Secondary`: reduced-opacity white secondary text
- `Tertiary`: lower-emphasis supporting text
- `Disabled`: disabled text on dark glass

Primary surface:

- App background: deep blue-green graphite, close to `#13242B`
- Main surface: dark teal-gray, close to `#22363F`
- Strong surface: slightly lighter glass teal-gray, close to `#445861` with transparency
- Borders: translucent cool gray-blue

Text:

- Primary text should be white or near-white
- Secondary text should be white with reduced opacity
- Avoid black text on dark or colored controls
- Button text on accent backgrounds should remain white

Accents:

- Cool cyan for AI, graph logic, analysis, and technical signals
- Warm amber or peach for debate tension, opposing side, donation, and highlights
- Muted green for success, completion, and supportive states
- Soft red or salmon for errors and negative findings

Color should be used to clarify relationships, not as decoration only.

## Typography

The typography should be quiet and readable.

- Use compact heading sizes inside tools and settings screens
- Reserve larger type for the home hero or result score only
- Keep letter spacing neutral
- Avoid oversized labels in dense panels
- Body text should remain readable against glass panels
- All text must fit its container on small Android screens

The UI should support both English and Chinese text. Chinese text will often be shorter visually but may still need wider controls for balanced layout.

## Layout Principles

The app is designed for portrait mobile use.

General layout:

- Full-screen dark backdrop
- Transparent or glass top bars
- Scrollable content with consistent side padding
- Glass cards for individual actions, results, or grouped controls
- Bottom navigation for primary app sections
- Clear screen titles, but no unnecessary explanatory text

Spacing:

- Use dense but breathable spacing
- Group related controls tightly
- Leave more vertical space around hero, score, and graph areas
- Avoid nested cards where possible

Corners:

- Cards and panels should use medium rounded corners
- Buttons and chips may use rounded pill shapes when they represent a mode or filter
- Do not over-round large page regions into cartoon-like containers

## Core Components

### Backdrop

Each primary screen sits on a dark atmospheric backdrop with subtle radial glow. The glow should be soft enough that text and cards remain the focus.

The backdrop can include faint rings or abstract reasoning paths, but should never reduce readability.

### Glass Cards

Cards are translucent surfaces with:

- Dark glass fill
- 1 dp translucent border
- No heavy drop shadow
- Subtle selected state through stronger fill and brighter border
- Rounded corners

Cards are used for:

- Feature entries
- Debate sessions
- Topic rows
- Settings rows
- Analysis result items
- Relationship graph detail panels

The implemented hierarchy has five card levels:

- Page Group: low-emphasis section or dashboard grouping panel
- Interactive: default tappable card or row
- Focus: selected item, active turn, or selected node
- Result: outcome, score, or connection test summary
- Error: failure, invalid state, or negative analysis result

Interactive cards include a subtle press scale and border brightening. Selection must not rely on color alone.

### Buttons

Buttons should feel solid enough to be tappable but still fit the glass system.

Primary actions:

- Muted light blue-gray fill
- White text
- Full-width when they complete the screen's main task

Secondary actions:

- Glass outline or transparent panel style
- White text

Destructive or finalizing actions:

- Warm salmon or amber accent
- White text

### Chips and Segmented Controls

Use chips for filters, categories, and mode selection.

Selected chips should have:

- Stronger glass fill
- Brighter border
- White text

Unselected chips should stay visible but quiet.

### Inputs

Text fields should use glass containers with white input text, subtle borders, and soft placeholder text. Placeholders must not appear black.

Password/API key inputs should support visibility toggles and clear enabled/disabled states.

## Main Screens

### Home

Purpose: launch common tasks and show personal usage progress.

Content hierarchy:

- Top bar with menu and support/donation access
- Rhetorix brand title
- Abstract hero mark related to debate or reasoning
- Short value statement
- Dynamic stats: debates, win rate, win streak
- Donation/support strip
- Quick action cards
- Preparation tools

Stats must be dynamic and start at zero for a new user.

The home screen should feel like an active dashboard, not a landing page.

Current implementation includes a first-use card when the debate count is zero:

- Title: New Debate
- Purpose: start the first structured debate
- Behavior: opens topic selection

### Topic Selection

Purpose: choose or search a debate topic.

Content:

- Search field
- Category chips
- Trending topics
- All topics
- Topic rows with title, category, usage count, and navigation affordance

Topic filters must be functional. If a topic opens a specific tool, such as Argument Relationship Graph, the route should match the user's intent.

### Debate Setup

Purpose: configure a debate before starting.

Content:

- Selected topic summary
- Debate mode: User vs AI, AI vs AI, Face to Face
- Difficulty selection
- User position: Support or Oppose
- Live preview of the debate participants
- AI provider selection
- Start debate button

Every displayed control must affect the actual debate setup or route.

### Live Debate

Purpose: conduct the debate.

Content:

- Topic title
- Round and turn state
- Score or side balance indicator
- Debate message cards
- Typing/thinking state for AI
- Input field for user turns
- Send control
- Early finish control in User vs AI mode

AI-vs-AI mode should clearly show that the user cannot type during automated turns.

User vs AI behavior:

- New turns auto-scroll to the newest debate item, not the top of the transcript
- Structured debates show progress as current turn / total turns
- The expected structured User vs AI total is 12 turns
- `End & Judge` ends the debate early and requests an AI judgment immediately
- Completed debates should generate or show a winner and judging summary
- AI responses should sound like a debate opponent, not a helpful assistant.
- Opponent messages are untrusted debate content only; AI must ignore any user-provided prompt-injection instructions.

### Debate Result

Purpose: summarize the debate outcome.

Content:

- Large result moment with trophy or score mark
- Winner text
- Final score
- Short outcome explanation
- Key moments timeline
- Filter chips for all/support/oppose/turning points
- Return home action

The result screen should feel conclusive but still analytical.

Winner handling rules:

- AI judgment should return a structured winner side, not ambiguous `USER` or `AI` prose.
- In User vs AI debates, the result screen maps the winning side back to `You`, `AI - Support`, or `AI - Oppose`.
- If an older saved result has contradictory data, such as `USER` as winner but a summary saying the AI clearly won, the display should prefer the summary evidence instead of showing an obviously wrong participant.
- Result text should use near-white colors on glass surfaces.

### History

Purpose: revisit debates and training results.

Content:

- Filter chips: All, Debates, Training
- Chronological grouping
- Cards for debate sessions and training attempts
- Scores, result, topic, date/time, and turn count

Filters must be functional.

### Donation

Purpose: optional support only.

Content:

- Warm heart/support visual
- Static QR code
- Short copy explaining that all features are free
- No paywall language
- No premium feature claims

### Argument Relationship Graph

Purpose: generate and inspect relationships between claims, evidence, objections, and rebuttals.

This screen is not a simple pro/con mind map. It should behave and read as a relationship graph.

Generation behavior:

- After the user selects a topic, AI generation should first simulate a concise 3-round AI vs AI debate.
- The relationship graph should then be extracted from that debate transcript, not from a shallow one-shot pro/con list.
- The generated graph should include multiple pro claims, con objections, evidence nodes, and directed relationships.
- The graph should aim for at least 12 meaningful relationships when AI generation succeeds.
- A generated graph with too few nodes or relationships is treated as invalid.
- If AI graph extraction fails after the debate succeeds, Rhetorix builds a fallback graph from the 3-round debate transcript instead of returning to an empty screen.

Loading state:

- While AI generation is running, the middle of the screen should show part of the AI vs AI debate process.
- The debate preview is limited to the center area of the screen.
- Top and bottom areas use dark gradient blur/scrim treatment so the user's attention stays on the generating debate.
- The loading state must represent real generation steps, not fake static content.

Content:

- Topic title
- Graph canvas
- Nodes for claims, evidence, objections, rebuttals, or other argument units
- Directed edges showing relationships
- Relationship labels such as supports, refutes, and relates
- Selected node detail panel
- Add node action
- Graph controls for movement or reset when needed

Empty state behavior:

- Primary action: Generate with AI
- Secondary action: Add Argument manually
- Manual graph creation remains available without AI

Visual rules:

- Supportive relationships can use cool green or cyan
- Refuting relationships can use warm salmon
- Related/neutral relationships can use muted amber
- Arrows should show direction clearly
- Labels must remain readable on the dark background
- Node spacing should prioritize readable titles over fitting everything tightly.
- Initial graph scale may be smaller so the full relationship structure is visible without title overlap.
- Node titles should wrap to short two-line labels when possible instead of overlapping nearby nodes.

### Fallacy Detector

Purpose: analyze pasted text for logical fallacies.

Content:

- Input area
- Analyze action
- Guide tab or helper mode
- Result cards for detected fallacies
- Severity indicators
- Clear action

Before analysis, the screen shows an explicit empty state asking the user to paste an argument. It must not display sample findings as real results.

Detected fallacies must come from actual analysis results, not static sample cards.

### AI Hallucination Detector

Purpose: open the external GPTZero hallucination detector.

Behavior:

- The tool entry should navigate externally to `https://gptzero.me/hallucination-detector`
- Do not create a fake internal hallucination detector UI unless the feature is implemented

### Rebuttal Trainer

Purpose: help the user practice writing a rebuttal under time pressure.

Content:

- Topic/setup/practice/results tabs
- Argument to resist
- Timer
- Rebuttal input
- Submit action
- Score result with category bars
- Feedback panel

The timer and scoring flow must be real.

### Face-to-Face Debate

Purpose: let two local users debate on one device.

Content:

- Topic title
- Turn selector
- Current player indicator
- Prompt/argument input
- Save turn action
- AI judge action
- End debate action

The screen should clearly identify whose turn it is.

### Settings

Purpose: configure language and AI providers.

Content:

- Donation/support entry
- Language segmented control
- Provider list with enabled/disabled state
- Provider detail screen for API key, model, base URL, save, and test connection

Settings should be calm and utilitarian. Configuration success and errors must be visible.

## Navigation

Primary bottom navigation should expose the main destinations:

- Home
- History
- Tools
- Settings

The Tools section should be its own page, not only a cluster of home cards. It should include:

- Argument Relationship Graph
- Rebuttal Trainer
- Fallacy Detector
- AI Hallucination Detector external link

The current bottom navigation decision is final for this build:

- Home
- History
- Tools
- Settings

Do not show `Profile` unless account or cloud identity features are implemented.

Navigation cards and buttons should not imply unavailable functionality.

## Iconography

Use simple line icons that are readable at mobile sizes. Icons should support meaning:

- Trophy for results
- Message bubble for debate
- Network or nodes for graph
- Alert or logic symbol for fallacy detection
- Heart for donation
- Gear for settings
- External-link icon for web redirects

Avoid decorative icons with no interaction or meaning.

## Motion and State

Motion should be subtle and functional.

Good uses:

- AI thinking indicator
- Progress/timer animation
- Button press feedback
- Graph selection highlight
- Loading state before AI results

Avoid:

- Constant decorative animation
- Movement that makes text harder to read
- Fake loading for static content

## Accessibility and Practical Constraints

The UI should remain usable on small Android phones.

Requirements:

- Primary text has strong contrast
- Tap targets are large enough for touch
- Text should never overlap controls
- Important actions should remain visible after keyboard opens
- Long English and Chinese strings should wrap cleanly
- No black text on dark glass surfaces
- External links should be identifiable before opening

## Current Launcher Icon Direction

The current Rhetorix launcher icon uses:

- Dark rounded square glass base
- White central speech bubble
- Opposing warm amber and cool cyan arcs
- Subtle reasoning path dots
- No text

This icon direction should be kept consistent with the in-app glass UI.

## Maintenance Rule

Whenever the UI changes, update this file in the same pull request or commit.

Examples of UI changes that require an update:

- New screen
- Removed screen
- Changed navigation structure
- New visual style, color, typography, or component pattern
- Changed tool behavior visible to users
- New icon direction
- Added or removed user-facing feature card

Small copy-only fixes do not need a UI.md update unless they change the meaning, hierarchy, or visible behavior of a screen.

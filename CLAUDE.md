# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the plugin
./gradlew build

# Run IntelliJ IDE with the plugin installed for testing
./gradlew runIde

# Format code (uses Google Java Format via Spotless)
./gradlew spotlessApply

# Run tests
./gradlew test

# Verify plugin compatibility
./gradlew verifyPlugin
```

## Architecture Overview

CodeClocker is an IntelliJ IDEA plugin that tracks coding time and activity. It operates in two modes:
- **Local Mode** (default): All tracking data stays on the user's machine
- **Hub Mode** (optional): Syncs data to CodeClocker Hub web dashboards via API key

### Core Components

**Time Tracking Pipeline:**
- `FocusListener` (AWT event listener) → detects user activity in the IDE
- `TimeSpentActivityTracker` → manages activity detection with 2-minute inactivity timeout
- `TimeSpentPerProjectLogger` → accumulates time per project using `ProjectTimeAccumulator`
- `LocalStateRepository` → persists hourly activity snapshots to XML via IntelliJ's `PersistentStateComponent`
- `DataReportingTask` → scheduled task that reports accumulated data to Hub (if API key present)

**VCS/Git Integration:**
- `ChangesActivityTracker` → tracks added/removed lines from VCS
- `GitCommitStatsListener` (CheckinHandlerFactory) → captures commit statistics
- `BranchActivityTracker` → tracks time per git branch
- Git features are optional via `git-features.xml` config that depends on `Git4Idea`

**UI Components:**
- `TimeTrackerWidget` / `TimeTrackerWidgetFactory` → status bar widget showing daily time
- `TimeTrackerPopup` → popup panel with detailed stats and goal progress
- `BranchActivityToolWindowFactory` → tool window for branch activity reports

**State Persistence:**
- Local state stored in `codeclocker-local-state.xml` with hourly granularity
- Data retained for max 2 weeks, auto-cleaned on load
- Hour keys use UTC timezone (migration from local timezone happens automatically)

### Package Structure

- `services/` - Core tracking services (time tracking, VCS changes, per-project accumulation)
- `local/` - Local state persistence and data models
- `reporting/` - Hub sync and HTTP clients for data reporting
- `widget/` - Status bar widget and popup UI
- `toolwindow/` - Branch activity tool window
- `goal/` - Daily/weekly goal tracking and notifications
- `analytics/` - Anonymous usage analytics
- `onboarding/` - First-run onboarding flow
- `apikey/` - API key management for Hub Mode
- `git/` - Git/VCS integration handlers

### Key Design Patterns

- Application-level services registered in `plugin.xml` for singleton tracking components
- Project-level services for project-specific state
- `ListenerRegistrator` is the main startup entry point that initializes all background tasks
- Scheduled tasks use a shared `ScheduledExecutor` thread pool

## Code Style

- Only add comments for complex logic; avoid commenting obvious code

## Release Process

1. Update `pluginVersion` in `gradle.properties`
2. Add release note in `CHANGELOG.md`

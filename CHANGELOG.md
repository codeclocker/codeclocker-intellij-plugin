<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# CodeClocker Changelog

## [Unreleased]

### Added

- **Per-Project Goal Tracking** - Set custom daily and weekly coding time goals for individual projects:
  - New "Set Project Goals..." option in the status bar popup
  - Enable/disable custom goals per project (falls back to global goals when disabled)
  - Independent daily and weekly goal targets per project
  - Per-project goal notifications with toggle to enable/disable
- **Project Goal Progress in Popup** - When custom goals are enabled, the status bar popup shows a "Project Goals" section with daily and weekly progress bars for the current project

## [1.7.0] - 2026-01-11

### Added

- **Activity Report Info Banner** - New informational banner in Activity Report tool window:
  - Shows days of activity history stored locally
  - Displays local storage retention limit (14 days)
  - Quick link to connect to Hub for unlimited history retention
  - Adapts message based on Hub connection status

### Changed

- **Local Trend Calculations** - "Today vs Yesterday" and "This Week vs Last Week" comparisons now calculated from local data:
  - Works offline without Hub connection
  - Faster popup display (no network requests)
  - Removed dependency on Hub API for trend data
- **Async Branch Tracking Initialization** - Branch tracker now initializes asynchronously:
  - Faster IDE startup
  - Prevents UI blocking when Git services are slow to initialize
- **Unified Hub Sync** - All activity data now sent in a single API payload:
  - Time spent, VCS changes, branch activity, and commits synced together
  - More efficient network usage
  - Better data consistency

### Removed

- `TimeComparisonFetchTask` and `TimeComparisonHttpClient` - replaced by local calculations
- `DataAccessPolicy` - simplified data access architecture

## [1.6.0] - 2026-01-08

### Added

- **Activity Report Tool Window** - New IDE tool window accessible from the status bar popup showing detailed activity breakdown:
  - Tree-table view with daily activity organized by project
  - Commit history display with hash and message
  - Project filter dropdown to view all projects or a specific one
  - Auto-refresh every 10 seconds to show live data
  - Expand/collapse all functionality
- **CSV Export for Invoicing** - Export activity data to CSV format:
  - Date range selection dialog
  - Includes date, project, hours (decimal), and commit descriptions
  - Proper CSV escaping for special characters
- **Git Branch and Commit Tracking** - Enhanced VCS integration:
  - Track time spent per Git branch within each project
  - Record commits with hash, message, author, timestamp, and changed files count
  - Branch change listener to track branch switches
- **Auto-Pause Settings** - Configure tracking behavior via "Auto-Pause..." in status bar popup:
  - Toggle pause when IDE loses focus
  - Configure inactivity timeout with minutes and seconds precision (10 sec - 60 min range)

### Changed

- **UTC Timezone Storage** - Local storage now uses UTC timezone for hour buckets:
  - Consistent data storage regardless of timezone changes
  - Automatic conversion to local timezone for display in Activity Report
- **Idempotent Hub Sync** - Improved data sync reliability:
  - Added `recordId` field for local storage records
  - Prevents data duplication on double-sync while supporting multiple IDEs
  - Live reporting still uses delta (ADD) mode for real-time updates
- **Reworked Time Tracking Architecture** - Internal improvements:
  - New `ProjectTimeAccumulator` for per-project time accumulation
  - `CodingTimeCalculator` for total coding time calculations
  - Better separation of concerns between tracking and reporting
- **Analytics Event Types** - Cleaner analytics tracking:
  - Unique event type constants for each trackable action
  - Removed Map-based properties in favor of descriptive event names

### Fixed

- Activity Report now shows same totals as status bar widget (includes unsaved deltas)
- Improved data persistence during IDE shutdown with final flush

## [1.5.2] - 2025-12-31

- Update README

## [1.5.1] - 2025-12-27

- Fix showing multiple goal notifications per each opened project

## [1.5.0] - 2025-12-27

- Onboarding guide after plugin installation

## [1.4.0] - 2025-12-24

- Add daily and weekly coding time goals with progress tracking and notifications

## [1.3.1] - 2025-12-22

- Support latest IDE version

## [1.3.0] - 2025-12-15

- Save activity data to local storage to survive IDE restarts

## [1.2.3] - 2025-11-29

- Reset VCS changes at midnight in IDE status bar

## [1.2.2] - 2025-11-28

- Team support

## [1.2.1] - 2025-11-26

- Update plugin name

## [1.2.0] - 2025-11-26

- Skip showing "Enter CodeClocker API Key" dialog on every IDE start

## [1.1.0] - 2025-11-22

- Show VCS changes in IDE popup

## [1.0.11] - 2025-11-19

- Fix bug with time tracking after project closure

## [1.0.10] - 2025-11-17

- Fix bug with time tracking after project closure
- Update plugin description

## [1.0.9] - 2025-11-16

- Add status bar widget with daily coding activity

## [1.0.8] - 2025-10-29

### Added

- Update to IntelliJ Platform plugin version 2.9.0

## [1.0.7] - 2025-04-16

### Added

- Show subscription expiration notification on every IDE restart

## [1.0.6] - 2025-04-08

### Added

- Improve onboarding UX

## [1.0.5] - 2025-04-04

### Added

- Add plugin icon

## [1.0.4] - 2025-04-03

### Added

- Validate API key input
- Improve onboarding UX

## [1.0.2] - 2025-04-01

### Added

- Support IntelliJ Platform 2024.3.5

[Unreleased]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.7.0...HEAD
[1.7.0]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.6.0...v1.7.0
[1.6.0]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.5.2...v1.6.0
[1.5.2]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.5.1...v1.5.2
[1.5.1]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.5.0...v1.5.1
[1.5.0]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.3.1...v1.4.0
[1.3.1]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.2.3...v1.3.0
[1.2.3]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.2.2...v1.2.3
[1.2.2]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.0.11...v1.1.0
[1.0.11]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.0.10...v1.0.11
[1.0.10]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.0.9...v1.0.10
[1.0.9]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.0.8...v1.0.9
[1.0.8]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.0.7...v1.0.8
[1.0.7]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.0.6...v1.0.7
[1.0.6]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.0.5...v1.0.6
[1.0.5]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.0.4...v1.0.5
[1.0.4]: https://github.com/codeclocker/codeclocker-intellij-plugin/compare/v1.0.2...v1.0.4
[1.0.2]: https://github.com/codeclocker/codeclocker-intellij-plugin/commits/v1.0.2

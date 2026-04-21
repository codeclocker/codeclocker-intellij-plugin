# CodeClocker – Time Tracking, AI Timesheets & Team Pulse for JetBrains IDEs

![Build](https://github.com/codeclocker/codeclocker-intellij-plugin/actions/workflows/gradle.yml/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/26962.svg)](https://plugins.jetbrains.com/plugin/26962)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/26962.svg)](https://plugins.jetbrains.com/plugin/26962)

<!-- Plugin description -->

## Automatic Time Tracking for JetBrains + AI Timesheets in Hub

CodeClocker tracks active coding time per project in your JetBrains IDE and works locally by default — no account required. 
It helps you understand where your time went, prepare standup updates, and export activity data without leaving the IDE.

Optionally connect to [CodeClocker Hub](https://hub.codeclocker.com/) to turn IDE activity, commits, and branches into AI-generated weekly timesheet drafts, My Brief summaries, Jira exports, invoices, Team Pulse summaries, approvals, and billing-ready reports.

### Use it locally (default)

- **Automatic time tracking** — records active coding time per project in the background.
- **What I Was Doing** — generate standup-ready summaries with time breakdowns, branch details, and commits.
- **Activity Report** — review recent activity by project and commit history inside the IDE.
- **Daily & weekly goals** — set coding targets globally or per project, and get notified when you hit them.
- **Pomodoro Timer** — built-in timer with configurable work and break intervals.
- **CSV export** — export activity data for invoicing or reporting.
- **Privacy first** — all data stays on your machine in Local Mode.
- **Auto-pause** — pauses tracking when the IDE loses focus or on inactivity timeout.

### Connect to Hub for AI timesheets, exports, and team workflows

- **AI-generated weekly timesheets** — turn IDE activity, commits, and branches into editable weekly timesheet drafts.
- **My Brief** — get an AI-generated summary of your recent work across projects before standup or weekly review.
- **AI standup summaries** — generate concise standup-ready updates from your actual work.
- **Jira worklog export** — export approved timesheets to Jira and attach logged time to matching tickets using issue keys parsed from IDE activity.
- **CSV & PDF exports** — export timesheets for payroll, accounting, client billing, or personal reporting.
- **Invoices** — generate invoice-ready outputs for freelance and consulting work.
- **Team Pulse summaries** — see AI-generated summaries of what moved across projects and across the team.
- **Approvals workflow** — managers review submitted timesheets and request changes when needed.
- **Evidence-linked worklogs** — connect timesheet entries to branches and commits.
- **Automated reminders** — remind developers to submit and managers to review on time.

### Supported IDEs

IntelliJ IDEA, PyCharm, Android Studio, WebStorm, PhpStorm, Rider, CLion, GoLand, DataGrip, RubyMine, AppCode, RustRover — and all other JetBrains IDEs.

Learn more at [site.codeclocker.com](https://site.codeclocker.com/).

<!-- Plugin description end -->

---

## Screenshots

### Team Timesheet Approvals

Review submission status and weekly worklogs for every team member. Approve timesheets and export the week — all from one screen.

<img src="https://site.codeclocker.com/screenshots/team-dashboard.jpg" alt="CodeClocker auto-generated weekly timesheet with commit evidence" width="100%" />

### AI-Generated Timesheets

AI turns IDE activity, commits, and branches into an editable weekly timesheet draft. Developers review, adjust, and submit. Every entry links back to real commits and branches.

<img src="https://site.codeclocker.com/screenshots/timesheet.png" alt="CodeClocker team timesheet approval dashboard" width="100%" />

### Team Pulse

Stay aligned on what changed across the team. 
Team Pulse turns commit and project activity into AI-generated summaries, meaningful change highlights, and coordination signals — giving your team shared awareness without extra status meetings.

<img src="https://site.codeclocker.com/screenshots/team-pulse.jpg" alt="CodeClocker Team Pulse dashboard with AI-powered team awareness and project summaries" width="100%" />

### Reports & Exports

Export approved timesheets as CSV for payroll or PDF for clients. Close the week once everything is approved.

<img src="https://site.codeclocker.com/screenshots/export-everywhere.png" alt="CodeClocker reports and exports — CSV, PDF, close week" width="100%" />

### In-IDE Activity Dashboard

The status bar widget shows today's tracked time, goal progress, coding trends, and per-project breakdown — without leaving your IDE.

<img src="https://site.codeclocker.com/screenshots/status-bar-popup.png" alt="CodeClocker IDE status bar widget with goals and coding trends" width="400" />

### Activity Report

Detailed tree-view of daily coding activity organized by project. Expand any day to see commits with hashes and messages. Filter by project and export to CSV.

<img src="https://site.codeclocker.com/screenshots/activity-report.png" alt="CodeClocker activity report tool window with commit details" width="100%" />

---

## Installation

- **From your IDE:**

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "CodeClocker"</kbd> > <kbd>Install</kbd>

- **From JetBrains Marketplace:**

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26962) and click <kbd>Install to ...</kbd>.

  Or download the [latest release](https://plugins.jetbrains.com/plugin/26962/versions) and install manually via <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---

## Getting Started with Teams

1. **Install the plugin** — from JetBrains Marketplace or directly in your IDE.
2. **Sign up on [CodeClocker Hub](https://hub.codeclocker.com/)** — create your account and get an API key.
3. **Connect the plugin** — paste the API key via <kbd>Tools</kbd> > <kbd>CodeClocker API Key</kbd>.
4. **Create a team** — invite developers via a shareable link.
5. **Approve & export** — review AI-generated timesheet drafts each week, approve, and export to CSV or PDF.

Developers only need the plugin installed and connected. CodeClocker builds draft timesheets automatically from IDE activity.

<a href="https://hub.codeclocker.com/"><strong>Start your free team trial →</strong></a>

---

## Links

- [CodeClocker Website](https://site.codeclocker.com/) — features, pricing, and how it works
- [CodeClocker Hub](https://hub.codeclocker.com/) — web dashboard for timesheets, approvals, and exports
- [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26962) — plugin page with reviews and ratings
- [GitHub Issues](https://github.com/codeclocker/codeclocker-intellij-plugin/issues) — report bugs or request features
- [Changelog](CHANGELOG.md) — release history and what's new

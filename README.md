# CodeClocker – IntelliJ Plugin for Tracking Developer Activity

![Build](https://github.com/codeclocker/codeclocker-intellij-plugin/actions/workflows/gradle.yml/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/26962)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/26962)

<!-- Plugin description -->

## What's CodeClocker

CodeClocker automatically tracks coding activity and visualizes it in the IDE status bar and on [web dashboard on CodeClocker Hub](https://hub.codeclocker.com/).

![Demo](https://raw.githubusercontent.com/codeclocker/codeclocker-intellij-plugin/main/docs/img/demo.gif)

### Features

* **Coding Time Tracking** – Automatically records time spent on each project.
* **VCS Added & Removed Lines** – Tracks the number of added and removed lines in VCS.
* **IDE Status Bar Widget** – Displays current coding activity directly in IDE status bar for quick at-a-glance monitoring.
* **Activity Web Dashboard** – Visualises coding activity on web dashboard on [CodeClocker Hub](https://hub.codeclocker.com/).

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "CodeClocker"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26962) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/26962/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/codeclocker/codeclocker-intellij-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## API Key Activation

1. After [installation](#installation), a dialog will prompt you to enter your CodeClocker API key. 
  Click "Get API Key" to be redirected to the [API Key page on CodeClocker Hub](https://hub.codeclocker.com/api-key). 
  Copy your API key from this page.
2. Return to your running IntelliJ IDE, paste the copied API key into the dialog window, and click "OK".

Once set up, CodeClocker will automatically track your coding activity and report it to [CodeClocker Hub](https://hub.codeclocker.com). 
If your API key status initially shows "Inactive: No received data yet," don’t worry—it may take up to a minute for activity tracking to start. 
Once active, it will update to "Active: Receiving data".

If you need to open the API Key dialog window in your IntelliJ IDE (for example, you accidentally closed it during initial plugin installation), then go to **Tools > CodeClocker API Key** at the top of the dropdown list.

![API key menu](docs/img/api-key-menu.jpg)

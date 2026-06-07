# Roadmap vs Tasks

This document explains how Droid Mod Loader separates long-term planning from immediate work.

## Core Rule

The roadmap is not the task list.

The roadmap explains where the project is going.

Tasks explain what should be done next.

## Use ROADMAP.md For

- major development phases
- pre-1.0 goals
- release direction
- large feature areas
- project priorities
- long-term systems

Examples:

- Safe deployment
- Recovery tools
- Resolved data graph
- Plugin intelligence
- Downloads and metadata
- Advanced installer support
- Diagnostics
- Release polish

## Use docs/tasks/current-priorities.md For

The small set of tasks that should be worked on soon.

This file should stay short.

Use it for:

- the next 3 to 7 tasks
- bugs that are actively being fixed
- cleanup items that should happen before more features
- implementation steps pulled from the roadmap

## Use docs/tasks/backlog.md For

Rough or deferred ideas.

Use it for:

- future ideas
- maybe-later features
- notes that need more thought
- reminders not ready for code
- ideas that need requirements before implementation

## Use GitHub Issues For

Scoped work that is ready to implement.

A GitHub Issue should include:

- requirement IDs
- problem
- desired behavior
- scope
- out of scope
- files likely affected
- test steps
- done criteria

## Use docs/decisions.md For

Choices that affect future work.

Examples:

- physical deployment instead of VFS
- GameNative as main early target
- recovery tools are user-facing
- profiles are first-class app state
- advanced UI uses progressive disclosure

## Planning Flow

Use this flow:

1. Idea appears.
2. Put rough idea in backlog.
3. If important, connect it to a requirement.
4. If ready, move it to current priorities.
5. If implementation is clear, make a GitHub Issue.
6. Code from the issue or current-priority task.
7. Update docs/changelog if needed.
8. Commit and push.

## Rule for New Coding Work

Do not code directly from the roadmap.

Before coding, convert roadmap direction into a scoped task.

## Example

Roadmap item:

Safe deployment.

Task:

Add unfinished deployment warning.

Issue:

Detect unfinished deployment journal on app launch and show a visible recovery warning with review/clear options.

Related requirements:

- REQ-RECOVERY-001
- REQ-RECOVERY-002
# Contributing to BorbaCar Labs

## Overview

BorbaCar Labs is an Android OBD-II diagnostic app written in Java. This guide covers the branch model, local setup, commit messages and the pull request process.

## Git Workflow (GitFlow)

### Main Branches

- `main`: published versions only; every merge receives a `vX.Y.Z` tag.
- `develop`: integration branch for the next version (default branch).

### Supporting Branches

- `feature/<name>`: new features, from `develop`.
- `fix/<name>`: bug fixes, from `develop`.
- `docs/<name>` and `chore/<name>`: documentation and maintenance, from `develop`.
- `release/X.Y.Z`: version bump and CHANGELOG, from `develop`, merged into `main`.
- `hotfix/X.Y.Z`: urgent fixes, from `main`, merged into `main` and `develop`.

### Workflow

```bash
git switch develop
git pull
git switch -c feature/my-feature

# work, commit, push
git push -u origin feature/my-feature
```

Open a pull request to `develop`. After a release is merged into `main`, tag it and merge `main` back into `develop`:

```bash
git switch main && git pull
git tag -a v1.0.3 -m "BorbaCar Labs 1.0.3"
git push origin v1.0.3
git switch develop && git merge --no-ff main && git push
```

## Development Environment Setup

### Prerequisites

- JDK 17 or newer
- Android SDK with platform 35
- Node.js 20+ (only for the AI server)

### Initial Setup

```bash
git clone https://github.com/YagoBorba/BorbaCarLabs.git
cd BorbaCarLabs
./gradlew assembleDebug
```

Demo mode lets you go through every screen without a car or adapter.

## Project Architecture

- The OBD stack (`ElmSession`, `ObdReader`, `ObdParser`, `Transports`) only sends read-only queries; clearing codes lives in `EmissionCodeReset` and always asks for confirmation.
- The rules engine and the AI receive data, never commands, and never reach the transport.
- Every fact in a report keeps its evidence and source (`FactProvenance`, `SourceReference`).
- Simulated data is always labeled as simulated, on screen and in reports.
- Saved cases are recalculated and compared byte by byte when opened, so catalogs and rules used by a saved case are versioned and frozen.

## Commit Convention

Commits follow [Conventional Commits](https://www.conventionalcommits.org/), in English and without emojis. See the [Commit Convention](.github/COMMIT_CONVENTION.md) for types, scopes and examples.

```bash
feat(journal): add service reminders by mileage
fix(reports): keep page breaks inside the freeze frame table
```

## Pull Request Process

### Before Submitting

- [ ] `./gradlew testDebugUnitTest lintDebug` passes locally
- [ ] New behavior is covered by tests
- [ ] CHANGELOG updated when the change is visible to users
- [ ] Screenshots attached for UI changes

### Review Process

The CI must be green before merging. Pull requests are merged with a merge commit and the branch is deleted afterwards.

## Testing

```bash
./gradlew testDebugUnitTest        # JVM and Robolectric tests
./gradlew lintDebug                # Android lint
```

- `FakeAdapter` plays scripted ELM327 responses for the OBD tests.
- `ScreensTest` opens the real `MainActivity` with Robolectric and saves screenshots to `app/build/screenshots/`.

## Code Standards

- Java 17, Android SDK only (no external runtime libraries).
- Views are built in code through `UiKit` and `ThemePalette`.
- Use Android constants (`View.GONE`, `Gravity.CENTER`, ...) instead of raw numbers.
- Write user-facing text in Brazilian Portuguese and add its English version to `app/src/main/res/raw/translations_en.json` (exact text in `exact`, text with numbers or names in `patterns`). `EnglishScreensTest` fails when a screen still shows Portuguese in English mode.

## Reporting Issues

Open an issue with the app version, Android version, adapter model and connection type, the steps to reproduce and, when possible, the exported case JSON.

## Contact

- Email: yagoaborba@hotmail.com
- LinkedIn: https://www.linkedin.com/in/yagoborba/

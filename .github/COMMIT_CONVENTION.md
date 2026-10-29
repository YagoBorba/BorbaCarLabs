# Conventional Commits Reference

## Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

## Types

| Type       | Description                              |
| ---------- | ---------------------------------------- |
| `feat`     | Introduce new features                   |
| `fix`      | Fix a bug                                |
| `docs`     | Add or update documentation              |
| `style`    | Formatting changes with no logic change  |
| `refactor` | Refactor code                            |
| `perf`     | Improve performance                      |
| `test`     | Add or update tests                      |
| `chore`    | Maintenance and configuration            |
| `build`    | Build system, dependencies and releases  |
| `ci`       | CI workflows                             |
| `revert`   | Revert a previous commit                 |

Messages are written in English, in lowercase after the colon, without emojis and without a trailing period.

## Project-Specific Scopes

### Feature Scopes
- `connection`: ELM327 adapters over Bluetooth Classic, BLE and Wi-Fi
- `reading`: guided reading, trouble codes, readiness and freeze frame
- `live`: live sensors, chart and CSV recording
- `diagnosis`: investigations, rules engine and verification checklist
- `reports`: JSON, TXT and PDF reports
- `garage`: vehicle profiles and the Inmetro catalog
- `journal`: fill-ups, services, fuel economy and reminders
- `ai`: AI analysis contract and client

### Technical Scopes
- `obd`: ELM session, OBD parser and sensor catalog
- `knowledge`: trouble code catalog and SAE J2012 structure
- `history`: SQLite stores and JSON codecs
- `ui`: screens, theme and motion
- `server`: Node.js AI proxy server
- `tools`: catalog generation scripts
- `deps`: dependency updates
- `release`: version bumps and changelog

## Examples

```bash
feat(live): add coolant temperature to the default sensor set
fix(connection): retry BLE notifications when the descriptor write fails
docs: update setup instructions for the AI server
refactor(diagnosis): extract verification checklist from the rules engine
test(journal): cover reminders for services without mileage
chore(deps): upgrade Robolectric to 4.17
ci: upload lint report as a workflow artifact
build(release): bump version to 1.0.3
```

## Breaking Changes

Add `!` after the type for breaking changes, such as a new saved case format:

```bash
feat(history)!: store readings with schema version 2
```

## Body and Footer Guidelines

### Body
- Use imperative mood ("add" not "added")
- Explain the **what** and **why**, not the how
- Keep lines under 72 characters
- Separate paragraphs with blank lines

### Footer
- Reference issues: `Closes #123`, `Fixes #456`, `Relates to #789`
- Breaking changes: `BREAKING CHANGE: description`

## Validation Checklist

Before committing, ensure:
- [ ] Type accurately describes the change
- [ ] Scope is relevant to BorbaCar Labs
- [ ] Description is clear and concise
- [ ] Body explains context when needed
- [ ] Issues are properly referenced
- [ ] Breaking changes are clearly marked

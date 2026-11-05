# Changelog

## Unreleased

- Friendlier AI chat: questions and replies appear as chat bubbles, each reply answers the question directly in plain language, and possible causes, missing information and what to check sit in a collapsible section under the reply. Suggestions send with one tap and change after the first answer. The raw JSON preview before every request was replaced by a plain-language notice shown once per provider ("See the data sent to the AI" stays in More options). Errors appear in the chat with "Try again" or "Set up the key".
- AI replies no longer fail when the model writes text instead of a reference in a hypothesis: the response schema lists the refs that were sent, and anything else is dropped from the reply instead of discarding it. Overlong lists are trimmed.
- A busy model (HTTP 500/503/529) is retried once, then the lighter model of the same provider is used (`gemini-3.1-flash-lite` or `claude-haiku-5-5`) when no model was chosen.
- The key setup shows whether a key is saved, colors the test result and ignores spaces, line breaks and quotes copied with the key.
- AI with your own API key: the app calls Gemini (Google) or Claude (Anthropic) directly over HTTPS. Paste a key in **Settings › AI analysis**, choose the provider and tap **Save and test key**; the test checks the key and model without generating text. Keys are encrypted with an Android Keystore key, kept per provider and only sent in a header to that provider. No computer, local server or shared Wi-Fi is needed, and the release build works too.
- The Node.js AI server, `start-ai.ps1`, the server invitation and the server CI job were removed. The instructions, response schema and case limits now live in the app.
- Clear messages for a rejected key, usage limit, unavailable model, overloaded provider, refusal, truncated reply and provider errors such as a low credit balance. Default models: `gemini-3.5-flash-lite` and `claude-sonnet-5-5`, both overridable; Claude keys without a workspace can set its ID.
- The debug build no longer allows cleartext HTTP.
- Academic AI experience: case-scoped conversation per revision, suggested questions, local history kept apart from the facts, sharing with an AI app chosen by the user, and an offline guide that is explicitly not AI.
- Restorable logical backup of garage, readings, investigation revisions and journal; validation before replacing data, undo, and recovery of an interrupted restore. CSV, credentials, preferences and AI conversations are not part of the backup.
- Maintenance overdue by distance now comes before services that are only due later by date; the summary flags the distance overdue.
- Academic protocol with five synthetic cases, optional evaluation against a real model, human review and a pending physical test checklist.
- The Today screen shows the odometer of the latest journal entry on the "Open the Journal" shortcut. The theme can be changed during the demo: the demo ends and the app reopens with the new theme.
- Trouble code catalog with 26 more common codes, checked against Ford's public 2014 OBD-II code table: camshaft timing (P0011 to P0019), air flow (P0101), thermostat (P0128), crankshaft position sensor (P0335, P0336), idle speed (P0506, P0507), system voltage (P0562, P0563), fuel pump (P0627) and module communication (U0001, U0073, U0100, U0101, U0121, U0140, U0155). System voltage gained guidance and common causes.
- Settings grouped into Demo (now at the top), Everyday use (reminders, theme and language) and Advanced (AI analysis, automatic identification and privacy). The theme appears once, on its own button. No tab stays highlighted in Settings (the bar used to highlight "Today"), and the header says "DESCONECTADO" instead of "OFFLINE" in the Portuguese app.
- Fewer disclaimers on the main screens: the Journal and the entry form only show the profile name, and the last reading on Today no longer says "origin as recorded". The origin is still in the details, the reports and the workshop summary.
- "Can I keep driving?": each code shows general guidance (stop as soon as it is safe, avoid driving until the workshop, or drive with care and book a visit) and, in "Understand this reading", a sentence on the effect and common causes, marked as unconfirmed. The workshop summary includes both. Manufacturer codes and other systems (P1, B, C, U) get no general guidance.
- Cleaner Live screen: sensor cards no longer repeat "Simulated" (the simulated-data notice, the open card and the screen reader still do) and they warn about high engine temperature, module voltage out of range and very high engine speed. "0 s ago" became "now" and the "Back to diagnosis" button left the top (the tab and the Back button go to the same place).
- The guided reading explains the queries only the first time; afterwards it starts right away.
- Simpler connection: "Find my scanner" first offers "Reconnect" to the last adapter that answered (Bluetooth, BLE or Wi-Fi); paired devices with an OBD adapter name (OBDII, ELM, Vgate, V-LINK…) come before headphones and watches, BLE scanning hides unnamed devices, and Wi-Fi starts at 192.168.0.10:35000. A profile is no longer needed to connect (the guided reading still asks for one).
- More stable connection: a single unanswered query no longer drops the session (only three in a row do); when the connection drops, the notice offers "Reconnect". Disconnecting keeps the last reading on the Diagnosis screen. The screen stays on in Live during a session, and CSV recording continues when switching tabs.
- Faster fill-ups: "Log fill-up" on Today opens the form directly. The form follows the pump order (odometer, liters, price per liter, total, fuel, full tank) with the note last; with two of the three values the app computes the third; fuel starts with the last one used; the odometer shows the last entry and immediately rejects a lower value; ticking "I filled the tank" also marks that every fill-up in the interval was logged. The fuel list is translated in English.
- More useful Journal: fill-up cards show liters, price per liter, full or partial tank and the km/L of that tank; the entry type label no longer has spaced-out letters. "Fuel economy, costs and reminders" adds the average of the tanks, the last six tanks with cost per km, fuel spending in the last 30 days and the ethanol vs. gasoline comparison (by your car's cost per km or by the 70% rule). "New entry" starts with Fill-up; services and symptoms have a list of common items; when saving a service with a due date, the app offers to turn on reminders.
- Car search from 2012 to 2026: the catalog grows from 516 versions of the 2019 model year to 9,362 versions of 504 models and 43 makes, extracted from the yearly PBE Veicular tables (Inmetro). Missing models were added (Kwid, Tracker, Jetta, T-Cross, Montana, Equinox, Peugeot 308 and 3008) and names split or broken in the 2019 table were fixed ("Turbo", "Town &", "RAV4 4X2", "Saveiro CD", "Novo Cruze", "XC90 (2019)"). The catalog loads in the background and the official fuel economy applies to the new years.
- Garage with text search: "Find my car" finds the version with a few words ("onix 2019 ltz") and goes straight to the review. Lists are sorted alphabetically regardless of case, years start with the newest, "I don't know" goes last and versions no longer repeat the year. Typing by hand a year that exists in the catalog no longer clears the make and model. The review shows the data origin only once, and the default name includes the make ("Chevrolet Cobalt 2019").
- VIN and calibration show the make and model year decoded from the VIN, offline, and warn when they do not match the active profile.
- More compact Today screen: vehicle card with a smaller illustration next to the name and a single summary with the last reading, next service and fuel economy, visible without scrolling on 360x640 dp screens and readable with large fonts.
- App in Portuguese and English: new Language item in Settings (Automatic, Português or English). Screens, dialogs, reminder notifications, TXT/PDF reports and the trouble code catalog are translated; numbers follow the language (12,4 or 12.4). The AI analysis replies in the chosen language. Saved cases do not change: translation happens only on display.
- Diagnosis shows the result first: after a reading, the verdict and codes open the screen and the connection controls are folded into "Connection and new reading"; during the reading the previous layout stays, with progress visible.
- Next step after a reading with codes: "Understand this reading" explains each code with its meaning, the service that reported it (stored, pending or permanent), the check engine light and the limits of the reading; "Prepare workshop summary" builds a text with vehicle, codes, queries and collected supplement to share or save as PDF. Everything works offline. The report PDF no longer shows "falha indicada" in Portuguese when the app is in English.
- More compact Journal: summary at the top with fuel economy, next service and last entry side by side; "New entry" and the filter on the same line; smaller entry cards, tappable as a whole, with date, odometer and amount on one line. The fuel economy explanation, total costs and how reminders work are in collapsible sections. The first entries appear without scrolling.
- Live with a summary of every sensor: a grid shows value, unit and state of each selected sensor (up to 6) at the top; tapping a sensor opens its large card with the session chart. Each sensor keeps its own history (before, only engine speed had a chart) and the chart uses the sensor's decimals. The default selection now has 6 sensors and the demo also simulates intake temperature, manifold pressure, ignition timing, MAF, fuel level and module voltage.
- Accessibility: at 200% font size the header stays on a single line (icon, state and Settings), the simulated-data notice is short and the tab bar uses text only, leaving more than half of the screen for content. The verdict badge no longer clips its symbol. TalkBack reads the connection state in full ("No connection to the adapter" instead of "circle OFFLINE") and finds screen titles as headings. New tests walk every screen at 200% font size and on a 320 dp screen looking for clipped text, broken words, off-screen content, unlabeled controls and touch targets smaller than 48 dp; another test checks a contrast of at least 4.5:1 in the light and dark theme colors.
- Reference screenshots: nine screens (Portuguese and English, with and without data) are compared with approved images stored in the repository, with a fixed clock; a color or text change fails the test and produces an image with the difference in red. Updating the references is one command (`-PupdateScreenshots`). Displayed dates and times follow a single app clock, and the device time zone is read on every use.
- Garage fully in English: "Model year", the catalog coverage note, the nickname hint, the field names read by the screen reader, transmissions ("6-speed", "Dual clutch") and the review with catalog sources used to appear in Portuguese. The English screens test now walks the whole editor.

## 1.1.1 / Critical fix — 2026-11-01

- Saving a Garage profile with make, model, year or version chosen from the catalog froze the app ("app isn't responding"): the catalog check never finished. The profile now saves at once, and catalog data that is not in the catalog or contradicts the other choices is still rejected with a notice. New tests save catalog profiles straight to storage (with a timeout) and through the full screen flow.

## 1.1.0 / Reliability — 2026-10-29

- Maintenance reminders per service: a new service no longer hides another service's reminder (oil change + tire rotation), logging the same service without a new due date completes the reminder, and Today shows the next one due, not the last one created.
- A release is only published with a certificate: without the signing secrets the workflow fails before creating the version, and the signature is checked with `apksigner` before publishing. A manual run only builds the APK as an artifact.
- The Journal describes reminders as they work today (it used to say there were no notifications); readiness monitors and freeze frame get a short explanation in the guided reading confirmation, in the supplement and in the scanner tools.
- AI analysis removes phone numbers, e-mails, CPF, CNPJ and license plates from symptoms before sending; the preview shows the exact text that leaves the phone and what was removed. Texts and documents no longer promise full anonymization: freely written names and addresses are not detected.
- The AI server reserves quota before calling the provider and stores the day's usage in a file: concurrent requests do not exceed the limit and restarting the server does not reset the count.
- AI server with behavior tests (fake provider, no network): request contract, token, quota, provider error and delay, invalid reply or reply with invented evidence; CI runs `npm test`. An AI reply that is not JSON now returns 502 instead of the request's "invalid JSON", and the provider has a timeout.
- AI analysis with its own error messages: a server that is down is no longer described as a Wi-Fi adapter problem, and an unexpected reply format no longer shows the library's technical error. New failure tests: connection dropping in the middle of reading codes and recording, invalid AI server replies and Bluetooth permission denied.

## 1.0.2 / Tooling — 2026-10-28

- Android Gradle Plugin 8.13.2 and Gradle 8.14.6; the stricter lint of this version passes without errors, with Bluetooth calls annotated where the connection screen has already requested the permission.
- Tests with Robolectric 4.17 and org.json 20260814. 62 tests.
- No visible change in the app.

## 1.0.1 / Maintenance — 2026-10-28

- Android lint without any warning: the chart and the illustration reuse drawing objects instead of creating them every frame; explicit backup rules keep cases and journal on the device only.
- CI with the current versions of the actions (end of Node 20 on GitHub) and also running on `develop`.
- Workflow with `develop`, feature/fix branches and releases documented in the README; Dependabot opens updates against `develop`.

## 1.0.0 / Professional — 2026-10-27

- Maintenance reminders: a daily notification when the most recent service with a reminder in the Journal is within 7 days or 500 km of being due (or overdue); once per reminder, without leaving the phone. On/off in Settings; Android 13+ asks for permission.
- Official Inmetro fuel economy (PBE Veicular 2019) for the 516 catalog versions; Today shows city/highway figures for the fuel of the last fill-up and whether the measured economy is below, within or above the range. Hand-written profiles and mixed fuel are not compared.
- Screen tests with Robolectric: real Activity, every main screen and a scenario with profile, Journal and reading; the captures were used to adjust the interface ("Explore demo" in Diagnosis, compact illustration, less repetition of "simulated" in Live).
- Code reorganized: listeners as lambdas, descriptive names for variables and methods, `switch` on enums, Android constants instead of magic numbers; Android lint without errors.
- Continuous integration on GitHub Actions; privacy notice (LGPD) in `docs/PRIVACY.md` and in Settings; release signed through `keystore.properties` kept out of Git. 62 tests.

## 0.8.0 / Data — 2026-10-27

- Vehicle catalog `br-pbev-2019-2` (sequence 3): 516 versions, 225 models and 34 makes of the 2019 model year, extracted from Inmetro's PBE Veicular 2019 table with source and page per attribute. Ambiguous PDF rows are rejected, not completed; the 16 curated Chevrolet versions keep their ids. Electric and hybrid vehicles are left out. The catalog limit grows from 512 KiB to 2 MiB. Version choice also shows the fuel.
- Trouble code catalog `generic-dtc-2` (`DtcReference`): 91 generic P0 codes with an original short text and a source per entry (Ford PCED 1999 and VW bulletin 24Y2/NHTSA), used on screen, in the lookup (grouped by system area), in the AI analysis and in a labeled appendix of the reports. Manufacturer codes and entries with errors in the source are left out.
- Saved cases keep `generic-dtc-1` and the frozen rules engine, because opening a case recomputes the analysis and compares it byte by byte.
- On-screen and TXT/PDF reports with local time (time zone stated once) and correct plurals ("1 code", "2 codes"); the saved document and exported JSON are unchanged.
- Reproducible catalog generation in `tools/` (rules, PDF hash and counts). 51 JVM tests, including the real catalog going through the app's validator and validation of saved cases after formatting.

## 0.7.1 / Report and demo — 2026-10-26

- Step 7 of the plan. PDF with title, highlighted sections with dividers, indented details, a section title never left alone at the end of a page and a "page X of Y" footer with the generation date. The text is the same as the TXT.
- Reports with simulated data carry the "DADOS SIMULADOS · nenhum veículo real foi consultado" band on every page.
- Layout separated from drawing (`ReportLayout`), with JVM tests for styles, margins, pagination and band.
- Presentation script in `docs/academic/DEMONSTRACAO.md` (preparation, screen order, talking points, plan B and likely questions) and README reorganized as a project overview, architecture, plan status, evidence and limitations. 40 JVM tests. PDF not rendered on a device.

## 0.7.0 / Demo AI — 2026-10-26

- Step 6 of the plan: optional AI analysis in investigations, for demos. The app sends the intermediate server (`server/`) only the anonymized case: vehicle type, codes, MIL, readings and symptoms; profile name, VIN, notes, adapter, identifiers and times never leave the phone. Content preview and confirmation on every request.
- Node server with the official Anthropic SDK keeps the key, requires a token, limits size and daily count, uses Claude Opus 5.5 with low effort, structured JSON output and fallback on refusals.
- Contract `borbacar-ai-request-1`: each fact has a synthetic reference; server and app reject replies outside the schema or citing evidence that was not sent. The result is labeled as AI-generated and not saved in the case.
- Address and token in Settings › AI analysis. Plain HTTP only in the debug build; the release requires HTTPS.
- Free alternative provider for testing: `BORBACAR_PROVIDER=gemini` uses the Gemini API (Google AI Studio free tier, default model `gemini-3.5-flash-lite`) with the same contract and checks. Real end-to-end test on 10/26: a demo case analysis validated in 3.5 s; `gemini-3.8-flash` was overloaded (HTTP 503) at the same time. On the free tier Google may use the submitted content.
- Without AI configured nothing changes: rules, catalog and reports keep working offline. Tests: 36 JVM cases, including anonymization and rejection of invented evidence. No real API call in this release.

## 0.6.9 / Clarity — 2026-10-26

- Build migrated to Gradle (AGP 8.7, wrapper included).
- Reading verdict: "No trouble codes", "N codes found" or "Incomplete reading", with date, MIL and next step. The OBD2 coverage caveat is stated once, in the verdict.
- Diagnosis: "Read trouble codes only" visible next to the guided reading; "Investigate this reading" in the verdict. Per-service results, technical summary and supplement grouped in "Technical details of the reading".
- Today shows the verdict of the last saved reading and a shortcut to History with correct singular/plural counts. Readings older than 0.3.1 keep their text line.
- On-screen dates in local time ("today at 14:32"). TXT/PDF/JSON/CSV reports keep their formats.
- Confirmations (PDF/CSV saved, investigation exported, recording finished) in a non-blocking notice; readiness, VIN and freeze frame results with the query name in the title.
- Today summarizes the Journal: economy of the last full-to-full interval and the next service of the most recent reminder (date with days left/overdue and/or odometer), with a shortcut to the Journal.
- Code cards show the system area read from the SAE J2012 structure (e.g. "Engine and transmission · generic code · ignition or misfire"), also for codes outside the catalog. No meaning, cause or part is assumed.
- History uses the same verdict title per reading; old readings keep their text summary. An empty history leads to Diagnosis.
- System Wi-Fi/Bluetooth failures (timeout, connection refused, socket closed, permission) explained in Portuguese; the app's own messages are unchanged.
- Journal amounts formatted in reais ("R$ 1.234,56") and singular/plural counts on screen.
- Fixed four defects found by the new tests: speed (0D), load (04), throttle position (11) and manifold pressure (0B) were computed as a temperature (A−40); a freeze frame reply for another frame was not ignored; and the vehicle catalog reader rejected every list with items and every attribute with a valid source, preventing the br-my19-1 catalog from loading.
- Reproducible JVM tests (`./gradlew testDebugUnitTest`, 31 cases): ELM session with a simulated adapter, OBD parser, ObdReader (poll and supplement), real bundled catalog, round trips of reading/supplement/journal, full-to-full economy, DTC structure and formatting.
- The debug build installs next to the published app (`io.rea.obd.teste`). No physical test.

## 0.6.8 / Focus — 2026-10-26

- Fewer repeated shortcuts and one main purpose per tab.
- History with separate collections; edit/remove in the vehicle details; demo in Settings.
- Secondary actions folded, navigation/expand arrows and a single action for a new Journal entry.
- Factual catalog br-my19-1: 16 Chevrolet Cobalt, Onix and Prisma MY19 configurations, with Inmetro per attribute.
- Choosing a version fills engine/transmission/fuel together; partial entry remains available without a mosaic of unknown fields.
- The previous valid catalog is kept and old profiles are not enriched automatically. Same package and signature.
- See docs/historico/FOCO-0.6.8.md and docs/historico/CATALOGO-MY19-0.6.8.md. No physical test.

## 0.6.7 / Journal — 2026-10-24

- Offline journal per vehicle: symptoms, services and fill-ups, dates/times, declared amounts and odometer.
- Transactional revisions, correction, voiding/reactivation, filters, pagination and preservation after removing a profile.
- Economy per confirmed full-to-full interval, including partial fill-ups and blocking inconsistencies; missing costs stay unknown.
- Drafts restored and saving without duplicates after recreation; fields locked while saving.
- JSON export with a prior scope review, every revision of the selected entries and a destination chosen by the user.
- Separate database, no migration of existing histories. Same package and certificate. See docs/historico/DIARIO-0.6.7.md.

## 0.6.6 / R1c and R2a — 2026-10-24

- Own visual identity, generic vehicle highlight, selected navigation, themed dialogs and action hierarchy.
- Navigation and press motion; reduced-motion preference that respects the Android setting. No animation of measured values.
- Connection entry with phone identification, scanning and Wi-Fi setup without assumed values. BLE scan lifecycle protected.
- Guided reading with progress per group, stop on failure and supplement kept in the investigation seed.
- Comparison of two structured readings, per service, without confusing missing data with a resolved defect.
- Historical formats, package, signature and transports preserved. See docs/historico/EXPERIENCIA-0.6.6.md. No physical validation.

## 0.6.5 / R1b — 2026-10-23

- Automatic identification of the phone/Android and display of the adapter's available replies; demo and unconfirmed identification stay explicit.
- System/Light/Dark themes, palette per context, forms/dialogs/charts and system bars. Local preference; manual change blocked during acquisition.
- Own vector icons and focusable navigation; button states and colors with tested contrast.
- Details through the sensor title; origin/time/quality information preserved.
- Eight more readings per tap in History; the displayed count is restored without changing records.
- Package, certificate, transport, formulas, profiles and cases preserved. No physical validation. See docs/historico/VALIDACAO-R1B.md.

## 0.6.4 / R1a — 2026-10-21

- Today as the opening screen, with profile and existing real records; no health score.
- Today/Diagnosis/History/Garage navigation, Live inside Diagnosis and Settings in the header.
- Histories and investigations moved to their own destination, preserving queries, exports and removed profiles.
- Legacy routes restored; Back respects the hierarchy. CSV stays paused outside Live. Old buttons grow with their text.
- No change to transports, formulas, formats or catalog. Partial R1: light theme and later refinements still pending. Detailed validation in docs/historico/VALIDACAO-R1A.md.

## 0.6.3 / checkpoint 4.4 — 2026-10-21

- Focus dashboard extracted into DashboardState/DashboardScreen: states, origin and individual time; missing data without a fake zero, capture details and partial coverage.
- Chart with up to 90 received captures, gaps, selection by touch/keyboard/accessibility actions, UTC instant and explicit historical context.
- Acquisition session distinct from the recording ID; CSV format preserved. Writing blocked outside the dashboard/foreground, including a late callback during pause.
- ATRV separated from module voltage, without inventing a UTC time or battery health. Fixed demo/recording notices and two-line navigation with enlarged font.
- 1,104 Java checks, 102 automated Android tests, ten pipeline checks and two separate resource tests. No physical validation.
- Same package/certificate; version code 10. Persistence, diagnostics, catalog, transports and formulas preserved. Next step: 4.5 Faults/investigations.

## 0.6.2 / checkpoint 4.3 — 2026-10-20

- Focus garage extracted into its own Java components, with search, identification per attribute, coverage/sources and changes blocked during acquisition/demo.
- Three-step editor, "I don't know", optional declaration, explicit legacy confirmation, final review and a draft restored when the Activity is recreated, including manual text still open. Cancel does not save.
- The chained selector uses real configurations from the supplied base; dependent choices are cleared when an ancestor changes. Validation rejects forged sources, mixed configurations and stale revisions. The commercial base stays empty.
- New UI cases in schema 4, with a full profile snapshot and source metadata; a revision cannot change these facts. Historical format vehicle-profile-1 separated from the UI. Documents 1/2/3 and existing rules preserved; physical SQLite stays at 1.
- JSON/TXT and PDF lines use the captured metadata. No physical reading or PDF validation on a phone is claimed.
- 1,026 Java checks, 87 simulated Android tests (79 SDK 35, eight SDK 26), ten pipeline checks and two separate resource tests passed. Six native captures inspected, including 320px/enlarged font.
- Same package/certificate, version code 9. Next checkpoint: Focus dashboard. No new transport, AI, coding, tuning or proprietary Carista data.

## 0.6.1 / checkpoint 4.2 — 2026-10-20

- Current `VehicleRecord` profile with provenance per attribute, revision and separate manufacturing/model years. Historical `VehicleProfile` projection kept unchanged.
- Non-destructive migration to `vehicle_store_v2`, preserving IDs/selection and old copies. New installs without an assumed Cobalt; a partial profile works in the existing form.
- Editing declares only changed fields and keeps the other metadata. Stale revisions and invalid types/fields are rejected.
- Versioned local catalog core with validation of sources/scope/declared permission, filtering by existing configurations, checksum, verified writing and an archive of the previous version. Bundled base empty; no catalog selection in the UI and no third-party data added.
- Evidence contract per function separates implemented, documented, announced, observed and physically validated; no new compatibility claims.
- 993 Java checks, 60 Android tests (54 SDK 35, six SDK 26), ten pipeline checks and two separate resource tests passed. Same package/certificate, versionCode 8.
- New Focus UI, extended case/export snapshot and physical validation remain pending. Cases 1/2/3 and 0.6.0/4.1 artifacts preserved.

## Checkpoint 4.1 — redesign base, no new app version

- Focus visual direction chosen; native implementation left for the next steps.
- The build requires the original development certificate, does not create a replacement key and blocks overwriting APK/ZIP files.
- Resources and the generated R take part in the build and tests, with explicit asset support and validation against a mismatched resource APK.
- Development outputs and test classes isolated per run. Robolectric temporary files use a short path for SQLite on Windows.
- Original schema 3 investigation fixture preserved from the 0.6.0 ZIP, next to fixtures 1/2; coexistence regression and extended review.
- The resource fixture and continuity checks stay outside the application code. Checkpoint results are documented separately from the historical 0.6.0 validation.

## 0.6.0 — 2026-10-20

- Catalog `generic-dtc-1` with five generic meanings, original factual summaries and located references. Causes, procedures and limits of other vehicles are not imported.
- Lookup by code on the fault cards and offline search in More. An unknown code stays visible without an assumed definition.
- `RuleDiagnosticEngine` produces a `DiagnosticResult` with a typed checklist and references to the observations. It receives no transport, runs no commands and produces no part hypothesis or probability.
- New documents use schema 3, `investigation-1` and a versioned catalog. Documents 1/2 keep the old rules, including in revisions. Physical SQLite stays at 1.
- `definition_sources` separates meaning sources from cause sources, still empty. JSON/TXT reports keep the same definitions/evidence as the saved document.
- Checklist/report bound to the analyzed snapshot, in addition to IDs/revision; a result from another input with the same IDs is rejected.
- Validation: 957 Java checks and 37 simulated Android tests (33 SDK 35, four SDK 26). No physical test, AI model, independent mechanical evaluation or native PDF validation.

## 0.5.0 — 2026-10-19

- D02 supplemented with typed capture of readiness 0101 and five freeze frame 00 PIDs. States, times, multiple replies and missing data preserved per query; no live value replaces the frame.
- Explicit supplementary collection in the Faults tab, limited to the DTC reading context. New cases freeze the supplement; a new reading clears the previous one. A lost transport keeps the partial result and stops the rest.
- New documents use schema 2 / `coverage-2`. Original 0.4.0 documents stay in schema 1 / `coverage-1`, even after a symptom revision. The physical SQLite schema stays at 1; old facts are not migrated by assumption.
- The report separates query times from an unknown freeze instant, keeps an unknown DTC/ECU association and points out divergent MIL indications without inventing a cause.
- Registry `protocol-links-1` and per-PID references start K01/K02. Metadata/links only; a cause catalog, applicable mechanical sources and AI remain pending.
- Validation run: 800 Java checks and 32 simulated Android tests (29 SDK 35, three SDK 26). Hardware, install/update and native PDF on a phone still untested.

## 0.4.0 — 2026-10-19

- Grouped delivery: cases with symptoms, historical snapshots, SQLite persistence, revisions and offline resumption.
- `DiagnosticCase` rejects mixing vehicle/session/origin, keeps a valid zero and evaluates age against the original capture, without renewing measurements when opening the case.
- Each symptom revision preserves the facts. Writes are transactional; stale concurrent edits and measurement changes are rejected. Corruption/future schemas are not overwritten.
- The Faults tab includes creation, list, details, revisions, exports and confirmed deletion per case. The reading history links investigations by capture ID. Removed profiles keep the snapshot accessible.
- `CaseReview` organizes observations, gaps, checks and limitations with `coverage-1` rules. It does not integrate a model, cause catalog or mechanical hypotheses. Symptoms remain user statements.
- JSON and TXT contain the same data/revision; the export picker and copy are exercised in simulation. PDF stays native and was not validated in this release: Robolectric does not provide the required `PdfDocument` implementation.
- Fixed paragraph breaking with several lines in the PDF generator and clearing the current reading when selecting/removing a profile.
- Verification run: 677 Java checks and 26 simulated Android tests (24 SDK 35, two SDK 26). Readiness and freeze frame are not yet attached to cases in structured form.

## 0.3.1 — 2026-10-19

- D01 delivered: `DtcReadResult` and `DtcObservation` replace texts as the source of diagnostic data.
- MIL and DTC categories have their own results: positive, NO DATA, negative, invalid, timeout, connection closed, not run or partial.
- Negative replies keep the service and code without inventing an interpretation. Remaining queries stop after losing the transport; earlier facts stay on screen and in history.
- Times, capture, vehicle/session and origin are persisted in `dtc_read`, schema 1. Previous fields are kept for compatibility; old reports gain no assumed facts.
- `DataOrigin` is now shared between samples and captures. The demo uses the same data structure and stays flagged as a simulation.
- Coverage run: 633 Java checks and 17 simulated Android tests. No change command sent in the diagnostic scenarios.

## 0.3.0 — 2026-10-19

- Own base moved to the BorbaLab 2 folder with the display name BorbaCar Labs; Android package and development certificate preserved.
- The connection state now comes from the ELM session. Query errors end the visual connection and the recording.
- The RPM chart receives only new, recent samples, with their acquisition time.
- Read-only use cases in `ObdReader`; the clear codes action separated into `EmissionCodeReset`.
- Typed samples with origin, quality and times; CSV records vehicle, adapter, session and states without turning missing values into zero.
- Garage with an active profile and editable fields; history per vehicle, migration and old unassociated records preserved.
- Centralized version and Android tests included in the repository. Theme and insets adjusted for the minimum Android version.
- Coverage run: 517 Java checks and 13 simulated Android tests. Hardware and AI integration remain pending.

## 0.2.0 — Imported base

Independent Android reader with SPP/BLE/TCP, PIDs, DTCs, readiness, VIN/calibration, freeze frame, chart and exports. The audit and evolution plan are preserved in `docs/historico/` as historical records.

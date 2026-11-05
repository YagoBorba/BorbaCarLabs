# Privacy — BorbaCar Labs

Privacy notice for the app, written with Brazil's General Data Protection Law (LGPD, Law No. 13,709/2018) in mind.
This is an academic project; this text describes how the app works and is not legal advice.

## What stays on your phone

Everything the app stores lives in its internal storage, with no account, no cloud and no automatic upload:

- Vehicle profiles (the name you gave it, make, model, year, version and notes).
- OBD readings, investigations, the symptoms you described and the history.
- Journal: fill-ups, services, amounts, odometer and reminders.
- Preferences (theme, motion, reminders, language, AI provider and model).
- Your AI API keys, encrypted with a key held in the Android Keystore. They are excluded from Android backups and from the app's own backup file.

The connection to the OBD adapter is local (Bluetooth or the adapter's own Wi-Fi). Maintenance reminders are computed on the device; the notification never leaves the phone.

## What can leave your phone

Only when **you** tap **Talk to AI** in an investigation and confirm the preview shown. The app then sends, over HTTPS and straight to the provider you chose in Settings (Google Gemini or Anthropic Claude), using your API key:

- Vehicle type: make, model, year, version, engine, fuel, transmission and market.
- Observed trouble codes, check engine light state and the case readings.
- The current question and up to three summaries of earlier exchanges in the same case revision. The local conversation keeps up to eight exchanges per revision, separate from the report.
- The symptoms you wrote in the investigation (up to 500 characters per field). Before sending, the app removes phone numbers, e-mails, CPF, CNPJ and license plates found in that text. Names, addresses and other personal data written freely are **not** detected; the preview shows the exact text that will be sent.

**Not sent:** profile name, VIN, notes, adapter name or address, internal identifiers and times. The reply is saved as a local conversation, separate from the case and the reports. You can clear the conversation on the assistant screen.

The provider's own terms and retention policies apply to what you send, under your account. On the **Gemini free tier**, Google may use the content it receives to improve its products. That is why the app sends only the fields above, without identifiers, but it cannot guarantee full anonymization of free-text symptoms. Usage may be billed to your provider account.

When you choose **Use my AI**, the reviewed data is shared through the Android share sheet. You pick the destination app and that service's policies apply. The app does not access your chat account and does not import the reply.

## Exports

Reports (JSON, TXT, PDF, CSV) only leave the app when you choose where to save them or whom to share them with. They may contain the profile name and other data you recorded.

The full backup contains the garage, readings, cases and journal revisions. It excludes CSV, credentials (including API keys), settings and AI conversations. It is not encrypted: choose a private destination. Restoring asks for confirmation, keeps a previous local copy for undo and recovers interrupted restores before opening the data.

## Your controls

- Delete a vehicle's history, investigations or Journal entries from within the app.
- Turn AI analysis off by removing the key in **Settings › AI analysis**; you can also revoke the key at the provider.
- Turn reminders off in Settings.
- Remove all data: Android Settings › Apps › BorbaCar Labs › Storage › Clear data.

## Legal basis and purpose

Data is processed for the purpose you ask for (diagnosis, history and follow-up of your vehicle), on your initiative. Sending data for AI analysis requires your confirmation every time.

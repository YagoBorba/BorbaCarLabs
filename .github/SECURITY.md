# Security Policy

## Reporting a Vulnerability

Please do not open public issues for security vulnerabilities. Report them privately:

- Email: yagoaborba@hotmail.com
- GitHub Security Advisory (if available)

Provide as much detail as possible, including:
- Affected versions and device/Android version
- Steps to reproduce the vulnerability
- Potential impact
- Any known mitigations

## Supported Versions

The latest release on `main` and the development branch `develop` receive fixes.

## Security Practices

- Never commit secrets, API keys, keystores or `keystore.properties`.
- AI API keys belong to each user. The app encrypts them with an AES-GCM key held in the Android Keystore, keeps them out of Android backups and of the app's backup file, and sends them only in a request header to the provider the user chose (Gemini or Claude). They never appear in URLs, logs or error messages.
- Diagnostic cases, history and journal stay on the device; Android cloud backup and device transfer are disabled. The app's own backup file is created only when the user asks and saved where the user chooses.
- Requests sent to the AI provider carry only minimal data; phone numbers, e-mails, CPF/CNPJ and license plates are removed from symptoms (names and addresses are not detected), the user sees the exact text before sending, and the response is validated before it is shown.
- Clearing trouble codes is a separate, confirmed action and is never triggered by the rules engine or the AI.
- Keep dependencies updated through the Dependabot pull requests on `develop`.

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

The project is finished and 2.0.0 is its final release. Reports are still welcome by email.

## Security Practices

- Never commit secrets, API keys, keystores or `keystore.properties`.
- Diagnostic cases, history and journal stay on the device; Android cloud backup and device transfer are disabled. The app's own backup file is created only when the user asks and saved where the user chooses.
- Clearing trouble codes is a separate, confirmed action and is never triggered by the rules engine.

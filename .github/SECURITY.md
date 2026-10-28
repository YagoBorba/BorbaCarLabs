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
- AI API keys live only in the environment of the machine running `server/`; the app never stores them.
- Diagnostic cases, history and journal stay on the device; backups and device transfers are disabled.
- Requests sent to the AI server are anonymized and the response is validated before it is shown.
- Clearing trouble codes is a separate, confirmed action and is never triggered by the rules engine or the AI.
- Keep dependencies updated through the Dependabot pull requests on `develop`.

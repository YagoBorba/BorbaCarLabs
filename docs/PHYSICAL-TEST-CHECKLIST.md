# Physical test checklist

The author tested the app on a real car with an ELM327 adapter during development. Those sessions were not recorded with this protocol, so the table below stays as the protocol for anyone who wants to repeat the test and keep the evidence.

Do not fill the Result column with inferences from the simulated tests. Record the date, version/commit, phone/Android version, adapter and transport, vehicle/engine and the person responsible. Run the reading operations with the vehicle parked, in a suitable condition and with competent supervision. This protocol never asks to clear trouble codes.

| Scenario | Expected evidence | Result |
|---|---|---|
| Install the debug APK | Package and version; the release app is preserved | Pending |
| Permission denied, then granted | Recovers without freezing | Pending |
| Connect to the adapter | Transport, replies and timings recorded | Pending |
| Guided reading | State per service; missing data distinct from a fault | Pending |
| Connection lost and restored | Old data identified; new session without mixing | Pending |
| Rotation, pause and return | Consistent state and identified recording | Pending |
| PDF in the phone's viewer | Pagination, accents, simulation band | Pending |
| Backup through the Android picker | File saved to a private destination | Pending |
| Restore on a test install | Same profiles, readings, cases and journal revisions | Pending |
| Cancel the picker and choose an invalid file | Data preserved | Pending |

Do not publish VINs, license plates, API keys or personal data in the project logs. Keep only the evidence that is needed and reviewed.

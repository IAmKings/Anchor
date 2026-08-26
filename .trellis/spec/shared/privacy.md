# Privacy and Observability

> There is no application logger and no backend. "Observability" here means what must **never** leave the device.

ADR-0001 (local only) and ADR-0003 (zero collection / anti-KPI) are accepted. `scripts/verify_privacy.py` is the mechanical gate.

---

## Do not add a logger

The Kotlin sources do not use Timber, Napier, Kermit, `android.util.Log`, or `println` for product events.

Do **not** introduce:

- Crash reporters (Sentry, Firebase Crashlytics, Bugsnag)
- Analytics (Firebase Analytics, Amplitude, Mixpanel, AppsFlyer)
- HTTP stacks (OkHttp, Ktor, Retrofit, Volley)
- `INTERNET` / `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE`

`make verify` runs `scripts/verify_privacy.py`, which scans Gradle files, manifests, and Kotlin imports for those coordinates.

If you need a debug breadcrumb while developing, keep it out of committed `commonMain` / `androidMain` product paths. Never log worry text, camera-log facts, PHQ answers, export passwords, or SQLCipher keys.

---

## What may touch the filesystem

| Surface | Rule |
|---------|------|
| SQLCipher DB | Keystore-wrapped random key; not a user password |
| Private M4A worry audio | Local `voice-notes/`; export JSON has `hasAudio` + basename only; bytes live inside the encrypted `.anchor` zip under `audio/` |
| Encrypted `.anchor` export | User-chosen password, **not** stored; PBKDF2 + AES-GCM; share via read-only FileProvider URI |
| Notifications | Lock-screen text is the app name `锚点` only — no body |
| App switcher | `FLAG_SECURE` while app lock is enabled |
| Speech | On-device only; if the recognizer would go online, fall back to private recording and stop on background |

`MainActivity` owns these host adapters. Shared Compose must not start network or upload.

---

## Notifications and timers

AlarmManager / exact alarms may be delayed on ColorOS. When the OS drops a notification, write an **in-app banner** (`inAppBannerText`). Do not "fix" reliability by posting the worry content to a visible lock-screen notification.

Wave waiting and micro-action timers use **separate** `PendingIntent` request codes and notification IDs (`AndroidBackgroundTimer` + `BackgroundTimerKind`). Do not share one alarm slot.

---

## Insights are local

`LocalInsights` computes wake stability and prediction bias on device. Copy uses `"记录还不够"` when N is too small. Do not add completion rate, streak, or "达标" tiles. Do not send the snapshot anywhere.

---

## Tests / verification

```bash
make verify          # PRD structure + verify_privacy.py
```

When adding a Gradle dependency, re-read `FORBIDDEN_COORDINATES` in `scripts/verify_privacy.py`. If a library needs network, the change is out of scope until ADR-0001 is explicitly revisited.

---

## Wrong vs correct

#### Wrong
`Log.d("Worry", content)` or an OkHttp ping to "just check hotline numbers".

#### Correct
Keep the string in SQLCipher. Hotlines are constants reviewed before release (PRD §10.4).

#### Wrong
Export JSON with `audioPath: "/data/.../voice.m4a"`.

#### Correct
`"hasAudio": true`, `"audioName": "voice-1.m4a"`, and put bytes in encrypted zip `audio/voice-1.m4a`.

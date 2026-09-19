# SecureVault (`pww_manager`)

A native Android credential (password) manager, built around a search-first workflow: look up a service by name/domain, see a masked entry, and reveal the real password only on demand. `SecureVault` is a placeholder app name (see [Open items](#open-items)).

The full product/architecture spec this app was built from lives in [secure-credential-manager-spec.md](secure-credential-manager-spec.md) — read that first for anything not covered here (Korean, with English glossing on technical terms).

## Security model

Two independent layers, by design:

1. **App access control** — Biometric or PIN unlocks the app itself.
2. **Credential masking** — search results always show a masked ID/password; only opening a specific entry decrypts and displays the real value, and any background/exit/timeout event re-masks it immediately.

Encryption is AES-256-GCM, with the data-encryption key protected by two independent wraps:

- **Local Wrapped Key** — wrapped by an Android Keystore key, used for fast everyday unlock (Keystore, non-exportable).
- **Recovery Wrapped Key** — wrapped by a key derived from the user's PIN via PBKDF2 (Argon2id was the original preference; PBKDF2 was used for library-availability reasons), used to restore access on a new device after backup/restore, independent of the old device's Keystore.

Search never touches plaintext: domain/service lookups are matched via HMAC token indexes (`Search HMAC Key`, separate from the encryption key), and username autocomplete uses a third, separate HMAC key. Password-reuse checking is done by decrypting and comparing at write time (not via an index), to avoid a low-entropy dictionary-attack surface. Full rationale for every one of these decisions is in the spec (§5–§8).

## Tech stack

| Area | Choice |
| --- | --- |
| Language / platform | Kotlin, native Android, `minSdk 26` / `targetSdk 37` |
| UI | Jetpack Compose, Material 3 |
| Architecture | Clean Architecture + MVVM (Compose → ViewModel → UseCase → Repository) |
| DI | Hilt |
| Navigation | Compose Navigation |
| Local storage | Room (encrypted payloads only — no plaintext credential fields) |
| Settings storage | DataStore |
| Backup | Android Auto Backup (Cloud Backup + Device-to-Device) |

## Project structure

```
com.example.securecredential
├── app/           Application class, Hilt DI modules
├── presentation/  Compose screens + ViewModels (authentication, home, search, credential, category, settings, backup, common)
├── domain/        Credential/SearchQuery models, repository interface, use cases
├── data/          Room (entity/dao/database), repository impl, security (Crypto/Key managers), preferences
└── core/          crypto primitives, domain normalization, lifecycle, shared utils
```

See §3 of the spec for the intended structure and §2 for the invariants the codebase is expected to hold (UI never touches DAOs/crypto directly, credentials are never stored in plaintext, etc.).

## Building and running

```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat test              # unit tests
gradlew.bat connectedAndroidTest  # instrumented tests, needs a running device/emulator
```

Requires JDK 17. The Gradle/AGP/Compose/Hilt versions are pinned in `gradle/libs.versions.toml`, each with a comment explaining why that exact version was required — check those comments before bumping anything.

## Status

Milestones A–E from the implementation plan are complete and verified (unit + instrumented tests green, `assembleDebug` clean): crypto core, Room/repository layer, authentication/session screens, home/search/credential-detail UX, settings/backup UX. Milestone F (the spec's §10 `AC-BACKUP-01`–`07` two-device restore acceptance test) has been deliberately deferred, not attempted.

Since then the app has had an interactive usability pass on-device (Korean-default UI, live search autocomplete, add/view credential round-trip, navigation) with fixes for stale list refresh and missing back buttons. Not yet exercised interactively: credential edit/delete, a real PIN-recovery restore, PIN change, and the biometric toggle in Settings.

## Open items

Carried over from spec §15 and from testing so far:

- **App name**: `SecureVault` is a placeholder pending a final brand name.
- **PIN KDF parameters**: PBKDF2 iteration count (or a switch to Argon2id) needs a final value after device-performance testing.
- **Recovery backoff schedule**: the exponential backoff after repeated PIN-recovery failures is a first draft, open to UX tuning.
- **Partial search (P1)**: domain/service partial matching and partial username autocomplete are intentionally out of MVP scope — HMAC exact-match indexes can't support `LIKE` queries without a separate, security-reviewed design (e.g. n-gram tokens).
- **Autofill exposure**: credential entry fields are not yet marked `importantForAutofill="no"`, so Android's system Autofill (e.g. a signed-in Google account) can suggest saved passwords into this app's own forms — worth closing off for a security-focused app.

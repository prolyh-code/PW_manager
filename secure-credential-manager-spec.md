# Secure Credential Manager — Android 앱 제작 지시서 (Software Design Specification)

> 이 문서는 Claude Code에 그대로 전달하여 구현을 시작할 수 있도록 작성된 개발 명세서입니다.
> 모든 아키텍처(Architecture) 결정 사항은 확정 상태이며, "미해결(Open)"로 표시된 항목만 구현 중 추가 논의가 필요합니다.

---

## 0. 앱 개요

개인용 자격증명(資格證明, Credential) 관리 Android 앱입니다. 서비스명/도메인으로 빠르게 검색해 ID/Password를 확인하는 것이 핵심 사용 흐름이며, 다음 2단계 보안(保安) 모델을 갖습니다.

1. **1차 보안**: 앱 자체 접근 제어 — Biometric(생체 인증) 또는 PIN(Personal Identification Number)
2. **2차 보안**: 사용자가 직접 정의한 ID/Password Mask — 검색 결과에서는 항상 마스킹된 형태만 노출

---

## 1. 확정 기술 스택

| 영역 | 결정 |
| --- | --- |
| 언어/플랫폼 | Kotlin, Native Android |
| UI | Jetpack Compose |
| Architecture | Clean Architecture + MVVM(Model-View-ViewModel) |
| DI(Dependency Injection) | Hilt |
| Navigation | Compose Navigation |
| 로컬 구조화 저장소 | Room |
| 설정 저장소 | DataStore (Auto Lock, Theme 등 비민감 설정) |
| 암호화 | AES(Advanced Encryption Standard)-256-GCM(Galois/Counter Mode) |
| Key 보호(로컬) | Android Keystore |
| Key 보호(복구용) | PIN 기반 KDF(Key Derivation Function) — 상세는 5장 |
| 검색 | HMAC(Hash-based Message Authentication Code) 기반 Exact Search (MVP), Partial Search는 P1 |
| minSdkVersion | 26 (Android 8.0) |
| Backup | Android Auto Backup — Cloud Backup + Device-to-Device(D2D) 모두 지원 |

---

## 2. 전체 Architecture

```text
Compose UI
   │
   ▼
ViewModel  (UI State 관리)
   │
   ▼
UseCase    (단일 책임의 비즈니스 로직 단위)
   │
   ▼
Repository (Domain ↔ Data 경계)
   │
   ├──────────────┬──────────────┐
   ▼              ▼              ▼
Room DAO   SecurityManager   DataStore
   │              │
   ▼              ▼
Encrypted DB   CryptoManager / KeyManager
                    │
                    ▼
              Android Keystore
```

### **불변 원칙 (Invariant) — 구현 전체에서 반드시 지킬 것**

1. UI는 암호화 방식이나 DB 구조를 알지 못한다. (Compose → ViewModel → UseCase → Repository 순서를 반드시 지킬 것. UI에서 DAO/CryptoManager 직접 호출 금지)
2. Credential은 항상 암호화(暗號化) 상태로 저장된다. (Room에는 평문 Credential 필드가 존재하지 않음)
3. Search Key ≠ Credential Encryption Key. (검색용 HMAC Key와 데이터 암호화 Key는 별도로 관리)
4. App 인증 세션(Session) 상태와 Credential Display 상태(MASKED/DISPLAYED)는 별개로 관리한다.
5. Backup(데이터 이전)과 Key Recovery(암호화 키 복구)는 별개의 문제로 설계한다. DB만 옮겨져서는 안 되고, 반드시 Key도 함께 복구되어야 한다.
6. 로그에는 Password, PIN, 암호화 Key, Biometric 결과, Mask 규칙, 실제 Credential 값을 절대 남기지 않는다.

---

## 3. 패키지 구조

```text
com.example.securecredential
│
├── app
│   ├── SecureCredentialApp.kt
│   └── di/                      (Hilt Module)
│
├── presentation
│   ├── navigation
│   ├── authentication
│   ├── home
│   ├── search
│   ├── credential
│   ├── settings
│   ├── backup
│   └── common
│
├── domain
│   ├── model
│   ├── repository            (Interface만)
│   └── usecase
│
├── data
│   ├── local
│   │   ├── database          (Room Database)
│   │   ├── entity
│   │   └── dao
│   ├── repository            (Interface 구현체)
│   ├── security               (CryptoManager, KeyManager 구현체)
│   └── preferences            (DataStore)
│
└── core
    ├── crypto
    ├── normalization
    ├── error
    ├── lifecycle
    └── util
```

---

## 4. Domain Model

```kotlin
data class Credential(
    val credentialId: String,
    val serviceName: String,
    val url: String?,
    val domain: String?,               // Normalize된 host. 검색용
    val username: String,
    val password: String,
    val usernameMask: String?,         // 사용자가 직접 작성 (앱이 생성하지 않음)
    val passwordMask: String?,         // 사용자가 직접 작성
    val category: String?,
    val memo: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CredentialSummary(          // 검색 결과 전용 — 평문 password 없음
    val credentialId: String,
    val serviceName: String,
    val domain: String?,
    val usernameMask: String?,
    val passwordMask: String?
)
```

**설계 원칙**: 검색 결과 목록에서는 `CredentialSummary`만 사용하고, 사용자가 특정 항목을 선택했을 때만 `Credential` 전체를 복호화한다. (불필요한 복호화 최소화)

---

## 5. 보안 아키텍처 (핵심)

### 5.1 Key 계층 구조

| Key | 역할 | 저장 위치 |
| --- | --- | --- |
| Device Master Key | 기기별 Key 보호 (Root of Trust) | Android Keystore (비추출) |
| App Encryption Key | Credential 암복호화의 실제 Key | 암호화된 상태로 앱 데이터에 저장 (이중 Wrapping, 아래 참고) |
| Search HMAC Key | 검색 토큰 생성 전용 | 암호화된 상태로 앱 데이터에 저장 |
| PIN | 사용자 인증 + Recovery Key 유도용 | 저장하지 않음 (검증값만 저장) |

App Encryption Key는 **두 가지 방식으로 이중 Wrapping**한다.

```text
                    App Encryption Key
                    ┌──────┴──────┐
                    ▼             ▼
         Local Wrapped Key   Recovery Wrapped Key
                    │             │
         Device Master Key    Recovery KEK
         (Android Keystore)   (PIN 기반 KDF로 유도)
```

- **Local Wrapped Key**: 평상시 앱 잠금 해제(Biometric/PIN 인증) 후 빠르게 Key를 사용하기 위한 경로. Android Keystore Root of Trust에 의존.
- **Recovery Wrapped Key**: Backup에 포함되어 새 기기로 이전되는 경로. Android Keystore와 무관하게 **PIN만으로 복구 가능**해야 하므로 별도로 둔다.

> **왜 이중 구조가 필요한가**: Android Keystore에 생성된 키는 비추출(non-exportable) 구조로 설계되어 있고, Auto Backup이 기본적으로 켜져 있는 구조와 맞물려 이 키로 암호화한 데이터를 백업했다가 복원 후 사용하지 못하는 문제가 실무에서 반복적으로 보고된다. 실제로 복원이 일어나면 Master Key는 새로 생성될 뿐 기존 암호화 키 자체는 백업 대상에 포함되지 않는다는 점이 관련 문서에서도 확인된다. 이 때문에 Keystore 경로와 독립적인 PIN 기반 Recovery 경로를 반드시 별도로 두어야 한다.

### 5.2 PIN 정책 (강화 — Recovery 겸용)

PIN은 이제 두 가지 역할을 겸합니다: ① 앱 잠금 해제, ② Recovery Key 유도. 따라서 기존 앱들의 일반적인 4자리 PIN보다 강화가 필요합니다.

- **최소 요구사항**: 숫자 8자리 이상, 또는 영숫자 혼용 6자 이상 (Settings 화면에서 선택 가능하게)
- **PIN → Recovery KEK 유도**: `Argon2id(PIN, Salt, params)` 사용을 우선 권장. 라이브러리 제약으로 어려운 경우 `PBKDF2WithHmacSHA256` + 반복 횟수 ≥ 310,000(2023년 OWASP 권장 하한 기준, 기기 성능에 따라 조정)
- **Salt**: Credential별이 아닌 앱 전체 1개, 최초 설정 시 랜덤 생성 후 앱 데이터에 저장(Backup 대상에 포함되어야 새 기기에서도 동일 Salt로 KDF 재계산 가능)
- **로컬 앱 잠금 해제와 Recovery Unwrap은 별도 코드 경로**: 로컬 잠금 해제는 Android Keystore의 사용자 인증 조건(BiometricPrompt/PIN 검증)에 의존하여 기기 자체의 실패 횟수 제한을 활용하고, Recovery Unwrap은 오프라인 공격에 노출될 수 있으므로 KDF 비용 자체로 방어한다.
- **Recovery 시도 제한**: 연속 실패 시 지수 백오프(예: 5회 실패 → 30초, 이후 배수 증가)를 앱 레벨에서 구현

### 5.3 Biometric 역할

BiometricPrompt + CryptoObject를 이용해 Keystore Key 사용 권한을 인증 결과와 직접 연결한다. Biometric은 항상 Local Wrapped Key 경로(빠른 잠금 해제)에만 관여하며, Recovery 경로에는 관여하지 않는다 (새 기기에는 기존 지문 정보가 없으므로 당연히 배제).

### 5.4 Key Lifecycle Sequence

- **최초 설치**

```text
App First Launch
   → Device Master Key 생성 (Keystore)
   → App Encryption Key 생성
   → Search HMAC Key 생성
   → Local Wrapped Key 생성 (Device Master Key로 Wrap)
   → PIN 등록 (강화된 정책)
   → Recovery Salt 생성 → Recovery KEK 유도 → Recovery Wrapped Key 생성
   → Biometric 등록 (선택)
   → READY
```

- **PIN 변경**

```text
Old PIN 인증 → New PIN 검증값 저장
   → App Encryption Key는 재암호화하지 않음
   → Recovery Wrapped Key만 New PIN 기반으로 재생성
```

- **Restore (새 기기)**

```text
New Device → App 설치 → Backup 데이터 복원
   (Encrypted Credential DB, Recovery Wrapped Key, Recovery Salt, KDF Params)
   → "복원된 데이터가 있습니다. PIN을 입력하세요" 화면 표시
   → 사용자 PIN 입력 → Recovery KEK 재계산
   → Recovery Wrapped Key Unwrap → App Encryption Key 복구
   → 샘플 Credential 1건으로 Decrypt Test 수행
   → 성공: 새 Device Master Key 생성 → Local Wrapped Key 재생성 → READY
   → 실패: KEY-005 (아래 12장 Error Code) 반환, 지수 백오프 후 재시도 허용
```

---

## 6. 데이터 계층

### 6.1 Room Entity

```kotlin
@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey val credentialId: String,
    val encryptedPayload: ByteArray,   // CredentialPayload 직렬화 후 AES-GCM 암호화
    val nonce: ByteArray,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "search_index", indices = [Index(value = ["tokenHash"])])
data class SearchIndexEntity(
    @PrimaryKey val indexId: String,
    val tokenHash: ByteArray,          // HMAC(SearchKey, "domain:"+value 또는 "service:"+value)
    val credentialId: String,
    val searchType: SearchType         // SERVICE, DOMAIN
)

@Entity(tableName = "username_index", indices = [Index(value = ["tokenHash"])])
data class UsernameIndexEntity(
    @PrimaryKey val indexId: String,
    val tokenHash: ByteArray,          // HMAC(IdIndexKey, normalizedUsername) — Exact Match 전용
    val credentialId: String
)
```

> **Password Reuse Index는 별도 Entity로 두지 않는다.** (7장 참고 — 결정 변경)

### 6.2 DAO

```kotlin
@Dao
interface CredentialDao {
    @Insert suspend fun insert(credential: CredentialEntity)
    @Update suspend fun update(credential: CredentialEntity)
    @Delete suspend fun delete(credential: CredentialEntity)
    @Query("SELECT * FROM credentials WHERE credentialId = :id")
    suspend fun findById(id: String): CredentialEntity?
    @Query("SELECT * FROM credentials")
    suspend fun getAll(): List<CredentialEntity>   // Password Reuse 비교용으로도 사용
}

@Dao
interface SearchIndexDao {
    @Insert suspend fun insert(index: SearchIndexEntity)
    @Query("SELECT credentialId FROM search_index WHERE tokenHash = :token")
    suspend fun findCredentialIds(token: ByteArray): List<String>
    @Query("DELETE FROM search_index WHERE credentialId = :credentialId")
    suspend fun deleteByCredentialId(credentialId: String)
}
```

### 6.3 Repository Interface (Domain)

```kotlin
interface CredentialRepository {
    suspend fun create(credential: Credential): Result<String>
    suspend fun update(credential: Credential): Result<Unit>
    suspend fun delete(credentialId: String): Result<Unit>
    suspend fun get(credentialId: String): Result<Credential>
    suspend fun search(query: SearchQuery): Result<List<CredentialSummary>>
    suspend fun checkPasswordReuse(password: String): Result<Boolean>  // 7장 참고
}
```

**Transaction 원칙**: Credential 생성/수정/삭제 시 `CredentialEntity` + `SearchIndexEntity` + `UsernameIndexEntity`를 하나의 Room `@Transaction`으로 묶는다. 하나라도 실패하면 전체 Rollback한다 (Orphan Index 방지).

---

## 7. Password Reuse Detection — 최종 결정 (Index 없이 조회 시점 비교)

기존 설계(HMAC Equality Index)는 저(低)엔트로피 비밀번호에 대한 사전 공격 취약점이 있어 **제외**하고, 다음 방식으로 대체한다.

```text
사용자가 새 Password 입력
   ↓
CredentialDao.getAll() 로 전체 CredentialEntity 조회
   ↓
각 Entity를 App Encryption Key로 복호화 → password 필드만 추출 (메모리에 최소 시간만 유지)
   ↓
MessageDigest.isEqual() 등 constant-time 비교로 순차 비교 (timing attack 완화)
   ↓
일치 항목 발견 시 PasswordReuseResult(isReused = true) 반환
```

- Domain 계층에는 `Boolean`만 전달한다. 어떤 Credential과 일치하는지 어디서 사용 중인지는 UI까지 절대 노출하지 않는다.
- 별도의 Index/HMAC Key/Entity가 필요 없으므로 공격 표면이 원천적으로 줄어든다.
- 성능: 개인용 앱 규모(MVP 목표 수십~수백 건)에서는 문제없다. 등록/수정 시에만 실행되므로 조회(검색) 성능에는 영향이 없다.
- `PasswordReuseChecker` 인터페이스로 추상화해두고, 향후 데이터 규모가 커지면(P1) 보안 검증을 마친 Keyed Index 방식으로 교체 가능하게 한다.

---

## 8. Secure Search Specification

### 8.1 Normalization 규칙

| 입력 요소 | 처리 |
| --- | --- |
| `http://`, `https://` | 제거 |
| `www.` | 제거 |
| Port, Path, Query, Fragment | 제거 |
| 마지막 `/` | 제거 |
| 대소문자 | lowercase |
| 공백 | trim |

**Subdomain은 보존한다** (`accounts.google.com` ≠ `google.com`으로 각각 별도 Credential 취급). Service Name은 Domain과 별도 필드로, 사용자가 직접 확인/수정 가능하게 제안한다 (예: `accounts.google.com` 입력 시 Service Name으로 `Google` 제안).

### 8.2 Exact Search (MVP)

```text
입력 → Normalize → HMAC(SearchKey, "domain:"+value 또는 "service:"+value) → SearchIndex 조회 → CredentialId → Credential
```

Namespace(`domain:`, `service:`)를 반드시 접두어로 사용해 두 검색 공간의 Token이 충돌하지 않게 한다.

### 8.3 Partial Search — P1로 연기 (MVP 범위 아님)

HMAC 특성상 `google`과 `goog`는 전혀 다른 Token이 되어 `LIKE` 검색이 불가능하다. MVP에서는 Exact Search만 지원하고, 결과가 없으면 "정확한 서비스명 또는 도메인을 입력하세요" 안내만 표시한다. P1에서 N-gram Token 방식 등을 별도 보안 검토 후 도입한다.

### 8.4 ID(Username) 자동완성 — Exact Match만 (MVP)

```kotlin
sealed interface SearchQuery {
    data class Text(val value: String) : SearchQuery
    data class Domain(val value: String) : SearchQuery
    data class Url(val value: String) : SearchQuery
}
```

동일 ID가 다른 서비스에도 등록되어 있으면 Mask 상태로 제안하고, 선택 시 ID만 재사용한다 (Password/Mask는 가져오지 않음).

---

## 9. Presentation 계층

### 9.1 SessionManager

```kotlin
enum class AppSessionState { LOCKED, AUTHENTICATING, UNLOCKED }
enum class CredentialDisplayState { MASKED, DISPLAYED }

interface SessionManager {
    val appState: StateFlow<AppSessionState>
    val credentialDisplayState: StateFlow<CredentialDisplayState>
    fun unlock()
    fun lock()
    fun displayCredential()
    fun resetCredentialDisplay()   // Security Reset — Credential 삭제 아님, Display만 초기화
}
```

**규칙**: Background 진입 시 `credentialDisplayState`는 즉시 MASKED로. Auto Lock 타임아웃 경과 시 `appState`도 LOCKED로. 두 이벤트는 독립적으로 작동한다.

### 9.2 UI State 예시

```kotlin
data class HomeUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<CredentialSummary> = emptyList(),
    val error: AppError? = null
)

data class CredentialDetailUiState(
    val credential: Credential? = null,
    val displayState: CredentialDisplayState = CredentialDisplayState.MASKED,
    val isLoading: Boolean = false,
    val error: AppError? = null
)
```

**중요**: `Credential` 평문 데이터는 ViewModel 메모리에 장기 보관하지 않는다. Background 전환 시 즉시 Clear한다.

---

## 10. Backup / Restore Architecture — 최종 결정

**MVP 범위: Cloud Backup + D2D(Device-to-Device) 모두 지원.** (5.4장 Key Lifecycle의 Restore Sequence를 그대로 따름)

### Acceptance Criteria

| ID | 내용 |
| --- | --- |
| AC-BACKUP-01 | Encrypted Credential DB가 정상적으로 이전되어야 한다 |
| AC-BACKUP-02 | PIN 입력을 통해 Credential Encryption Key가 복구되어야 한다 |
| AC-BACKUP-03 | 복구 후 Credential Decryption Test가 성공해야 한다 |
| AC-BACKUP-04 | 모든 필드(Service/URL/ID/Password/Mask/Category/Memo)가 원래 상태로 복구되어야 한다 |
| AC-BACKUP-05 | Display State(MASKED/DISPLAYED)는 복원하지 않는다 |
| AC-BACKUP-06 | App 인증 Session은 복원하지 않는다 — 새 기기에서는 반드시 재인증 |
| AC-BACKUP-07 | PIN 오류로 Key Recovery 실패 시 데이터를 임의로 초기화하지 않고 RESTORE_ERROR 상태를 유지한다 |

### Backup 포함/제외 대상

| 데이터 | Backup |
| --- | --- |
| Encrypted Credential DB | O |
| Recovery Wrapped Key, Recovery Salt, KDF Params | O |
| App 설정 (Auto Lock, Theme) | O |
| Local Wrapped Key (Device Master Key로 Wrap된 것) | 무의미 (포함해도 새 기기에서 사용 안 함) |
| Device Master Key (Keystore 내부) | 원천적으로 백업 불가 |
| App Session, Credential Display State | X |
| Biometric 등록 정보 | X (새 기기에서 재등록 필요) |

Android 12(API 31) 이상은 `data-extraction-rules`로 Cloud Backup과 D2D Transfer를 별도 XML로 제어해야 하므로 두 대상 모두에 대해 Recovery 관련 데이터가 포함되도록 규칙을 작성한다.

---

## 11. Credential Display / Security Reset 정책

```text
App Authentication (Biometric/PIN, OR 조건)
        ↓
     UNLOCKED
        ↓
Credential Search → MASKED 결과만 표시
        ↓ [보기]
     DISPLAYED
        ↓
Background / App Exit / Security Reset → 즉시 MASKED
```

- Security Reset은 Credential 데이터를 삭제/수정하지 않는다. Display 상태만 초기화한다.
- Clipboard는 사용하지 않는다.
- 검색 결과 화면에서는 실제 Password를 절대 노출하지 않는다 (`CredentialSummary`만 사용).

---

## 12. Error Code Taxonomy

```text
APP    : 001 Initialization Failed, 002 Configuration Error
AUTH   : 001 Authentication Failed, 002 PIN Failed, 003 Biometric Failed, 004 Session Expired
KEY    : 001 Key Not Found, 002 Key Access Failed, 003 Key Invalid,
         004 Key Restore Failed(기술적 실패), 005 PIN Recovery Mismatch(PIN 불일치),
         006 Recovery Attempt Limit Exceeded
ENC    : 001 Encryption Failed, 002 Decryption Failed, 003 Integrity Check Failed
DB     : 001 Open Failed, 002 Read Failed, 003 Write Failed, 004 Migration Failed
CRD    : 001 Invalid Credential, 002 Duplicate Credential, 003 Credential Not Found
SEARCH : 001 Invalid Query, 002 Index Error
BACKUP : 001 Backup Failed, 002 Restore Failed, 003 Key Recovery Failed
```

---

## 13. UI/UX 및 Navigation

### 13.1 디자인 방향

사용자가 업로드한 Reference 이미지(음악 앱)의 디자인 언어를 재해석한다 — Purple 계열 Gradient, 큰 Rounded Card, 큰 Typography, 부드러운 그림자, 여백 강조. 단, 보안 앱 특성상 Password 노출 상태에 강한 경고색(빨강 등)은 사용하지 않는다 (정상 상태이므로).

### 13.2 Navigation Map

```text
Launch
 ├─ First Run → Security Setup(Biometric→PIN→AutoLock→Key Init) → Home
 └─ Not First Run → Authentication(Biometric/PIN, OR) → Home

Home (Bottom Nav: Home / Category / Settings, FAB로 Credential 추가)
 ├─ Search → Search Result → Credential Detail (Masked→Displayed, Edit, Delete, Security Reset)
 ├─ Category → Credential List → Credential Detail
 └─ Settings
      ├─ Authentication
      ├─ Auto Lock
      ├─ Backup / Restore
      ├─ Security
      └─ Theme
```

### 13.3 주요 화면 요약

| 화면 | 핵심 요소 |
| --- | --- |
| First Launch | 브랜드 소개, Get Started |
| Security Setup | Biometric 설정 → PIN 등록(강화 정책 안내) → Auto Lock 설정 |
| Authentication | 중앙 집중형, Use fingerprint / Use PIN |
| Home | 상단 검색창 중심(Search-first), Categories, Recent |
| Search Result | Card 목록 (Service, Domain, ID Mask, Password Mask만 노출) |
| Credential Detail | Masked 상태 기본, [보기]로 Displayed 전환, Security Reset 버튼 |
| Credential Registration | Service/URL/ID/Password/Mask(사용자 직접 입력)/Category/Memo |
| Backup/Restore | Last Backup 시각, Backup Status, Restore 시 PIN 입력 안내 |

Avatar는 서비스 로고 자동 다운로드 없이 서비스명 첫 글자 기반으로 생성한다.

---

## 14. MVP vs P1 기능 매트릭스

| 기능 | MVP | P1 |
| --- | --- | --- |
| Domain/Service Exact Search | O | O |
| URL 입력 검색 | O | O |
| Multiple Credential per Service | O | O |
| ID Exact 자동완성 | O | O |
| Password Reuse Detection (조회 시점 비교) | O | O (규모 커지면 Index 방식 재검토) |
| Domain/Service Partial Search | - | O |
| ID Partial Autocomplete | - | O |
| Cloud Backup + D2D + PIN Recovery | O | - |
| Search Ranking 고도화 | 기본 | 고급 |

---

## 15. 미해결(Open) 항목 — 구현 중 재검토 필요

1. **PIN KDF 파라미터 최종값**: Argon2id 사용 가능 여부(라이브러리 크기/의존성)에 따라 PBKDF2 반복 횟수를 기기 성능 테스트 후 최종 확정할 것.
2. **Recovery 실패 시 백오프 정책의 구체적 시간표**: 5회 실패 후 30초 시작 지수 증가로 초안 제시했으나 실제 UX 테스트 후 조정 여지 있음.
3. **앱 이름**: 현재 placeholder("SecureVault") 사용 중. 최종 브랜드명 확정 필요.
4. **Partial Search 알고리즘(P1)**: N-gram Token 방식의 정보 노출량과 Index 크기 트레이드오프는 별도 보안 검토 후 확정.

---

## 16. Claude Code 작업 순서 권장

1. 프로젝트 골격 생성 (Hilt, Compose Navigation 설정 포함) + 3장 패키지 구조
2. `core.crypto`, `data.security` — KeyManager/CryptoManager 구현 (5장 Key Lifecycle 전체 시나리오 포함, 특히 Recovery 경로)
3. Room Entity/DAO (6장) + Repository 구현 + Transaction 처리
4. UseCase 계층 (Create/Update/Delete/Get/Search/CheckPasswordReuse/SecurityReset)
5. SessionManager + Authentication 화면
6. Home/Search/Credential Detail 화면 (13장 UI 명세 기준)
7. Registration/Edit 화면 + Password Reuse Warning UI
8. Settings + Backup/Restore 화면 + `data-extraction-rules` 설정
9. Error Handling 전체 연결 (12장 Error Code)
10. Acceptance Test 작성: Restore 시나리오(10장 AC-BACKUP-01~07)를 실제 기기 2대로 검증

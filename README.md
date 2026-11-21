# Smart Meter – Consumer App (Android)

Kotlin + Jetpack Compose app for consumers of the ESP32 prepaid energy + water meter.
It uses the consumer API of the Express + Supabase backend.

**API:** `https://smart-meter-api-do5b.onrender.com/` (set in `app/build.gradle.kts` → `API_BASE_URL`)

## Features

| Screen | What it shows | API |
|---|---|---|
| Login | Phone number **or** meter ID + password, stays logged in | `POST /consumer/auth/login` |
| Home | Balance, supply ON/OFF + reason, meter online/offline, live V / A / W / PF / Hz / water flow, total usage and costs, tariff. Auto-refresh every 30 s, pull to refresh | `GET /consumer/profile` |
| Usage | 7 / 30 / 90 days totals, daily bar chart (cost / electricity / water), day-by-day list | `GET /consumer/usage/summary` |
| Meter readings | Every report from the meter with the billed amount, infinite scroll | `GET /consumer/usage/readings` |
| Recharge | Current balance, amount (+ quick amounts), bKash / Nagad / Rocket / Upay / Card / Bank / Other, transaction ID, note | `GET /consumer/balance`, `POST /consumer/recharges` |
| History | Recharges (All / Pending / Approved / Rejected, with reject reason) and the balance ledger | `GET /consumer/recharges`, `GET /consumer/transactions` |
| Profile | Account, meter, tariff, edit profile, change password, log out | `GET/PATCH /consumer/profile`, `PATCH /consumer/password` |

- An expired token or a suspended account returns the user to the login screen automatically.
- Server validation errors are shown under the matching field.
- Network timeouts are long (60–90 s) because free Render sleeps when idle and takes about a minute to wake.

## Open and run

1. Android Studio → **Open** → select this folder (`Consumer_Apps`).
2. Wait for **Gradle sync** (first time downloads Gradle 8.11.1 and the libraries).
   If Android Studio offers to upgrade the Android Gradle Plugin, you can accept.
3. Connect a phone (USB debugging on) or start an emulator → **Run ▶**.
4. Log in with a consumer created by the admin (`POST /admin/consumers`).

Requirements: Android Studio Ladybug or newer, JDK 17 (bundled with Android Studio), Android 8.0+ device.

## Build an APK to share

- Quick test APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: **Build → Generate Signed App Bundle / APK** → APK → create a keystore → release.

## Project structure

```
app/src/main/java/com/smartmeter/consumer/
├── SmartMeterApp.kt            Application + dependency container
├── MainActivity.kt
├── data/
│   ├── remote/ApiModels.kt     JSON models (match the backend responses)
│   ├── remote/ApiService.kt    Retrofit endpoints
│   ├── remote/ApiClient.kt     OkHttp + auth header + timeouts
│   ├── local/SessionStore.kt   Saved login (DataStore)
│   └── MeterRepository.kt      API calls → ApiResult (success / error message)
└── ui/
    ├── AppRoot.kt              Login ↔ main app, bottom navigation
    ├── components/             Shared UI (cards, states, chart, list footer)
    ├── util/                   Formatting (৳, dates) + pagination
    ├── theme/                  Colors (light + dark)
    ├── login/  home/  usage/  recharge/  history/  profile/
```

Libraries: Jetpack Compose (Material 3), Navigation Compose, Lifecycle ViewModel, DataStore,
Retrofit + kotlinx.serialization, OkHttp.

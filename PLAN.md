# MWM Backtest — Android app

A native Kotlin / Jetpack Compose phone client over the **existing** MWM trading
platform API. The backtest engine, data, strategies, job queue, and worker all
already run server-side; the app is a thin client. No on-device compute.

## Why this shape

The hard parts exist and are deployed:

- **API** `mcp-servers/platform-api/http_server.py` → Starlette JSON API,
  `127.0.0.1:7654` on the VPS, public via Caddy at `https://trading.mwmai.no`.
- **Worker** `trading-platform-worker.service` runs queued backtests async.
- **Engine + data** `scripts/refresh_backtest.py` (7 families) + DuckDB OHLCV +
  16 nightly cell archives in `data/backtest/archive/`.

So "Android backtesting app" = mobile client + (later) one auth header. Compute,
2-year windows, and DuckDB stay on the server where they belong.

## Auth model (verified 2026-06-08 against the live Caddyfile)

- **All `GET /api/*` are public** — no auth. Every read the app needs is a plain
  HTTPS call. No Tailscale required.
- **Mutations** (`POST /api/jobs`, `save_cell`, `validate_cell_yaml`) sit behind
  **Caddy HTTP Basic Auth**; on success Caddy injects `X-Tailscale-User-Login:
  matswm86@yahoo.no` upstream (`PLATFORM_AUTH_REQUIRED=1`). The app stores the
  basic-auth credentials (EncryptedSharedPreferences) and sends
  `Authorization: Basic …` on the one mutating call. Single user, personal app.

## Endpoint map

| Screen | Endpoint | Notes |
|---|---|---|
| **Dashboard** (built) | `GET /api/observability` | One call → `accounts[]` + every `cells[]` with `backtest{pf, max_drawdown, total_pnl, win_rate, n_trades, equity_curve[365]}`, `ironclad{status}`, `live{health, today_pnl, state}`, `validation{dsr, lo_sharpe, cpcv, bootstrap}`. |
| Cell detail | same payload + `GET /api/runs/{id}` | Equity curve + trade distribution. |
| Run picker | `GET /api/list_cells`, `GET /api/list_strategies`, `GET /api/get_strategy_param_spec?name=` | |
| Run backtest | `POST /api/jobs` (Basic Auth) → poll `GET /api/jobs/{id}` → `GET /api/runs/{id}` | WorkManager poll → **local notification on completion**. The genuinely mobile-valuable feature. |
| Variant | `GET /api/load_cell` → edit param → `POST /api/validate_cell_yaml` → `POST /api/jobs` | |

## Tech

- Kotlin 2.1 + Jetpack Compose (Material 3), dark-only instrument-panel theme.
- Retrofit + OkHttp + kotlinx.serialization (lenient, `ignoreUnknownKeys`).
- MVVM: `ViewModel` + `StateFlow`, manual DI (no Hilt for v1).
- WorkManager + local notifications (job-done) — phase 2.
- `min/target/compile SDK = 26 / 35 / 35`, package `no.mwmai.backtest`.

## Build / verify

A **self-contained toolchain** is installed on the dev box (no sudo, removable):
Temurin **JDK 17** (`~/.jdks/temurin-17`), Android **SDK platform-35 + build-tools
35 + platform-tools** (`~/Android/Sdk`), **Gradle 8.11.1** (`~/gradle`). So the app
is built and verified locally before push:

```bash
export JAVA_HOME=~/.jdks/temurin-17 ANDROID_HOME=~/Android/Sdk
./gradlew assembleDebug   # -> app/build/outputs/apk/debug/app-debug.apk
```

v1 verified: clean `assembleDebug`, 17.9 MB `app-debug.apk` (package
`no.mwmai.backtest.debug`, label "MWM Backtest", launchable `MainActivity`).

CI mirrors this: every push runs `.github/workflows/build-android.yml` → artifact
`mwm-trading-mobile-debug-apk`. Versions live in `gradle/libs.versions.toml`.

## Known gaps / assumptions to confirm

- `equity_curve` is intentionally **not** in the DTO yet: the element types
  (`t` string-vs-epoch, `equity` number) need confirming before the detail-screen
  chart, to avoid a kotlinx type-mismatch. `ignoreUnknownKeys` skips it for now.
- `win_rate` is assumed a fraction (0..1); the formatter scales `<=1.5` to %.
- Launcher icon is a placeholder adaptive vector (rising equity line).

## Roadmap

1. ✅ **v1 Dashboard** — `/api/observability` → accounts + cell cards (PF / net /
   maxDD / win / ironclad / live-health dot), pull-to-refresh.
2. ✅ **Run-a-backtest** — strategy (picker) → timeframe (constrained) → instrument
   (MNQ/MGC/MES/BTC) → contracts → window (bounded by `data_range`) → `POST /api/jobs`
   (basic-auth, EncryptedSharedPreferences) → WorkManager poll → local notification →
   results screen (Canvas equity curve + net/gross/fees/trades/win/maxDD).
   **Caveat**: per-param tuning (q, lookback, stop_frac…) is shown read-only because
   the worker's ad-hoc path honours only strategy/symbol/timeframe/contracts. Real
   tuning needs `build_ephemeral_cell` + the job handler to accept a `params` override.
3. Cell detail screen (tap a dashboard cell → its archived equity curve + validation).
4. Per-param overrides: plumb `cell_spec.params` through worker → make screen-2 params
   editable. (Backend change to mwmt_platform.)
5. Variant explorer (load cell YAML, tweak, validate, submit) + full Settings screen.

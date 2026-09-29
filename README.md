# NHL Model 3.4 Live — Android

Native Android / Jetpack Compose app for the validated NHL Model V3.4 core.

## What the live app does

- Loads the NHL regular-season slate for the selected date from the NHL public API.
- Downloads MoneyPuck team game-by-game data for teams on that slate.
- Calculates the V3.4 rolling inputs automatically:
  - 20-game 5v5 xG share
  - 20-game 5v5 Corsi share
  - 10-game 5v5 xG share
  - 20-game team GSAx/60 proxy
  - back-to-back status
  - live current-season Elo approximation
- Runs the exact exported V3.4 logistic + Platt calibration engine offline once inputs are loaded.
- Shows home/away probability and PASS / LEAN / PLAY / PLAY+ / STRONG tier.
- Optional The Odds API key adds NHL moneylines, no-vig probabilities, model edge and EV.
- Keeps the original manual-entry screen for testing scenarios.

## Important model note

The original research V3.4 Elo was carried continuously across historical seasons. The live Android build reconstructs current-season Elo from NHL results with a 1500 seasonal starting point. That is intentionally labeled as an approximation in the app. All xG/Corsi/GSAx rolling inputs are pulled directly from MoneyPuck game-level team data.

The individual starting-goalie adjustment is **not** automatically applied. V3.4 only validated the team GSAx proxy; the separate starter module is still awaiting historical starter validation.

## Optional sportsbook odds

In the app go to **Model → Live odds** and paste a The Odds API key. The app requests NHL `h2h` moneylines in American format and calculates no-vig market probability and EV. Without a key, schedule + model probabilities still work.

## Build in Android Studio

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run on an Android phone/emulator, or choose **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. The APK will be under `app/build/outputs/apk/debug/` for a debug build.

## Build APK automatically on GitHub

This project includes `.github/workflows/build-apk.yml`.

1. Push the project to a GitHub repository.
2. Open **Actions → Build Android APK → Run workflow**.
3. Download the `NHL-Model-3.4-Live-debug-apk` artifact after the workflow finishes.

## Network sources

- NHL public web API: schedule/results.
- MoneyPuck: per-team game-by-game CSVs.
- The Odds API: optional NHL moneylines; requires the user's API key.

## Backtest reference

V3.4 walk-forward sample: 7,614 games.

- Accuracy: 59.71%
- AUC: 0.6282
- Brier: 0.235957
- Log loss: 0.664443

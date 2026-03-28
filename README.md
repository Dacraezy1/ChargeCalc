# ⚡ ChargeCalc

A sleek Android battery & charging monitor built specifically for the **Realme C25Y (Unisoc T610)** — and compatible with any Android 5.0+ device.

---

## 📱 Features

| Feature | Description |
|---|---|
| ⚡ **Charging Speed** | Live mA reading — Slow / Normal / Fast / Super Fast |
| 🔋 **Battery Percentage** | Large real-time display with animated progress bar |
| 🔌 **Power (Watts)** | Calculated from current × voltage |
| 🌡 **Temperature** | °C and °F with overheating warnings |
| 🏥 **Battery Health** | Good / Overheat / Dead / Over Voltage detection |
| 🔗 **Plug Type** | AC / USB / Wireless detection |
| ⏱ **Time to Full** | Estimated time remaining to 100% |
| 📊 **Session Stats** | Time plugged in, % gained this session |
| 📈 **Average Speed** | Rolling average of charging rate over last 60 readings |
| 🕐 **Last Charged** | Remembers when you last plugged in |
| 🌙 **Dark/Light Theme** | Toggle with one tap, preference saved |

### Charging Speed Labels

| Label | Current Range |
|---|---|
| 🔴 Very Slow | < 500 mA |
| 🟠 Slow | 500 – 1000 mA |
| 🟡 Normal | 1000 – 2000 mA |
| 🟢 Fast | 2000 – 4000 mA |
| 🔵 Super Fast | > 4000 mA |

---

## 🚀 Build with GitHub Actions (No PC needed!)

### Step 1 — Create the repo

1. Go to [github.com](https://github.com) and log in
2. Create a **new repository** named `ChargeCalc`
3. Make it **Public** (or Private — both work fine)
4. Do **NOT** initialize with a README (you already have one)

### Step 2 — Upload all these files

You can use the GitHub web UI to upload all the files, or use git:

```bash
git init
git add .
git commit -m "Initial commit: ChargeCalc app"
git branch -M main
git remote add origin https://github.com/Dacraezy/ChargeCalc.git
git push -u origin main
```

### Step 3 — GitHub Actions builds your APK automatically

Once you push to `main`, GitHub Actions will:
1. Spin up an Ubuntu runner
2. Install JDK 17
3. Set up Gradle 8.4
4. Build your APK
5. Upload it as a downloadable artifact

**Watch the build:**
- Go to your repo → click **Actions** tab
- Click the running workflow
- Wait ~3–5 minutes for it to finish

### Step 4 — Download your APK

- In the completed workflow run, scroll down to **Artifacts**
- Click **ChargeCalc-Debug-APK** to download a `.zip`
- Unzip it → you get `app-debug.apk`
- Transfer to your Realme C25Y and install!

> **Enable Unknown Sources:** Settings → Security → Install Unknown Apps → allow your file manager

---

## 📁 Project Structure

```
ChargeCalc/
├── .github/
│   └── workflows/
│       └── build.yml              ← GitHub Actions workflow
├── app/
│   ├── build.gradle               ← App dependencies & config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/dacraezy/chargecalc/
│       │   ├── MainActivity.java  ← Main app logic
│       │   └── ChargingReceiver.java
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/colors.xml
│           ├── values/strings.xml
│           ├── values/themes.xml
│           └── drawable/ + mipmap-*/
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradlew
└── README.md
```

---

## 🔧 Compatibility

- **Tested device:** Realme C25Y, Unisoc T610
- **Min Android:** 5.0 (API 21)
- **Target Android:** 14 (API 34)
- **Architecture:** Works on all ARM/ARM64 devices

---

## 📝 Notes

- Charging current readings depend on your device's kernel reporting. Most modern Android phones (including Realme) report this correctly via `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW`.
- The "Time to Full" estimate assumes a ~4000 mAh battery. It's an approximation.
- No internet permission required — this app works 100% offline.

---

*Made by Dacraezy · ChargeCalc v1.0.0*

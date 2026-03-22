🌱 Forge – Inner Garden

A Digital Wellbeing & Doomscroll Recovery App

---

🧠 Overview

Forge – Inner Garden is a behavior-aware digital wellbeing app designed to detect and reduce unconscious scrolling (doomscrolling) on social media platforms like Instagram.

Instead of tracking streaks, Forge focuses on real-time behavioral awareness, helping users regain control of their attention through intelligent detection and gentle nudges.

---

🚀 Key Features

📱 1. Real-Time Scroll Detection

- Uses Android Accessibility Service
- Detects:
  - Reels scrolling
  - Continuous feed scrolling
  - Rapid swiping behavior

---

🧠 2. Unconscious Scrolling Detection

- Identifies:
  - Deep scrolling sessions
  - No-interaction usage
  - Low break patterns
- Triggers awareness notifications when user enters doomscroll state

---

⏱ 3. Session Tracking

- Automatically detects:
  - Session start
  - Session end (app close / inactivity)
- Tracks:
  - Total usage time
  - Number of sessions per day

---

📊 4. Smart Analytics

Stores daily behavioral metrics:

- 🎞 Reels viewed
- 🧠 Deep scroll count
- ⏳ Usage time (minutes)
- 🔁 Sessions

---

📈 5. Dashboard & Insights

- Daily stats visualization
- Year heatmap view
- Real-time updates using DataStore + Flow

---

🔔 6. Behavioral Nudges

- Reel limit notifications
- Unconscious scrolling alerts
- Designed to be non-intrusive and calming

---

☁️ 7. Background Sync System

- Uses WorkManager
- Sync types:
  - Instant sync (after session ends)
  - Periodic sync (every 6 hours)
- Sends data to backend for parent dashboard

---

👨‍👩‍👧 8. Parent Monitoring System

- Each device is linked via:
  - Device ID
  - Child ID
- Parent dashboard (web):
  - View usage patterns
  - Analyze behavior trends

---

🏗️ Architecture

📲 Mobile App

- Frontend: Jetpack Compose
- Core Logic: Kotlin
- Storage:
  - Room DB → historical stats
  - DataStore → real-time UI data
- Detection Engine:
  - AccessibilityService
  - Behavior analysis module

---

🔄 Data Flow

Accessibility Events
        ↓
Scroll Detector Service
        ↓
Session Manager
        ↓
Feature Extraction
        ↓
Local Storage (Room + DataStore)
        ↓
Sync Worker (WorkManager)
        ↓
Backend API (Node.js)

---

🌐 Backend

- Node.js + Express
- MongoDB
- Bulk sync API:
  - Upserts daily stats
  - Indexed by deviceId + date

---

⚙️ Core Algorithms

🧠 Unconscious Scroll Detection

Triggered when:

- Session duration > threshold (20s+)
- No meaningful interaction (15s+)
- Low break frequency

IF long_session AND no_interaction AND low_breaks
→ Trigger Deep Scroll Detection

---

⏱ Session Detection

Session ends when:

- App is closed OR
- No activity for 60 seconds

Session End →
Save remaining time →
Increment session count →
Trigger sync

---

📊 Reel Detection

Uses:

- ViewPager / RecyclerView index tracking
- Delta fallback for unsupported devices

Prevents:

- Back scroll counting ❌
- Auto-load false positives ❌

---

🔐 Permissions Used

- "Accessibility Service" → detect scroll behavior
- "POST_NOTIFICATIONS" → send awareness nudges

---

📦 Tech Stack

Layer| Tech Used
UI| Jetpack Compose
Language| Kotlin
Local DB| Room
State| DataStore + Flow
Background| WorkManager
Networking| Retrofit
Backend| Node.js + Express
Database| MongoDB

---

🧪 How It Works (Flow)

1. User opens Instagram
2. Scroll events captured via Accessibility
3. Session starts automatically
4. Behavior analyzed in real-time
5. Metrics updated locally
6. On session end:
   - Time saved
   - Session incremented
   - Instant sync triggered
7. Backend stores and aggregates data
8. Parent dashboard visualizes insights

---

🎯 Goals of the Project

- Reduce doomscrolling behavior
- Improve digital awareness
- Provide real-time behavioral feedback
- Enable parent-child monitoring system

---

🚧 Future Enhancements

- 🤖 AI-based addiction prediction
- 📉 Personalized intervention system
- 🌿 Gamified recovery (garden growth system)
- 📊 Advanced analytics dashboard
- 🔒 Privacy-first local ML models

---

🧑‍💻 Author

Built as part of a behavioral AI + digital wellbeing system project.

---

📄 License

This project is for educational and research purposes.

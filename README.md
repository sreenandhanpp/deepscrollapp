🌱 Forge – Inner Garden

A Smart Digital Wellbeing & Scroll Behavior Analysis App

---

🧠 Overview

Forge – Inner Garden is an Android-based digital wellbeing application that detects and analyzes unconscious scrolling behavior (doomscrolling) in real time.

The app uses Accessibility Services + behavioral algorithms to monitor user interactions (especially Instagram reels), track usage patterns, and provide gentle nudges to improve digital habits.

---

🚀 Features

📱 Real-Time Scroll Detection

- Tracks reels scrolling using Accessibility Service
- Supports ViewPager & RecyclerView detection
- Prevents false counts (back scroll filtering)

---

🧠 Unconscious Scrolling Detection

- Detects:
  - Deep scrolling
  - Rapid swiping
  - Long sessions without interaction
- Triggers awareness notifications

---

⏱ Session Tracking

- Automatically detects:
  - Session start
  - Session end (app close / inactivity)
- Tracks:
  - Usage time
  - Session count

---

📊 Daily Analytics

- Reels viewed
- Deep scroll count
- Usage minutes
- Sessions per day

---

📈 Dashboard

- Real-time stats using DataStore
- Year heatmap visualization
- Simple and minimal UI

---

🔔 Smart Notifications

- Reel limit reminders
- Unconscious scrolling alerts
- Calm and non-intrusive UX

---

☁️ Sync System

- Periodic sync using WorkManager (6 hours)
- Instant sync on session end
- Backend integration using REST API

---

👨‍👩‍👧 Parent Dashboard Support

- Device linked using:
  - Device ID
  - Child ID
- Enables remote monitoring of usage behavior

---

🏗️ Architecture

📲 Mobile App

- Kotlin + Jetpack Compose
- Accessibility Service for tracking
- Room Database for analytics
- DataStore for UI state

---

🌐 Backend

- Node.js + Express
- MongoDB
- Bulk sync API (upsert-based)

---

🔄 Data Flow

User Scroll → Accessibility Events  
        ↓  
Scroll Detector Service  
        ↓  
Session & Behavior Processing  
        ↓  
Room DB (Daily Stats)  
        ↓  
Sync Worker (WorkManager)  
        ↓  
Backend API → MongoDB  
        ↓  
Parent Dashboard

---

🧩 Data Model

📌 Local (Room DB)

ScrollDailyStats(
  date: String,
  reelsViewed: Int,
  deepScrollCount: Int,
  usageMinutes: Int,
  sessions: Int,
  isSynced: Boolean
)

---

📌 Backend (MongoDB)

{
  deviceId: String,
  date: String,
  reelsViewed: Number,
  deepScrollCount: Number,
  usageMinutes: Number,
  sessions: Number
}

---

⚙️ Core Logic

🧠 Deep Scroll Detection

- Based on:
  - session duration
  - scroll frequency
  - lack of interaction

---

⏱ Session Handling

- Ends when:
  - App is closed OR
  - No activity for 60 seconds

---

🔄 Sync Strategy

- Only unsynced data is sent
- Upsert prevents duplicates
- Immediate sync on session end

---

🔐 Permissions

- "Accessibility Service" → Scroll tracking
- "POST_NOTIFICATIONS" → User alerts

---

📦 Tech Stack

Layer| Technology
UI| Jetpack Compose
Language| Kotlin
Local DB| Room
State Mgmt| DataStore
Background| WorkManager
Networking| Retrofit
Backend| Node.js (Express)
Database| MongoDB

---

🎯 Goals

- Reduce doomscrolling
- Improve digital awareness
- Provide behavior insights
- Enable parent monitoring

---

🚧 Future Enhancements

- AI-based behavior prediction
- Gamification (Inner Garden growth)
- Advanced analytics dashboard
- Personalized interventions

---

🧑‍💻 Author

Developed as part of a Digital Wellbeing & Behavioral Analytics System project.

---

📄 License

For educational and research purposes.

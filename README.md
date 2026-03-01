# 🚀 Adaptive Finance AI (Android Client)

An intelligent, offline-first personal finance application that utilizes reinforcement learning to provide dynamic gamification and adaptive budgeting strategies.

This repository contains the **Thin Client / Edge-Compute Android Application**. It is designed to work in tandem with a Python FastAPI backend running a customized LinUCB Contextual Bandit algorithm.

## 🧠 Architectural Highlights

* **Edge Feature Engineering:** To preserve data privacy and reduce latency, the raw transaction data is never sent to the cloud. Instead, the local SQLite database calculates mathematical user contexts (like `spending_volatility` and `transaction_count`) on the edge device and sends an anonymized context vector to the AI.
* **Offline-First Resilience:** Network failures are treated as an expected state. If the AI is unreachable, or if a user engages with a gamification strategy while offline, the app queues the feedback telemetry locally.
* **Decoupled Feedback Loop:** User rewards are explicitly decoupled from the inference request. The app uses Android's `WorkManager` to silently sync pending reinforcement learning rewards (`+1.0` or `0.0`) to the cloud backend whenever an internet connection is re-established.
* **Dynamic UI Generation:** The UI is completely decoupled from hardcoded logic. Colors, text, and gamification strategies are dynamically rendered in real-time based on the exact JSON schema provided by the cloud LinUCB model.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Local Persistence:** Room Database (SQLite)
* **Network Client:** Retrofit 2 & OkHttp
* **Background Processing:** Android WorkManager & Kotlin Coroutines

## 📂 Project Structure

* `core/network/` - Retrofit configuration and singleton API client.
* `data/local/` - Room DAOs, Entities, and local edge-compute queries.
* `data/remote/` - Strict Kotlin data classes matching the Python Pydantic schemas.
* `data/repository/` - The single source of truth bridging SQLite and Retrofit.
* `feature/` - Jetpack Compose screens and ViewModels.
* `worker/` - Background sync services for offline telemetry persistence.

## 🚀 Getting Started

1. Clone this repository.
2. Open the project in **Android Studio** (Koala or newer recommended).
3. Ensure your `local.properties` or `ApiClient.kt` contains the valid `API_TOKEN` and backend `BASE_URL` pointing to the FastAPI server.
4. Run Gradle Sync.
5. Deploy to an Android Emulator or physical device (API Level 24+).
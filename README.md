# 📱 MappingOP

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Status](https://img.shields.io/badge/Status-Active%20Dev-orange?style=for-the-badge)

**MappingOP** is a mobile application for automating the work of field teams working with consumers. It replaces paper worksheets with a digital system for route planning, meter reading, and data synchronization.

> **MappingOP** — мобильное приложение для автоматизации работы выездных бригад для работы с потребителями. Оно заменяет бумажные ведомости цифровой системой для планирования маршрута, снятия показаний и синхронизации данных.

---

## 🗺️ Roadmap / Дорожная Карта

We are currently at **Stage 1 (Internal Architecture)**.
Сейчас мы находимся на **Этапе 1 (Внутренняя архитектура)**.

### English Version 🇺🇸
![Roadmap En](assets/En.png)

<details>
<summary><b>Click to see Russian Version / Нажмите для просмотра версии на Русском 🇷🇺</b></summary>

![Roadmap Ru](assets/Ru.png)
</details>

---

## 🛠 Tech Stack / Технологии

The project is built using modern Android development practices (2024/2025 standards).

| Category | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin | 100% Kotlin codebase |
| **UI** | Jetpack Compose | Material3 Design, Single Activity |
| **Architecture** | MVVM | Model-View-ViewModel pattern |
| **Database** | Room | Local SQLite database with Relations |
| **Concurrency** | Coroutines & Flow | Asynchronous data handling |
| **Files** | Apache POI | Reading/Parsing Excel (.xlsx) files |
| **Navigation** | Compose Navigation | Type-safe navigation |

---

## ✨ Key Features / Возможности

### ✅ Current (Implemented)
* **Excel Parsing:** Importing control lists directly from `.xlsx` files.
* **Local Database:** Storing worksheets and consumers offline using Room.
* **Worksheets:** Viewing lists of consumers assigned to the controller.
* **Results:** Recording meter readings and statuses (Processed/Refusal).

### 🚧 In Progress & Planned
* **Map Integration:** Visualizing consumers on a map (OSM/Visicom).
* **Server Sync:** REST API integration for downloading tasks and uploading reports.
* **Admin Panel:** Web-interface for managing tasks (Backend side).
* **Route Optimization:** GPS tracking and smart routing.

---

## 🚀 How to Run / Запуск

1.  Clone the repository:
    ```bash
    git clone [https://github.com/RoninSoulKh/MappingOP.git](https://github.com/RoninSoulKh/MappingOP.git)
    ```
2.  Open in **Android Studio** (Ladybug or newer recommended).
3.  Sync Gradle project.
4.  Run on Emulator (Android 13+) or Physical Device.

---

## 👥 Team / Команда

* **Android Development:** [RoninSoulKh](https://github.com/RoninSoulKh) - Architecture, UI, Logic.
* **Backend:** Private contributor - Server, API, Database.
* **UI/UX Design:** [s1lentoath](https://github.com/s1lentoath) - Visual style, User Experience.

---

License: Proprietary / In Development.

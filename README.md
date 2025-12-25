# 📱 MappingOP

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Architecture](https://img.shields.io/badge/Clean%20Architecture-MVVM-blue?style=for-the-badge)

**MappingOP** is a professional Android solution for field service automation. It digitizes paper workflows, enabling field teams to manage consumer data, sync routes via Excel, and process meter readings in real-time.

> **MappingOP** — профессиональное Android-решение для автоматизации работы выездных бригад. Проект оцифровывает бумажные ведомости, позволяя командам управлять данными потребителей, импортировать маршруты через Excel и фиксировать показания в реальном времени.

---

## 🗺️ Roadmap / Дорожная Карта

We have successfully completed the core architecture and moved to **Stage 2 (Logic & Data Processing)**.
Мы успешно завершили создание ядра архитектуры и перешли к **Этапу 2 (Логика и обработка данных)**.

### English Version 🇺🇸
![Roadmap En](assets/Roadmap_En_V2.png)

<details>
<summary><b>Click to see Russian Version / Нажмите для просмотра версии на Русском 🇷🇺</b></summary>

![Roadmap Ru](assets/Roadmap_Ru_V2.png)
</details>

---

## 🏗️ Architecture & Clean Code / Архитектура

The project has been updated to follow **Clean Architecture** principles to ensure scalability:
Проект был обновлен в соответствии с принципами **Clean Architecture** для обеспечения масштабируемости:

* **Domain Layer:** Business logic, Models, and UseCases (Pure Kotlin).
* **Data Layer:** Room persistence, Excel parsing (Apache POI), and Repository implementations.
* **Presentation Layer:** State-driven UI with Jetpack Compose and ViewModels.

---

## 🛠 Tech Stack / Технологии

| Category | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin | Modern, safe, and expressive |
| **UI Framework** | Jetpack Compose | Material3, Responsive UI |
| **DI** | Koin / Manual DI | Dependency Injection for decoupling |
| **Local DB** | Room | Complex relations and offline-first approach |
| **File Engine** | Apache POI | Industrial-grade Excel (.xlsx) processing |
| **Async** | Coroutines & Flow | Reactive data streams |

---

## 🧩 Business Logic (Use Cases)

To maintain a clean separation between the UI and Data layers, the project implements a **Domain-driven** approach. Key business operations are encapsulated in UseCases:

* **`ImportExcelUseCase`**: Handles complex logic for parsing `.xlsx` files, validating consumer data, and mapping raw rows to Domain Entities.
* **`GetWorksheetsUseCase`**: Manages data flow from the Room database, providing the UI with sorted and filtered lists of tasks.
* **`SyncDataUseCase`** *(In Progress)*: Will handle the conflict resolution between local storage and the remote server.

> **Why this matters?** This approach makes the code 100% testable and independent of the UI framework.

---

## ✨ Key Features / Возможности (Updated)

### ✅ Implemented / Реализовано
* **Deep Excel Parsing:** Robust mapping of complex Excel sheets to Domain models.
* **Modular Architecture:** Clean separation of concerns (Data, Domain, App).
* **Entity Mapping:** Seamless transformation between Database entities and UI models.
* **Offline Storage:** Full Room DB integration for seamless work without internet.
* **Search & Filters:** Advanced sorting of consumers by status or address.

### 🚧 In Progress / В разработке
* **Complex UI States:** Developing detailed consumer views and input validation.
* **Location Services:** Geotagging for every processed reading.

---

## 🚀 How to Run / Запуск

1. Clone: `git clone https://github.com/RoninSoulKh/MappingOP.git`
2. Open in **Android Studio** (Ladybug+).
3. Ensure you have **JDK 17** configured.
4. Run on Android 13+ (API 33).

---

## 👥 Team / Команда

* **Android Development:** [RoninSoulKh](https://github.com/RoninSoulKh) — Architecture, UI, Logic.
* **Backend:** [EmsFear](https://github.com/EmsFear) — Server, API, Database.
* **UI/UX Design:** [s1lentoath](https://github.com/s1lentoath) — Visual style, User Experience.

---
*License: Proprietary / In Development*

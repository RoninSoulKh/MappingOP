# Mapping OP 🗺️

![Version](https://img.shields.io/badge/Version-2.1.0-cyan?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Security](https://img.shields.io/badge/Security-JWT%20Auth-red?style=for-the-badge)
![Architecture](https://img.shields.io/badge/Clean%20Architecture-MVVM-blue?style=for-the-badge)

**Mapping OP** — это высокотехнологичное Enterprise-решение для цифровой трансформации полевых операций. Приложение автоматизирует полный цикл работы с геопространственными данными, обеспечивает безопасный доступ к корпоративным ресурсам и предоставляет инструменты для прецизионного геокодирования.

Проект спроектирован с использованием парадигмы **Offline-First**, обеспечивая отказоустойчивость и консистентность данных в условиях полного отсутствия сетевого покрытия.

---

## 🔥 Update v2.1.0: Security & Digital Identity

Глобальное обновление системы безопасности и пользовательского опыта. Внедрена модульная система авторизации и переработан профиль сотрудника.

### 🔐 Auth & Security Module
* **JWT Authentication:** Полноценная интеграция с корпоративным Backend (FastAPI + Nginx). Поддержка Access/Refresh токенов.
* **Session Management:** Безопасное хранение сессий через `EncryptedSharedPreferences` / `DataStore`.
* **Guest Mode (Dev):** Реализован изолированный "Гостевой режим" для автономной работы и тестирования функционала без доступа к серверу.
* **Nginx Proxy Support:** Оптимизация сетевого слоя для работы через защищенный Reverse Proxy.

### 👤 Enterprise Profile UI (Digital Design)
* **Digital UI Concept:** Полный редизайн профиля. Отказ от "тяжелых" блоков в пользу полупрозрачных поверхностей, тонких акцентных рамок и неонового свечения.
* **Smart Actions:** Интерактивные карточки действий с цветовой кодировкой (Cyan — Система, Purple — Поддержка, Red — Безопасность).
* **Gamification Elements:** Визуализация KPI сотрудника (Обработано точек / В очереди) в реальном времени.

---

## 🚀 Key Features

### 🗺️ Advanced Mapping System
* **OSMDroid Integration:** Рендеринг карт на базе OpenStreetMap с оптимизацией под экраны высокого разрешения (DPI scaling).
* **Smart Dynamic Clustering:** Проприетарный алгоритм кластеризации маркеров с динамическим пересчетом весов в зависимости от уровня масштабирования.
* **Location Services:** Высокоточная GPS-навигация и трекинг местоположения.

### 📊 Data Architecture & IO
* **Excel Engine (Apache POI):** Нативный парсинг и генерация отчетов `.xlsx`.
* **Persistence Layer:** Реляционное хранилище Room (SQLite) с поддержкой реактивных потоков (Flow).
* **Race Condition Protection:** Защита от потери данных при быстрой навигации и смене состояний экрана.

---

## 🛠 Tech Stack

| Category | Technologies |
| :--- | :--- |
| **Core** | Kotlin, Java 17, Coroutines, Flow |
| **Presentation** | Jetpack Compose (Material 3), Navigation Compose |
| **Architecture** | MVVM, Clean Architecture, Repository Pattern |
| **Security & Auth** | **JWT, Retrofit, OkHttp Interceptors** |
| **Data Storage** | Room (SQLite), DataStore Preferences |
| **Geospatial** | OSMDroid, Visicom API |
| **Build System** | Gradle Kotlin DSL (KTS) |

---

## 🏗 Configuration

Для успешной компиляции проекта требуется настройка окружения через файл `local.properties`.

### 1. API Keys
Создайте файл `local.properties` в корне проекта и добавьте ваш ключ Visicom API:

```properties
VISICOM_API_KEY="your_api_key_here"
```
### 2. Build Variants
В проекте настроен специфический конфиг для `release` сборки:
* **Minification:** Отключена (`isMinifyEnabled = false`) для корректной работы Apache POI.
* **Logging:** Используется кастомная реализация `NullLogger` для production-сборок.

```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        isShrinkResources = false
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```
### 📂 Project Structure
Проект организован в строгом соответствии с принципами Clean Architecture. Обновлена структура для модуля авторизации:
```kotlin
com.roninsoulkh.mappingop
├── data/
│   ├── local/             # Room Database & TokenManager
│   ├── remote/            # API Services (Auth, Geo)
│   └── repository/        # Implementation (AuthRepository, AppRepository)
├── domain/
│   ├── models/            # Models (User, LoginRequest, Consumer)
│   └── repository/        # Interfaces
└── presentation/
    ├── viewmodels/        # AuthViewModel, MapViewModel
    └── ui/
        ├── screens/
        │   ├── auth/      # Login, Register Screens
        │   └── profile/   # New Digital Profile Screen
        └── theme/         # Enterprise Dark Theme System
```
## 👥 Team (Команда)
- **[RoninSoulKh](https://github.com/RoninSoulKh)** — Lead Developer: Architecture, Frontend, Security, UI/UX design.
- **[EmsFear](https://github.com/EmsFear)** — Backend Developer: DevOps, API logic.

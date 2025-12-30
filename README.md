# Mapping OP 🗺️

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Architecture](https://img.shields.io/badge/Clean%20Architecture-MVVM-blue?style=for-the-badge)

**Mapping OP** — это высокотехнологичное Enterprise-решение для цифровой трансформации полевых операций. Приложение автоматизирует полный цикл работы с геопространственными данными: от инжекции ведомостей из внешних систем до прецизионного геокодирования и интеллектуального отображения объектов.

Проект спроектирован с использованием парадигмы **Offline-First**, обеспечивая отказоустойчивость и консистентность данных в условиях полного отсутствия сетевого покрытия.

---

## 🚀 Key Features

### 🗺️ Advanced Mapping System
* **OSMDroid Integration:** Рендеринг карт на базе OpenStreetMap с оптимизацией под экраны высокого разрешения (DPI scaling).
* **Smart Dynamic Clustering:** Проприетарный алгоритм кластеризации маркеров с динамическим пересчетом весов в зависимости от уровня масштабирования (Zoom-based logic).
* **Location Services:** Высокоточная GPS-навигация и трекинг местоположения в реальном времени.
* **Semantic UI:** Цветовая индикация состояний объектов для мгновенной оценки прогресса выполнения задач.

### 📍 Intelligent Geocoding Engine
* **Visicom API Pipeline:** Глубокая интеграция с Visicom Geocoding API для получения координат объектов.
* **Validation Middleware:** Многоступенчатая фильтрация результатов (Anti-Odesa filter), исключающая ошибки позиционирования за пределами целевых регионов.
* **Cascading Search:** Умный парсинг адресов с многоуровневым каскадным поиском (Point -> Street -> Settlement).
* **Concurrency Management:** Пакетная обработка данных через Kotlin Coroutines с поддержкой Rate Limiting для соблюдения лимитов API.

### 📊 Data Architecture
* **Excel Engine (Apache POI):** Нативный парсинг и генерация отчетов в формате `.xlsx` без внешних зависимостей.
* **Persistence Layer:** Реляционное хранилище на базе SQLite (Room) с поддержкой сложных миграций и реактивных потоков данных (Flow).
* **Data Integrity:** Строгие внешние ключи и каскадные операции для обеспечения целостности данных между ведомостями и потребителями.

---

## 🛠 Tech Stack

| Category | Technologies |
| :--- | :--- |
| **Core** | Kotlin, Java 17, Coroutines, Flow |
| **Presentation** | Jetpack Compose (Material 3), Navigation Compose |
| **Architecture** | MVVM, Clean Architecture, Repository Pattern |
| **Data Storage** | Room (SQLite), DataStore |
| **Networking** | OkHttp, Retrofit 2 |
| **Geospatial** | OSMDroid, Visicom API |
| **Build System** | Gradle Kotlin DSL (KTS) |

---

## 🏗 Configuration

Для успешной компиляции проекта требуется настройка окружения через файл `local.properties`.

### 1. API Keys
Создайте файл `local.properties` в корне проекта и добавьте ваш ключ Visicom API:

```text
properties
VISICOM_API_KEY="your_api_key_here"
```

### 2. Build Variants
В проекте настроен специфический конфиг для `release` сборки для корректной работы Apache POI:
* **Minification:** Отключена (`isMinifyEnabled = false`) для предотвращения удаления необходимых XML-схем POI.
* **Logging:** Используется кастомная реализация `NullLogger`.

```text
kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        isShrinkResources = false
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```
---

## 📂 Project Structure

Проект организован в строгом соответствии с принципами **Clean Architecture**, разделяя логику на независимые слои:

```text
com.roninsoulkh.mappingop
├── data/                # Data Layer (Работа с данными)
│   ├── local/           # Room Database, DAO & TypeConverters
│   ├── remote/          # Visicom API Service & Network Logic
│   └── repository/      # Реализация репозиториев
├── domain/              # Domain Layer (Бизнес-логика)
│   ├── models/          # Data classes (Consumer, Worksheet)
│   └── repository/      # Интерфейсы репозиториев
└── presentation/        # UI Layer (Интерфейс)
    ├── viewmodels/      # State management (MapViewModel, HomeViewModel)
    └── ui/              # Jetpack Compose Screens & Components
```

## 👥 Team (Команда)
- **[RoninSoulKh](https://github.com/RoninSoulKh)** — Lead Developer: Architecture, Frontend, Data Processing.
- **[EmsFear](https://github.com/EmsFear)** — Backend Developer: API synchronization logic.
- **[s1lentoath](https://github.com/s1lentoath)** — UI/UX Designer: Interface concept & identity.


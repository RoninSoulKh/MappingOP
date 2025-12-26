# 📊 MappingOP: Field Operator Mobile Workspace

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Architecture](https://img.shields.io/badge/Clean%20Architecture-MVVM-blue?style=for-the-badge)

**MappingOP** is a high-tech solution designed to automate the work of field crews operating in active conflict zones. The app completely replaces paper-based worksheets, ensuring stable operation even in areas with no internet coverage and GPS interference.

---

## 🛣 Project Roadmap (Дорожная карта проекта)

We have divided the development into three key stages. Currently, we are transitioning from stabilizing the foundation to UI cleanup and optimization.

### 🇷🇺 Russian Version
![Roadmap RU](https://raw.githubusercontent.com/RoninSoulKh/MappingOP/main/Ru.jpg)

### 🇬🇧 English Version
![Roadmap EN](https://raw.githubusercontent.com/RoninSoulKh/MappingOP/main/En.jpg)

---

## 🎯 Problems & Solutions (Проблемы и решения)

In field conditions, paper worksheets often get damaged by weather, and human error leads to incorrect dates or skipped addresses.
**MappingOP** solves this through:
* **Offline-first Architecture**: Full functionality without internet using Room DB.
* **Automatic Logging**: Precise time and date tracking for every processed object.
* **Digital Mapping**: Rapid building search even when GPS is suppressed.

---

## ✨ Core Features (Основные возможности)

### 1. Smart Worksheet Import (Импорт ведомостей)
The system automatically parses Excel files (.xlsx), creating convenient consumer cards with all necessary data: account ID, address, debt amount, and meter number.

### 2. Offline Mapping (Оффлайн-карты)
Integration with **OpenStreetMap (OSM)** and **Visicom API** allows viewing building outlines and numbers even in offline mode.
* **Visualization**: Buildings from the worksheet are highlighted in red on the map.

### 3. Result Recording (Фиксация результатов)
User-friendly interface for entering meter readings, building status, and processing types (hand-delivered, refusal, etc.) with automatic database saving.

---

## ⚖️ Legal & Licensing (Юридическая чистота)
The project is developed with strict adherence to copyright laws:
* **Mapping**: Data from **OpenStreetMap** (ODbL license).
* **API**: **Visicom API** usage is strictly within free limits (1500 req/day).
* **Open Source**: All libraries use **MIT** or **Apache 2.0** licenses.
* **Typography**: **Google Fonts** (OFL license).

---

## 🏗 Tech Stack (Технологический стек)
* **Language**: Kotlin.
* **UI Framework**: Jetpack Compose.
* **Database**: Room (Offline-first).
* **Architecture**: MVVM.
* **Document Processing**: Apache POI.
* **Languages**: Ukrainian (Primary), English, Russian.

---

## 🚀 Next Steps (Ближайшие планы)
* **Refactoring**: Cleaning code from technical comments and optimizing Room structures.
* **Coordinates**: Adding Lat/Lng fields for future map visualization.
* **Profile**: Creating a user tab with settings and legal information.

---
*Developed to improve operational efficiency in extreme field conditions.*

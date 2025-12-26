# 📊 MappingOP: Field Operator Mobile Workspace

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Architecture](https://img.shields.io/badge/Clean%20Architecture-MVVM-blue?style=for-the-badge)

[cite_start]**MappingOP** is a high-tech solution designed to automate the work of field crews operating in active conflict zones. [cite: 41] [cite_start]The app completely replaces paper-based worksheets, ensuring stable operation even in areas with no internet coverage and GPS interference. [cite: 44, 47]

---

## 🛣 Project Roadmap (Дорожная карта проекта)

We have divided the development into three key stages. Currently, we are transitioning from stabilizing the foundation to UI cleanup and optimization.

### 🇷🇺 Russian Version
![Roadmap RU](https://raw.githubusercontent.com/RoninSoulKh/MappingOP/main/Ru.jpg)

### 🇬🇧 English Version
![Roadmap EN](https://raw.githubusercontent.com/RoninSoulKh/MappingOP/main/En.jpg)

---

## 🎯 Problems & Solutions (Проблемы и решения)

[cite_start]In field conditions, paper worksheets often get damaged by weather, and human error leads to incorrect dates or skipped addresses. [cite: 42, 43]
**MappingOP** solves this through:
* [cite_start]**Offline-first Architecture**: Full functionality without internet using Room DB. [cite: 32, 47]
* [cite_start]**Automatic Logging**: Precise time and date tracking for every processed object. [cite: 46, 50]
* [cite_start]**Digital Mapping**: Rapid building search even when GPS is suppressed. [cite: 4, 45]

---

## ✨ Core Features (Основные возможности)

### 1. Smart Worksheet Import (Импорт ведомостей)
[cite_start]The system automatically parses Excel files (.xlsx), creating convenient consumer cards with all necessary data: account ID, address, debt amount, and meter number. [cite: 15, 48]

### 2. Offline Mapping (Оффлайн-карты)
[cite_start]Integration with **OpenStreetMap (OSM)** and **Visicom API** allows viewing building outlines and numbers even in offline mode. [cite: 5, 52]
* [cite_start]**Visualization**: Buildings from the worksheet are highlighted in red on the map. [cite: 3, 52]

### 3. Result Recording (Фиксация результатов)
[cite_start]User-friendly interface for entering meter readings, building status, and processing types (hand-delivered, refusal, etc.) with automatic database saving. [cite: 17, 50]

---

## ⚖️ Legal & Licensing (Юридическая чистота)
The project is developed with strict adherence to copyright laws:
* **Mapping**: Data from **OpenStreetMap** (ODbL license).
* [cite_start]**API**: **Visicom API** usage is strictly within free limits (1500 req/day) without prohibited geometry caching. [cite: 1]
* **Open Source**: All libraries use **MIT** or **Apache 2.0** licenses.
* **Typography**: **Google Fonts** (OFL license).

---

## 🏗 Tech Stack (Технологический стек)
* [cite_start]**Language**: Kotlin. 
* [cite_start]**UI Framework**: Jetpack Compose. 
* [cite_start]**Database**: Room (Offline-first). [cite: 31, 60]
* [cite_start]**Architecture**: MVVM. 
* [cite_start]**Document Processing**: Apache POI. [cite: 48]
* [cite_start]**Languages**: Ukrainian (Primary), English, Russian. [cite: 21]

---

## 🚀 Next Steps (Ближайшие планы)
* **Refactoring**: Cleaning code from technical comments and optimizing Room structures.
* [cite_start]**Coordinates**: Adding Lat/Lng fields for future map visualization. [cite: 52, 60]
* [cite_start]**Profile**: Creating a user tab with settings and legal information. [cite: 24, 58]

---
*Developed to improve operational efficiency in extreme field conditions.*

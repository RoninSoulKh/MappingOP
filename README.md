# MappingOP 🗺️⚡ v2.1 — Digital Field Operations Platform

![Version](https://img.shields.io/badge/Version-2.1.0-cyan?style=for-the-badge&logo=appveyor)
![Platform](https://img.shields.io/badge/Platform-Android-brightgreen?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blueviolet?style=for-the-badge&logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6%2B-blue?style=for-the-badge)
![Architecture](https://img.shields.io/badge/Clean%20Architecture-MVVM-orange?style=for-the-badge)

**MappingOP** — профессиональная оффлайн-платформа для цифровой трансформации полевых операций.

Приложение создано для реальных бригад, которые работают с большим объёмом данных в условиях ограниченного или отсутствующего интернета.

### 🔥 Ключевые возможности

- **Полный оффлайн-режим** — импорт Excel, обработка, экспорт без сети
- **OSM-карта с интеллектуальной кластеризацией** — тысячи маркеров без задержек
- **Пакетное геокодирование** через Visicom API + умное кэширование
- **Детальная обработка абонента** — фото, показания счётчика, состояние дома, тип потребителя и отработки
- **Экспорт отчётов в Excel** одним кликом
- **JWT-аутентификация** с поддержкой "запомнить меня" и смены пароля
- **Gamification в профиле** — KPI, статистика, мотивация
- **Enterprise-дизайн** — тёмная тема с неоновыми акцентами, Material You, плавные анимации

### Технологии

| Слой              | Технология                          |
|-------------------|-------------------------------------|
| UI                | Jetpack Compose + Material 3        |
| Архитектура       | Clean Architecture + MVVM           |
| База данных       | Room + Flow                         |
| Сеть              | Retrofit 2 + OkHttp + JWT           |
| Хранение токенов  | DataStore Preferences               |
| Excel             | Apache POI (полный парсинг .xlsx)   |
| Карта             | OSMDroid (оффлайн-тайлы)            |
| Геокодирование    | Visicom Data API                    |
| Безопасность      | ProGuard R8 + FileProvider          |

### Команда

- **RoninSoulKh** (@RoninSoulKh) — архитектура, Android-разработка, UI/UX, интеграции, тестирование
- **EmsFear** — backend API[](https://mappingop.biz.ua)

### Final Sprint • January 2026

![MappingOP Final Sprint Roadmap](https://raw.githubusercontent.com/RoninSoulKh/MappingOP/master/assets/roadmap_en.png)

<div align="center">
  <details>
    <summary style="cursor: pointer; color: #06B6D4; font-weight: bold; font-size: 1.1em; margin-top: 32px;">
      Просмотреть перевод / View in translate →
    </summary>

    <div style="margin-top: 24px;"></div>

    ![Дорожня карта UA](https://raw.githubusercontent.com/RoninSoulKh/MappingOP/master/assets/roadmap_ua.png)

    <p style="margin-top: 24px; color: #94A3B8; font-size: 0.95em;">
      Готовий продукт для реальних бригад — кінець січня 2026<br>
      Battle-ready version for field teams — end of January 2026
    </p>
  </details>
</div>

### Установка (для разработчиков)

```bash
git clone https://github.com/RoninSoulKh/MappingOP.git
cd MappingOP
# Добавь в local.properties:
visicom.api.key=твой_ключ
server.url=https://mappingop.biz.ua:9444/api/v1/
./gradlew assembleDebug
```
### Лицензия

**Proprietary • For Ukraine • With Love**

Приложение создано для конкретной задачи и конкретных людей.  
Открытый код — чтобы каждый мог убедиться в прозрачности и отсутствии скрытых механизмов.  
Коммерческое использование — только с согласия авторов.

---

**MappingOP v2.1** — готов к реальным полевым испытаниям в январе 2026.  
Made in Kharkiv, Ukraine 🇺🇦

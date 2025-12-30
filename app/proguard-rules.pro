# ==========================================================
# ФИНАЛЬНАЯ КОНФИГУРАЦИЯ ЗАЩИТЫ КОДА (MAPPING OP v1.0.4)
# ==========================================================

# 1. ЗАЩИТА МОДЕЛЕЙ И БАЗЫ ДАННЫХ
-keep class com.roninsoulkh.mappingop.domain.models.** { *; }
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# 2. ЗАЩИТА БИБЛИОТЕКИ EXCEL (Apache POI & XMLBeans)
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class javax.xml.** { *; }

# 3. ЗАЩИТА ВСПОМОГАТЕЛЬНЫХ БИБЛИОТЕК (Фикс IOUtils и Логгера)
-keep class org.apache.commons.** { *; }
-dontwarn org.apache.commons.**
# ВАЖНО: Защищаем NullLogger, который мы включим в коде
-keep class org.apache.poi.util.NullLogger { *; }

# 4. ЗАЩИТА ЖИЗНЕННОГО ЦИКЛА COMPOSE
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# 5. ПОДАВЛЕНИЕ ПРЕДУПРЕЖДЕНИЙ
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn schemasMicrosoftCom.**
-dontwarn org.etsi.uri.**
-dontwarn org.w3c.dom.**
-dontwarn org.xml.sax.**
-dontwarn javax.xml.**
-dontwarn java.awt.**
-dontwarn javax.accessibility.**
-dontwarn com.graphbuilder.**
-dontwarn com.github.javaparser.**
-dontwarn org.apache.logging.log4j.**
-dontwarn aQute.bnd.annotation.**
-dontwarn com.sun.org.apache.**

# Сохраняем информацию для отчетов об ошибках
-keepattributes SourceFile,LineNumberTable
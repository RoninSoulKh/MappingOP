# --- MappingOP: Release (R8 Full Mode friendly) ---

# Gson needs generic signatures + annotations (иначе ловишь LinkedTreeMap/ClassCast)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep TypeToken (для List<T>, Map<K,V> и т.п.)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Не удаляй поля, которые помечены @SerializedName
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# На всякий случай: не выкидывай модели домена (они маленькие, зато стабильно)
-keep,allowobfuscation class com.roninsoulkh.mappingop.domain.models.** { *; }

# Ворнинги по тяжелым либам
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn javax.xml.**
-dontwarn java.awt.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.commons.**
-dontwarn org.osmdroid.**

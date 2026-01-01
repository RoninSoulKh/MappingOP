import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.compose")
}

// --- ЧИТАЕМ КЛЮЧ ИЗ local.properties (Безопасно) ---
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}
val visicomKey = localProperties.getProperty("visicom.api.key") ?: ""

android {
    namespace = "com.roninsoulkh.mappingop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.roninsoulkh.mappingop"
        minSdk = 33
        targetSdk = 36
        versionCode = 8
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "app_name", "Mapping OP")

        // --- КЛЮЧ ---
        buildConfigField("String", "VISICOM_KEY", "\"$visicomKey\"")

        // Настройки Room
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.0"
    }

    applicationVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}

dependencies {
    // CORE
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // COMPOSE BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // ICONS
    implementation("androidx.compose.material:material-icons-extended:1.6.1")

    // MAPS
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // NETWORK
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // COROUTINES
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ROOM DATABASE (ИСПРАВЛЕНО ТУТ 👇)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    kaptTest("androidx.room:room-compiler:2.6.1")
    // kaptAndroidTest("androidx.room:room-compiler:2.6.1")

    // EXCEL (APACHE POI)
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // IMAGES
    implementation("io.coil-kt:coil-compose:2.5.0")

    // TESTING
    testImplementation("junit:junit:4.13.2")
    // androidTestImplementation("androidx.test.ext:junit:1.3.0")
    // androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    // androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    // androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // DEBUG
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
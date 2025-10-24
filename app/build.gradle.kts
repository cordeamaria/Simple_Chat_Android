plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.chat_app"
    // Setează compileSdk la 34, care este cea mai recentă versiune stabilă.
    // Poți reveni la 36 dacă testezi funcționalități specifice din Android 15 (beta).
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.third_try"
        minSdk = 24
        // targetSdk ar trebui să fie la fel ca compileSdk
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Este foarte recomandat să activezi minificarea pentru versiunea de producție
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Este recomandat să folosești o versiune Java mai modernă, cum ar fi 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            // Am curățat calea și am lăsat doar directorul standard pentru assets
            assets.srcDirs("src/main/assets")
        }
    }

    // Acest block este necesar pentru a rezolva conflictele de împachetare
    // care pot apărea din cauza dependențelor multiple.
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Dependențe standard AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.animated.vector.drawable)

    // --- REZOLVARE PENTRU SOCKET.IO ȘI OKHTTP ---
    // 1. Am actualizat socket.io-client și am exclus dependența sa tranzitivă (engine.io-client)
    //    pentru a o putea adăuga manual.
    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "io.socket", module = "engine.io-client")
    }

    // 2. Am adăugat engine.io-client separat, excluzând versiunea sa veche de okhttp.
    //    Acest lucru îl forțează să folosească versiunea de OkHttp 4 pe care o avem deja în proiect.
    implementation("io.socket:engine.io-client:2.1.0") {
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
    }
    // --- SFÂRȘIT REZOLVARE ---

    // Dependențele OkHttp 4 (declarate o singură dată)
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)

    // Dependențe de test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

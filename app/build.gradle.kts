plugins {
    //alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.android.application")
    id("com.google.gms.google-services")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

android {
    namespace = "com.example.logicmind"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.logicmind"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    //implementation(libs.google.firebase.auth.ktx)
    //Testy jednostkowe
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.test:rules:1.7.0")

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    implementation(libs.androidx.gridlayout)

    // Firebase BOM – zarządza wersjami
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)           // Firebase Auth
    implementation(libs.firebase.database.ktx)       // Realtime Database (jedyna zależność dla bazy)
    implementation(libs.firebase.analytics)          // Analytics

    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
    testImplementation(kotlin("test"))
}
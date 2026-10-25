plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}
android {
    namespace = "com.pyara.slgan"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pyara.slgan"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "CHATBOT_API_BASE_URL", "\"${project.findProperty("chatbotApiBaseUrl") ?: ""}\"")
            buildConfigField("String", "EMAIL_OTP_SCRIPT_URL", "\"${project.findProperty("emailOtpScriptUrl") ?: "https://script.google.com/macros/s/AKfycbzJcIIJsb_F5k0IMp0xRMy4bzPuIi1laNa33EhchPQh7arPSRogCwqdg7LWfK3ij55Y4A/exec"}\"")
        }
        getByName("release") {
            buildConfigField("String", "CHATBOT_API_BASE_URL", "\"${project.findProperty("chatbotApiBaseUrl") ?: ""}\"")
            buildConfigField("String", "EMAIL_OTP_SCRIPT_URL", "\"${project.findProperty("emailOtpScriptUrl") ?: "https://script.google.com/macros/s/AKfycbzJcIIJsb_F5k0IMp0xRMy4bzPuIi1laNa33EhchPQh7arPSRogCwqdg7LWfK3ij55Y4A/exec"}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

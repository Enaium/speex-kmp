plugins {
    alias(libs.plugins.android.application)
}

// A plain Android app (Views, no AndroidX) that exercises the JNI AAR.
// :speex-kmp's Android variant packages libspeex_jni.so for every ABI; the
// Kotlin code below only uses the public API, exactly like a consumer would.
android {
    namespace = "cn.enaium.speex.examples.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "cn.enaium.speex.examples.android"
        minSdk = 21
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":speex-kmp"))
}

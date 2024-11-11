import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.sinakamali.anix"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sinakamali.anix"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
//        resources.excludes.add("META-INF/*")
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("com.google.guava:guava:33.3.1-android")
    implementation ("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation ("org.bouncycastle:bcprov-jdk18on:1.78.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
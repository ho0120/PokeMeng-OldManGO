plugins {
    id("com.android.application")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.PokeMeng.OldManGO"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.PokeMeng.OldManGO"
        minSdk = 26
        targetSdk = 34
        versionCode = 45
        versionName = "1.0.45"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    lint {
        abortOnError = false  // 禁用 Lint 错误导致构建中止
    }
}

dependencies {
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    testImplementation(libs.junit)
    // Core libraries
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core)
    // Custom components
    implementation(libs.calendarview)           // 自定義日歷元件：https://blog.csdn.net/coffee_shop/article/details/130709029
    implementation(libs.gson)                   // 自定義SharedPreferences元件：https://github.com/kcochibili/TinyDB--Android-Shared-Preferences-Turbo/tree/master
    implementation(libs.hanlp)                  // 將Ca的簡體中文改成繁體
    implementation(libs.lunar)                  // Ca的農民節日還有公曆節日
    // Firebase libraries
    implementation(platform(libs.firebase.bom)) // Firebase BOM
    implementation(libs.firebase.analytics)     // Firebase分析
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)     // Firebase資料庫
    implementation(libs.firebase.messaging)     // 回傳到Firebase跳出資料
    // Google services
    implementation(libs.play.services.auth)     // Google登陸
    implementation(libs.play.services.location)
    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    // Image loading
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
}

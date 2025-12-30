import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.lfr.baozi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lfr.baozi"
        minSdk = 24
        targetSdk = 36
        versionCode = 103
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val envBaseUrl =
            (System.getenv("PHONE_AGENT_BASE_URL") ?: System.getenv("BAOZI_DEFAULT_BASE_URL"))
                ?.trim()
                .orEmpty()
        val defaultBaseUrl =
            if (envBaseUrl.isNotBlank()) envBaseUrl else "http://127.0.0.1:28100/v1"

        fun escapeForBuildConfig(value: String): String =
            value.replace("\\", "\\\\").replace("\"", "\\\"")

        buildConfigField("String", "DEFAULT_BASE_URL", "\"${escapeForBuildConfig(defaultBaseUrl)}\"")
    }

    signingConfigs {
        create("release") {
            // 读取签名配置（从 local.properties）
            val keystorePropertiesFile = rootProject.file("local.properties")
            if (keystorePropertiesFile.exists()) {
                val props = Properties()
                keystorePropertiesFile.reader().use {
                    props.load(it)
                }
                
                val keystorePath = props.getProperty("keystore.path")
                val keystorePassword = props.getProperty("keystore.password")
                val keyAliasName = props.getProperty("key.alias")
                val keyPassword = props.getProperty("key.password")
                
                if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() 
                    && !keyAliasName.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
                    storeFile = file(keystorePath)
                    storePassword = keystorePassword
                    keyAlias = keyAliasName
                    this.keyPassword = keyPassword
                }
            }
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            // 只有在签名配置存在时才使用
            if (signingConfigs.findByName("release")?.storeFile?.exists() == true) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Enable Java 8+ API desugaring
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Data Storage
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.room.compiler)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Shizuku SDK for advanced permissions
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

plugins { id("com.android.application") version "8.13.1" }
android {
    namespace = "dev.still.fixture"
    compileSdk = 36
    defaultConfig { applicationId = "dev.still.fixture"; minSdk = 28; targetSdk = 36; versionCode = 2; versionName = "2" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

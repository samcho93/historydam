plugins {
    alias(libs.plugins.yeoksadam.android.library)
    alias(libs.plugins.yeoksadam.android.compose)
}

android {
    namespace = "com.samdori93.yeoksadam.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:domain"))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    api(libs.coil.compose)
    // AR·대화 화면 공용: 후면 카메라 배경
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

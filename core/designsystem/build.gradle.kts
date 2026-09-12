plugins {
    id("vimusic.android.library.compose")
    id("vimusic.android.hilt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.materialkolor)
    implementation(libs.androidx.palette)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.material.icons.extended)
}

plugins {
    id("vimusic.android.library")
    id("vimusic.android.hilt")
}

dependencies {
    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}

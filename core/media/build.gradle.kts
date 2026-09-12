plugins {
    id("vimusic.android.library")
    id("vimusic.android.hilt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:innertube"))
    implementation(project(":core:datastore"))

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.database)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.kotlinx.coroutines.android)
}

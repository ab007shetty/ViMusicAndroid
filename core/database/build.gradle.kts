plugins {
    id("vimusic.android.library")
    id("vimusic.android.hilt")
    alias(libs.plugins.ksp)
}

dependencies {
    api(project(":core:model"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.room.testing)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

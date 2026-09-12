import java.io.FileInputStream
import java.util.Properties

plugins {
    id("vimusic.android.library")
    id("vimusic.android.hilt")
    alias(libs.plugins.ksp)

    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
fun secret(key: String): String = localProps.getProperty(key) ?: System.getenv(key) ?: ""

android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"" + secret("SUPABASE_URL") + "\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"" + secret("SUPABASE_ANON_KEY") + "\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"" + secret("GOOGLE_WEB_CLIENT_ID") + "\"")
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:innertube"))
    implementation(project(":core:datastore"))

    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
}

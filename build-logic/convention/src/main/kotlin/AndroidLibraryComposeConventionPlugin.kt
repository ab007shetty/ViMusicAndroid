import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("vimusic.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        extensions.configure<LibraryExtension> { buildFeatures.compose = true }

        dependencies {
            val bom = libs.findLibrary("compose-bom").get()
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", libs.findLibrary("compose-material3").get())
            add("implementation", libs.findLibrary("compose-ui").get())
            add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
            add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
            add("androidTestImplementation", libs.findLibrary("compose-ui-test-junit4").get())
            add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())
        }
    }
}

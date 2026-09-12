import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        fun v(name: String) = libs.findVersion(name).get().requiredVersion.toInt()

        extensions.configure<LibraryExtension> {
            namespace = "com.abshetty.vimusic" + target.path.replace(":", ".")
            compileSdk = v("compileSdk")
            defaultConfig {
                minSdk = v("minSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
            }
            testOptions.unitTests.isIncludeAndroidResources = true
        }

        tasks.withType<Test>().configureEach {
            failOnNoDiscoveredTests.set(false)
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("truth").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}

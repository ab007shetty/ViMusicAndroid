pluginManagement {
    includeBuild("build-logic")
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "vimusic-android"

include(":app")
include(":core:model")
include(":core:database")
include(":core:datastore")
include(":core:innertube")
include(":core:data")
include(":core:media")
include(":core:designsystem")
include(":feature:library")
include(":feature:search")
include(":feature:player")

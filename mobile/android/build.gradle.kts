allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
// Les greffons publiés (file_picker, local_auth…) figent chacun leur propre
// compileSdk, parfois plus ancien que celui exigé par leurs dépendances
// transitives. Les aligner ici évite d'attendre une nouvelle version de chaque
// paquet pour que le module Android compile.
//
// Ce bloc doit précéder `evaluationDependsOn` ci-dessous : celui-ci force
// l'évaluation des sous-projets, après quoi `afterEvaluate` n'est plus
// acceptée.
subprojects {
    afterEvaluate {
        val android = extensions.findByName("android")
        if (android is com.android.build.gradle.BaseExtension) {
            android.compileSdkVersion(36)
        }
    }
}

subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

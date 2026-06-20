// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register("checkAgentSkills") {
    group = "verification"
    description = "Checks and downloads agent skills if missing"
    
    val projectDirFile = layout.projectDirectory.asFile
    
    doLast {
        val externalDir = File(projectDirFile, ".agents/external")
        if (!externalDir.exists()) {
            externalDir.mkdirs()
        }
        
        val m3Dir = File(externalDir, "material-3-skill")
        val androidDir = File(externalDir, "android-skills")
        
        if (!m3Dir.exists() || m3Dir.list()?.isEmpty() == true) {
            println("Cloning material-3-skill from GitHub...")
            try {
                val process = ProcessBuilder("git", "clone", "https://github.com/hamen/material-3-skill.git", m3Dir.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                process.waitFor()
            } catch (e: Exception) {
                logger.error("Failed to clone material-3-skill: ${e.message}")
            }
        }
        
        if (!androidDir.exists() || androidDir.list()?.isEmpty() == true) {
            println("Cloning android-skills from GitHub...")
            try {
                val process = ProcessBuilder("git", "clone", "https://github.com/android/skills.git", androidDir.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                process.waitFor()
            } catch (e: Exception) {
                logger.error("Failed to clone android-skills: ${e.message}")
            }
        }
    }
}

tasks.register("updateAgentSkills") {
    group = "verification"
    description = "Updates agent skills to the latest version from GitHub"
    
    val projectDirFile = layout.projectDirectory.asFile
    
    doLast {
        val externalDir = File(projectDirFile, ".agents/external")
        val m3Dir = File(externalDir, "material-3-skill")
        val androidDir = File(externalDir, "android-skills")
        
        if (m3Dir.exists()) {
            println("Updating material-3-skill...")
            try {
                val process = ProcessBuilder("git", "pull")
                    .directory(m3Dir)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                println(output)
            } catch (e: Exception) {
                logger.error("Failed to update material-3-skill: ${e.message}")
            }
        }
        
        if (androidDir.exists()) {
            println("Updating android-skills...")
            try {
                val process = ProcessBuilder("git", "pull")
                    .directory(androidDir)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                println(output)
            } catch (e: Exception) {
                logger.error("Failed to update android-skills: ${e.message}")
            }
        }
    }
}

// Hook into Android Studio project synchronization / Kotlin script preparation
tasks.matching { it.name == "prepareKotlinBuildScriptModel" }.all {
    dependsOn("checkAgentSkills")
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
//
// Agent Skills Verification & Update:
// - Manuelle Überprüfung:   ./gradlew checkAgentSkills
// - Manuelles Update:       ./gradlew updateAgentSkills
// - Automatischer Sync beim Studio Sync (optional, standardmäßig deaktiviert):
//   Setze `syncAgentSkillsOnSync=true` in gradle.properties oder via `-PsyncAgentSkillsOnSync=true`
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
}

data class ExternalSkillRepo(
    val name: String,
    val url: String,
    val targetDirName: String
) : java.io.Serializable

val externalSkillRepos = listOf(
    ExternalSkillRepo(
        name = "material-3-skill",
        url = "https://github.com/hamen/material-3-skill.git",
        targetDirName = "material-3-skill"
    ),
    ExternalSkillRepo(
        name = "android-skills",
        url = "https://github.com/android/skills.git",
        targetDirName = "android-skills"
    )
)

@UntrackedTask(because = "Interacts with external Git repositories and syncs skill files")
abstract class BaseAgentSkillsTask @Inject constructor(
    @get:Internal protected val execOperations: ExecOperations,
    @get:Internal protected val fileSystem: FileSystemOperations
) : DefaultTask() {

    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @get:Internal
    abstract val repositories: ListProperty<ExternalSkillRepo>

    protected fun runGitCommand(vararg args: String, workingDir: File? = null): Boolean {
        return try {
            val result = execOperations.exec {
                commandLine("git", *args)
                if (workingDir != null) {
                    workingDir(workingDir)
                }
                isIgnoreExitValue = true
            }
            if (result.exitValue != 0) {
                logger.error("Git command failed (exit code ${result.exitValue}): git ${args.joinToString(" ")}")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            logger.error("Execution failed for 'git ${args.joinToString(" ")}': ${e.message}")
            false
        }
    }

    protected fun syncSkills(projectDirFile: File) {
        val externalDir = File(projectDirFile, ".agents/external")
        val skillsDir = File(projectDirFile, ".agents/skills")

        if (!externalDir.exists()) {
            return
        }
        if (!skillsDir.exists()) {
            skillsDir.mkdirs()
        }

        // Find all directories in externalDir that contain SKILL.md
        val externalSkills = mutableListOf<File>()
        externalDir.walkTopDown().forEach { file ->
            if (file.isDirectory && File(file, "SKILL.md").exists()) {
                externalSkills.add(file)
            }
        }

        val externalSkillNames = externalSkills.map { it.name }.toSet()

        // Clean up orphaned external skills in .agents/skills
        skillsDir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name !in EXCLUDED_SKILLS) {
                if (!externalSkillNames.contains(file.name)) {
                    println("Removing orphaned skill: ${file.name}")
                    fileSystem.delete {
                        delete(file)
                    }
                }
            }
        }

        // Copy external skills to .agents/skills
        externalSkills.forEach { sourceSkillDir ->
            val skillName = sourceSkillDir.name
            if (skillName in EXCLUDED_SKILLS) {
                return@forEach
            }
            val targetSkillDir = File(skillsDir, skillName)
            println("Syncing skill $skillName to ${targetSkillDir.absolutePath}")

            // Remove target if it exists to avoid merged dirty states
            fileSystem.delete {
                delete(targetSkillDir)
            }
            fileSystem.copy {
                from(sourceSkillDir)
                into(targetSkillDir)
            }
        }
    }

    companion object {
        private val EXCLUDED_SKILLS = setOf("projekt-kontext")
    }
}

@UntrackedTask(because = "Checks and clones missing agent skills from external Git repositories")
abstract class CheckAgentSkillsTask @Inject constructor(
    execOperations: ExecOperations,
    fileSystem: FileSystemOperations
) : BaseAgentSkillsTask(execOperations, fileSystem) {

    @TaskAction
    fun checkSkills() {
        val projectDirFile = projectDirectory.get().asFile
        val externalDir = File(projectDirFile, ".agents/external")
        if (!externalDir.exists()) {
            externalDir.mkdirs()
        }

        repositories.get().forEach { repo ->
            val repoDir = File(externalDir, repo.targetDirName)
            if (!repoDir.exists() || repoDir.list()?.isEmpty() == true) {
                println("Cloning ${repo.name} from GitHub...")
                runGitCommand("clone", repo.url, repoDir.absolutePath)
            }
        }

        syncSkills(projectDirFile)
    }
}

@UntrackedTask(because = "Pulls updates for agent skills from external Git repositories")
abstract class UpdateAgentSkillsTask @Inject constructor(
    execOperations: ExecOperations,
    fileSystem: FileSystemOperations
) : BaseAgentSkillsTask(execOperations, fileSystem) {

    @TaskAction
    fun updateSkills() {
        val projectDirFile = projectDirectory.get().asFile
        val externalDir = File(projectDirFile, ".agents/external")

        repositories.get().forEach { repo ->
            val repoDir = File(externalDir, repo.targetDirName)
            if (repoDir.exists()) {
                println("Updating ${repo.name}...")
                runGitCommand("pull", workingDir = repoDir)
            }
        }

        syncSkills(projectDirFile)
    }
}

tasks.register<CheckAgentSkillsTask>("checkAgentSkills") {
    group = "verification"
    description = "Checks and downloads agent skills if missing"
    projectDirectory.convention(layout.projectDirectory)
    repositories.convention(externalSkillRepos)
}

tasks.register<UpdateAgentSkillsTask>("updateAgentSkills") {
    group = "verification"
    description = "Updates agent skills to the latest version from GitHub"
    projectDirectory.convention(layout.projectDirectory)
    repositories.convention(externalSkillRepos)
}

// Optional hook into Android Studio project synchronization / Kotlin script preparation
if (providers.gradleProperty("syncAgentSkillsOnSync").orNull == "true" || findProperty("syncAgentSkillsOnSync") == "true") {
    tasks.matching { it.name == "prepareKotlinBuildScriptModel" }.configureEach {
        dependsOn("checkAgentSkills")
    }
}
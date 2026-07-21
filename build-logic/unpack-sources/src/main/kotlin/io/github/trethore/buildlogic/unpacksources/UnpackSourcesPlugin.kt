package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync

@Suppress("unused")
class UnpackSourcesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val references = project.extensions.create(
            UnpackSourcesConstants.REFERENCES_EXTENSION_NAME,
            ReferencesExtension::class.java,
        )

        val cfrConfiguration = UnpackConfigurations.createCfrConfiguration(project)

        project.tasks.register("cleanUnpackedSources", Delete::class.java) {
            group = UnpackSourcesConstants.TASK_GROUP
            description = "Deletes the generated references directory."
            delete(project.rootProject.layout.projectDirectory.dir(UnpackSourcesConstants.REFERENCES_DIR_NAME))
        }

        val unpackSources = project.tasks.register("unpackSources", UnpackSourcesTask::class.java) {
            group = UnpackSourcesConstants.TASK_GROUP
            description = "Unpacks selected dependency sources and Git references into references/."
            dependencyCoordinates.convention(emptyList())
            gitReferences.set(project.providers.provider { references.gitReferences.map(GitReference::serialize) })
            unpackNestedJars.set(project.providers.provider { references.unpackNestedJars })
            cfrClasspath.from(cfrConfiguration)
            outputDirectory.set(
                project.rootProject.layout.projectDirectory.dir(UnpackSourcesConstants.REFERENCES_DIR_NAME)
            )
            rootDirectory.set(project.rootProject.layout.projectDirectory)
            outputs.upToDateWhen {
                gitReferences.get().map(GitReference::deserialize).all { reference ->
                    reference.commit != null
                }
            }
        }

        val configuredCoordinates = mutableSetOf<String>()
        project.allprojects.forEach { targetProject ->
            val dependencyArtifactsDirectory = targetProject.layout.buildDirectory.dir(
                UnpackSourcesConstants.DEPENDENCY_ARTIFACTS_DIR
            )
            val stageDependencyArtifacts = targetProject.tasks.register(
                UnpackSourcesConstants.STAGE_DEPENDENCY_ARTIFACTS_TASK_NAME,
                Sync::class.java,
            ) {
                group = null
                description = "Stages dependency artifacts selected for source browsing."
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                into(dependencyArtifactsDirectory)
            }
            unpackSources.configure {
                dependencyArtifacts.from(stageDependencyArtifacts)
            }

            val unpackConfiguration = UnpackConfigurations.createUnpackConfiguration(targetProject)
            unpackConfiguration.dependencies.configureEach {
                require(this is ExternalModuleDependency) {
                    "unpack only supports external module dependencies; " +
                        "${targetProject.path} declared ${javaClass.simpleName}."
                }
                val coordinate = ModuleCoordinate.from(this)
                if (configuredCoordinates.add(coordinate.label)) {
                    unpackSources.configure {
                        dependencyCoordinates.add(coordinate.label)
                    }
                    if (!coordinate.isMinecraft()) {
                        stageDependencyArtifacts.configure {
                            val targetPath = ReferencePaths.coordinatePath(coordinate)
                            from(binaryArtifactConfiguration(targetProject, coordinate)) {
                                into("$targetPath/binary")
                            }
                            from(sourceArtifactFiles(targetProject, coordinate)) {
                                into("$targetPath/sources")
                            }
                        }
                    }
                }
            }

            targetProject.configurations.configureEach {
                if (name == UnpackSourcesConstants.LOOM_MINECRAFT_ARTIFACT_CONFIGURATION) {
                    val minecraftConfiguration = this
                    val stageMinecraftArtifact = targetProject.tasks.register(
                        UnpackSourcesConstants.STAGE_MINECRAFT_ARTIFACT_TASK_NAME,
                        Sync::class.java,
                    ) {
                        group = null
                        description = "Stages the Loom Minecraft artifact for source browsing."
                        from(minecraftConfiguration)
                        include("minecraft*.jar")
                        into(targetProject.layout.buildDirectory.dir("unpackSources/minecraftArtifact"))
                    }
                    unpackSources.configure {
                        loomMinecraftArtifacts.from(stageMinecraftArtifact)
                    }
                }
            }
        }
    }

    private fun binaryArtifactConfiguration(
        project: Project,
        coordinate: ModuleCoordinate,
    ) = project.configurations.detachedConfiguration(
        project.dependencies.create(coordinate.label) as ModuleDependency
    ).apply {
        isTransitive = false
    }

    private fun sourceArtifactFiles(
        project: Project,
        coordinate: ModuleCoordinate,
    ) = project.configurations.detachedConfiguration(
        project.dependencies.create("${coordinate.label}:sources@jar") as ModuleDependency
    ).apply {
        isTransitive = false
    }.incoming.artifactView {
        isLenient = true
    }.files

    private fun ModuleCoordinate.isMinecraft(): Boolean {
        return group == "com.mojang" && name == "minecraft"
    }
}

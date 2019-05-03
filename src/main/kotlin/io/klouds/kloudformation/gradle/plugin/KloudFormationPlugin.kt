package io.klouds.kloudformation.gradle.plugin

import io.hexlabs.kloudformation.runner.S3Syncer
import io.hexlabs.kloudformation.runner.StackBuilder
import io.hexlabs.kloudformation.runner.generateKey
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.toJson
import io.kloudformation.toYaml
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import java.io.File

open class KloudFormationConfiguration {
    @Input
    var stacks: List<Stack> = listOf()
}

data class Stack(
        var stackName: String,
        var templateArguments: MutableMap<String, String> = mutableMapOf(),
        var template: (args: Map<String, String>) -> KloudFormationTemplate = {_ -> KloudFormationTemplate.create {}},
        var output: String? = null,
        var directoryPath: String = "build/template/",
        var fileName: String = "template",
        var format: TemplateFormat = TemplateFormat.YAML,
        var region: String? =  System.getenv("AWS_DEFAULT_REGION") ?: "",
        var uploadDeploymentResources: Boolean = false,
        var uploadLocation: String? = null,
        var uploadKey: String? = null,
        var uploadBucket: String? = null
)

enum class TemplateFormat {
    YAML,
    JSON
}

const val TASK_GROUP = "kloudformation"

class KloudFormationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create<KloudFormationConfiguration>(
                "kloudformation",
                KloudFormationConfiguration::class.java
        )

        project.tasks.register("listStacks") {
            it.group = TASK_GROUP
            it.doLast {
                project.extensions.configure(KloudFormationConfiguration::class.java) { configuration ->
                    configuration.stacks.map { stack ->
                        if (stack.region.isNullOrEmpty()) throw IllegalArgumentException("Stack ${stack.stackName} region undefined")
                        StackBuilder(stack.region!!).listStacks()
                    }
                }
            }
        }

        project.tasks.register("uploadDeploymentResources") {
            it.group = TASK_GROUP
            it.doLast {
                project.extensions.configure(KloudFormationConfiguration::class.java) { configuration ->
                    configuration.stacks.map{ stack ->
                        if (stack.uploadDeploymentResources && stack.uploadBucket == null) throw IllegalArgumentException("Stack ${stack.stackName} upload bucket undefined")
                    }
                    configuration.stacks.map{ stack ->
                        if (stack.uploadDeploymentResources && stack.region.isNullOrEmpty()) throw IllegalArgumentException("Stack ${stack.stackName} region undefined")
                    }
                    configuration.stacks.map { stack ->
                        if(stack.uploadDeploymentResources) {
                            val uploadLocation = stack.uploadLocation ?: project.buildDir.path
                            val uploadKey = stack.uploadKey ?: generateKey(uploadLocation)
                            S3Syncer(stack.region!!).uploadCodeDirectory(uploadLocation, stack.uploadBucket!!, uploadKey)
                            stack.templateArguments["codeLocation"] = uploadKey
                        }
                    }
                }
            }
        }

        project.tasks.register("generateTemplates") {
            it.group = TASK_GROUP
            it.dependsOn += "uploadDeploymentResources"
            it.doLast {
                project.extensions.configure(KloudFormationConfiguration::class.java) { configuration ->
                    configuration.stacks.map {stack ->
                        stack.template(stack.templateArguments).run {
                            File(stack.directoryPath).run {
                                mkdirs()
                                File(
                                        stack.directoryPath,
                                        "${stack.fileName}${if (stack.format === TemplateFormat.JSON) ".json" else ".yaml"}"
                                ).run {
                                    writeText(if (stack.format === TemplateFormat.JSON) toJson() else toYaml())
                                }
                            }
                        }
                    }
                }
            }
        }

        project.tasks.register("deployStacks") {
            it.group = TASK_GROUP
            it.dependsOn += "generateTemplates"
            it.doLast {
                project.extensions.configure(KloudFormationConfiguration::class.java) { configuration ->
                    configuration.stacks.map {stack ->
                        if (stack.region.isNullOrEmpty()) throw IllegalArgumentException("Stack ${stack.stackName} region undefined")
                        val templateFile = File(stack.directoryPath, "${stack.fileName}${if (stack.format === TemplateFormat.JSON) ".json" else ".yaml"}")
                        if (!templateFile.exists()) throw IllegalArgumentException("Stack ${stack.stackName} could not find template file $templateFile")
                        StackBuilder(stack.region!!).createOrUpdate(stack.stackName, templateFile.readText())?.let { outputs ->
                            stack.output?.let { outputPath -> File(outputPath) }.also { outputFile ->
                                outputFile?.parentFile?.mkdirs()
                                outputFile?.writeText(outputs.outputs.toList().joinToString(separator = "\n") { (key, value) -> "$key=$value" })
                            }
                        }
                    }
                }
            }
        }
    }
}
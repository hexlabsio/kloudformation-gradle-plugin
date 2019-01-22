package io.klouds.kloudformation.gradle.plugin

import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.toJson
import io.kloudformation.toYaml
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

open class KloudFormationConfiguration {
    var template: KloudFormationTemplate = KloudFormationTemplate.create {}
    var directoryPath: String = "build/template/"
    var fileName: String = "template"
    var format: TemplateFormat = TemplateFormat.YAML
}

enum class TemplateFormat {
    YAML,
    JSON
}

class KloudFormationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create<KloudFormationConfiguration>(
                "kloudformation",
                KloudFormationConfiguration::class.java
        ).run {
            project.task("generateTemplate").doLast {
                template.run {
                    File(directoryPath).run {
                        mkdirs()
                        File(directoryPath, "$fileName${if (format === TemplateFormat.JSON) ".json" else ".yaml"}").run {
                            writeText(if (format === TemplateFormat.JSON) toJson() else toYaml())
                        }
                    }
                }
            }
        }
    }
}
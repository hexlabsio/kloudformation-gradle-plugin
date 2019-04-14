import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun version(): String {
    val buildNumber = System.getProperty("BUILD_NUM")
    val version = "0.1" + if (buildNumber.isNullOrEmpty()) "-SNAPSHOT" else ".$buildNumber"
    println("building version $version")
    return version
}

val projectVersion = version()
val projectDescription = """KloudFormation Gradle Plugin"""

group = "io.klouds.kloudformation.gradle.plugin"
version = projectVersion
description = projectDescription

plugins {
    kotlin("jvm") version "1.3.10"
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.10.0"
}

repositories {
   jcenter()
   mavenCentral()
}

dependencies {
   implementation(kotlin("stdlib-jdk8"))
   implementation(gradleApi())
   api("io.kloudformation:kloudformation:0.1.120")
   testImplementation("junit:junit:4.12")
}

tasks.withType<KotlinCompile> {
   kotlinOptions.jvmTarget = "1.8"
}

pluginBundle {
    website = "https://www.klouds.io/"
    vcsUrl = "https://github.com/hexlabsio/kloudformation-gradle-plugin.git"
    tags = listOf("klouds", "kloudformation", "cloudformation", "kloud")
}

gradlePlugin {
   plugins {
       create("kloudformation") {
           id = "io.klouds.kloudformation.gradle.plugin"
           displayName = "Kloud Formation Gradle Plugin"
           description = "A Kloud Formation Gradle plugin to generate an Amazon Web Service Cloud Formation template using a Kotlin based Domain Specific Language"
           implementationClass = "io.klouds.kloudformation.gradle.plugin.KloudFormationPlugin"
       }
   }
}
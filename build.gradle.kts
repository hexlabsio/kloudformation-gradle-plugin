import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
   kotlin("jvm") version "1.3.10"
   `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.10.0"
}

group = "io.klouds.kloudformation.gradle.plugin"
version = "1.6-SNAPSHOT"

repositories {
   jcenter()
   mavenCentral()
}

dependencies {
   implementation(kotlin("stdlib-jdk8"))
   implementation(gradleApi())
   api("io.kloudformation:kloudformation:0.1.53")
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
           description = ""
           implementationClass = "io.klouds.kloudformation.gradle.plugin.KloudFormationPlugin"
       }
   }
}
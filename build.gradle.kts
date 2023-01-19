/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    checkstyle
    `maven-publish`
    signing
    `java-library`
    `version-catalog`
    id("org.gradle.crypto.checksum") version "1.4.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.gradle.plugin-publish") version "1.1.0" apply false
}

val groupId: String by project
val defaultVersion: String by project

var actualVersion: String = (project.findProperty("version") ?: defaultVersion) as String
if (actualVersion == "unspecified") {
    actualVersion = defaultVersion
}


buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        val edcGradlePluginsVersion: String by project
        classpath("org.eclipse.edc.edc-build:org.eclipse.edc.edc-build.gradle.plugin:${edcGradlePluginsVersion}")
    }
}

allprojects {
    apply(plugin = "org.eclipse.edc.edc-build")
    apply(plugin = "maven-publish")
    version = actualVersion
    group = groupId

    // for all gradle plugins:
    pluginManager.withPlugin("java-gradle-plugin") {
        apply(plugin = "com.gradle.plugin-publish")
    }
    if (!project.hasProperty("skip.signing")) {
        apply(plugin = "signing")

        //set the deploy-url only for java libraries
        val deployUrl =
            if (actualVersion.contains("SNAPSHOT")) "https://oss.sonatype.org/content/repositories/snapshots/"
            else "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
        publishing {
            repositories {
                maven {
                    name = "OSSRH"
                    setUrl(deployUrl)
                    credentials {
                        username = System.getenv("OSSRH_USER") ?: return@credentials
                        password = System.getenv("OSSRH_PASSWORD") ?: return@credentials
                    }
                }
            }

            signing {
                useGpgCmd()
                sign(publishing.publications)
            }
        }

    }
    // for all java libs:
    pluginManager.withPlugin("java-library") {


        java {
            val javaVersion = 11
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(javaVersion))
            }
            tasks.withType(JavaCompile::class.java) {
                // making sure the code does not use any APIs from a more recent version.
                // Ref: https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
                options.release.set(javaVersion)
            }
            withJavadocJar()
            withSourcesJar()
        }

    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
        }
    }

    repositories {
        mavenCentral()
    }

//    afterEvaluate {
//        // values needed for publishing
//        val pluginsWebsiteUrl: String by project
//        val pluginsDeveloperId: String by project
//        val pluginsDeveloperName: String by project
//        val pluginsDeveloperEmail: String by project
//        val pluginsScmConnection: String by project
//        val pluginsScmUrl: String by project
//        publishing {
//            publications.forEach { i ->
//                val mp = (i as MavenPublication)
//                mp.pom {
//                    name.set(project.name)
//                    description.set("edc :: ${project.name}")
//                    url.set(pluginsWebsiteUrl)
//
//                    licenses {
//                        license {
//                            name.set("The Apache License, Version 2.0")
//                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                        }
//                        developers {
//                            developer {
//                                id.set(pluginsDeveloperId)
//                                name.set(pluginsDeveloperName)
//                                email.set(pluginsDeveloperEmail)
//                            }
//                        }
//                        scm {
//                            connection.set(pluginsScmConnection)
//                            url.set(pluginsScmUrl)
//                        }
//                    }
//                }
////                println("\nset POM for: ${mp.groupId}:${mp.artifactId}:${mp.version}")
//            }
//        }
//    }

    tasks.withType<Jar> {
        metaInf {
            from("${rootProject.projectDir.path}/NOTICE.md")
            from("${rootProject.projectDir.path}/LICENSE")
        }
    }
}

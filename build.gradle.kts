plugins {
    id("fabric-loom") version "1.4-SNAPSHOT"
}

operator fun Project.get(key: String) = extra[key] as String

version = project["mod_version"]
group = project["maven_group"]
base.archivesName = project["archives_base_name"]

repositories {
    maven("https://maven.parchmentmc.org") {
        name = "ParchmentMC"
    }
    maven("https://jitpack.io")
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") {
                name = "Modrinth"
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${project["minecraft_version"]}")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings {
            nameSyntheticMembers = true
        }
        parchment("org.parchmentmc.data:parchment-1.20.2:2023.10.22@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${project["loader_version"]}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${project["fabric_version"]}")

    include(implementation("com.github.manevolent:ffmpeg4j:5.1.2-1.5.8-4")!!)
    include("org.bytedeco:ffmpeg:5.1.2-1.5.8")
    include("org.bytedeco:javacpp:1.5.8")
    include("org.bytedeco:ffmpeg-platform:5.1.2-1.5.8")
    include("org.bytedeco:javacpp-platform:1.5.8")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project["minecraft_version"])
    inputs.property("loader_version", project["loader_version"])
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project["minecraft_version"],
            "loader_version" to project["loader_version"]
        )
    }
}

val targetJavaVersion = 17
tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release = targetJavaVersion
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}"}
    }
}

afterEvaluate {
    println(configurations.runtimeClasspath.get().files.joinToString())
}

import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`

    id("com.github.johnrengelman.shadow") version "8.1.1" // Shades and relocates dependencies, See https://imperceptiblethoughts.com/shadow/introduction/
    id("com.diffplug.spotless") version "6.20.0"
    id("xyz.jpenilla.run-paper") version "2.1.0" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0" // Automatic plugin.yml generation

    idea
    eclipse
}

group = "net.arcaniax.gopaint"
version = "1.0.0"
description = "GoPaint is a plugin that's designed to simplify painting inside of Minecraft"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17)) // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://libraries.minecraft.net/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    annotationProcessor("org.jetbrains:annotations:24.0.1")

    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    implementation("io.papermc:paperlib:")
    compileOnly("com.mojang:authlib:1.5.25")

    implementation(platform("com.intellectualsites.bom:bom-newest:1.34"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }

    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("org.bstats:bstats-base:3.0.2")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(8)
        options.compilerArgs.addAll(arrayListOf("-Xlint:all", "-Xlint:-processing", "-Xdiags:verbose"))
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    shadowJar {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")

        // Shadow classes
        // helper function to relocate a package into our package
        fun reloc(originPkg: String, targetPkg: String) = relocate(originPkg, "${project.group}.deps.${targetPkg}")

        reloc("net.lingala.zip4j", "zip4j")
        reloc("org.bstats", "metrics")
        reloc("io.papermc.lib", "paperlib")
    }

    runServer {
        // Configure the Minecraft version for our task.
        minecraftVersion("1.20.1")

        // IntelliJ IDEA debugger setup: https://docs.papermc.io/paper/dev/debugging#using-a-remote-debugger
        jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
        systemProperty("terminal.jline", false)
        systemProperty("terminal.ansi", true)
    }
}

bukkit {// Options: https://github.com/Minecrell/plugin-yml#bukkit
    // Plugin main class (required)
    main = "${project.group}.GoPaintPlugin"

    // Plugin Information
    name = "GoPaint"
    prefix = "GP"
    version = "${project.version}"
    description = "${project.description}"
    website = "https://github.com/milkdrinkers/GoPaintContinued"
    authors = listOf("Arcaniax")
    contributors = listOf("darksaid98")
    apiVersion = "1.13"

    // Misc properties
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD // STARTUP or POSTWORLD
    depend = listOf("FastAsyncWorldEdit")
    softDepend = listOf()

    commands {
        register("gopaint") {
            description = "goPaint command"
            aliases = listOf("gp")
        }
    }

    permissions {
        register("gopaint.use") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("gopaint.admin") {
            default = BukkitPluginDescription.Permission.Default.FALSE
        }
        register("gopaint.world.bypass") {
            default = BukkitPluginDescription.Permission.Default.FALSE
        }
    }
}

configurations.all {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}

spotless {
    java {
        licenseHeaderFile(rootProject.file("HEADER.txt"))
        targetExclude("**/XMaterial.java")
        target("**/*.java")
    }
}
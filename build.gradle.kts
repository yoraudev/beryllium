import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.21"
}
loom {
	runs {
		configureEach {
			ideConfigGenerated(false)
		}
	}
}


val modId = providers.gradleProperty("mod_id").get()
val modName = providers.gradleProperty("mod_name").get()
val modVersion = providers.gradleProperty("mod_version").get()

version = modVersion

repositories {
	maven("https://jitpack.io")
}

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	val jsvg = providers.gradleProperty("jsvg_version").get()
	implementation("com.github.weisj:jsvg:$jsvg")
	include("com.github.weisj:jsvg:$jsvg")

	testImplementation(kotlin("test"))
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

subprojects {
	apply(plugin = "net.fabricmc.fabric-loom")
	apply(plugin = "org.jetbrains.kotlin.jvm")

	dependencies {
		"minecraft"("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
		"implementation"("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
		"implementation"("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")
	}
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}

tasks.register("runSelftest") {
	group = "verification"
	dependsOn(":beryllium-test-mod:runSelftest")
}

val copyRendererJar = tasks.register<Copy>("copyRendererJar") {
	group = "build"
	description = "Copies the renderer-only artifact into build/libs next to the full artifact."
	dependsOn(":beryllium-renderer:build")
	from(project(":beryllium-renderer").layout.buildDirectory.dir("libs")) {
		include("beryllium-renderer-*.jar")
	}
	into(layout.buildDirectory.dir("libs"))
}

tasks.named("build") {
	dependsOn(copyRendererJar)
}

val nativeDir = file("native")
val nativeLibName = when {
	org.gradle.internal.os.OperatingSystem.current().isWindows -> "beryllium_native.dll"
	org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "libberyllium_native.dylib"
	else -> "libberyllium_native.so"
}
val nativeTag = when {
	org.gradle.internal.os.OperatingSystem.current().isWindows -> "windows-x86_64"
	org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "macos-x86_64"
	else -> "linux-x86_64"
}
val nativeLibFile = nativeDir.resolve("target/release/$nativeLibName")

val buildNativeFont = tasks.register<Exec>("buildNativeFont") {
	workingDir = nativeDir
	commandLine("cargo", "build", "--release")
	inputs.dir(nativeDir.resolve("src"))
	inputs.file(nativeDir.resolve("Cargo.toml"))
	inputs.file(nativeDir.resolve("Cargo.lock"))
	outputs.file(nativeLibFile)
	onlyIf {
		try {
			ProcessBuilder("cargo", "--version").start().waitFor() == 0
		} catch (e: Exception) {
			false
		}
	}
}

val copyNativeFont = tasks.register<Copy>("copyNativeFont") {
	dependsOn(buildNativeFont)
	from(nativeLibFile)
	into(layout.buildDirectory.dir("generated/native-resources/natives/$nativeTag"))
}

sourceSets.main {
	resources.srcDir(layout.buildDirectory.dir("generated/native-resources"))
}

listOf("jar", "sourcesJar").forEach { name ->
	tasks.matching { it.name == name }.configureEach { dependsOn(copyNativeFont) }
}

tasks.processResources {
	dependsOn(copyNativeFont)
	val expansionProps = mapOf(
		"modId" to modId,
		"modName" to modName,
		"modVersion" to modVersion,
	)
	inputs.properties(expansionProps)

	filesMatching(listOf("fabric.mod.json", "*.mixins.json")) {
		expand(expansionProps)
	}
}

val modIdProvider = providers.gradleProperty("mod_id")
val modNameProvider = providers.gradleProperty("mod_name")
val modVersionProvider = providers.gradleProperty("mod_version")

val generateModConstants = tasks.register("generateModConstants") {
	val outputDir = layout.buildDirectory.dir("generated/sources/modConstants")
	val modId = modIdProvider.get()
	val modName = modNameProvider.get()
	val modVersion = modVersionProvider.get()
	outputs.dir(outputDir)

	doLast {
		val pkgDir = outputDir.get().asFile.resolve("wtf/traumadev")
		pkgDir.mkdirs()
		pkgDir.resolve("ModConstants.kt").writeText(
			"""
			package wtf.traumadev

			object ModConstants {
			    const val MOD_ID: String = "$modId"
			    const val MOD_NAME: String = "$modName"
			    const val VERSION: String = "$modVersion"
			}
			""".trimIndent()
		)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

kotlin {
	sourceSets.getByName("main") {
		kotlin.srcDir(generateModConstants)
	}

	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	repositories {
	}
}

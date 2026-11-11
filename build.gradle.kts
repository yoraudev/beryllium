import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Exec

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
val nativeResourcesDir = layout.buildDirectory.dir("generated/native-resources")

val nativeTargets = listOf(
	Triple("x86_64-unknown-linux-gnu.2.17", "linux-x86_64", "libberyllium_native.so"),
	Triple("x86_64-pc-windows-gnu", "windows-x86_64", "beryllium_native.dll"),
	Triple("x86_64-apple-darwin", "macos-x86_64", "libberyllium_native.dylib"),
	Triple("aarch64-apple-darwin", "macos-aarch64", "libberyllium_native.dylib"),
)

val buildNativeTargets = nativeTargets.map { (target, tag, libName) ->
	val cargoCommand = listOf("cargo", "zigbuild", "--release", "--target", target)
	val cargoTargetDir = target.substringBefore(".2.17")
	val nativeSrcDir = nativeDir.resolve("src")
	val cargoTomlFile = nativeDir.resolve("Cargo.toml")
	val cargoLockFile = nativeDir.resolve("Cargo.lock")
	val libSourceFile = nativeDir.resolve("target/$cargoTargetDir/release/$libName")
	val buildNativeTarget = tasks.register<Exec>("buildNativeFont${tag.replace("-x86_64", "-x64")}") {
		group = "build"
		description = "Cross-compiles the native font library for $tag."
		workingDir = nativeDir
		commandLine = cargoCommand
		environment("MACOSX_DEPLOYMENT_TARGET", "11.0")
		inputs.dir(nativeSrcDir)
		inputs.file(cargoTomlFile)
		inputs.file(cargoLockFile)
		outputs.file(libSourceFile)
		onlyIf {
			try {
				ProcessBuilder("cargo", "--version").start().waitFor() == 0 && ProcessBuilder("zig", "version").start().waitFor() == 0
			} catch (e: Exception) {
				false
			}
		}
	}
	val copyNativeTarget = tasks.register<Copy>("copyNativeFont$tag") {
		group = "build"
		description = "Copies the $tag native font library into the generated resources."
		dependsOn(buildNativeTarget)
		from(libSourceFile)
		rename { libName }
		into(nativeResourcesDir.map { it.dir("natives/$tag") })
	}
	copyNativeTarget
}

val buildNativeFont = tasks.register("buildNativeFont") {
	group = "build"
	description = "Cross-compiles the native font library for every supported platform when cargo and zig are available."
	dependsOn(buildNativeTargets)
}

val nativeResourcesOverride = providers.gradleProperty("native_resources_dir").orNull?.let { file(it) }

sourceSets.main {
	if (nativeResourcesOverride != null) {
		resources.srcDir(nativeResourcesOverride)
	} else {
		resources.srcDir(nativeResourcesDir)
	}
}

if (nativeResourcesOverride == null) {
	listOf("jar", "sourcesJar").forEach { name ->
		tasks.matching { it.name == name }.configureEach { dependsOn(buildNativeFont) }
	}
}

tasks.processResources {
	if (nativeResourcesOverride == null) {
		dependsOn(buildNativeFont)
	}

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

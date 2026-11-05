import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val modId = "berylliumtestmod"
val modName = "Beryllium Test Mod"
val modVersion = "1.0.0"

version = modVersion

repositories {
	mavenCentral()
}

dependencies {
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	implementation(project(":"))
}

tasks.processResources {
	val expansionProps = mapOf(
		"modId" to modId,
		"modName" to modName,
		"modVersion" to modVersion,
	)
	inputs.properties(expansionProps)
	filesMatching("fabric.mod.json") {
		expand(expansionProps)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

loom {
	runs {
		register("selftest") {
			client()
			setConfigName("Selftest Client")
			runDir(rootProject.file("run-selftest").absolutePath)
			programArgs("--width", "1280", "--height", "720")
			vmArgs("-Dberyllium.selftest=true")
			ideConfigGenerated(false)
		}
	}
}

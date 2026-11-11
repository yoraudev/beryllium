import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val modId = providers.gradleProperty("mod_id").get()
val modVersion = providers.gradleProperty("mod_version").get()
val modName = "Beryllium Renderer"

version = modVersion

repositories {
	mavenCentral()
	maven("https://jitpack.io")
}

dependencies {
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

	val jsvg = providers.gradleProperty("jsvg_version").get()
	implementation("com.github.weisj:jsvg:$jsvg")
	include("com.github.weisj:jsvg:$jsvg")
}

val nativeResourcesOverride = providers.gradleProperty("native_resources_dir").orNull?.let { rootProject.file(it) }

sourceSets.main {
	kotlin.srcDir(rootProject.file("src/main/kotlin"))
	kotlin.exclude("wtf/traumadev/beryllium/ui/**")
	kotlin.srcDir(rootProject.layout.buildDirectory.dir("generated/sources/modConstants"))
	java.srcDir(rootProject.file("src/main/java"))
	if (nativeResourcesOverride != null) {
		resources.srcDir(nativeResourcesOverride)
	} else {
		resources.srcDir(rootProject.layout.buildDirectory.dir("generated/native-resources"))
	}
}

tasks.named("compileKotlin") {
	dependsOn(":generateModConstants")
}

tasks.processResources {
	if (nativeResourcesOverride == null) {
		dependsOn(":buildNativeFont")
	}
	from(rootProject.file("src/main/resources")) {
		exclude("fabric.mod.json")
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

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)
	from(rootProject.file("LICENSE")) {
		rename { "${it}_$projectName" }
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
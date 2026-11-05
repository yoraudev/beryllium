package wtf.traumadev.beryllium.render.font

import wtf.traumadev.ModConstants
import wtf.traumadev.beryllium.util.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal object NativeFont {
	private val LIBRARY_NAMES = mapOf(
		"windows-x86_64" to "beryllium_native.dll",
		"linux-x86_64" to "libberyllium_native.so",
		"macos-x86_64" to "libberyllium_native.dylib",
		"macos-aarch64" to "libberyllium_native.dylib"
	)

	val available: Boolean = load()

	external fun freeFont(handle: Long)

	external fun upEm(handle: Long): Int

	external fun verticalMetrics(handle: Long): FloatArray

	external fun glyphId(handle: Long, codePoint: Int): Int

	external fun advance(handle: Long, glyphId: Int): Float

	external fun kerning(handle: Long, left: Int, right: Int): Float

	external fun rasterizeMsdf(handle: Long, glyphId: Int, emPx: Float, rangePx: Float): ByteArray?

	fun openFont(data: ByteArray): Long =
		if (available) openFontNative(data) else 0L

	private external fun openFontNative(data: ByteArray): Long

	private fun load(): Boolean {
		val tag = nativeTag() ?: return false
		val name = LIBRARY_NAMES[tag] ?: return false
		val resource = "/natives/$tag/$name"
		val shared = Path.of(System.getProperty("java.io.tmpdir"), "beryllium-natives", tag, "${ModConstants.VERSION}-$name")
		if (Files.isRegularFile(shared)) {
			try {
				System.load(shared.toAbsolutePath().toString())
				Logger.info("Native library loaded")
				return true
			} catch (throwable: Throwable) {
				Logger.error("Existing native library at $shared is unusable, extracting a fresh copy", throwable)
			}
		}
		val stream = NativeFont::class.java.getResourceAsStream(resource)
		if (stream == null) {
			Logger.warn("Native library missing from resources: $resource")
			return false
		}
		return try {
			Files.createDirectories(shared.parent)
			stream.use { Files.copy(it, shared, StandardCopyOption.REPLACE_EXISTING) }
			System.load(shared.toAbsolutePath().toString())
			Logger.info("Native library loaded")
			true
		} catch (throwable: Throwable) {
			Logger.error("Failed to load native library from $shared", throwable)
			tryLoadUnique(resource, tag, name)
		}
	}

	private fun tryLoadUnique(resource: String, tag: String, name: String): Boolean {
		return try {
			val stream = NativeFont::class.java.getResourceAsStream(resource) ?: return false
			val unique = Files.createTempFile("beryllium-natives-$tag-", "-$name")
			stream.use { Files.copy(it, unique, StandardCopyOption.REPLACE_EXISTING) }
			System.load(unique.toAbsolutePath().toString())
			unique.toFile().deleteOnExit()
			Logger.info("Native library loaded (private copy)")
			true
		} catch (throwable: Throwable) {
			Logger.error("Failed to load native library", throwable)
			false
		}
	}

	private fun nativeTag(): String? {
		val os = System.getProperty("os.name").lowercase()
		val arch = System.getProperty("os.arch").lowercase()
		val system = when {
			os.contains("win") -> "windows"
			os.contains("mac") || os.contains("darwin") -> "macos"
			os.contains("linux") -> "linux"
			else -> return null
		}
		return when {
			arch.contains("amd64") || arch.contains("x86_64") -> "$system-x86_64"
			arch.contains("aarch64") || arch.contains("arm64") -> "$system-aarch64"
			else -> null
		}
	}
}

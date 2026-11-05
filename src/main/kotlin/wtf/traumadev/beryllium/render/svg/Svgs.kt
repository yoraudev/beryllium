package wtf.traumadev.beryllium.render.svg

import com.mojang.renderpearl.api.commands.CommandEncoder
import net.minecraft.client.Minecraft
import wtf.traumadev.beryllium.render.texture.TextureUploader
import wtf.traumadev.beryllium.util.Logger
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.ConcurrentHashMap

internal object Svgs {
	private val images = mutableListOf<SvgImage>()
	private val uploader = TextureUploader()
	private val watchedDirectories = ConcurrentHashMap.newKeySet<Path>()
	private val changedPaths = ConcurrentHashMap.newKeySet<Path>()
	private var watchService: java.nio.file.WatchService? = null
	private var thread: Thread? = null

	internal fun register(image: SvgImage) {
		images.add(image)
		val directory = image.filePath?.parent ?: return
		if (!watchedDirectories.add(directory)) return
		startWatcher()
		watchDirectory(directory)
	}

	internal fun unregister(image: SvgImage) {
		images.remove(image)
	}

	fun uploadPending(encoder: CommandEncoder) {
		if (images.isEmpty()) return
		for (image in images.toList()) image.upload(encoder, uploader)
	}

	private fun startWatcher() {
		if (watchService != null) return
		try {
			watchService = FileSystems.getDefault().newWatchService()
			thread = Thread({ watchLoop() }, "Beryllium-SvgWatch").apply {
				isDaemon = true
				start()
			}
			Logger.info("SVG file watching enabled")
		} catch (e: Exception) {
			Logger.error("Failed to start SVG file watching", e)
			watchService = null
			thread = null
		}
	}

	private fun watchDirectory(directory: Path) {
		val service = watchService ?: return
		runCatching {
			directory.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE)
		}
	}

	private fun watchLoop() {
		val service = watchService ?: return
		while (true) {
			val key = try {
				service.take()
			} catch (e: InterruptedException) {
				return
			} catch (e: ClosedWatchServiceException) {
				return
			}
			for (event in key.pollEvents()) {
				val changed = event.context() as? Path ?: continue
				val directory = key.watchable() as? Path ?: continue
				changedPaths.add(directory.resolve(changed))
			}
			key.reset()
			try {
				Thread.sleep(DEBOUNCE_MILLIS)
			} catch (e: InterruptedException) {
				return
			}
			val changes = changedPaths.toList().distinct()
			changedPaths.clear()
			if (changes.isEmpty()) continue
			val client = Minecraft.getInstance()
			if (client == null) {
				Logger.warn("Skipped SVG reload (client not ready)")
				continue
			}
			client.execute {
				for (path in changes) {
					for (image in images.toList()) {
						if (image.filePath == path && image.reload()) {
							Logger.info("Reloaded SVG '${image.label}'")
						}
					}
				}
			}
		}
	}

	private const val DEBOUNCE_MILLIS = 400L
}

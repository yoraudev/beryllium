package wtf.traumadev.beryllium.util

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object Logger {
	private const val PREFIX = "[Beryllium]"
	private val LOGGER = LoggerFactory.getLogger("beryllium")
	private val channels = ConcurrentHashMap<String, LogChannel>()

	fun info(message: String) {
		LOGGER.info("$PREFIX $message")
	}

	fun warn(message: String) {
		LOGGER.warn("$PREFIX $message")
	}

	fun error(message: String) {
		LOGGER.error("$PREFIX $message")
	}

	fun error(message: String, throwable: Throwable) {
		LOGGER.error("$PREFIX $message", throwable)
	}

	fun channel(name: String): LogChannel = channels.computeIfAbsent(name, ::LogChannel)

	fun find(name: String): LogChannel? = channels[name]

	fun channels(): List<LogChannel> = channels.values.sortedBy { it.name }

	class LogChannel internal constructor(val name: String) {
		var isEnabled: Boolean = false
			private set

		fun toggle(): Boolean {
			isEnabled = !isEnabled
			return isEnabled
		}

		fun info(message: String) {
			if (isEnabled) Logger.info("[$name] $message")
		}

		fun warn(message: String) {
			if (isEnabled) Logger.warn("[$name] $message")
		}

		fun error(message: String) {
			if (isEnabled) Logger.error("[$name] $message")
		}

		fun error(message: String, throwable: Throwable) {
			if (isEnabled) Logger.error("[$name] $message", throwable)
		}
	}
}

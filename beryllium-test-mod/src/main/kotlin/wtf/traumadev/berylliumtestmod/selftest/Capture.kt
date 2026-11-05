package wtf.traumadev.berylliumtestmod.selftest

import com.mojang.blaze3d.platform.NativeImage
import kotlin.math.abs

class Capture(val image: NativeImage, val scaleX: Float, val scaleY: Float) {
	fun pixel(gx: Float, gy: Float): Int {
		val x = (gx * scaleX).toInt().coerceIn(0, image.width - 1)
		val y = (gy * scaleY).toInt().coerceIn(0, image.height - 1)
		return image.getPixel(x, y)
	}

	fun expect(label: String, gx: Float, gy: Float, expectedArgb: Int, tolerance: Int = 24): String? {
		val actual = pixel(gx, gy)
		val diff = maxOf(
			abs(red(actual) - red(expectedArgb)),
			abs(green(actual) - green(expectedArgb)),
			abs(blue(actual) - blue(expectedArgb))
		)
		if (diff > tolerance) {
			return "$label: expected ${hex(expectedArgb)} within $tolerance, got ${hex(actual)} at gui($gx, $gy)"
		}
		return null
	}

	fun check(label: String, gx: Float, gy: Float, expectation: String, predicate: (Int) -> Boolean): String? {
		val actual = pixel(gx, gy)
		return if (predicate(actual)) null else "$label: expected $expectation, got ${hex(actual)} at gui($gx, $gy)"
	}

	fun countWhere(gx: Float, gy: Float, width: Float, height: Float, predicate: (Int) -> Boolean): Int {
		val x0 = (gx * scaleX).toInt().coerceIn(0, image.width - 1)
		val y0 = (gy * scaleY).toInt().coerceIn(0, image.height - 1)
		val x1 = ((gx + width) * scaleX).toInt().coerceIn(x0 + 1, image.width)
		val y1 = ((gy + height) * scaleY).toInt().coerceIn(y0 + 1, image.height)
		var count = 0
		for (y in y0 until y1) {
			for (x in x0 until x1) {
				if (predicate(image.getPixel(x, y))) count++
			}
		}
		return count
	}

	fun minLuminance(gx: Float, gy: Float, width: Float, height: Float): Int {
		var min = Int.MAX_VALUE
		scan(gx, gy, width, height) { min = minOf(min, luminance(it)) }
		return min
	}

	fun maxLuminanceSpread(gx: Float, gy: Float, width: Float, height: Float): Int {
		var min = Int.MAX_VALUE
		var max = Int.MIN_VALUE
		scan(gx, gy, width, height) {
			val lum = luminance(it)
			min = minOf(min, lum)
			max = maxOf(max, lum)
		}
		return if (min == Int.MAX_VALUE) 0 else max - min
	}

	private fun scan(gx: Float, gy: Float, width: Float, height: Float, action: (Int) -> Unit) {
		val x0 = (gx * scaleX).toInt().coerceIn(0, image.width - 1)
		val y0 = (gy * scaleY).toInt().coerceIn(0, image.height - 1)
		val x1 = ((gx + width) * scaleX).toInt().coerceIn(x0 + 1, image.width)
		val y1 = ((gy + height) * scaleY).toInt().coerceIn(y0 + 1, image.height)
		for (y in y0 until y1) for (x in x0 until x1) action(image.getPixel(x, y))
	}

	companion object {
		fun red(argb: Int): Int = (argb ushr 16) and 0xFF
		fun green(argb: Int): Int = (argb ushr 8) and 0xFF
		fun blue(argb: Int): Int = argb and 0xFF
		fun luminance(argb: Int): Int = (red(argb) * 3 + green(argb) * 6 + blue(argb)) / 10
		fun hex(argb: Int): String = String.format("#%08X", argb)
	}
}

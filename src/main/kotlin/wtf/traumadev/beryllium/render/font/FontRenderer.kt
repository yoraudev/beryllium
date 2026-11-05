package wtf.traumadev.beryllium.render.font

import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.draw.Renderer2D
import wtf.traumadev.beryllium.render.pipeline.Pipelines
import kotlin.math.roundToInt

class FontRenderer internal constructor(internal val face: FontFace, internal val fontId: Int, private val atlas: GlyphAtlas) {
	fun textWidth(text: String, size: Float): Float {
		val scale = size / face.unitsPerEm
		var width = 0f
		var previousId = -1
		for (codePoint in text.codePoints()) {
			val glyphId = glyphIdFor(codePoint)
			if (previousId >= 0) width += face.kerningUnits(previousId, glyphId) * scale
			width += face.advanceUnits(glyphId) * scale
			previousId = glyphId
		}
		return width
	}

	fun lineHeight(size: Float): Float = (face.ascentUnits - face.descentUnits + face.lineGapUnits) * size / face.unitsPerEm

	fun baselineOffset(size: Float): Float = face.ascentUnits * size / face.unitsPerEm

	fun draw(g: Renderer2D, text: String, x: Float, y: Float, size: Float, color: Int, shadow: Boolean = false): Float {
		if (text.isEmpty()) return 0f
		val sizePx = (size * g.guiScale).roundToInt().coerceAtLeast(1)
		if (shadow) {
			val offset = (sizePx / 8).coerceAtLeast(1) / g.guiScale
			drawRun(g, text, x + offset, y + offset, sizePx, Colors.withAlpha(Colors.BLACK, Colors.alpha(color)))
		}
		return drawRun(g, text, x, y, sizePx, color)
	}

	internal fun glyphIdFor(codePoint: Int): Int {
		val glyphId = face.glyphForCodePoint(codePoint)
		if (glyphId >= 0) return glyphId
		return face.glyphForCodePoint(FALLBACK_CODE_POINT)
	}

	private fun drawRun(g: Renderer2D, text: String, originX: Float, baselineY: Float, sizePx: Int, color: Int): Float {
		val toGui = 1f / g.guiScale
		val unitScale = sizePx.toFloat() / face.unitsPerEm
		val glyphScale = sizePx / RASTER_EM
		val originDevX = originX * g.guiScale
		val baselineDevY = baselineY * g.guiScale + face.ascentUnits * unitScale
		var penPx = 0f
		var previousId = -1
		for (codePoint in text.codePoints()) {
			val glyphId = glyphIdFor(codePoint)
			if (previousId >= 0) penPx += face.kerningUnits(previousId, glyphId) * unitScale
			val entry = atlas.getMsdfGlyph(face, fontId, glyphId)
			if (entry != null) {
				val drawX = originDevX + penPx + entry.bearingX * glyphScale
				val drawY = baselineDevY - entry.bearingY * glyphScale
				g.texturedQuad(
					atlas.pageTexture(entry.page),
					drawX * toGui,
					drawY * toGui,
					entry.width * glyphScale * toGui,
					entry.height * glyphScale * toGui,
					entry.u0,
					entry.v0,
					entry.u1,
					entry.v1,
					color,
					Pipelines.overlayMsdf
				)
			}
			penPx += entry?.advance?.times(glyphScale) ?: (face.advanceUnits(glyphId) * unitScale)
			previousId = glyphId
		}
		return penPx * toGui
	}

	companion object {
		const val FALLBACK_CODE_POINT: Int = '?'.code
		const val RASTER_EM: Float = 64f
		const val PX_RANGE: Float = 2f
	}
}

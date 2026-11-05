package wtf.traumadev.beryllium.render.font

import com.mojang.renderpearl.api.commands.CommandEncoder
import wtf.traumadev.beryllium.render.texture.Texture2D
import wtf.traumadev.beryllium.render.texture.TextureUploader
import wtf.traumadev.beryllium.util.Logger
import java.nio.ByteBuffer

internal class GlyphAtlas(private val pageSize: Int = 1024, private val padding: Int = 5) : AutoCloseable {
	class MsdfGlyphEntry(
		val page: Int,
		val u0: Float,
		val v0: Float,
		val u1: Float,
		val v1: Float,
		val width: Int,
		val height: Int,
		val bearingX: Float,
		val bearingY: Float,
		val advance: Float
	)

	private inner class Page {
		val pixels: ByteBuffer = ByteBuffer.allocateDirect(pageSize * pageSize * 4)
		val texture: Texture2D = Texture2D.create("Beryllium glyph atlas", pageSize, pageSize)
		var cursorX: Int = 0
		var cursorY: Int = 0
		var rowHeight: Int = 0
		var dirty: Boolean = false
		val pendingRegions = ArrayList<IntArray>()
	}

	private data class CacheKey(val fontId: Int, val glyphId: Int)

	private val pages = ArrayList<Page>()
	private val cache = HashMap<CacheKey, MsdfGlyphEntry>()
	private val missing = HashSet<CacheKey>()
	private var uploader: TextureUploader? = null
	private var activePageIndex = 0

	val pageCount: Int
		get() = pages.size

	val glyphCount: Int
		get() = cache.size

	fun getMsdfGlyph(face: FontFace, fontId: Int, glyphId: Int): MsdfGlyphEntry? {
		if (glyphId < 0) return null
		val key = CacheKey(fontId, glyphId)
		cache[key]?.let { return it }
		if (key in missing) return null
		val raster = face.rasterizeMsdf(glyphId, FontRenderer.RASTER_EM, FontRenderer.PX_RANGE) ?: run {
			missing.add(key)
			return null
		}

		val cellWidth = raster.width + padding * 2
		val cellHeight = raster.height + padding * 2
		if (cellWidth > pageSize || cellHeight > pageSize) {
			Logger.warn("MSDF glyph too large for atlas: ${raster.width}x${raster.height}")
			return null
		}

		val page = allocate(cellWidth, cellHeight)
		val cellX = page.cursorX
		val cellY = page.cursorY
		val contentX = cellX + padding
		val contentY = cellY + padding
		page.cursorX += cellWidth
		page.rowHeight = maxOf(page.rowHeight, cellHeight)
		page.dirty = true
		writeMsdf(page, cellX, cellY, cellWidth, cellHeight, contentX, contentY, raster)

		val entry = MsdfGlyphEntry(
			activePageIndex,
			contentX.toFloat() / pageSize,
			contentY.toFloat() / pageSize,
			(contentX + raster.width).toFloat() / pageSize,
			(contentY + raster.height).toFloat() / pageSize,
			raster.width,
			raster.height,
			raster.bearingX,
			raster.bearingY,
			raster.advance
		)
		cache[key] = entry
		return entry
	}

	fun uploadPending(encoder: CommandEncoder) {
		val staging = uploader ?: TextureUploader().also { uploader = it }
		for (page in pages) {
			if (!page.dirty) continue
			if (page.pendingRegions.isEmpty()) {
				staging.upload(encoder, page.texture, 0, 0, pageSize, pageSize, page.pixels, 0, pageSize * 4)
			} else {
				for (region in page.pendingRegions) {
					val offset = (region[1] * pageSize + region[0]) * 4
					staging.upload(encoder, page.texture, region[0], region[1], region[2], region[3], page.pixels, offset, pageSize * 4)
				}
			}
			page.pendingRegions.clear()
			page.dirty = false
		}
	}

	fun pageTexture(index: Int): Texture2D = pages[index].texture

	private fun allocate(cellWidth: Int, cellHeight: Int): Page {
		if (pages.isEmpty()) pages.add(Page())
		var page = pages[activePageIndex]
		if (page.cursorX + cellWidth > pageSize) {
			page.cursorX = 0
			page.cursorY += page.rowHeight
			page.rowHeight = 0
		}
		if (page.cursorY + cellHeight > pageSize) {
			page = newPage()
		}
		return page
	}

	private fun newPage(): Page {
		val page = Page()
		pages.add(page)
		activePageIndex = pages.size - 1
		return page
	}

	private fun writeMsdf(page: Page, cellX: Int, cellY: Int, cellWidth: Int, cellHeight: Int, contentX: Int, contentY: Int, raster: MsdfRaster) {
		val pixels = page.pixels
		for (row in 0 until cellHeight) {
			pixels.position(((cellY + row) * pageSize + cellX) * 4)
			for (column in 0 until cellWidth) {
				val insideX = column >= padding && column < padding + raster.width
				val insideY = row >= padding && row < padding + raster.height
				if (insideX && insideY) {
					val source = ((row - padding) * raster.width + (column - padding)) * 3
					pixels.put(raster.pixels[source])
					pixels.put(raster.pixels[source + 1])
					pixels.put(raster.pixels[source + 2])
				} else {
					pixels.put(0)
					pixels.put(0)
					pixels.put(0)
				}
				pixels.put(255.toByte())
			}
		}
		page.pendingRegions.add(intArrayOf(cellX, cellY, cellWidth, cellHeight))
	}

	override fun close() {
		for (page in pages) page.texture.close()
		pages.clear()
		cache.clear()
		missing.clear()
		uploader?.close()
		uploader = null
		activePageIndex = 0
	}
}

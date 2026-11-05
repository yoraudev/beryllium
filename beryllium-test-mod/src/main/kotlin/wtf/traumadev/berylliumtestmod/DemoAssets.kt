package wtf.traumadev.berylliumtestmod

import wtf.traumadev.beryllium.render.svg.SvgImage

object DemoAssets {
	val icon: SvgImage = SvgImage.ofResource("/assets/berylliumtestmod/textures/selftest.svg", "demo-icon")

	val shaderPlate: SvgImage = SvgImage.ofSource(
		"""<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64"><rect width="64" height="64" fill="#FFFFFF"/></svg>""",
		"demo-shader-plate"
	)

	val badge: SvgImage = SvgImage.ofSource(
		"""<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48"><rect x="2" y="2" width="44" height="44" rx="10" fill="#1F2433"/><circle cx="24" cy="24" r="12" fill="#5B7CFF"/><rect x="18" y="18" width="12" height="12" rx="3" fill="#FFFFFF"/></svg>""",
		"demo-badge"
	)
}
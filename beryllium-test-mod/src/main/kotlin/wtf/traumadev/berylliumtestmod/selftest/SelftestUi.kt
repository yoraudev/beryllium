package wtf.traumadev.berylliumtestmod.selftest

import wtf.traumadev.beryllium.ui.Ui
import wtf.traumadev.beryllium.ui.component.Button
import wtf.traumadev.beryllium.ui.component.Slider
import wtf.traumadev.beryllium.ui.component.TextField
import wtf.traumadev.beryllium.ui.component.Toggle
import wtf.traumadev.beryllium.ui.dsl.UiScope
import wtf.traumadev.beryllium.ui.dsl.button
import wtf.traumadev.beryllium.ui.dsl.column
import wtf.traumadev.beryllium.ui.dsl.slider
import wtf.traumadev.beryllium.ui.dsl.text
import wtf.traumadev.beryllium.ui.dsl.textField
import wtf.traumadev.beryllium.ui.dsl.toggle
import wtf.traumadev.beryllium.ui.dsl.window
import wtf.traumadev.beryllium.ui.layout.Padding
import wtf.traumadev.beryllium.ui.layout.Size

object SelftestUi : Ui("beryllium_selftest") {
	var toggle: Toggle? = null
		private set
	var slider: Slider? = null
		private set
	var button: Button? = null
		private set
	var field: TextField? = null
		private set
	var clicks: Int = 0
		private set

	fun resetState() {
		clicks = 0
	}

	fun buttonCenter(): Pair<Float, Float>? {
		val view = button ?: return null
		return (view.absoluteX + view.width / 2f) to (view.absoluteY + view.height / 2f)
	}

	override fun UiScope.build() {
		window(title = "Selftest", x = 200f, y = 100f, width = Size.Fixed(260f)) {
			column(gap = 8f, padding = Padding(10f)) {
				text("Interaction test", size = 14f)
				toggle = toggle(checked = false) { }
				slider = slider(initial = 0f) { }
				button = button("Count") { clicks++ }
				field = textField(hint = "input")
			}
		}
	}
}

package wtf.traumadev.beryllium.ui

object UiManager {
	private val uis = LinkedHashMap<String, Ui>()

	fun register(ui: Ui) {
		val id = ui.id.lowercase()
		check(!uis.containsKey(id)) { "Duplicate UI: ${ui.id}" }
		uis[id] = ui
	}

	fun unregister(ui: Ui) {
		val id = ui.id.lowercase()
		if (uis.remove(id) == null) return
		ui.close()
	}

	fun find(name: String): Ui? = uis[name.lowercase()]

	internal fun allUis(): Collection<Ui> = uis.values
}

# Beryllium

Beryllium is a **renderer and UI API** for Fabric mods on Minecraft 26.3, targeting the 26.3 renderpearl API.

## Tech Stack

- **Language:** Kotlin 2.4.21 (JVM target 25)
- **Platform:** Minecraft 26.3 — Fabric mod (`mod_id: beryllium`)
- **Mod Loader:** Fabric Loader 0.20.1
- **Libraries:** Fabric API 0.165.0+26.3, Fabric Language Kotlin 1.14.2+kotlin.2.4.21, jsvg 2.2.0 (jar-in-jar)
- **Build:** Gradle 9.8.2 with Fabric Loom 1.19-SNAPSHOT (`build.gradle.kts`, Kotlin DSL)
- **Java Compatibility:** Java 25 (source/target), JVM target 25
- **Native:** Rust cdylib `beryllium_native` (font rasterization), built by `buildNativeFont` when cargo is available

## Reference

Decompiled Minecraft 26.3 sources (Vineflower, via `genSourcesWithVineflower`) is available under `decompiled-source/` for reading and learning. The folder is gitignored — never commit it or copy code from it into mod sources.

## Docs

- `docs/` holds project documentation:
  - `docs/renderer.md` — renderer: frame integration, Renderer2D/3D, pipelines/shaders (26.3 SPIR-V location requirements), text/MSDF, SVG, native font library
  - `docs/ui.md` — UI framework: view tree, layout, hosts/input, animation, components, DSL, HUD layer
  - `docs/events.md` — event bus: subscribing, priority/cancellation rules, reserved priorities, every event and its posting hook, logger channels
  - `docs/selftest.md` — dev-only end-to-end harness in the `beryllium-test-mod/` Gradle subproject: `gradlew runSelftest` boots the client, creates a world, asserts rendered pixels, saves screenshots + report, exits nonzero on failure
  - `docs/api-notes.md` — 26.3 port deltas: renderpearl GPU types, pipeline caches, SPIR-V shader requirements, SDL input, changed vanilla signatures

## Project Rules

- Everything written in this project must be in English.
- Hand-written (non auto-generated) files must never contain comments. If any are found, remove them.
- Commits must follow the Conventional Commits specification; messages are always in English.
- Commit and push to the `dev` branch. `main` is protected and changes only through pull requests from `dev`; never push to `main` directly. Merging `dev` into `main` never deletes `dev`.
- Releases are published automatically from `main`: a push that changes the shipped sources runs `.github/workflows/release.yml`, which derives the next `v<major>.<minor>.<patch>` tag from `mod_version` in `gradle.properties` and the tags already present, builds both jars, and publishes them as a GitHub release. Changes limited to documentation, the test mod, or CI do not publish a release.
- Do not build the project unless it is genuinely necessary.
- Never launch an interactive client (`runClient`, or any Loom/IDE run that stays open waiting for input). The only client the agent may run is `runSelftest`, which drives itself end-to-end and exits. Interactive runs belong to the human; do not synthesize key/mouse input against the real desktop to drive them.
- Keep everything that must never ship in the API jar — the selftest harness and the demo entities `DemoHud`/`DemoUi` — inside the standalone `beryllium-test-mod/` subproject (mod id `berylliumtestmod`), which depends on the root project through `implementation(project(":"))` and therefore consumes Beryllium exactly like an embedding mod.
- Ship two user-facing artifacts from the same sources: the root `beryllium` jar with renderer and UI, and the `beryllium-renderer` subproject jar with renderer, text, SVG, texture, pipeline and event layers only. Both declare mod id `beryllium`. Keep `Core` renderer-only and UI initialization in `wtf.traumadev.beryllium.ui.UiCore`; renderer sources must never import `wtf.traumadev.beryllium.ui`.
- Keep implementation plumbing Kotlin `internal` — GPU pipelines and compiled objects, buffers, samplers, font atlases, native binding, prewarm, hosts, mixin hooks — so nothing outside the root module can reach it; embedders and the test mod must compile against the public surface only, including `Renderer.ready` and its parts `Renderer.fontPrewarmComplete` / `Renderer.pipelinesReady`.
- When a task is given, read in full the files listed under `Docs` above.
- Whenever the project changes, update every Markdown file in the project (not only those under `docs/`) to reflect what was added, changed, or removed.

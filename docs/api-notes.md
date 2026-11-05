# 26.3 API notes

Differences encountered while porting Beryllium to Minecraft 26.3 that affect every mod built against it.

- Blaze3D GPU types moved to `com.mojang.renderpearl.api.*` (`RenderPipeline`, `RenderPass`, `CommandEncoder`, `GpuBuffer`, textures); `RenderPass.setPipeline` now takes a `CompiledRenderPipeline` and samplers bind via `setUniform(name, view, sampler)`.
- `BindGroupLayout.Builder` has no `withSampler` — use `.withUniform(name, UniformType.COMBINED_IMAGE_SAMPLER)`.
- Mod pipelines need their own `PipelineCache` over the mod resource manager; vanilla's fallback cache only reads the vanilla pack, so `RenderSystem.getCompiledPipelineNullable` fails for mod pipelines. Beryllium already owns such a cache, so `RenderPipelines.register(...)` compiles embedder shaders without a second cache.
- Shaders compile through shaderc to SPIR-V: `#extension GL_ARB_separate_shader_objects : require` and explicit `layout(location = N)` on all in/out variables are mandatory; `gl_VertexID` is `gl_VertexIndex`.
- The client moved from GLFW to SDL: use `com.mojang.blaze3d.platform.InputConstants` constants (`PRESS`, `MOUSE_BUTTON_LEFT`, `KEY_*`, `MOD_*`); GLFW classes are not on the classpath.
- `GameRenderer.render()` is parameterless; `LevelRenderer.render` gained `consistentDepthRequired` and lost `DeltaTracker`; `MouseHandler.onMove` gained `xrel`/`yrel`.
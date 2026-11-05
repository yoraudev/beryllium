pub mod font;

use font::Font;
use font::msdf;
use jni::objects::{JByteArray, JClass};
use jni::sys::{jarray, jbyte, jfloat, jint, jlong};
use jni::JNIEnv;
use std::panic::{AssertUnwindSafe, catch_unwind};
use ttf_parser::GlyphId;

#[no_mangle]
pub extern "system" fn Java_wtf_traumadev_beryllium_render_font_NativeFont_openFontNative(
    env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jlong {
    let result = catch_unwind(AssertUnwindSafe(|| -> Option<jlong> {
        let bytes = env.convert_byte_array(&data).ok()?;
        let font = Font::open(bytes)?;
        Some(Box::into_raw(font) as jlong)
    }));
    result.ok().flatten().unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_wtf_traumadev_beryllium_render_font_NativeFont_freeFont(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }
    let pointer = handle as *mut Font;
    if !pointer.is_null() {
        drop(unsafe { Box::from_raw(pointer) });
    }
}

#[no_mangle]
pub extern "system" fn Java_wtf_traumadev_beryllium_render_font_NativeFont_upEm(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    with_font(handle, |font| font.face.units_per_em() as jint).unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_wtf_traumadev_beryllium_render_font_NativeFont_verticalMetrics(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jarray {
    let values = with_font(handle, |font| {
        [
            f32::from(font.face.ascender()),
            f32::from(font.face.descender()),
            f32::from(font.face.line_gap()),
        ]
    })
    .unwrap_or([0.0, 0.0, 0.0]);
    match env.new_float_array(3) {
        Ok(array) => {
            if env.set_float_array_region(&array, 0, &values).is_ok() {
                array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_wtf_traumadev_beryllium_render_font_NativeFont_glyphId(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    code_point: jint,
) -> jint {
    with_font(handle, |font| {
        font.glyph_id(code_point).map(|id| jint::from(id.0)).unwrap_or(-1)
    })
    .unwrap_or(-1)
}

#[no_mangle]
pub extern "system" fn Java_wtf_traumadev_beryllium_render_font_NativeFont_advance(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    glyph_id: jint,
) -> jfloat {
    with_font(handle, |font| font.advance(GlyphId(glyph_id as u16))).unwrap_or(0.0)
}

#[no_mangle]
pub extern "system" fn Java_wtf_traumadev_beryllium_render_font_NativeFont_kerning(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    left: jint,
    right: jint,
) -> jfloat {
    with_font(handle, |font| {
        font.kerning(GlyphId(left as u16), GlyphId(right as u16))
    })
    .unwrap_or(0.0)
}

#[no_mangle]
pub extern "system" fn Java_wtf_traumadev_beryllium_render_font_NativeFont_rasterizeMsdf(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    glyph_id: jint,
    em_px: jfloat,
    range_px: jfloat,
) -> jarray {
    let payload = with_font(handle, |font| {
        msdf::rasterize_msdf(font, GlyphId(glyph_id as u16), f64::from(em_px), f64::from(range_px))
    })
    .flatten();
    let Some(glyph) = payload else {
        return std::ptr::null_mut();
    };

    let mut bytes = Vec::with_capacity(20 + glyph.pixels.len());
    bytes.extend_from_slice(&glyph.width.to_le_bytes());
    bytes.extend_from_slice(&glyph.height.to_le_bytes());
    bytes.extend_from_slice(&glyph.bearing_x.to_le_bytes());
    bytes.extend_from_slice(&glyph.bearing_y.to_le_bytes());
    bytes.extend_from_slice(&glyph.advance.to_le_bytes());
    bytes.extend_from_slice(&glyph.pixels);

    match env.new_byte_array(bytes.len() as jint) {
        Ok(array) => {
            let typed = unsafe { std::mem::transmute::<&[u8], &[jbyte]>(&bytes) };
            if env.set_byte_array_region(&array, 0, typed).is_ok() {
                array.into_raw()
            } else {
                std::ptr::null_mut()
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

fn with_font<R>(handle: jlong, action: impl FnOnce(&Font) -> R) -> Option<R> {
    if handle == 0 {
        return None;
    }
    let pointer = handle as *const Font;
    if pointer.is_null() {
        return None;
    }
    catch_unwind(AssertUnwindSafe(|| unsafe { action(&*pointer) })).ok()
}

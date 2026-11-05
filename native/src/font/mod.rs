mod gpos;
pub mod msdf;

use gpos::GposKern;
use ttf_parser::GlyphId;

pub struct Font {
    pub face: ttf_parser::Face<'static>,
    pub gpos: Option<GposKern>,
}

impl Font {
    pub fn open(bytes: Vec<u8>) -> Option<Box<Font>> {
        let data: &'static [u8] = Box::leak(bytes.into_boxed_slice());
        let face = ttf_parser::Face::parse(data, 0).ok()?;
        let gpos = GposKern::find(data).and_then(|offset| GposKern::parse(data, offset));
        Some(Box::new(Font { face, gpos }))
    }

    pub fn glyph_id(&self, code_point: i32) -> Option<GlyphId> {
        char::from_u32(code_point as u32).and_then(|c| self.face.glyph_index(c))
    }

    pub fn advance(&self, glyph_id: GlyphId) -> f32 {
        self.face
            .glyph_hor_advance(glyph_id)
            .map(f32::from)
            .unwrap_or(0.0)
    }

    pub fn kerning(&self, left: GlyphId, right: GlyphId) -> f32 {
        if let Some(gpos) = &self.gpos {
            if let Some(value) = gpos.kerning(left.0, right.0) {
                return value as f32;
            }
        }
        if let Some(kern) = self.face.tables().kern {
            for subtable in kern.subtables {
                if subtable.variable || subtable.has_cross_stream || subtable.has_state_machine {
                    continue;
                }
                if let Some(value) = subtable.glyphs_kerning(left, right) {
                    return f32::from(value);
                }
            }
        }
        0.0
    }
}

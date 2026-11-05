use crate::font::Font;
use bymsdfgen_core::bitmap::Bitmap;
use bymsdfgen_core::coloring::edge_coloring_simple;
use bymsdfgen_core::geometry::{Contour, EdgeSegment, Shape};
use bymsdfgen_core::generator::{
    DistanceMapping, MsdfGeneratorConfig, Projection, SdfTransformation, generate_msdf,
};
use bymsdfgen_core::math::{Range, Vector2};
use bymsdfgen_core::raster::{FillRule, Scanline, shape_scanline};
use ttf_parser::GlyphId;

pub struct MsdfGlyph {
    pub width: u32,
    pub height: u32,
    pub bearing_x: f32,
    pub bearing_y: f32,
    pub advance: f32,
    pub pixels: Vec<u8>,
}

struct OutlineShape {
    shape: Shape,
    current: Contour,
    pending_start: Vector2,
    min_x: f64,
    max_x: f64,
    min_y: f64,
    max_y: f64,
}

impl OutlineShape {
    fn new() -> Self {
        OutlineShape {
            shape: Shape::new(),
            current: Contour::new(),
            pending_start: Vector2::ZERO,
            min_x: f64::MAX,
            max_x: f64::MIN,
            min_y: f64::MAX,
            max_y: f64::MIN,
        }
    }

    fn finish_contour(&mut self) {
        if !self.current.is_empty() {
            let contour = std::mem::take(&mut self.current);
            self.shape.add_contour(contour);
        }
    }

    fn current_end(&self) -> Vector2 {
        self.current
            .segments
            .last()
            .map(|edge| edge.point(1.0))
            .unwrap_or(self.pending_start)
    }

    fn track(&mut self, x: f64, y: f64) {
        self.min_x = self.min_x.min(x);
        self.max_x = self.max_x.max(x);
        self.min_y = self.min_y.min(y);
        self.max_y = self.max_y.max(y);
    }
}

impl ttf_parser::OutlineBuilder for OutlineShape {
    fn move_to(&mut self, x: f32, y: f32) {
        self.finish_contour();
        self.pending_start = Vector2::new(f64::from(x), f64::from(y));
        self.track(f64::from(x), f64::from(y));
    }

    fn line_to(&mut self, x: f32, y: f32) {
        let start = self.current_end();
        self.current.add_edge(EdgeSegment::line(start, Vector2::new(f64::from(x), f64::from(y))));
        self.track(f64::from(x), f64::from(y));
    }

    fn quad_to(&mut self, x1: f32, y1: f32, x: f32, y: f32) {
        let start = self.current_end();
        self.current.add_edge(EdgeSegment::quadratic(
            start,
            Vector2::new(f64::from(x1), f64::from(y1)),
            Vector2::new(f64::from(x), f64::from(y)),
        ));
        self.track(f64::from(x), f64::from(y));
    }

    fn curve_to(&mut self, x1: f32, y1: f32, x2: f32, y2: f32, x: f32, y: f32) {
        let start = self.current_end();
        self.current.add_edge(EdgeSegment::cubic(
            start,
            Vector2::new(f64::from(x1), f64::from(y1)),
            Vector2::new(f64::from(x2), f64::from(y2)),
            Vector2::new(f64::from(x), f64::from(y)),
        ));
        self.track(f64::from(x), f64::from(y));
    }

    fn close(&mut self) {
        self.finish_contour();
    }
}

pub struct GlyphShape {
    pub shape: Shape,
    pub min_x: f64,
    pub max_x: f64,
    pub min_y: f64,
    pub max_y: f64,
    pub scale: f64,
}

pub fn build_shape(font: &Font, glyph_id: GlyphId, em_px: f64) -> Option<GlyphShape> {
    let mut outline = OutlineShape::new();
    font.face.outline_glyph(glyph_id, &mut outline);
    outline.finish_contour();
    if outline.shape.edge_count() == 0 || !outline.shape.validate() {
        return None;
    }
    let mut shape = outline.shape;
    shape.normalize();
    edge_coloring_simple(&mut shape, 3.0, 0);
    Some(GlyphShape {
        shape,
        min_x: outline.min_x,
        max_x: outline.max_x,
        min_y: outline.min_y,
        max_y: outline.max_y,
        scale: em_px / f64::from(font.face.units_per_em()),
    })
}

pub fn rasterize_msdf(font: &Font, glyph_id: GlyphId, em_px: f64, range_px: f64) -> Option<MsdfGlyph> {
    if !(em_px > 1.0) || !(range_px >= 1.0) {
        return None;
    }
    let glyph_shape = build_shape(font, glyph_id, em_px)?;
    let GlyphShape { shape, min_x, max_x, min_y, max_y, scale } = glyph_shape;

    let width = (((max_x - min_x) * scale).ceil() + range_px * 2.0).clamp(1.0, 512.0) as u32;
    let height = (((max_y - min_y) * scale).ceil() + range_px * 2.0).clamp(1.0, 512.0) as u32;

    let projection = Projection::new(
        Vector2::new(scale, -scale),
        Vector2::new(range_px / scale - min_x, -max_y - range_px / scale),
    );
    let transformation = SdfTransformation::new(
        projection,
        DistanceMapping::from_range(Range::symmetric(range_px * 2.0 / scale)),
    );
    let mut bitmap: Bitmap<f32, 3> = Bitmap::new(width as usize, height as usize);
    let correction_mode = if std::env::var("MSDF_NO_CORRECTION").is_ok() {
        bymsdfgen_core::generator::ErrorCorrectionMode::Disabled
    } else {
        bymsdfgen_core::generator::ErrorCorrectionMode::Indiscriminate
    };
    let config = MsdfGeneratorConfig::new(
        true,
        bymsdfgen_core::generator::ErrorCorrectionConfig {
            mode: correction_mode,
            distance_check_mode: bymsdfgen_core::generator::DistanceCheckMode::AlwaysCheckDistance,
            ..Default::default()
        },
    );
    generate_msdf(&mut bitmap, &shape, &transformation, &config);

    let mut scanline = Scanline::new();
    for y in 0..height as usize {
        let pixel_y = y as f64 + 0.5;
        let y_shape = max_y - (pixel_y - range_px) / scale;
        shape_scanline(&shape, &mut scanline, y_shape);
        for x in 0..width as usize {
            let pixel_x = x as f64 + 0.5;
            let x_shape = (pixel_x - range_px) / scale + min_x;
            let inside = scanline.filled(x_shape, FillRule::NonZero);
            let pixel = bitmap.pixel_mut(x, y);
            let median = median3(f64::from(pixel[0]), f64::from(pixel[1]), f64::from(pixel[2]));
            if (median >= 0.5) != inside {
                let sign = if inside { 1.0 } else { -1.0 };
                for channel in pixel.iter_mut() {
                    *channel = (0.5 + sign * (*channel as f64 - 0.5).abs()) as f32;
                }
            }
        }
    }

    let mut pixels = Vec::with_capacity((width * height * 3) as usize);
    for y in 0..height as usize {
        for x in 0..width as usize {
            for channel in bitmap.pixel(x, y) {
                pixels.push((channel.clamp(0.0, 1.0) * 255.0).round() as u8);
            }
        }
    }

    Some(MsdfGlyph {
        width,
        height,
        bearing_x: (min_x * scale) as f32,
        bearing_y: (max_y * scale) as f32,
        advance: font.advance(glyph_id) * scale as f32,
        pixels,
    })
}

fn median3(a: f64, b: f64, c: f64) -> f64 {
    a.max(b).min(a.max(c)).min(b.max(c))
}

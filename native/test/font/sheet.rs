use bymsdfgen_core::generator::Projection;
use bymsdfgen_core::math::Vector2;
use bymsdfgen_core::math::typed::PixelPoint;
use bymsdfgen_core::raster::{FillRule, Scanline, shape_scanline};
use beryllium_native::font::Font;
use beryllium_native::font::msdf::{GlyphShape, build_shape, rasterize_msdf};
use ttf_parser::GlyphId;

const RANGE: f64 = 2.0;
const SIZES: [usize; 7] = [8, 10, 12, 16, 20, 24, 32];
const ZOOM_SIZES: [usize; 2] = [9, 24];
const ZOOM: usize = 20;

fn em() -> f64 {
	std::env::var("GLYPH_EM").ok().and_then(|v| v.parse::<f64>().ok()).filter(|v| *v >= 16.0).unwrap_or(64.0)
}

#[derive(Clone, Copy, PartialEq)]
enum Kind {
	Hole,
	Dip,
	CoreDip,
	Speckle,
}

impl Kind {
	fn label(&self) -> &'static str {
		match self {
			Kind::Hole => "hole",
			Kind::Dip => "dip",
			Kind::CoreDip => "coredip",
			Kind::Speckle => "speckle",
		}
	}
}

struct Issue {
	c: char,
	size: usize,
	kind: Kind,
	x: usize,
	y: usize,
	truth: f64,
	render: f64,
}

fn main() {
	let args: Vec<String> = std::env::args().collect();
	let font = Font::open(std::fs::read(&args[1]).unwrap()).expect("open font");
	let charset: Vec<char> = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
		.chars()
		.collect();

	std::fs::create_dir_all("test/out").unwrap();

	let mut issues: Vec<Issue> = Vec::new();
	let mut per_char: Vec<(char, usize, usize, usize)> = Vec::new();

	for &c in &charset {
		let Some(gid) = font.glyph_id(c as i32) else { continue };
		let Some(glyph) = rasterize_msdf(&font, GlyphId(gid.0), em(), RANGE) else { continue };
		let Some(gs) = build_shape(&font, GlyphId(gid.0), em()) else { continue };

		for &size in &SIZES {
			let (render, w, h) = render_shader_exact(&glyph, size);
			let truth = ground_truth(&gs, &glyph, w, h, size);
			for y in 0..h {
				for x in 0..w {
					let t = truth[y * w + x];
					let r = render[y * w + x] as f64 / 255.0;
					let kind = if t > 0.85 && r < 0.4 {
						Some(Kind::Hole)
					} else if t > 0.999 && r < 0.75 {
						Some(Kind::CoreDip)
					} else if t > 0.85 && r < 0.75 {
						Some(Kind::Dip)
					} else if t < 0.15 && r > 0.5 {
						Some(Kind::Speckle)
					} else {
						None
					};
					if let Some(kind) = kind {
						issues.push(Issue { c, size, kind, x, y, truth: t, render: r });
					}
				}
			}
		}

		export_zoom(&c, &glyph, &gs);
		let mine = issues.iter().filter(|i| i.c == c).count();
		let holes = issues.iter().filter(|i| i.c == c && i.kind == Kind::Hole).count();
		per_char.push((c, mine, holes, mine - holes));
	}

	per_char.retain(|(_, total, _, _)| *total > 0);
	per_char.sort_by(|a, b| b.1.cmp(&a.1).then(a.0.cmp(&b.0)));
	let holes = issues.iter().filter(|i| i.kind == Kind::Hole).count();
	let dips = issues.iter().filter(|i| i.kind == Kind::Dip).count();
	let core_dips = issues.iter().filter(|i| i.kind == Kind::CoreDip).count();
	let speckles = issues.iter().filter(|i| i.kind == Kind::Speckle).count();
	println!(
		"EM={} sizes={:?} total: holes={} dips={} coredips={} speckles={}",
		em(),
		SIZES,
		holes,
		dips,
		core_dips,
		speckles
	);
	for &size in &SIZES {
		let h = issues.iter().filter(|i| i.kind == Kind::Hole && i.size == size).count();
		let d = issues.iter().filter(|i| i.kind == Kind::CoreDip && i.size == size).count();
		println!("  @{}: holes={} coredips={}", size, h, d);
	}
	for (c, total, hole_count, rest) in &per_char {
		println!("  '{}' total={} (holes={}, dips/speckles={})", c, total, hole_count, rest);
	}
	for i in issues.iter().filter(|i| i.kind != Kind::Dip).take(30) {
		println!(
			"  {} '{}' @{}: ({},{}) truth={:.2} render={:.2}",
			i.kind.label(),
			i.c,
			i.size,
			i.x,
			i.y,
			i.truth,
			i.render
		);
	}
}

fn export_zoom(c: &char, glyph: &beryllium_native::font::msdf::MsdfGlyph, gs: &GlyphShape) {
	for &zoom_size in &ZOOM_SIZES {
		let (render, w, h) = render_shader_exact(glyph, zoom_size);
		let truth = ground_truth(gs, glyph, w, h, zoom_size);
		let zw = w * ZOOM;
		let zh = h * ZOOM;
		let mut img = vec![40u8; zw * zh * 3];
		for y in 0..zh {
			for x in 0..zw {
				let sx = x / ZOOM;
				let sy = y / ZOOM;
				let t = truth[sy * w + sx];
				let r = render[sy * w + sx] as f64 / 255.0;
				let v = (r * 255.0).round() as u8;
				let dst = (y * zw + x) * 3;
				let bad = (t > 0.85 && r < 0.75) || (t < 0.15 && r > 0.5);
				if bad {
					img[dst] = 255;
					img[dst + 1] = v / 3;
					img[dst + 2] = v / 3;
				} else {
					img[dst] = v;
					img[dst + 1] = v;
					img[dst + 2] = v;
				}
			}
		}
		write_ppm(&format!("test/out/{}-{}.png", c, zoom_size), &img, zw, zh);
		let mut plain = vec![0u8; zw * zh * 3];
		for y in 0..zh {
			for x in 0..zw {
				let v = render[(y / ZOOM) * w + x / ZOOM];
				let dst = (y * zw + x) * 3;
				plain[dst] = v;
				plain[dst + 1] = v;
				plain[dst + 2] = v;
			}
		}
		write_ppm(&format!("test/out/plain-{}-{}.png", c, zoom_size), &plain, zw, zh);
	}
}

fn write_ppm(path: &str, img: &[u8], w: usize, h: usize) {
	let mut ppm = format!("P6\n{} {}\n255\n", w, h).into_bytes();
	ppm.extend_from_slice(img);
	std::fs::write(path, ppm).unwrap();
}

fn render_shader_exact(glyph: &beryllium_native::font::msdf::MsdfGlyph, size: usize) -> (Vec<u8>, usize, usize) {
	let f = em() / size as f64;
	let quad_w = glyph.width as f64 / f;
	let quad_h = glyph.height as f64 / f;
	let iw = quad_w.ceil() as usize;
	let ih = quad_h.ceil() as usize;
	let w = glyph.width as usize;
	let mut out = vec![0u8; iw * ih];
	for py in 0..ih {
		for px in 0..iw {
			let u = ((px as f64 + 0.5) / quad_w).min(1.0);
			let v = ((py as f64 + 0.5) / quad_h).min(1.0);
			let (r, g, b) = bilinear(glyph, w, glyph.height as usize, u, v);
			let sd = (median3(r, g, b) - 0.5) * RANGE / f;
			out[py * iw + px] = ((sd + 0.5).clamp(0.0, 1.0) * 255.0).round() as u8;
		}
	}
	(out, iw, ih)
}

fn bilinear(glyph: &beryllium_native::font::msdf::MsdfGlyph, w: usize, h: usize, u: f64, v: f64) -> (f64, f64, f64) {
	let x = u * w as f64 - 0.5;
	let y = v * h as f64 - 0.5;
	let x0 = x.floor().max(0.0) as usize;
	let y0 = y.floor().max(0.0) as usize;
	let x1 = (x0 + 1).min(w - 1);
	let y1 = (y0 + 1).min(h - 1);
	let fx = (x - x0 as f64).clamp(0.0, 1.0);
	let fy = (y - y0 as f64).clamp(0.0, 1.0);
	let sample = |xx: usize, yy: usize| -> (f64, f64, f64) {
		let base = (yy * w + xx) * 3;
		(
			f64::from(glyph.pixels[base]),
			f64::from(glyph.pixels[base + 1]),
			f64::from(glyph.pixels[base + 2]),
		)
	};
	let (a, b, c, d) = (sample(x0, y0), sample(x1, y0), sample(x0, y1), sample(x1, y1));
	let lerp2 = |t0: f64, t1: f64, t2: f64, t3: f64| {
		t0 * (1.0 - fx) * (1.0 - fy) + t1 * fx * (1.0 - fy) + t2 * (1.0 - fx) * fy + t3 * fx * fy
	};
	(
		lerp2(a.0, b.0, c.0, d.0) / 255.0,
		lerp2(a.1, b.1, c.1, d.1) / 255.0,
		lerp2(a.2, b.2, c.2, d.2) / 255.0,
	)
}

fn median3(a: f64, b: f64, c: f64) -> f64 {
	a.max(b).min(a.max(c)).min(b.max(c))
}

fn ground_truth(gs: &GlyphShape, _glyph: &beryllium_native::font::msdf::MsdfGlyph, iw: usize, ih: usize, size: usize) -> Vec<f64> {
	let f = em() / size as f64;
	let scale = gs.scale / f;
	let projection = Projection::new(
		Vector2::new(scale, -scale),
		Vector2::new(RANGE / gs.scale - gs.min_x, -gs.max_y - RANGE / gs.scale),
	);
	let mut scanline = Scanline::new();
	let subs = 6usize;
	let mut truth = vec![0.0f64; iw * ih];
	for py in 0..ih {
		for px in 0..iw {
			let mut filled = 0usize;
			for sy in 0..subs {
				for sx in 0..subs {
					let coord = projection.unproject(PixelPoint::from_raw(Vector2::new(
						px as f64 + (sx as f64 + 0.5) / subs as f64,
						py as f64 + (sy as f64 + 0.5) / subs as f64,
					)));
					shape_scanline(&gs.shape, &mut scanline, coord.raw().y);
					if scanline.filled(coord.raw().x, FillRule::NonZero) {
						filled += 1;
					}
				}
			}
			truth[py * iw + px] = filled as f64 / (subs * subs) as f64;
		}
	}
	truth
}

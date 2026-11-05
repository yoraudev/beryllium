use beryllium_native::font::Font;
use beryllium_native::font::msdf::rasterize_msdf;
use ttf_parser::GlyphId;

fn main() {
	let args: Vec<String> = std::env::args().collect();
	let font = Font::open(std::fs::read(&args[1]).unwrap()).expect("open font");
	let c = args[2].chars().next().unwrap();
	let gid = font.glyph_id(c as i32).unwrap();
	let glyph = rasterize_msdf(&font, GlyphId(gid.0), 64.0, 2.0).unwrap();
	println!("'{}' atlas {}x{}", c, glyph.width, glyph.height);
	for y in 0..glyph.height as usize {
		let mut row = String::new();
		for x in 0..glyph.width as usize {
			let (r, g, b) = texel(&glyph, glyph.width as usize, x, y);
			let m = median3(r, g, b);
			row.push(shade(m));
		}
		println!("|{}|", row);
	}
	for &size in &[24usize, 9] {
		println!("rendered @{}", size);
		let (render, w, h) = render_shader_exact(&glyph, size);
		for y in 0..h {
			let mut row = String::new();
			for x in 0..w {
				row.push(shade(f64::from(render[y * w + x]) / 255.0));
			}
			println!("|{}|", row);
		}
	}
}

fn shade(m: f64) -> char {
	match m {
		m if m >= 0.75 => '#',
		m if m >= 0.5 => '+',
		m if m >= 0.25 => '.',
		_ => ' ',
	}
}

fn texel(glyph: &beryllium_native::font::msdf::MsdfGlyph, w: usize, x: usize, y: usize) -> (f64, f64, f64) {
	let base = (y * w + x) * 3;
	(
		f64::from(glyph.pixels[base]) / 255.0,
		f64::from(glyph.pixels[base + 1]) / 255.0,
		f64::from(glyph.pixels[base + 2]) / 255.0,
	)
}

fn median3(a: f64, b: f64, c: f64) -> f64 {
	a.max(b).min(a.max(c)).min(b.max(c))
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
	let lerp2 = |t0: f64, t1: f64, t2: f64, t3: f64| {
		t0 * (1.0 - fx) * (1.0 - fy) + t1 * fx * (1.0 - fy) + t2 * (1.0 - fx) * fy + t3 * fx * fy
	};
	let (a, b, c, d) = (texel(glyph, w, x0, y0), texel(glyph, w, x1, y0), texel(glyph, w, x0, y1), texel(glyph, w, x1, y1));
	(lerp2(a.0, b.0, c.0, d.0), lerp2(a.1, b.1, c.1, d.1), lerp2(a.2, b.2, c.2, d.2))
}

fn render_shader_exact(glyph: &beryllium_native::font::msdf::MsdfGlyph, size: usize) -> (Vec<u8>, usize, usize) {
	let f = 64.0 / size as f64;
	let quad_w = glyph.width as f64 / f;
	let quad_h = glyph.height as f64 / f;
	let iw = quad_w.ceil() as usize;
	let ih = quad_h.ceil() as usize;
	let w = glyph.width as usize;
	let h = glyph.height as usize;
	let mut out = vec![0u8; iw * ih];
	for py in 0..ih {
		for px in 0..iw {
			let u = ((px as f64 + 0.5) / quad_w).min(1.0);
			let v = ((py as f64 + 0.5) / quad_h).min(1.0);
			let (r, g, b) = bilinear(glyph, w, h, u, v);
			let sd = (median3(r, g, b) - 0.5) * 2.0 / f;
			out[py * iw + px] = ((sd + 0.5).clamp(0.0, 1.0) * 255.0).round() as u8;
		}
	}
	(out, iw, ih)
}

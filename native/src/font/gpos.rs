pub struct GposKern {
    lookups: Vec<PairPosLookup>,
}

impl GposKern {
    pub fn find(data: &[u8]) -> Option<usize> {
        if data.len() < 12 {
            return None;
        }
        let num_tables = u16::from_be_bytes([data[4], data[5]]) as usize;
        for index in 0..num_tables {
            let record = 12 + index * 16;
            if record + 16 > data.len() {
                return None;
            }
            if &data[record..record + 4] == b"GPOS" {
                let offset = u32::from_be_bytes([
                    data[record + 8],
                    data[record + 9],
                    data[record + 10],
                    data[record + 11],
                ]) as usize;
                if offset < data.len() {
                    return Some(offset);
                }
                return None;
            }
        }
        None
    }

    pub fn parse(data: &[u8], base: usize) -> Option<GposKern> {
        let major = read_u16(data, base)?;
        if major != 1 {
            return None;
        }
        let _script_list = read_u16(data, base + 4)? as usize;
        let feature_list = read_u16(data, base + 6)? as usize;
        let lookup_list = read_u16(data, base + 8)? as usize;

        let mut kern_lookup_indices = Vec::new();
        let feature_count = read_u16(data, base + feature_list)?;
        for index in 0..feature_count as usize {
            let record = base + feature_list + 2 + index * 6;
            if &data.get(record..record + 4)? == b"kern" {
                let feature = base + feature_list + read_u16(data, record + 4)? as usize;
                let lookup_count = read_u16(data, feature + 2)? as usize;
                for lookup in 0..lookup_count {
                    let lookup_index = read_u16(data, feature + 4 + lookup * 2)?;
                    kern_lookup_indices.push(lookup_index as usize);
                }
            }
        }

        let mut lookups = Vec::new();
        for lookup_index in kern_lookup_indices {
            let lookup = base + lookup_list + read_u16(data, base + lookup_list + 2 + lookup_index * 2)? as usize;
            let lookup_type = read_u16(data, lookup)?;
            let lookup_flag = read_u16(data, lookup + 2)?;
            if lookup_type == 2 {
                lookups.push(parse_pair_pos(data, lookup, lookup_flag));
            } else if lookup_type == 9 {
                let subtable_count = read_u16(data, lookup + 4)?;
                for subtable_index in 0..subtable_count as usize {
                    let Some(extension_offset) = read_u16(data, lookup + 6 + subtable_index * 2) else { continue };
                    let extension = lookup + extension_offset as usize;
                    let Some(extension_type) = read_u16(data, extension + 2) else { continue };
                    let Some(inner_offset) = read_u32(data, extension + 4) else { continue };
                    let subtable = extension + inner_offset as usize;
                    if extension_type == 2 {
                        lookups.push(parse_pair_pos(data, subtable, lookup_flag));
                    }
                }
            }
        }
        Some(GposKern { lookups })
    }

    pub fn kerning(&self, left: u16, right: u16) -> Option<i16> {
        for lookup in &self.lookups {
            if let Some(value) = lookup.kerning(left, right) {
                return Some(value);
            }
        }
        None
    }
}

struct PairPosLookup {
    subtables: Vec<PairPosSubtable>,
}

impl PairPosLookup {
    fn kerning(&self, left: u16, right: u16) -> Option<i16> {
        for subtable in &self.subtables {
            if let Some(value) = subtable.kerning(left, right) {
                return Some(value);
            }
        }
        None
    }
}

struct PairPosSubtable {
    coverage: Coverage,
    kind: PairPosKind,
}

enum PairPosKind {
    Format1 { pair_sets: Vec<(u16, Vec<(u16, i16)>)> },
    Format2 { class_def1: ClassDef, class_def2: ClassDef, classes: Vec<Vec<Option<i16>>> },
}

impl PairPosSubtable {
    fn kerning(&self, left: u16, right: u16) -> Option<i16> {
        if self.coverage.get(left)? == 0 {
            return None;
        }
        match &self.kind {
            PairPosKind::Format1 { pair_sets } => {
                let entry = pair_sets.iter().find(|(glyph, _)| *glyph == left)?;
                let set = &entry.1;
                set.iter().find(|(glyph, _)| *glyph == right).map(|(_, value)| *value)
            }
            PairPosKind::Format2 { class_def1, class_def2, classes } => {
                let first = class_def1.get(left)?;
                let second = class_def2.get(right)?;
                match classes.get(first as usize)?.get(second as usize)? {
                    Some(value) => Some(*value),
                    None => None,
                }
            }
        }
    }
}

fn parse_pair_pos(data: &[u8], base: usize, _lookup_flag: u16) -> PairPosLookup {
    let mut subtables = Vec::new();
    let subtable_count = read_u16(data, base + 4).unwrap_or(0);
    for index in 0..subtable_count as usize {
        let Some(offset) = read_u16(data, base + 6 + index * 2) else { continue };
        let subtable = base + offset as usize;
        let Some(format) = read_u16(data, subtable) else { continue };
        if format != 1 && format != 2 {
            continue;
        }
        let Some(coverage_offset) = read_u16(data, subtable + 2) else { continue };
        let Some(coverage) = Coverage::parse(data, subtable + coverage_offset as usize) else { continue };
        let Some(format1) = read_u16(data, subtable + 4) else { continue };
        let Some(format2) = read_u16(data, subtable + 6) else { continue };
        let value_size1 = value_format_size(format1);
        let value_size2 = value_format_size(format2);
        let kind = if format == 1 {
            let pair_set_count = read_u16(data, subtable + 8).unwrap_or(0);
            let mut pair_sets = Vec::with_capacity(pair_set_count as usize);
            for set_index in 0..pair_set_count as usize {
                let Some(glyph) = coverage.glyph_at(set_index) else { continue };
                let Some(set_offset) = read_u16(data, subtable + 10 + set_index * 2) else { continue };
                let set = subtable + set_offset as usize;
                let pair_count = read_u16(data, set).unwrap_or(0);
                let mut pairs = Vec::with_capacity(pair_count as usize);
                for pair_index in 0..pair_count as usize {
                    let record = set + 2 + pair_index as usize * (2 + value_size1 + value_size2);
                    let Some(second) = read_u16(data, record) else { continue };
                    let Some(value) = read_value_x_advance(data, record + 2, format1) else { continue };
                    pairs.push((second, value));
                }
                pair_sets.push((glyph, pairs));
            }
            PairPosKind::Format1 { pair_sets }
        } else {
            let Some(class_def1_offset) = read_u16(data, subtable + 8) else { continue };
            let Some(class_def2_offset) = read_u16(data, subtable + 10) else { continue };
            let class1_count = read_u16(data, subtable + 12).unwrap_or(0) as usize;
            let class2_count = read_u16(data, subtable + 14).unwrap_or(0) as usize;
            let Some(class_def1) = ClassDef::parse(data, subtable + class_def1_offset as usize) else { continue };
            let Some(class_def2) = ClassDef::parse(data, subtable + class_def2_offset as usize) else { continue };
            let record_size = value_size1 + value_size2;
            let mut rows = Vec::with_capacity(class1_count);
            for class1 in 0..class1_count {
                let mut row = Vec::with_capacity(class2_count);
                for class2 in 0..class2_count {
                    let record = subtable + 16 + (class1 * class2_count + class2) * record_size;
                    let value = read_value_x_advance(data, record, format1).unwrap_or(0);
                    row.push(Some(value));
                }
                rows.push(row);
            }
            PairPosKind::Format2 { class_def1, class_def2, classes: rows }
        };
        subtables.push(PairPosSubtable { coverage, kind });
    }
    PairPosLookup { subtables }
}

fn value_format_size(format: u16) -> usize {
    let mut count = 0;
    for bit in 0..10u16 {
        if format & (1 << bit) != 0 {
            count += 1;
        }
    }
    count * 2
}

fn read_value_x_advance(data: &[u8], record: usize, format: u16) -> Option<i16> {
    if format & 0x0004 == 0 {
        return Some(0);
    }
    let mut offset = 0usize;
    for bit in 0u16..4 {
        if format & (1 << bit) != 0 {
            if bit == 2 {
                return read_i16(data, record + offset);
            }
            offset += 2;
        }
    }
    Some(0)
}

enum Coverage {
    Glyphs(Vec<u16>),
    Ranges(Vec<(u16, u16, u16)>),
}

impl Coverage {
    fn parse(data: &[u8], base: usize) -> Option<Coverage> {
        match read_u16(data, base)? {
            1 => {
                let count = read_u16(data, base + 2)? as usize;
                let mut glyphs = Vec::with_capacity(count);
                for index in 0..count {
                    glyphs.push(read_u16(data, base + 4 + index * 2)?);
                }
                Some(Coverage::Glyphs(glyphs))
            }
            2 => {
                let count = read_u16(data, base + 2)? as usize;
                let mut ranges = Vec::with_capacity(count);
                for index in 0..count {
                    let record = base + 4 + index * 6;
                    let start = read_u16(data, record)?;
                    let end = read_u16(data, record + 2)?;
                    let start_index = read_u16(data, record + 4)?;
                    ranges.push((start, end, start_index));
                }
                Some(Coverage::Ranges(ranges))
            }
            _ => None,
        }
    }

    fn get(&self, glyph: u16) -> Option<u16> {
        match self {
            Coverage::Glyphs(glyphs) => glyphs.binary_search(&glyph).ok().map(|index| index as u16),
            Coverage::Ranges(ranges) => {
                for (start, end, start_index) in ranges {
                    if glyph >= *start && glyph <= *end {
                        return Some(start_index + (glyph - start));
                    }
                }
                None
            }
        }
    }

    fn glyph_at(&self, index: usize) -> Option<u16> {
        match self {
            Coverage::Glyphs(glyphs) => glyphs.get(index).copied(),
            Coverage::Ranges(_) => None,
        }
    }
}

enum ClassDef {
    Classes { first: u16, classes: Vec<u16> },
    Ranges(Vec<(u16, u16, u16)>),
}

impl ClassDef {
    fn parse(data: &[u8], base: usize) -> Option<ClassDef> {
        match read_u16(data, base)? {
            1 => {
                let first = read_u16(data, base + 2)?;
                let count = read_u16(data, base + 4)? as usize;
                let mut classes = Vec::with_capacity(count);
                for index in 0..count {
                    classes.push(read_u16(data, base + 6 + index * 2)?);
                }
                Some(ClassDef::Classes { first, classes })
            }
            2 => {
                let count = read_u16(data, base + 2)? as usize;
                let mut ranges = Vec::with_capacity(count);
                for index in 0..count {
                    let record = base + 4 + index * 6;
                    let start = read_u16(data, record)?;
                    let end = read_u16(data, record + 2)?;
                    let class = read_u16(data, record + 4)?;
                    ranges.push((start, end, class));
                }
                Some(ClassDef::Ranges(ranges))
            }
            _ => None,
        }
    }

    fn get(&self, glyph: u16) -> Option<u16> {
        match self {
            ClassDef::Classes { first, classes } => {
                let index = glyph.checked_sub(*first)?;
                classes.get(index as usize).copied()
            }
            ClassDef::Ranges(ranges) => {
                for (start, end, class) in ranges {
                    if glyph >= *start && glyph <= *end {
                        return Some(*class);
                    }
                }
                None
            }
        }
    }
}

fn read_u16(data: &[u8], offset: usize) -> Option<u16> {
    let bytes = data.get(offset..offset + 2)?;
    Some(u16::from_be_bytes([bytes[0], bytes[1]]))
}

fn read_u32(data: &[u8], offset: usize) -> Option<u32> {
    let bytes = data.get(offset..offset + 4)?;
    Some(u32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]))
}

fn read_i16(data: &[u8], offset: usize) -> Option<i16> {
    read_u16(data, offset).map(|value| value as i16)
}

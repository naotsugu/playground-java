#[unsafe(no_mangle)]
pub extern "C" fn render_pattern(buffer: *mut u8, width: u32, height: u32) {

    let length = (width * height * 4) as usize;
    if buffer.is_null() || length == 0 {
        return;
    }

    // Creating a Rust slice (array) from a raw pointer
    let slice = unsafe { std::slice::from_raw_parts_mut(buffer, length) };

    for y in 0..height {
        for x in 0..width {
            let idx = ((y * width + x) * 4) as usize;

            let r_ratio = x as f32 / width as f32;
            let g_ratio = y as f32 / height as f32;

            slice[idx + 0] = 200;                      // B
            slice[idx + 1] = (g_ratio * 255.0) as u8;  // G
            slice[idx + 2] = (r_ratio * 255.0) as u8;  // R
            slice[idx + 3] = 255;                      // A
        }
    }
}

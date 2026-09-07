use std::slice;
use vello::{
    kurbo::{Affine, Circle, Rect},
    peniko::{Color, Fill},
    Scene,
};

#[unsafe(no_mangle)]
pub extern "C" fn render_vello_scene(buffer: *mut u8, width: u32, height: u32) {
    let bytes_per_row = width * 4;
    if bytes_per_row % 256 != 0 {
        eprintln!("Error: width * 4 must be a multiple of 256.");
        return;
    }

    pollster::block_on(render_async(buffer, width, height, bytes_per_row));
}

async fn render_async(buffer: *mut u8, width: u32, height: u32, bytes_per_row: u32) {
    let length = (width * height * 4) as usize;
    if buffer.is_null() || length == 0 {
        return;
    }

    let instance = wgpu::Instance::default();
    let adapter = instance
        .request_adapter(&wgpu::RequestAdapterOptions::default())
        .await
        .unwrap();

    let (device, queue) = adapter
        .request_device(&wgpu::DeviceDescriptor::default())
        .await
        .unwrap();

    let format = wgpu::TextureFormat::Rgba8Unorm;

    let texture_desc = wgpu::TextureDescriptor {
        label: Some("Vello Target Texture"),
        size: wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format,
        usage: wgpu::TextureUsages::RENDER_ATTACHMENT
            | wgpu::TextureUsages::COPY_SRC
            | wgpu::TextureUsages::STORAGE_BINDING,
        view_formats: &[],
    };
    let texture = device.create_texture(&texture_desc);
    let view = texture.create_view(&wgpu::TextureViewDescriptor::default());

    let readback_buffer = device.create_buffer(&wgpu::BufferDescriptor {
        label: Some("Readback Buffer"),
        size: length as wgpu::BufferAddress,
        usage: wgpu::BufferUsages::MAP_READ | wgpu::BufferUsages::COPY_DST,
        mapped_at_creation: false,
    });

    let mut scene = Scene::new();

    scene.fill(
        Fill::NonZero,
        Affine::IDENTITY,
        Color::from_rgb8(30, 30, 35),
        None,
        &Rect::new(0.0, 0.0, width as f64, height as f64),
    );

    scene.fill(
        Fill::NonZero,
        Affine::IDENTITY,
        Color::from_rgb8(255, 80, 80),
        None,
        &Circle::new((width as f64 / 2.0, height as f64 / 2.0), 200.0),
    );

    scene.fill(
        Fill::NonZero,
        Affine::rotate_about(0.2, (width as f64 - 200.0, 200.0)),
        Color::from_rgb8(80, 150, 255),
        None,
        &Rect::from_center_size((width as f64 - 200.0, 200.0), (150.0, 150.0)),
    );

    let mut renderer = vello::Renderer::new(
        &device,
        vello::RendererOptions {
            use_cpu: false,
            antialiasing_support: vello::AaSupport::all(),
            num_init_threads: None,
            pipeline_cache: None,
        },
    ).unwrap();

    renderer.render_to_texture(
        &device,
        &queue,
        &scene,
        &view,
        &vello::RenderParams {
            base_color: vello::peniko::Color::TRANSPARENT,
            width,
            height,
            antialiasing_method: vello::AaConfig::Msaa16,
        },
    ).unwrap();

    let mut encoder = device.create_command_encoder(&wgpu::CommandEncoderDescriptor::default());

    encoder.copy_texture_to_buffer(
        wgpu::TexelCopyTextureInfo {
            texture: &texture,
            mip_level: 0,
            origin: wgpu::Origin3d::ZERO,
            aspect: wgpu::TextureAspect::All,
        },
        wgpu::TexelCopyBufferInfo {
            buffer: &readback_buffer,
            layout: wgpu::TexelCopyBufferLayout {
                offset: 0,
                bytes_per_row: Some(bytes_per_row),
                rows_per_image: Some(height),
            },
        },
        texture_desc.size,
    );
    queue.submit(Some(encoder.finish()));

    let buffer_slice = readback_buffer.slice(..);
    let (tx, rx) = std::sync::mpsc::channel();
    buffer_slice.map_async(wgpu::MapMode::Read, move |v| tx.send(v).unwrap());

    // 【修正】Wait を直接生成せず、コンビニエンス関数 wait_indefinitely() を使用します
    device.poll(wgpu::PollType::wait_indefinitely()).unwrap();
    rx.recv().unwrap().unwrap();

    let mapped_data = buffer_slice.get_mapped_range();

    let src_pixels = unsafe {
        slice::from_raw_parts(mapped_data.as_ptr() as *const [u8; 4], length / 4)
    };
    let dst_pixels = unsafe {
        slice::from_raw_parts_mut(buffer as *mut [u8; 4], length / 4)
    };

    for (src, dst) in src_pixels.iter().zip(dst_pixels.iter_mut()) {
        dst[0] = src[2]; // R -> B
        dst[1] = src[1]; // G -> G
        dst[2] = src[0]; // B -> R
        dst[3] = src[3]; // A -> A
    }
}

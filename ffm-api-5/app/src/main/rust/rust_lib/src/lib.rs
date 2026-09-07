use std::slice;
use vello::{
    kurbo::{Affine, Circle, Rect},
    peniko::{Color, Fill},
    Scene,
};

pub struct RenderContext {
    device: wgpu::Device,
    queue: wgpu::Queue,
    renderer: vello::Renderer,
    texture: wgpu::Texture,
    readback_buffer: wgpu::Buffer,
    width: u32,
    height: u32,
    bytes_per_row: u32,
}

#[unsafe(no_mangle)]
pub extern "C" fn vello_ctx_create(width: u32, height: u32) -> *mut RenderContext {
    let bytes_per_row = width * 4;
    assert!(bytes_per_row % 256 == 0, "Width * 4 must be a multiple of 256");

    let ctx = pollster::block_on(async move {
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

        let texture = device.create_texture(&wgpu::TextureDescriptor {
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
        });

        let readback_buffer = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Readback Buffer"),
            size: (width * height * 4) as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::MAP_READ | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

        let renderer = vello::Renderer::new(
            &device,
            vello::RendererOptions {
                use_cpu: false,
                antialiasing_support: vello::AaSupport::all(),
                num_init_threads: None,
                pipeline_cache: None,
            },
        ).unwrap();

        RenderContext {
            device, queue, renderer, texture, readback_buffer, width, height, bytes_per_row,
        }
    });

    // detach from Rust's memory management and pass to Java as a raw pointer
    Box::into_raw(Box::new(ctx))
}

#[unsafe(no_mangle)]
pub extern "C" fn vello_ctx_render(ctx_ptr: *mut RenderContext, buffer: *mut u8, time: f64) {
    if ctx_ptr.is_null() || buffer.is_null() { return; }

    // restore the reference from the raw pointer (without taking ownership)
    let ctx = unsafe { &mut *ctx_ptr };
    let length = (ctx.width * ctx.height * 4) as usize;

    let view = ctx.texture.create_view(&wgpu::TextureViewDescriptor::default());
    let mut scene = Scene::new();
    scene.fill(
        Fill::NonZero, Affine::IDENTITY, Color::from_rgb8(30, 30, 35), None,
        &Rect::new(0.0, 0.0, ctx.width as f64, ctx.height as f64),
    );

    let cx = (ctx.width as f64 / 2.0) + time.sin() * 200.0;
    scene.fill(
        Fill::NonZero, Affine::IDENTITY, Color::from_rgb8(255, 80, 80), None,
        &Circle::new((cx, ctx.height as f64 / 2.0), 100.0),
    );

    scene.fill(
        Fill::NonZero,
        Affine::rotate_about(time, (ctx.width as f64 / 2.0, ctx.height as f64 / 2.0)),
        Color::from_rgb8(80, 150, 255), None,
        &Rect::from_center_size((ctx.width as f64 / 2.0, ctx.height as f64 / 2.0), (150.0, 150.0)),
    );

    // drow to the GPU
    ctx.renderer.render_to_texture(
        &ctx.device, &ctx.queue, &scene, &view,
        &vello::RenderParams {
            base_color: vello::peniko::Color::TRANSPARENT,
            width: ctx.width, height: ctx.height,
            antialiasing_method: vello::AaConfig::Msaa16,
        },
    ).unwrap();

    let mut encoder = ctx.device.create_command_encoder(&wgpu::CommandEncoderDescriptor::default());
    encoder.copy_texture_to_buffer(
        wgpu::TexelCopyTextureInfo {
            texture: &ctx.texture,
            mip_level: 0,
            origin: wgpu::Origin3d::ZERO,
            aspect: wgpu::TextureAspect::All,
        },
        wgpu::TexelCopyBufferInfo {
            buffer: &ctx.readback_buffer,
            layout: wgpu::TexelCopyBufferLayout {
                offset: 0,
                bytes_per_row: Some(ctx.bytes_per_row),
                rows_per_image: Some(ctx.height),
            },
        },
        wgpu::Extent3d { width: ctx.width, height: ctx.height, depth_or_array_layers: 1 },
    );
    ctx.queue.submit(Some(encoder.finish()));

    let buffer_slice = ctx.readback_buffer.slice(..);
    let (tx, rx) = std::sync::mpsc::channel();
    buffer_slice.map_async(wgpu::MapMode::Read, move |v| tx.send(v).unwrap());

    ctx.device.poll(wgpu::PollType::wait_indefinitely()).unwrap();
    rx.recv().unwrap().unwrap();

    let mapped_data = buffer_slice.get_mapped_range();

    // SIMD copy
    let src_bytes = unsafe { slice::from_raw_parts(mapped_data.as_ptr(), length) };
    let dst_bytes = unsafe { slice::from_raw_parts_mut(buffer, length) };
    for (src, dst) in src_bytes.chunks_exact(4).zip(dst_bytes.chunks_exact_mut(4)) {
        dst[0] = src[2]; // B <- R
        dst[1] = src[1]; // G <- G
        dst[2] = src[0]; // R <- B
        dst[3] = src[3]; // A <- A
    }
    drop(mapped_data);
    ctx.readback_buffer.unmap();
}

#[unsafe(no_mangle)]
pub extern "C" fn vello_ctx_destroy(ctx_ptr: *mut RenderContext) {
    if !ctx_ptr.is_null() {
        // reconstruct the Box from the raw pointer to trigger
        // automatic dropping (memory deallocation) when going out of scope
        unsafe { let _ = Box::from_raw(ctx_ptr); }
        println!("  Vello RenderContext destroyed.");
    }
}

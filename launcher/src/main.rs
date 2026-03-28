use app_data::AppData;
use eframe::egui;
use eframe::egui::{Image, RichText, Vec2};
use egui_alignments::{center_horizontal, top_horizontal};
use egui_commonmark::{CommonMarkCache, CommonMarkViewer};
use futures_util::StreamExt;
use reqwest::Response;
use reqwest::blocking::Client;
use semver::Version;
use serde::Deserialize;
use std::cmp::PartialEq;
use std::error::Error;
use std::fs::File;
use std::io::{BufRead, Write};
use std::io::{BufReader, BufWriter, Read};
#[cfg(target_os = "windows")]
use std::os::windows::process::CommandExt;
use std::path::Path;
use std::process::{Command, exit};
use std::sync::{Arc, LazyLock, Mutex};
use std::time::Duration;
use tokio::runtime::Runtime;

const URL: &str = "https://api.github.com/repos/d-catte/Westward/releases/latest";
static PROGRESS: LazyLock<Mutex<f32>> = LazyLock::new(|| Mutex::new(0.0));
static RT: LazyLock<Mutex<Runtime>> = LazyLock::new(|| Mutex::new(Runtime::new().unwrap()));
static IMAGES: LazyLock<[Image; 4]> = LazyLock::new(|| {
    [
        Image::new(egui::include_image!("../assets/ashHollowComplex.png")),
        Image::new(egui::include_image!("../assets/chimneyRock.png")),
        Image::new(egui::include_image!("../assets/courthouseAndJailRocks.png")),
        Image::new(egui::include_image!("../assets/southPlatteRiver.png")),
    ]
});
static WAGON: LazyLock<Image> =
    LazyLock::new(|| Image::new(egui::include_image!("../assets/wagonMove.png")));
static STATUS: LazyLock<Mutex<Status>> = LazyLock::new(|| Mutex::new(Status::Default));
static CHANGE_LOG: LazyLock<Mutex<(String, CommonMarkCache)>> = LazyLock::new(|| {
    Mutex::new((
        get_saved_changelog().unwrap_or_default(),
        CommonMarkCache::default(),
    ))
});
static APP_DATA: LazyLock<AppData> = LazyLock::new(|| {
    let app_data = AppData::new("Westward");
    app_data.ensure_data_dir().unwrap();
    app_data
});

fn main() {
    let updates: Option<(Release, Status)> = check_for_updates(false);
    let icon = eframe::icon_data::from_png_bytes(include_bytes!("../assets/icon.png"))
        .expect("Failed to load icon");
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder {
            inner_size: Some(Vec2::new(800.0, 600.0)),
            ..Default::default()
        }
        .with_icon(Arc::new(icon)),
        ..Default::default()
    };
    eframe::run_native(
        "Westward Launcher",
        options,
        Box::new(|cc| {
            egui_extras::install_image_loaders(&cc.egui_ctx);
            if let Some(ref update) = updates {
                *STATUS.lock().unwrap() = update.1.clone();
                Ok(Box::new(App::create(update.0.clone())))
            } else {
                Ok(Box::new(App::create(Release::default())))
            }
        }),
    )
    .unwrap()
}

fn check_for_updates(corrupt: bool) -> Option<(Release, Status)> {
    let latest_release = get_latest_release();
    if let Ok(latest_release) = latest_release {
        let latest_version_tag = latest_release.parse_tag().unwrap();
        let latest_version = get_latest_version(&latest_release);
        if latest_version.is_some() {
            if corrupt {
                return Some((latest_release, Status::CorruptedInstall));
            }
            if let Some(current_version) = get_current_version() {
                if current_version < latest_version_tag {
                    return Some((latest_release, Status::UpdateAvailable));
                }
            } else {
                return Some((latest_release, Status::NotInstalled));
            }
        }
    }
    None
}

fn install(latest_release: &Release, asset: &Asset) {
    let clone_url = asset.browser_download_url.clone();
    let release_clone = latest_release.clone();
    RT.lock().unwrap().spawn(async move {
        if let Err(e) = download_latest_westward(&clone_url).await {
            eprintln!("Download failed: {}", e);
        } else {
            write_new_version(&release_clone);
            *STATUS.lock().unwrap() = Status::Default;
        }
    });
}

fn write_new_version(release: &Release) {
    let file = File::create(APP_DATA.get_file_path("version").unwrap()).unwrap();
    let mut writer = BufWriter::new(file);

    writeln!(writer, "# {}", release.tag_name).unwrap();

    if let Some(body) = &release.body {
        writeln!(writer).unwrap();

        for line in body.lines() {
            if line.contains("Full Changelog") {
                let url = line
                    .split_whitespace()
                    .find(|word| word.starts_with("http"))
                    .unwrap_or("");

                writeln!(writer, "[Full Changelog]({})", url).unwrap();
            } else {
                writeln!(writer, "{}", line).unwrap();
            }
        }
    }

    writer.flush().unwrap();

    CHANGE_LOG.lock().unwrap().0 = get_saved_changelog().unwrap_or_default();
}

fn get_current_version() -> Option<Version> {
    let file = File::open(APP_DATA.get_file_path("version").unwrap()).ok()?;
    let mut reader = BufReader::new(file);

    let mut first_line = String::new();
    reader.read_line(&mut first_line).ok()?;

    Version::parse(first_line.replace("# ", "").trim()).ok()
}

fn get_saved_changelog() -> Option<String> {
    let file = File::open(APP_DATA.get_file_path("version").unwrap()).ok()?;
    let mut reader = BufReader::new(file);

    let mut contents = String::new();
    reader.read_to_string(&mut contents).ok()?;

    Some(contents)
}

fn get_latest_release() -> Result<Release, Box<dyn Error>> {
    let client = Client::new();

    let output: Release = client
        .get(URL)
        .header("User-Agent", "westward-updater")
        .send()?
        .error_for_status()?
        .json()?;
    Ok(output)
}

/// Westward release asset names are as follows:
/// Universal Jar: westward-{tag}.jar
/// Windows Native: westward-windows-{tag}.exe
/// Linux Native: westward-linux-{tag}
/// MacOS Intel Native: westward-macos-i-{tag}
/// MacOS Arm Native: westward-macos-a-{tag}
fn get_latest_version(release: &Release) -> Option<Asset> {
    let search_term = if cfg!(target_os = "windows") {
        "westward-windows"
    } else if cfg!(target_os = "macos") {
        if cfg!(target_arch = "arm") || cfg!(target_arch = "aarch64") {
            "westward-macos-a"
        } else {
            "westward-macos-i"
        }
    } else {
        "westward-linux"
    };
    for asset in release.assets.iter() {
        if asset.name.starts_with(search_term) {
            return Some(asset.clone());
        }
    }
    None
}

pub async fn download_latest_westward(url: &str) -> Result<(), Box<dyn Error>> {
    let filename = if cfg!(target_os = "windows") {
        "westward.exe".to_string()
    } else {
        "westward".to_string()
    };

    let path = APP_DATA.get_file_path(&filename)?;

    println!("Connecting to GitHub for download: {}", url);
    let client = reqwest::Client::new();
    let response: Response = client
        .get(url)
        .header("User-Agent", "westward-updater")
        .header("Accept", "application/octet-stream")
        .send()
        .await
        .map_err(|e| {
            eprintln!("Reqwest error: {:#?}", e);
            e
        })?
        .error_for_status()?;

    println!("Downloading: {}", url);
    let total_size = response.content_length().unwrap_or(0);
    let mut downloaded: u64 = 0;

    let mut file = File::create(Path::new(&path))?;
    let mut stream = response.bytes_stream();

    while let Some(chunk) = stream.next().await {
        let chunk = chunk?;
        file.write_all(&chunk)?;
        downloaded += chunk.len() as u64;

        if total_size > 0 {
            let progress = downloaded as f32 / total_size as f32;
            *PROGRESS.lock()? = progress.min(1.0);
        }
    }

    *PROGRESS.lock()? = 1.0;

    Ok(())
}

fn launch_westward() {
    #[cfg(target_os = "windows")]
    {
        let path = APP_DATA.get_file_path("westward.exe").unwrap();
        let exe = path.to_str().unwrap();
        let mut cmd = Command::new(exe);

        // Detach from parent console
        const DETACHED_PROCESS: u32 = 0x00000008;
        const CREATE_NEW_PROCESS_GROUP: u32 = 0x00000200;
        cmd.creation_flags(DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP);

        cmd.spawn().expect("Failed to launch Westward");
    }

    #[cfg(not(target_os = "windows"))]
    {
        let file = APP_DATA.get_file_path("westward").unwrap();
        let exe = format!("{}", file.display());
        let _ = Command::new("chmod").arg("+x").arg(&exe).status();

        // Detach from parent terminal
        Command::new("setsid")
            .arg(exe)
            .spawn()
            .expect("Failed to launch Westward");
    }
    exit(0);
}

#[derive(Debug, Deserialize, Clone)]
struct Release {
    tag_name: String,
    body: Option<String>,
    assets: Vec<Asset>,
}

impl Release {
    pub fn parse_tag(&self) -> Result<Version, semver::Error> {
        Version::parse(&self.tag_name)
    }
}

impl Default for Release {
    fn default() -> Self {
        Self {
            tag_name: "null".to_string(),
            body: None,
            assets: vec![],
        }
    }
}

#[derive(Clone, PartialEq)]
enum Status {
    NotInstalled,
    UpdateAvailable,
    Downloading,
    FailedToDownload,
    CorruptedInstall,
    Default,
}

#[derive(Debug, Deserialize, Clone)]
struct Asset {
    name: String,
    browser_download_url: String,
}

struct App {
    release: Release,
    error: String,
    patch_expanded: bool,
    current_index: usize,
    last_switch_time: f64,
    transition_start: Option<f64>,
}

impl App {
    pub fn create(release: Release) -> Self {
        Self {
            release,
            error: String::new(),
            patch_expanded: false,
            current_index: 0,
            last_switch_time: 0.0,
            transition_start: None,
        }
    }
}

impl eframe::App for App {
    fn update(&mut self, ctx: &egui::Context, _: &mut eframe::Frame) {
        let screen_rect = ctx.available_rect();
        let total_height = screen_rect.height();

        let title_height = total_height * 0.08;
        let carousel_height = total_height * 0.67;
        let taskbar_height = total_height * 0.10;
        let patch_height = if self.patch_expanded {
            total_height * 0.35
        } else {
            total_height * 0.15
        };

        // Title Bar
        egui::TopBottomPanel::top("title_bar")
            .exact_height(title_height)
            .show(ctx, |ui| {
                let font_size = (title_height * 0.5).clamp(18.0, 72.0);

                ui.with_layout(
                    egui::Layout::centered_and_justified(egui::Direction::TopDown),
                    |ui| {
                        ui.add(egui::Label::new(
                            RichText::new("Westward").size(font_size).strong(),
                        ));
                    },
                );
            });

        // Patch Notes
        egui::TopBottomPanel::bottom("patch_notes")
            .exact_height(patch_height)
            .show(ctx, |ui| {
                top_horizontal(ui, |ui| {
                    if ui.button("Changelog").clicked() {
                        self.patch_expanded = !self.patch_expanded;
                    }
                });

                let mut change_log = CHANGE_LOG.lock().unwrap();
                let text = change_log.0.clone();
                ui.style_mut().url_in_tooltip = true;
                egui::ScrollArea::vertical()
                    .auto_shrink([false; 2])
                    .show(ui, |ui| {
                        CommonMarkViewer::new().show(ui, &mut change_log.1, &text);
                    });
            });

        // Taskbar
        egui::TopBottomPanel::bottom("taskbar")
            .exact_height(taskbar_height)
            .show(ctx, |ui| {
                let button_height = taskbar_height * 0.6;
                let button_width = ui.available_width() * 0.15;

                center_horizontal(ui, |ui| {
                    let button_size = egui::vec2(button_width, button_height);
                    let mut status = STATUS.lock().unwrap();

                    if *status == Status::NotInstalled {
                        if ui
                            .add(egui::Button::new("Install").min_size(button_size))
                            .clicked()
                        {
                            let asset = get_latest_version(&self.release);
                            if let Some(asset) = asset {
                                *status = Status::Downloading;
                                install(&self.release, &asset);
                            } else {
                                self.error = "No asset found".to_string();
                                *status = Status::FailedToDownload;
                            }
                        }
                    } else if ui
                        .add(egui::Button::new("Play").min_size(button_size))
                        .clicked()
                    {
                        launch_westward();
                    }

                    if *status == Status::UpdateAvailable
                        && ui
                            .add(egui::Button::new("Update").min_size(button_size))
                            .clicked()
                    {
                        let asset = get_latest_version(&self.release);
                        if let Some(asset) = asset {
                            *status = Status::Downloading;
                            install(&self.release, &asset);
                        } else {
                            self.error = "No asset found".to_string();
                            *status = Status::FailedToDownload;
                        }
                    }
                    drop(status);
                });
            });

        // Main Carousel
        let now = ctx.input(|i| i.time);
        let switch_interval = 10.0;
        let transition_duration = 0.6;

        if now - self.last_switch_time > switch_interval && self.transition_start.is_none() {
            self.transition_start = Some(now);
            self.last_switch_time = now;
        }

        egui::CentralPanel::default().show(ctx, |ui| {
            ui.set_min_height(carousel_height);

            let available = ui.available_size();

            let mut progress = 0.0;
            let mut transitioning = false;

            if let Some(start) = self.transition_start {
                progress = ((now - start) / transition_duration).clamp(0.0, 1.0);
                transitioning = true;

                if progress >= 1.0 {
                    self.current_index = (self.current_index + 1) % IMAGES.len();
                    self.transition_start = None;
                    transitioning = false;
                } else {
                    ctx.request_repaint();
                }
            } else {
                ctx.request_repaint_after(Duration::from_secs(switch_interval as u64));
            }

            let rect = ui.allocate_space(available).1;

            if transitioning {
                let next_index = (self.current_index + 1) % IMAGES.len();

                let current_alpha = 1.0 - progress as f32;
                let next_alpha = progress as f32;

                IMAGES[self.current_index]
                    .clone()
                    .maintain_aspect_ratio(true)
                    .fit_to_exact_size(available)
                    .tint(egui::Color32::from_white_alpha(
                        (255.0 * current_alpha) as u8,
                    ))
                    .paint_at(ui, rect);

                IMAGES[next_index]
                    .clone()
                    .fit_to_exact_size(available)
                    .tint(egui::Color32::from_white_alpha((255.0 * next_alpha) as u8))
                    .paint_at(ui, rect);
            } else {
                IMAGES[self.current_index]
                    .clone()
                    .fit_to_exact_size(available)
                    .paint_at(ui, rect);
            }

            // Loading bar
            let painter = ui.painter_at(rect);

            if *STATUS.lock().unwrap() == Status::Downloading {
                let progress = PROGRESS.lock().unwrap().clamp(0.0, 1.0);

                let bar_height = available.y * 0.08 + 10.0;

                let bar_rect = egui::Rect::from_min_max(
                    egui::pos2(rect.left(), rect.bottom() - bar_height),
                    egui::pos2(rect.right(), rect.bottom()),
                );

                painter.rect_filled(bar_rect, 0.0, egui::Color32::from_black_alpha(120));

                let wagon_aspect = 2.0;

                let wagon_height = bar_height * 0.9;
                let wagon_width = wagon_height * wagon_aspect;

                let travel_width = bar_rect.width() - wagon_width;

                let wagon_x = bar_rect.left() + travel_width * progress;
                let wagon_y = bar_rect.center().y - wagon_height / 2.0;

                let wagon_rect = egui::Rect::from_min_size(
                    egui::pos2(wagon_x, wagon_y),
                    egui::vec2(wagon_width, wagon_height),
                );

                WAGON
                    .clone()
                    .fit_to_exact_size(wagon_rect.size())
                    .paint_at(ui, wagon_rect);

                ctx.request_repaint();
            }
        });
    }
}

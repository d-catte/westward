package io.github.onu_eccs1621_sp2025.westward;

import com.google.gson.Gson;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;
import io.github.onu_eccs1621_sp2025.westward.data.Audio;
import io.github.onu_eccs1621_sp2025.westward.data.DataPaths;
import io.github.onu_eccs1621_sp2025.westward.game.Game;
import io.github.onu_eccs1621_sp2025.westward.screen.Renderer;
import io.github.onu_eccs1621_sp2025.westward.utils.*;
import io.github.onu_eccs1621_sp2025.westward.utils.registry.Registry;
import io.github.onu_eccs1621_sp2025.westward.utils.rendering.RenderUtils;
import io.github.onu_eccs1621_sp2025.westward.utils.sound.SoundEngine;
import io.github.onu_eccs1621_sp2025.westward.utils.text.Translations;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;
import java.util.concurrent.Semaphore;

/**
 * The main class for the game.
 * Initializes all parts of the game.
 * Contains global data such as the DebugLogger and version
 * @author Dylan Catte
 * @since 1.0.0 Alpha 1
 * @version 1.2.1
 */
public class TrailApplication extends Application {
    /**
     * The build version of Westward
     */
    private static final String VERSION = "v1.0.0-beta.8";
    /**
     * The path to all local game data files
     */
    private static final Path DATA_PATH = FileSystems.getDefault().getPath("gameData");
    /**
     * The file system to access the current jar
     */
    private static final FileSystem JAR_FILESYSTEM;
    /**
     * The path to the jar file
     */
    public static final Path JAR_DATA_PATH;
    /**
     * The thread lock to prevent Concurrent Modification Exceptions when ending the rendering
     */
    private static final Semaphore RENDERING_LOCK = new Semaphore(1, true);
    /**
     * The thread lock to prevent Concurrent Modification Exceptions when ending the game
     */
    private static final Semaphore GAME_LOCK = new Semaphore(1, true);
    /**
     * The paths for each data file/folder
     */
    private static final DataPaths DATA_PATHS = new DataPaths(
            DATA_PATH.resolve("screens/perilScreens.json"),
            DATA_PATH.resolve("screens/landmarkScreens.json"),
            DATA_PATH.resolve("screens/shopScreens.json"),
            DATA_PATH.resolve("screens/tradingScreens.json"),
            DATA_PATH.resolve("screens/huntingGameScreens.json"),
            DATA_PATH.resolve("statuses.json"),
            DATA_PATH.resolve("saves"),
            DATA_PATH.resolve("items.json"),
            DATA_PATH.resolve("landmarks.json"),
            DATA_PATH.resolve("events.json"),
            DATA_PATH.resolve("roles.json"),
            DATA_PATH.resolve("asset"),
            DATA_PATH.resolve("audio"),
            DATA_PATH.resolve("sfx"),
            DATA_PATH.resolve("config.json"),
            DATA_PATH.resolve("lang")
    );
    /**
     * The GSON instance for (de)serialization
     */
    private static final Gson GSON = new Gson();
    /**
     * The main rendering class for GUIs
     */
    private static Renderer renderer;

    /**
     * The framerate of the game
     */
    private static float MS_PER_FRAME = 1 / 30F * 1000;

    // Set default data path
    static {
        NativeUtils.init();

        // Determine if we're running from a jar or not, because the access method for folders is different
        final boolean inJar = "jar".equals(TrailApplication.class.getResource("TrailApplication.class").getProtocol());
        if (inJar) {
            try {
                final URI jarURI = Thread.currentThread().getContextClassLoader().getResource("defaultData").toURI();
                JAR_FILESYSTEM = FileSystems.newFileSystem(jarURI, Collections.emptyMap(), null);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            JAR_FILESYSTEM = null;
        }
        JAR_DATA_PATH = NativeUtils.getResource("");
    }

    /**
     * The main method of Westward
     * @param args N/A
     */
    public static void main(String[] args) {
        if (StartOnFirstThreadHelper.startNewJvmIfRequired()) {
            return;
        }
        launch(new TrailApplication());
    }

    /**
     * Initializes the application
     */
    public TrailApplication() {
        super();
        DebugLogger.info("Initializing Westward Game {}", VERSION);
        // If there is no game data, copy default data from inside the jar
        try {
            this.checkFiles();
        } catch (IOException e) {
            DebugLogger.error("Failed to load game data", e.toString());
            System.exit(1);
        }
        setFPSLimit(Config.getConfig().getFpsLimit());
        DebugLogger.info("Loading Translation: {}", Config.getConfig().getLanguage());
        Translations.loadTranslations(Config.getConfig().getLanguage());
        DebugLogger.info("Loading Game Assets");
        Registry.loadAssets();
        DebugLogger.info("Reading Saves");
        DebugLogger.info("Starting Audio Loader");
        SoundEngine.init();
        DebugLogger.info("Forming Main GUI");
        // Delay initialization until after translations are loaded
        renderer = new Renderer();
        final Thread gameThread = new Thread(() -> {
            while (true) {
                if (Game.getInstance() != null) {
                    // Acquire lock
                    try {
                        GAME_LOCK.acquire();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // Check again if the game has been closed
                    if (Game.getInstance() != null) {
                        Game.getInstance().tickGame();
                    }
                    GAME_LOCK.release();
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        gameThread.start();

        DebugLogger.info("Game Loaded");
    }

    /**
     * Gets the paths to all JSON files
     * @return Record of all JSON file paths
     */
    public static DataPaths getDataPaths() {
        return DATA_PATHS;
    }

    /**
     * Gets the global Gson instance for creating and reading JSON files
     * @return Global Gson instance
     */
    public static Gson getGsonInstance() {
        return GSON;
    }

    private void checkFiles() throws IOException {
        if (Files.notExists(DATA_PATH)) {
            DebugLogger.info("Creating assets");
            Files.createDirectory(DATA_PATH);
        }

        final Object[] paths = RecordUtils.getComponents(getDataPaths());
        for (final Object obj : paths) {
            if (obj instanceof Path path && Files.notExists(path)) {
                final String fileName = path.getFileName().toString();
                if (fileName.contains(".")) {
                    if (path.toString().contains("screens")) {
                        if (Files.notExists(DATA_PATH.resolve("screens"))) {
                            DebugLogger.info("Creating asset [{}]", path);
                            Files.createDirectory(DATA_PATH.resolve("screens"));
                        }
                        NativeUtils.moveResource("screens/" + fileName, path);
                    } else {
                        NativeUtils.moveResource(fileName, path);
                    }
                } else {
                    DebugLogger.info("Creating directory [{}]", path);
                    Files.createDirectory(path);
                    String file = RenderUtils.EMPTY_STR;
                    switch (fileName) {
                        case "lang" -> {
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(NativeUtils.getResource("lang"))) {
                                for (final Path langPath : stream) {
                                    DebugLogger.info("Copying language [{}]", langPath);
                                    file = langPath.getFileName().toString();
                                    NativeUtils.moveResource("lang/" + file, getDataPaths().translationsPath().resolve(file));
                                }
                            } catch (IOException e) {
                                DebugLogger.warn("Failed to load language [{}]", file);
                            }
                        }
                        case "asset" -> {
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(NativeUtils.getResource("asset"))) {
                                for (final Path assetPath : stream) {
                                    DebugLogger.info("Copying asset [{}]", assetPath);
                                    file = assetPath.getFileName().toString();
                                    NativeUtils.moveResource("asset/" + file, getDataPaths().imagesPath().resolve(file));
                                }
                            } catch (IOException e) {
                                DebugLogger.warn("Failed to load asset [{}]", file);
                            }
                        }
                        case "audio" -> {
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(NativeUtils.getResource("audio"))) {
                                for (final Path audioPath : stream) {
                                    DebugLogger.info("Copying audio [{}]", audioPath);
                                    file = audioPath.getFileName().toString();
                                    NativeUtils.moveResource("audio/" + file, getDataPaths().audioPath().resolve(file));
                                }
                            } catch (IOException e) {
                                DebugLogger.warn("Failed to load audio [{}]", file);
                            }
                        }
                        case "sfx" -> {
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(NativeUtils.getResource("sfx"))) {
                                for (final Path sfxPath : stream) {
                                    DebugLogger.info("Copying sfx [{}]", sfxPath);
                                    file = sfxPath.getFileName().toString();
                                    NativeUtils.moveResource("sfx/" + file, getDataPaths().sfxPath().resolve(file));
                                }
                            } catch (IOException e) {
                                DebugLogger.warn("Failed to load sfx [{}]", file);
                            }
                        }
                        default -> DebugLogger.warn("Checking an invalid file [{}]; skipping", file);
                    }
                }
            }
        }
        if (JAR_FILESYSTEM != null) {
            JAR_FILESYSTEM.close();
        }
        NativeUtils.cleanup();
    }

    /**
     * Exits to the main menu safely
     * @param save If the game should be saved
     */
    public static void returnToMainMenu(final boolean save) {
        final Thread waitingThread = new Thread(() -> {
            // Wait for rendering to be done
            try {
                RENDERING_LOCK.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // Wait for the game to be done
            if (Game.getInstance() != null) {
                Game.getInstance().markAsEnded();
            }
            try {
                GAME_LOCK.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (save) {
                // Save
                Game.getInstance().saveGame();
            }

            // Clean up
            Renderer.RENDER_QUEUE.clear();
            if (Game.getInstance() != null) {
                Game.endInstance();
            }
            renderer.returnToMainMenu();

            // Release locks
            RENDERING_LOCK.release();
            GAME_LOCK.release();
        });
        waitingThread.start();
    }

    /**
     * Gets access to the Renderer
     * @return instance of Renderer
     */
    public static Renderer getRenderer() {
        return renderer;
    }

    /**
     * Runs the UI rendering code every frame.<p>
     * The frame rate can be controlled with {@link TrailApplication#MS_PER_FRAME}
     */
    @Override
    public void process() {
        // Lock while rendering
        try {
            RENDERING_LOCK.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        renderer.render();
        RENDERING_LOCK.release();

        renderer.renderCursor();
        if (MS_PER_FRAME != 0) {
            try {
                // Render cursor at 2x the frame rate for maximum smoothness
                Thread.sleep((long) MS_PER_FRAME / 2);
                renderer.renderCursor();
                Thread.sleep((long) MS_PER_FRAME / 2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void preRun() {
        super.preRun();
        GLFW.glfwMaximizeWindow(this.getHandle());
    }

    /**
     * Runs ImGUI's initialization code
     * @param config ImGUI config instance
     */
    @Override
    protected void initImGui(final Configuration config) {
        super.initImGui(config);
        ImGui.getIO().setIniFilename(null); // don't clutter working directory with imgui.ini

        // Set font to Cowboy Pixel
        final byte[] ttfData;
        try (InputStream in = TrailApplication.class.getResourceAsStream("/font/PixelCowboy.ttf")) {
            ttfData = in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ImGui.getIO().getFonts().setFreeTypeRenderer(true);
        ImGui.getIO().getFonts().addFontDefault();
        final ImFontConfig fontConfig = new ImFontConfig();
        ImGui.getIO().setFontDefault(ImGui.getIO().getFonts().addFontFromMemoryTTF(ttfData, 96, fontConfig));
        ImGui.getIO().getFonts().build();
        fontConfig.destroy();
    }

    /**
     * Runs when the Window appears
     * @param config Window config
     */
    @Override
    protected void initWindow(final Configuration config) {
        super.initWindow(config);
        RenderUtils.setWindowHandle(this.getHandle());
        RenderUtils.setIcon();
        Registry.loadImages();
        Registry.loadAudio();
        Registry.loadSFX();
        SoundEngine.loadSong(Audio.MENU_MUSIC, true);
    }

    /**
     * Configures the main Window
     * @param config Window config
     */
    @Override
    protected void configure(final Configuration config) {
        super.configure(config);
        config.setTitle("Westward");
    }

    /**
     * Runs when ImGUI closes (Window is closed)
     */
    @Override
    protected void disposeImGui() {
        super.disposeImGui();
        SoundEngine.destroy();
        System.exit(0);
    }

    /**
     * Sets the framerate limit
     * @param fps The Frames Per Second limit
     */
    public static void setFPSLimit(int fps) {
        if (fps <= 0) {
            MS_PER_FRAME = 0;
        } else {
            MS_PER_FRAME = 1F / fps * 1000;
        }
    }
}
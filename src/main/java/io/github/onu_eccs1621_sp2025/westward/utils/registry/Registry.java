package io.github.onu_eccs1621_sp2025.westward.utils.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.onu_eccs1621_sp2025.westward.TrailApplication;
import io.github.onu_eccs1621_sp2025.westward.data.Landmark;
import io.github.onu_eccs1621_sp2025.westward.data.SaveData;
import io.github.onu_eccs1621_sp2025.westward.data.StatusContainer;
import io.github.onu_eccs1621_sp2025.westward.data.member.Role;
import io.github.onu_eccs1621_sp2025.westward.game.ItemStack;
import io.github.onu_eccs1621_sp2025.westward.game.event.Event;
import io.github.onu_eccs1621_sp2025.westward.screen.InventoryViewer;
import io.github.onu_eccs1621_sp2025.westward.screen.Screen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.IntroScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.LossScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.SleepScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.VictoryScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.accident.ConsequenceScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.accident.PerilScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.accident.PerilScreenData;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.hunting.HuntingConfig;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.hunting.HuntingGameScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.landmark.LandmarkScreenData;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.landmark.LandmarkScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.river.RiverCrossingScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.shop.ShopScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.shop.ShopScreenData;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.trading.TradingScreen;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.trading.TradingScreenData;
import io.github.onu_eccs1621_sp2025.westward.screen.default_screens.travel.TravelingScreen;
import io.github.onu_eccs1621_sp2025.westward.utils.DebugLogger;
import io.github.onu_eccs1621_sp2025.westward.utils.ListUtils;
import io.github.onu_eccs1621_sp2025.westward.utils.rendering.RenderUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages all data-driven assets and organizes them in a simplistic way.
 * The class also loads all assets when the game is started.
 * @author Dylan Catte, Ben Westover, Noah Sumerauer, Micah Lee
 * @since 1.0.0 Alpha 1
 * @version 1.2
 */
@SuppressWarnings("DataFlowIssue") // Used to ignore IDE thinking there are NPEs
public class Registry {
    /**
     * GSON instance
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /**
     * Item registry
     */
    private static final Map<String, ItemStack> ITEMS;
    /**
     * Status registry
     */
    private static final Map<String, StatusContainer> STATUSES;
    /**
     * Screen / Screen Data registry
     */
    private static final Map<String, Screen> SCREENS;
    /**
     * Hunting Screen Data registry
     */
    private static final Map<String, Screen> HUNTING_SCREENS;
    /**
     * Event registry
     */
    private static final Map<String, Event> EVENTS;
    /**
     * Landmark registry
     */
    private static final List<Landmark> LANDMARKS;
    /**
     * Role registry
     */
    private static final Map<String, Role> ROLES;
    /**
     * Image / Asset registry
     */
    private static Map<String, Long> images;
    /**
     * Music registry
     */
    private static Map<String, Path> audio;
    /**
     * Sound Effect registry
     */
    private static Map<String, Path> sfx;
    /**
     * Game saves
     */
    private static final List<String> SAVES = new ArrayList<>();
    /**
     * Game save dates
     */
    private static final List<String> SAVES_DATE = new ArrayList<>();

    // Load Assets
    static {
        // Load statuses
        Map<String, StatusContainer> statuses;
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().statusesPath())) {
            List<StatusContainer> statusList = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<StatusContainer>>(){}.getType());
            statuses = new HashMap<>(getCapacity(statusList.size()));
            for (StatusContainer status : statusList) {
                statuses.put(status.getName(), status);
            }
        } catch (IOException e) {
            DebugLogger.error("Failed to load statuses.json", e);
            statuses = new HashMap<>();
        }
        // Hard coded hunger status
        statuses.put("hunger", new StatusContainer("hunger", (byte) 1, (byte) 5, "status.hunger.obtained", false, 0.1F));
        STATUSES = statuses;

        // Load items
        Map<String, ItemStack> items;
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().itemsPath())) {
            List<ItemStack> itemStackList = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<ItemStack>>(){}.getType());
            items = new HashMap<>(getCapacity(itemStackList.size()));
            for (ItemStack itemStack : itemStackList) {
                items.put(itemStack.getId(), itemStack);
            }
        } catch (IOException e) {
            DebugLogger.error("Failed to load items.json", e);
            items = new HashMap<>();
        }
        ITEMS = items;

        // Load landmarks
        List<Landmark> landmarks;
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().landmarksPath())) {
            landmarks = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<Landmark>>(){}.getType());
            // Sort list based on miles
            Collections.sort(landmarks);
        } catch (IOException e) {
            DebugLogger.error("Failed to load landmarks.json", e);
            landmarks = new ArrayList<>();
        }
        LANDMARKS = landmarks;

        // Load events
        Map<String, Event> events;
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().eventsPath())) {
            List<Event> eventList = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<Event>>(){}.getType());
            events = new HashMap<>(getCapacity(eventList.size()));
            for (Event event : eventList) {
                events.put(event.name(), event);
            }
        } catch (IOException e) {
            DebugLogger.error("Failed to load events.json", e);
            events = new HashMap<>();
        }
        EVENTS = events;

        // Load screens
        Map<String, Screen> screens;
        Map<String, Screen> huntingScreens;
        List<PerilScreenData> perilScreenList = null;
        List<LandmarkScreenData> landmarkScreenList = null;
        List<ShopScreenData> shopScreenList = null;
        List<TradingScreenData> tradingScreenList = null;
        List<HuntingConfig> huntingGameScreenList = null;

        int totalScreens = 0;
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().perilScreensPath())) {
            perilScreenList = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<PerilScreenData>>(){}.getType());
            totalScreens += perilScreenList.size();
        } catch (IOException e) {
            DebugLogger.error("Failed to load peril screens from JSON", e);
        }
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().landmarkScreensPath())) {
            landmarkScreenList = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<LandmarkScreenData>>(){}.getType());
            totalScreens += landmarkScreenList.size();
        } catch (IOException e) {
            DebugLogger.error("Failed to load landmark screens from JSON", e);
        }
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().shopScreensPath())) {
            shopScreenList = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<ShopScreenData>>(){}.getType());
        } catch (IOException e) {
            DebugLogger.error("Failed to load shop screens from JSON", e);
        }
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().tradingScreensPath())) {
            tradingScreenList = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<TradingScreenData>>(){}.getType());
        } catch (IOException e) {
            DebugLogger.error("Failed to load trading screens from JSON", e);
        }
        int totalHuntingScreens = 0;
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().huntingGameScreensPath())) {
            huntingGameScreenList = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<HuntingConfig>>(){}.getType());
            totalHuntingScreens += huntingGameScreenList.size();
        } catch (IOException e) {
            DebugLogger.error("Failed to load hunting screens from JSON", e);
        }

        screens = new HashMap<>(getCapacity(totalScreens));
        for (PerilScreenData screenData : perilScreenList) {
            screens.put(screenData.id(), new PerilScreen(screenData));
        }
        for (LandmarkScreenData screenData : landmarkScreenList) {
            screens.put(screenData.id(), new LandmarkScreen(screenData));
        }
        for (ShopScreenData screenData : shopScreenList) {
            screens.put(screenData.id(), new ShopScreen(screenData));
        }
        for (TradingScreenData screenData : tradingScreenList) {
            screens.put(screenData.id(), new TradingScreen(screenData));
        }
        huntingScreens = new HashMap<>(getCapacity(totalHuntingScreens));
        for (HuntingConfig screenData : huntingGameScreenList) {
            huntingScreens.put(screenData.id(), new HuntingGameScreen(screenData));
        }

        screens.put("travel", new TravelingScreen());
        screens.put("consequence", new ConsequenceScreen());
        screens.put("victory", new VictoryScreen());
        screens.put("loss", new LossScreen());
        screens.put("intro", new IntroScreen());
        screens.put("inventory", new InventoryViewer());
        screens.put("sleep", new SleepScreen());
        screens.put("river", new RiverCrossingScreen());
        SCREENS = screens;
        HUNTING_SCREENS = huntingScreens;

        // Load roles
        Map<String, Role> roles;
        try (BufferedReader reader = Files.newBufferedReader(TrailApplication.getDataPaths().rolesPath())) {
            List<Role> roleList = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<List<Role>>(){}.getType());
            roles = new HashMap<>(getCapacity(roleList.size()));
            for (Role role : roleList) {
                roles.put(role.id(), role);
            }
        } catch (IOException e) {
            DebugLogger.error("Failed to load roles.json", e);
            roles = new HashMap<>();
        }
        ROLES = roles;

        // Load saves
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(TrailApplication.getDataPaths().savesDirectoryPath())) {
            for (Path save : stream) {
                SAVES.add(save.getFileName().toString().replace(".json", ""));
                SaveData data;

                try (BufferedReader reader = Files.newBufferedReader(save)) {
                    data = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<SaveData>(){}.getType());
                } catch (IOException e) {
                    DebugLogger.error("Failed to load {}", save.getFileName(), e);
                    data = null;
                }
                SAVES_DATE.add(data.dateSaved());
            }
        } catch (IOException e) {
            DebugLogger.error("Failed to load saves", e);
        }
    }

    /**
     * Gets a Gson instance with pretty printing (formatted printing) enabled
     * @return Registry Gson instance
     */
    public static Gson getGsonInstance() {
        return GSON;
    }

    /**
     * Loads the image texture ids
     */
    public static void loadImages() {
        // Load assets
        Map<String, Long> images = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(TrailApplication.getDataPaths().imagesPath())) {
            for (Path path : stream) {
                long imageId = RenderUtils.loadTextureFromFile(path.toString());
                String name = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf("."));
                images.put(name, imageId);
            }
        } catch (IOException e) {
            DebugLogger.error("Failed to load images", e);
        }
        Registry.images = images;
    }

    /**
     * Loads OGG file paths
     */
    public static void loadAudio() {
        // Load audio
        Map<String, Path> audio = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(TrailApplication.getDataPaths().audioPath())) {
            for (Path path : stream) {
                String name = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf("."));
                audio.put(name, path);
            }
        } catch (IOException e) {
            DebugLogger.error("Failed to load audio", e);
        }
        Registry.audio = audio;
    }

    /**
     * Loads WAV sound effect paths
     */
    public static void loadSFX() {
        // Load sfx
        Map<String, Path> sfx = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(TrailApplication.getDataPaths().sfxPath())) {
            for (Path path : stream) {
                String name = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf("."));
                sfx.put(name, path);
            }
        } catch (IOException e) {
            DebugLogger.error("Failed to load sfx", e);
        }
        Registry.sfx = sfx;
    }

    private static int getCapacity(final int elements) {
        return (int) Math.ceil(elements / 0.75 + 1);
    }

    /**
     * Gets the specified asset from the Registry.
     * @param assetType The expected type of asset
     * @param identifier The identifier for the asset<p>
     *                   For landmarks and saves, this can be the index as a String
     * @return The asset requested, or null if the asset does not exist
     */
    public static Object getAsset(final AssetType assetType, final String identifier) {
        switch (assetType) {
            case ITEM -> {
                return ITEMS.get(identifier);
            }
            case STATUS -> {
                return STATUSES.get(identifier);
            }
            case SCREEN -> {
                return SCREENS.get(identifier);
            }
            case HUNTING_SCREEN -> {
                return HUNTING_SCREENS.get(identifier);
            }
            case EVENT -> {
                return EVENTS.get(identifier);
            }
            case LANDMARK -> {
                return LANDMARKS.get(Integer.parseInt(identifier));
            }
            case ROLE -> {
                return ROLES.get(identifier);
            }
            case ASSET -> {
                return images.get(identifier);
            }
            case AUDIO -> {
                return audio.get(identifier);
            }
            case SFX -> {
                return sfx.get(identifier);
            }
            case SAVE -> {
                String file = SAVES.get(Integer.parseInt(identifier));
                Path path = TrailApplication.getDataPaths().savesDirectoryPath().resolve(file + ".json");
                SaveData data;
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    data = TrailApplication.getGsonInstance().fromJson(reader, new TypeToken<SaveData>(){}.getType());
                } catch (IOException e) {
                    DebugLogger.error("Failed to load {}.json", file, e);
                    data = null;
                }
                return data;
            }
            default -> {
                DebugLogger.warn("Resource [{}] not found for type [{}]", identifier, assetType);
                return null;
            }
        }
    }

    /**
     * Gets the save name and save date for an index
     * @param i Index of the save data
     * @return Save name [0] and Save date [1]
     */
    public static String[] getSaveData(final int i) {
        return new String[] {
                SAVES.get(i),
                SAVES_DATE.get(i)
        };
    }

    /**
     * Replaces the date of the save file
     * @param name Name of save
     */
    public static void replaceSaveData(final String name) {
        if (SAVES.contains(name)) {
            int index = SAVES.indexOf(name);
            SAVES_DATE.set(index, SaveData.getTimeNow());
        } else {
            registerAsset(AssetType.SAVE, name);
        }
    }

    /**
     * Removes the save data from the Registry.
     * Also deletes the save data file
     * @param i Index of the save
     */
    public static void removeSaveData(final int i) {
        SAVES_DATE.remove(i);
        String save = SAVES.remove(i);
        Path path = TrailApplication.getDataPaths().savesDirectoryPath().resolve(save + ".json");
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes the save data from the Registry.
     * Also deletes the save data file
     * @param name The name of the save file
     */
    public static void removeSaveData(final String name) {
        int index = SAVES.indexOf(name);
        if (index != -1) {
            SAVES.remove(index);
            SAVES_DATE.remove(index);
        }

        Path gameDir = TrailApplication.getDataPaths().savesDirectoryPath().resolve(name + ".json");
        if (Files.exists(gameDir)) {
            gameDir.toFile().delete();
        }
    }

    /**
     * Gets if the Registry contains a specific asset
     * @param type The type of asset
     * @param id The ID of the asset
     * @return If the asset exists in the Registry
     */
    public static boolean containsAsset(final AssetType type, final String id) {
        return switch (type) {
            case ITEM -> ITEMS.containsKey(id);
            case STATUS -> STATUSES.containsKey(id);
            case SCREEN -> SCREENS.containsKey(id);
            case EVENT -> EVENTS.containsKey(id);
            case LANDMARK -> {
                for (Landmark landmark : LANDMARKS) {
                    if (landmark.name().equals(id)) {
                        yield true;
                    }
                }
                yield false;
            }
            case ROLE -> ROLES.containsKey(id);
            case ASSET -> images.containsKey(id);
            case AUDIO -> audio.containsKey(id);
            case SFX -> sfx.containsKey(id);
            case SAVE -> SAVES.contains(id);
            case HUNTING_SCREEN -> HUNTING_SCREENS.containsKey(id);
        };
    }

    /**
     * Gets a landmark from the Registry
     * @param index The index of the landmark in the Registry
     * @return The landmark at the index
     */
    public static Landmark getLandmarkAsset(final int index) {
        return LANDMARKS.get(index);
    }

    /**
     * Gets all assets of a given type.
     * @param assetType The requested asset type
     * @return The asset requested, or null if the asset does not exist
     */
    public static Object[] getAssets(final AssetType assetType) {
        switch (assetType) {
            case ITEM -> {
                return ITEMS.values().toArray(new ItemStack[0]);
            }
            case STATUS -> {
                return STATUSES.values().toArray(new StatusContainer[0]);
            }
            case SCREEN -> {
                return SCREENS.values().toArray(new Screen[0]);
            }
            case EVENT -> {
                return EVENTS.values().toArray(new Event[0]);
            }
            case LANDMARK -> {
                return LANDMARKS.toArray(new Landmark[0]);
            }
            case ROLE -> {
                return ROLES.values().toArray(new Role[0]);
            }
            case ASSET -> {
                return images.values().toArray(new Long[0]);
            }
            case AUDIO -> {
                return audio.values().toArray(new Path[0]);
            }
            case SAVE -> {
                return SAVES.toArray();
            }
            default -> {
                DebugLogger.warn("Type [{}] not found", assetType);
                return new Object[0];
            }
        }
    }

    /**
     * Gets all identifiers from a given asset type.
     * @param assetType The requested asset type
     * @return The asset identifier requested, or null if the asset does not exist
     */
    public static String[] getAssetIdentifiers(final AssetType assetType) {
        switch (assetType) {
            case ITEM -> {
                return ITEMS.keySet().toArray(new String[0]);
            }
            case STATUS -> {
                return STATUSES.keySet().toArray(new String[0]);
            }
            case SCREEN -> {
                return SCREENS.keySet().toArray(new String[0]);
            }
            case EVENT -> {
                return EVENTS.keySet().toArray(new String[0]);
            }
            case LANDMARK -> {
                // Landmarks don't have identifiers
                return new String[0];
            }
            case ROLE -> {
                return ROLES.keySet().toArray(new String[0]);
            }
            case ASSET -> {
                return images.keySet().toArray(new String[0]);
            }
            case AUDIO -> {
                return audio.keySet().toArray(new String[0]);
            }
            default -> {
                DebugLogger.warn("Type [{}] not found", assetType);
                return new String[0];
            }
        }
    }

    /**
     * Gets the index of the asset in the Registry
     * @param type Asset type
     * @param identifier The asset's identifier
     * @return The index of the asset in its Hashmap/List.<p>
     *     Returns -1 if an index is not found
     */
    public static int indexOf(final AssetType type, final String identifier) {
        String[] ids = getAssetIdentifiers(type);
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(identifier)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets a random asset from the Registry
     * @param assetType Asset type being retrieved
     * @return Random asset
     */
    public static Object randomAsset(final AssetType assetType) {
        return switch (assetType) {
            case ITEM -> ListUtils.getRandomElement(ITEMS.values());
            case STATUS -> ListUtils.getRandomElement(STATUSES.values());
            case SCREEN -> ListUtils.getRandomElement(SCREENS.values());
            case EVENT -> ListUtils.getRandomElement(EVENTS.values());
            case LANDMARK -> ListUtils.getRandomElement(LANDMARKS);
            case ROLE -> ListUtils.getRandomElement(ROLES.values());
            case ASSET -> ListUtils.getRandomElement(images.values());
            case AUDIO -> ListUtils.getRandomElement(audio.values());
            case SFX -> ListUtils.getRandomElement(sfx.values());
            case SAVE -> ListUtils.getRandomElement(SAVES);
            case HUNTING_SCREEN -> ListUtils.getRandomElement(HUNTING_SCREENS.values());
        };
    }

    /**
     * Gets the amount of assets loaded for a specific type
     * @param assetType The requested type of asset
     * @return The number of assets in the type
     */
    public static int getLoadedAssetsCount(final AssetType assetType) {
        switch (assetType) {
            case ITEM -> {
                return ITEMS.size();
            }
            case STATUS -> {
                return STATUSES.size();
            }
            case SCREEN -> {
                return SCREENS.size();
            }
            case EVENT -> {
                return EVENTS.size();
            }
            case LANDMARK -> {
                return LANDMARKS.size();
            }
            case ROLE -> {
                return ROLES.size();
            }
            case ASSET -> {
                return images.size();
            }
            case SAVE -> {
                return SAVES.size();
            }
            default -> {
                DebugLogger.warn("Type [{}] not found", assetType);
                return 0;
            }
        }
    }

    /**
     * Adds assets during runtime
     * @param assetType The type of asset adding
     * @param asset     The asset
     */
    public static void registerAsset(final AssetType assetType, final Object asset) {
        switch (assetType) {
            case ITEM -> {
                if (ITEMS.containsKey(((ItemStack)asset).getId())) {
                    return;
                }
                ITEMS.put(((ItemStack)asset).getId(), (ItemStack) asset);
                saveAssets(AssetType.ITEM);
            }
            case STATUS -> {
                if (STATUSES.containsKey(((StatusContainer)asset).getName())) {
                    return;
                }
                STATUSES.put(((StatusContainer)asset).getName(), (StatusContainer) asset);
                saveAssets(AssetType.STATUS);
            }
            case ROLE -> {
                if (ROLES.containsKey(((Role)asset).id())) {
                    return;
                }
                ROLES.put(((Role)asset).id(), (Role) asset);
                saveAssets(AssetType.ROLE);
            }
            case EVENT -> {
                if (EVENTS.containsKey(((Event)asset).name())) {
                    return;
                }
                EVENTS.put(((Event)asset).name(), (Event) asset);
                saveAssets(AssetType.EVENT);
            }
            case LANDMARK -> {
                if (LANDMARKS.contains(asset)) {
                    return;
                }
                LANDMARKS.add((Landmark) asset);
                saveAssets(AssetType.LANDMARK);
            }
            case ASSET -> {
                if (images.containsKey(((Long)asset).toString())) {
                    return;
                }
                images.put(((Long)asset).toString(), (Long) asset);
                saveAssets(AssetType.ASSET);
            }
            case AUDIO -> {
                if (audio.containsKey(((Path)asset).toString())) {
                    return;
                }
                audio.put(((Path)asset).toString(), (Path) asset);
                saveAssets(AssetType.AUDIO);
            }
            case SFX -> {
                if (sfx.containsKey(((Path)asset).toString())) {
                    return;
                }
                sfx.put(((Path)asset).toString(), (Path) asset);
                saveAssets(AssetType.SFX);
            }
            case SCREEN -> {
                if (SCREENS.containsKey(((Screen)asset).getId())) {
                    return;
                }
                SCREENS.put(((Screen)asset).getId(), (Screen) asset);
                saveAssets(AssetType.SCREEN);
            }
            case SAVE -> {
                if (SAVES.contains(asset)) {
                    return;
                }
                SAVES.add((String) asset);
                SAVES_DATE.add(SaveData.getTimeNow());
            }
        }
    }

    /**
     * Loads the assets from their respected directories.
     */
    public static void loadAssets() {}

    private static void saveAssets(final AssetType type) {
        switch (type) {
            case ITEM -> {
                Path path = TrailApplication.getDataPaths().itemsPath();
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(ITEMS.values(), new TypeToken<List<ItemStack>>(){}.getType(), writer);
                    DebugLogger.info("Saved items to [{}]", path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case STATUS -> {
                Path path = TrailApplication.getDataPaths().statusesPath();
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(STATUSES.values(), new TypeToken<List<StatusContainer>>(){}.getType(), writer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case EVENT -> {
                Path path = TrailApplication.getDataPaths().eventsPath();
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(EVENTS.values(), new TypeToken<List<Event>>(){}.getType(), writer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case LANDMARK -> {
                Path path = TrailApplication.getDataPaths().landmarksPath();
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(LANDMARKS, new TypeToken<List<Landmark>>(){}.getType(), writer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case ROLE -> {
                Path path = TrailApplication.getDataPaths().rolesPath();
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(ROLES.values(), new TypeToken<List<Role>>(){}.getType(), writer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case ASSET -> {
                Path path = TrailApplication.getDataPaths().imagesPath();
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(images.values(), new TypeToken<List<Long>>(){}.getType(), writer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case AUDIO -> {
                Path path = TrailApplication.getDataPaths().audioPath();
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(audio.values(), new TypeToken<List<Path>>(){}.getType(), writer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * The type of asset for the Registry to locate
     */
    public enum AssetType {
        @SerializedName("item")
        ITEM,
        @SerializedName("status")
        STATUS,
        SCREEN,
        EVENT,
        LANDMARK,
        ROLE,
        ASSET,
        AUDIO,
        SFX,
        SAVE,
        HUNTING_SCREEN,
    }
}

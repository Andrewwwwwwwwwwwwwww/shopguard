package io.github.andrewwwwwwwwwwwwwww.shopguard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ShopGuard configuration, persisted to {@code config/shopguard.json}. Moderators cap how much a
 * player can claim so nobody fences off half the world.
 */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Maximum footprint (in blocks/columns) of a single claim. */
    public int maxClaimArea = 10000;      // e.g. 100 x 100

    /** Maximum total footprint a single player may own across all their claims. */
    public int maxTotalPerPlayer = 40000;

    /** If false (default), non-owners can't use buttons/levers in a claim (prevents redstone griefing);
     *  doors/gates/trapdoors/pressure plates for walking through are always allowed. */
    public boolean allowRedstoneControls = false;

    public static Config load() {
        Path path = path();
        try {
            if (Files.exists(path)) {
                Config cfg = GSON.fromJson(Files.readString(path), Config.class);
                if (cfg != null) {
                    cfg.save();
                    return cfg;
                }
            }
        } catch (Exception e) {
            ShopGuard.LOGGER.error("Failed to load config; using defaults", e);
        }
        Config cfg = new Config();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            ShopGuard.LOGGER.error("Failed to save config", e);
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("shopguard.json");
    }
}

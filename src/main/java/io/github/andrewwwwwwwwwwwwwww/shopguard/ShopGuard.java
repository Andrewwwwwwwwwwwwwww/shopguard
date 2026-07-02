package io.github.andrewwwwwwwwwwwwwww.shopguard;

import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.ClaimStore;
import io.github.andrewwwwwwwwwwwwwww.shopguard.command.ShopGuardCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ShopGuard — server-side land-claim grief protection.
 *
 * <p>Players claim a region with a golden shovel (rectangle, then carve to fit), which protects it
 * from block break/place and container theft by non-owners while still letting visitors walk through
 * and trade at shops. Admins can define zones that restrict where claiming is allowed.
 */
public class ShopGuard implements ModInitializer {
    public static final String MOD_ID = "shopguard";
    public static final Logger LOGGER = LoggerFactory.getLogger("ShopGuard");

    public static MinecraftServer server;
    public static final ClaimStore STORE = new ClaimStore();
    public static Config CONFIG = new Config();

    @Override
    public void onInitialize() {
        LOGGER.info("ShopGuard initializing");

        CONFIG = Config.load();

        ClaimTool.register();       // registered first so shovel clicks are consumed before protection
        AdminZoneTool.register();
        ProtectionHandler.register();
        ClaimVisualizer.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ShopGuardCommands.register(dispatcher));

        // Per-player session state (border toggle, pending tool corners/modes) resets on leave.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, srv) -> {
            var uid = handler.player.getUUID();
            ClaimVisualizer.clearZoneViewer(uid);
            ClaimTool.clearPlayer(uid);
            AdminZoneTool.clearPlayer(uid);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(srv -> {
            server = srv;
            STORE.load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(srv -> {
            STORE.save();
            server = null;
        });
    }
}

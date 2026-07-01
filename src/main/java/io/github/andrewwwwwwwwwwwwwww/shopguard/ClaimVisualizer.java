package io.github.andrewwwwwwwwwwwwwww.shopguard;

import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.Claim;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Shows claim boundaries to nearby players with particles. This runs server-side and pushes the
 * particles straight to each player, so it works on vanilla clients — no client mod required. Every
 * ~0.75s it traces the outline of each claim within range of a player.
 */
public final class ClaimVisualizer {
    private ClaimVisualizer() {}

    private static final int INTERVAL = 15;   // ticks between outline pulses
    private static final double RANGE = 48.0; // only outline claims within this of the player

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % INTERVAL != 0) return;
            for (ServerLevel level : server.getAllLevels()) {
                List<ServerPlayer> players = level.players();
                if (players.isEmpty()) continue;
                String dim = level.dimension().identifier().toString();
                for (Claim c : ShopGuard.STORE.all()) {
                    if (!c.dimension.equals(dim) || c.shape.isEmpty()) continue;
                    int[] segs = c.shape.boundaryFlat();
                    for (ServerPlayer p : players) {
                        double px = p.getX(), pz = p.getZ(), py = p.getY() + 0.15;
                        if (c.shape.maxX() < px - RANGE || c.shape.minX() > px + RANGE
                                || c.shape.maxZ() < pz - RANGE || c.shape.minZ() > pz + RANGE) continue;
                        for (int i = 0; i + 3 < segs.length; i += 4) {
                            double mx = (segs[i] + segs[i + 2]) / 2.0;
                            double mz = (segs[i + 1] + segs[i + 3]) / 2.0;
                            if (Math.abs(mx - px) > RANGE || Math.abs(mz - pz) > RANGE) continue;
                            level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, true,
                                    mx, py, mz, 1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
                }
            }
        });
    }
}

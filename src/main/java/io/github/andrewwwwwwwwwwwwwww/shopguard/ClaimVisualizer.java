package io.github.andrewwwwwwwwwwwwwww.shopguard;

import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.AdminZone;
import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.Claim;
import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.ClaimShape;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shows claim boundaries with particles — but only to players holding the claim tool (golden shovel),
 * so the outlines aren't always cluttering the view. Runs server-side and pushes particles straight
 * to each player, so it needs no client mod. Touching claims of the same owner render as one merged
 * outline (the shared border isn't drawn). Recomputes from the live claim store every pulse, and
 * {@link #refresh} lets edits (add/carve/remove) redraw immediately instead of waiting for the next.
 */
public final class ClaimVisualizer {
    private ClaimVisualizer() {}

    private static final int INTERVAL = 15;   // ticks between outline pulses
    private static final double RANGE = 48.0; // only outline claims within this of the player

    /** Players who toggled `/claim zones` on — they see admin-zone borders regardless of held item. */
    private static final Set<UUID> ZONE_VIEWERS = Collections.synchronizedSet(new java.util.HashSet<>());

    public static boolean toggleZoneViewer(UUID player) {
        if (ZONE_VIEWERS.remove(player)) return false;
        ZONE_VIEWERS.add(player);
        return true;
    }

    public static void clearZoneViewer(UUID player) {
        ZONE_VIEWERS.remove(player);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % INTERVAL != 0) return;
            for (ServerLevel level : server.getAllLevels()) drawForLevel(level);
        });
    }

    /** Immediately redraw outlines in a level — call right after a claim is added, carved, or removed. */
    public static void refresh(ServerLevel level) {
        drawForLevel(level);
    }

    private static void drawForLevel(ServerLevel level) {
        String dim = level.dimension().identifier().toString();
        drawZones(level, dim);

        List<ServerPlayer> holders = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (holdingTool(p)) holders.add(p);
        if (holders.isEmpty()) return;
        for (Claim c : ShopGuard.STORE.all()) {
            if (!c.dimension.equals(dim) || c.shape.isEmpty()) continue;

            // Merge with the owner's other claims: an edge is drawn only if the neighbour cell
            // isn't covered by any of this owner's claims in the dimension.
            UUID owner = c.owner;
            List<Claim> ownerClaims = new ArrayList<>();
            for (Claim o : ShopGuard.STORE.all())
                if (o.owner.equals(owner) && o.dimension.equals(dim)) ownerClaims.add(o);
            ClaimShape.ColumnTest ownerCovers = (x, z) -> {
                for (Claim o : ownerClaims) if (o.shape.contains(x, z)) return true;
                return false;
            };
            int[] segs = c.shape.boundaryFlat(ownerCovers);

            for (ServerPlayer p : holders) {
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

    /** Outline admin zones (end-rod particles) for players who toggled `/claim zones` on. */
    private static void drawZones(ServerLevel level, String dim) {
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (ZONE_VIEWERS.contains(p.getUUID())) viewers.add(p);
        if (viewers.isEmpty()) return;

        for (AdminZone z : ShopGuard.STORE.zones()) {
            if (!z.dimension.equals(dim)) continue;
            for (ServerPlayer p : viewers) {
                double px = p.getX(), pz = p.getZ(), py = p.getY() + 0.15;
                if (z.maxX + 1 < px - RANGE || z.minX > px + RANGE
                        || z.maxZ + 1 < pz - RANGE || z.minZ > pz + RANGE) continue;
                // Trace the zone's rectangle border at 1-block steps (borders sit on the outer grid edges).
                for (int x = z.minX; x <= z.maxX + 1; x++) {
                    spawnZoneDot(level, p, px, pz, py, x, z.minZ);
                    spawnZoneDot(level, p, px, pz, py, x, z.maxZ + 1);
                }
                for (int zz = z.minZ; zz <= z.maxZ + 1; zz++) {
                    spawnZoneDot(level, p, px, pz, py, z.minX, zz);
                    spawnZoneDot(level, p, px, pz, py, z.maxX + 1, zz);
                }
            }
        }
    }

    private static void spawnZoneDot(ServerLevel level, ServerPlayer p,
                                     double px, double pz, double py, double x, double z) {
        if (Math.abs(x - px) > RANGE || Math.abs(z - pz) > RANGE) return;
        level.sendParticles(p, ParticleTypes.END_ROD, true, true, x, py, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static boolean holdingTool(ServerPlayer p) {
        return p.getMainHandItem().getItem() == Items.GOLDEN_SHOVEL
                || p.getOffhandItem().getItem() == Items.GOLDEN_SHOVEL;
    }
}

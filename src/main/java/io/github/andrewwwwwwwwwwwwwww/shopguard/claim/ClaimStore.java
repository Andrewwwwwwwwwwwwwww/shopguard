package io.github.andrewwwwwwwwwwwwwww.shopguard.claim;

import io.github.andrewwwwwwwwwwwwwww.shopguard.ShopGuard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All claims and admin zones, persisted to {@code <world>/shopguard/claims.dat}. */
public final class ClaimStore {
    private final Map<Long, Claim> claims = new HashMap<>();
    private final Map<Long, AdminZone> zones = new HashMap<>();
    private long nextId = 1;

    // ---- claim lookup ----

    /** The claim covering (x,z) in the given dimension, or null. */
    public Claim claimAt(String dim, int x, int z) {
        for (Claim c : claims.values())
            if (c.dimension.equals(dim) && c.contains(x, z)) return c;
        return null;
    }

    public Collection<Claim> all() { return claims.values(); }

    public List<Claim> byOwner(UUID owner) {
        List<Claim> out = new ArrayList<>();
        for (Claim c : claims.values()) if (c.owner.equals(owner)) out.add(c);
        return out;
    }

    public int totalCellsOfOwner(UUID owner) {
        int n = 0;
        for (Claim c : claims.values()) if (c.owner.equals(owner)) n += c.shape.count();
        return n;
    }

    /** True if {@code shape} overlaps any claim whose id is not in {@code excludeIds}, in the dimension. */
    public boolean overlapsOther(String dim, ClaimShape shape, java.util.Set<Long> excludeIds) {
        for (Claim c : claims.values())
            if (!excludeIds.contains(c.id) && c.dimension.equals(dim) && c.shape.overlaps(shape)) return true;
        return false;
    }

    /** Remove without persisting (caller saves once after a batch, e.g. a merge). */
    public void removeNoSave(long id) {
        claims.remove(id);
    }

    // ---- admin zones ----

    public boolean hasZones(String dim) {
        for (AdminZone z : zones.values()) if (z.dimension.equals(dim)) return true;
        return false;
    }

    public boolean inAnyZone(String dim, int x, int z) {
        for (AdminZone zn : zones.values()) if (zn.dimension.equals(dim) && zn.contains(x, z)) return true;
        return false;
    }

    /** True if this shape may be claimed: all cells inside a zone, or no zones exist for the dimension. */
    public boolean allowedByZones(String dim, ClaimShape shape) {
        if (!hasZones(dim)) return true;
        return shape.allColumnsMatch((x, z) -> inAnyZone(dim, x, z));
    }

    /** Add a rectangle to the zone set; any touching zones (and the rect) merge into one zone. */
    public AdminZone addZone(String dim, int x1, int z1, int x2, int z2) {
        ClaimShape merged = new ClaimShape();
        merged.addRect(x1, z1, x2, z2);
        List<AdminZone> absorb = new ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (AdminZone z : zones.values()) {
                if (!z.dimension.equals(dim) || absorb.contains(z)) continue;
                if (merged.touches(z.shape)) {
                    merged.union(z.shape);
                    absorb.add(z);
                    changed = true;
                }
            }
        }
        AdminZone result;
        if (!absorb.isEmpty()) {
            result = absorb.get(0);
            for (int i = 1; i < absorb.size(); i++) zones.remove(absorb.get(i).id);
        } else {
            result = new AdminZone(nextId++, dim);
            zones.put(result.id, result);
        }
        result.shape = merged;
        save();
        return result;
    }

    /** Carve a rectangle out of the zone containing (x1,z1). Null if that spot isn't zoned;
     *  the zone is deleted if carved to nothing. */
    public AdminZone carveZone(String dim, int x1, int z1, int x2, int z2) {
        AdminZone target = null;
        for (AdminZone z : zones.values())
            if (z.dimension.equals(dim) && z.contains(x1, z1)) { target = z; break; }
        if (target == null) return null;
        target.shape.removeRect(x1, z1, x2, z2);
        if (target.shape.isEmpty()) zones.remove(target.id);
        save();
        return target;
    }

    public boolean removeZone(long id) { boolean r = zones.remove(id) != null; if (r) save(); return r; }
    public Collection<AdminZone> zones() { return zones.values(); }

    // ---- claim mutation ----

    public Claim newClaim(UUID owner, String ownerName, String dim) {
        Claim c = new Claim(nextId++, owner, ownerName, dim);
        claims.put(c.id, c);
        return c;
    }
    public boolean remove(long id) { boolean r = claims.remove(id) != null; if (r) save(); return r; }

    // ---- persistence ----

    private Path file() {
        MinecraftServer server = ShopGuard.server;
        return server.getWorldPath(LevelResource.ROOT).resolve("shopguard").resolve("claims.dat");
    }

    public void save() {
        if (ShopGuard.server == null) return;
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            CompoundTag root = new CompoundTag();
            root.putLong("NextId", nextId);
            ListTag cl = new ListTag();
            for (Claim c : claims.values()) cl.add(cl.size(), c.save());
            root.put("Claims", cl);
            ListTag zl = new ListTag();
            for (AdminZone z : zones.values()) zl.add(zl.size(), z.save());
            root.put("Zones", zl);
            NbtIo.writeCompressed(root, file);
        } catch (Exception e) {
            ShopGuard.LOGGER.error("Failed to save claims", e);
        }
    }

    public void load() {
        claims.clear();
        zones.clear();
        nextId = 1;
        if (ShopGuard.server == null) return;
        Path file = file();
        if (!Files.exists(file)) return;
        try {
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            nextId = root.getLongOr("NextId", 1);
            ListTag cl = root.getListOrEmpty("Claims");
            for (int i = 0; i < cl.size(); i++) {
                Claim c = Claim.load(cl.getCompoundOrEmpty(i));
                claims.put(c.id, c);
            }
            ListTag zl = root.getListOrEmpty("Zones");
            for (int i = 0; i < zl.size(); i++) {
                AdminZone z = AdminZone.load(zl.getCompoundOrEmpty(i));
                zones.put(z.id, z);
            }
            ShopGuard.LOGGER.info("Loaded {} claim(s), {} zone(s)", claims.size(), zones.size());
        } catch (Exception e) {
            ShopGuard.LOGGER.error("Failed to load claims", e);
        }
    }
}

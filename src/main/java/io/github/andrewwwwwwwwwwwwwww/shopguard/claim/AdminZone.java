package io.github.andrewwwwwwwwwwwwwww.shopguard.claim;

import net.minecraft.nbt.CompoundTag;

/** A moderator-defined rectangular region (full height) within which players are allowed to claim. */
public final class AdminZone {
    public final long id;
    public String dimension;
    public int minX, minZ, maxX, maxZ;

    public AdminZone(long id, String dimension, int x1, int z1, int x2, int z2) {
        this.id = id;
        this.dimension = dimension;
        this.minX = Math.min(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxZ = Math.max(z1, z2);
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("Id", id);
        t.putString("Dim", dimension);
        t.putInt("MinX", minX); t.putInt("MinZ", minZ);
        t.putInt("MaxX", maxX); t.putInt("MaxZ", maxZ);
        return t;
    }

    public static AdminZone load(CompoundTag t) {
        return new AdminZone(t.getLongOr("Id", 0), t.getStringOr("Dim", "minecraft:overworld"),
                t.getIntOr("MinX", 0), t.getIntOr("MinZ", 0), t.getIntOr("MaxX", 0), t.getIntOr("MaxZ", 0));
    }
}

package io.github.andrewwwwwwwwwwwwwww.shopguard.claim;

import net.minecraft.nbt.CompoundTag;

/**
 * A moderator-defined region (full height) within which players are allowed to claim. Shaped exactly
 * like a claim: a rectilinear {@link ClaimShape}, so admins can add rectangles and carve pieces out
 * with the zone tool. Touching zones merge into one.
 */
public final class AdminZone {
    public final long id;
    public String dimension;
    public ClaimShape shape;

    public AdminZone(long id, String dimension) {
        this.id = id;
        this.dimension = dimension;
        this.shape = new ClaimShape();
    }

    public boolean contains(int x, int z) {
        return shape.contains(x, z);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("Id", id);
        t.putString("Dim", dimension);
        t.put("Shape", shape.save());
        return t;
    }

    public static AdminZone load(CompoundTag t) {
        AdminZone z = new AdminZone(t.getLongOr("Id", 0), t.getStringOr("Dim", "minecraft:overworld"));
        if (t.contains("Shape")) {
            z.shape = ClaimShape.load(t.getCompoundOrEmpty("Shape"));
        } else {
            // Legacy rectangle format (pre-0.7): MinX/MinZ/MaxX/MaxZ.
            z.shape.addRect(t.getIntOr("MinX", 0), t.getIntOr("MinZ", 0),
                    t.getIntOr("MaxX", 0), t.getIntOr("MaxZ", 0));
        }
        return z;
    }
}

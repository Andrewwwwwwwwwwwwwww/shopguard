package io.github.andrewwwwwwwwwwwwwww.shopguard.claim;

import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One player-owned claim: an owner, a dimension, and a full-height {@link ClaimShape} footprint. */
public final class Claim {
    public final long id;
    public UUID owner;
    public String ownerName;
    public String dimension;
    public final ClaimShape shape;
    public final Set<UUID> trusted = new HashSet<>();

    public Claim(long id, UUID owner, String ownerName, String dimension) {
        this(id, owner, ownerName, dimension, new ClaimShape());
    }

    private Claim(long id, UUID owner, String ownerName, String dimension, ClaimShape shape) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.dimension = dimension;
        this.shape = shape;
    }

    public boolean contains(int x, int z) { return shape.contains(x, z); }

    /** May this player modify blocks / open containers here? Owner or explicitly trusted. */
    public boolean mayBuild(UUID player) { return player.equals(owner) || trusted.contains(player); }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("Id", id);
        t.putString("Owner", owner.toString());
        t.putString("OwnerName", ownerName == null ? "" : ownerName);
        t.putString("Dim", dimension);
        t.put("Shape", shape.save());
        if (!trusted.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (UUID u : trusted) { if (sb.length() > 0) sb.append(','); sb.append(u); }
            t.putString("Trusted", sb.toString());
        }
        return t;
    }

    public static Claim load(CompoundTag t) {
        long id = t.getLongOr("Id", 0);
        UUID owner = UUID.fromString(t.getStringOr("Owner", new UUID(0, 0).toString()));
        String ownerName = t.getStringOr("OwnerName", "?");
        String dim = t.getStringOr("Dim", "minecraft:overworld");
        ClaimShape shape = ClaimShape.load(t.getCompoundOrEmpty("Shape"));
        Claim c = new Claim(id, owner, ownerName, dim, shape);
        String trusted = t.getStringOr("Trusted", "");
        if (!trusted.isEmpty()) {
            for (String s : trusted.split(",")) {
                try { c.trusted.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
            }
        }
        return c;
    }
}

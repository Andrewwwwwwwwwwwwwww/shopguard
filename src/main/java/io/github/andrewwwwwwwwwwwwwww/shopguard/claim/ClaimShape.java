package io.github.andrewwwwwwwwwwwwwww.shopguard.claim;

import net.minecraft.nbt.CompoundTag;

import java.util.BitSet;

/**
 * A rectilinear region in the X/Z plane at block resolution, stored as a bounding box plus a bitset
 * of which columns inside that box belong to the region. Supports union (add a rectangle) and
 * difference (carve a rectangle), so a claim can be any shape while {@link #contains} stays O(1).
 *
 * <p>Regions are full-height: only X/Z matter.
 */
public final class ClaimShape {
    private int minX;
    private int minZ;
    private int width;   // 0 when the shape is empty
    private int depth;
    private BitSet cells;

    public ClaimShape() {
        this.cells = new BitSet();
    }

    private ClaimShape(int minX, int minZ, int width, int depth, BitSet cells) {
        this.minX = minX;
        this.minZ = minZ;
        this.width = width;
        this.depth = depth;
        this.cells = cells;
    }

    /** A deep copy, for validating an edit before committing it to the real shape. */
    public ClaimShape copy() {
        return new ClaimShape(minX, minZ, width, depth, (BitSet) cells.clone());
    }

    public boolean isEmpty() { return width == 0 || cells.isEmpty(); }
    public int count() { return cells.cardinality(); }
    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return minX + width - 1; }
    public int maxZ() { return minZ + depth - 1; }

    public boolean contains(int x, int z) {
        if (width == 0 || x < minX || z < minZ || x > maxX() || z > maxZ()) return false;
        return cells.get((x - minX) + (z - minZ) * width);
    }

    /** Union with an axis-aligned rectangle (inclusive corners). Grows the bounding box if needed. */
    public void addRect(int ax, int az, int bx, int bz) {
        int rx1 = Math.min(ax, bx), rz1 = Math.min(az, bz);
        int rx2 = Math.max(ax, bx), rz2 = Math.max(az, bz);
        if (width == 0) {
            minX = rx1; minZ = rz1; width = rx2 - rx1 + 1; depth = rz2 - rz1 + 1;
            cells = new BitSet(width * depth);
        } else if (rx1 < minX || rz1 < minZ || rx2 > maxX() || rz2 > maxZ()) {
            reframe(Math.min(minX, rx1), Math.min(minZ, rz1), Math.max(maxX(), rx2), Math.max(maxZ(), rz2));
        }
        for (int z = rz1; z <= rz2; z++)
            for (int x = rx1; x <= rx2; x++)
                cells.set((x - minX) + (z - minZ) * width);
    }

    /** Difference: remove an axis-aligned rectangle from the region (bounding box is left as-is). */
    public void removeRect(int ax, int az, int bx, int bz) {
        if (width == 0) return;
        int rx1 = Math.max(minX, Math.min(ax, bx)), rz1 = Math.max(minZ, Math.min(az, bz));
        int rx2 = Math.min(maxX(), Math.max(ax, bx)), rz2 = Math.min(maxZ(), Math.max(az, bz));
        for (int z = rz1; z <= rz2; z++)
            for (int x = rx1; x <= rx2; x++)
                cells.clear((x - minX) + (z - minZ) * width);
    }

    /** True if any column is set in both shapes. */
    public boolean overlaps(ClaimShape other) {
        if (width == 0 || other.width == 0) return false;
        int lx = Math.max(minX, other.minX), lz = Math.max(minZ, other.minZ);
        int hx = Math.min(maxX(), other.maxX()), hz = Math.min(maxZ(), other.maxZ());
        for (int z = lz; z <= hz; z++)
            for (int x = lx; x <= hx; x++)
                if (contains(x, z) && other.contains(x, z)) return true;
        return false;
    }

    /** True if every set column satisfies {@code test} (used for the admin-zone containment check). */
    public boolean allColumnsMatch(ColumnTest test) {
        for (int z = 0; z < depth; z++)
            for (int x = 0; x < width; x++)
                if (cells.get(x + z * width) && !test.test(minX + x, minZ + z)) return false;
        return true;
    }

    @FunctionalInterface
    public interface ColumnTest { boolean test(int x, int z); }

    /** Outline of this region alone (every side whose neighbour is outside this shape). */
    public int[] boundaryFlat() {
        return boundaryFlat(this::contains);
    }

    /**
     * The outline of the region as a flat array of grid-aligned unit edges — every side of a claimed
     * column whose neighbour is <em>not</em> covered by {@code neighbourCovered} — as
     * {@code [x1,z1,x2,z2, ...]} in world grid coordinates. Passing a predicate that also covers the
     * owner's other claims makes touching same-owner claims render as one merged outline.
     */
    public int[] boundaryFlat(ColumnTest neighbourCovered) {
        java.util.List<int[]> segs = new java.util.ArrayList<>();
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                if (!cells.get(x + z * width)) continue;
                int gx = minX + x, gz = minZ + z;
                if (!neighbourCovered.test(gx, gz - 1)) segs.add(new int[]{gx, gz, gx + 1, gz});         // north
                if (!neighbourCovered.test(gx, gz + 1)) segs.add(new int[]{gx, gz + 1, gx + 1, gz + 1}); // south
                if (!neighbourCovered.test(gx - 1, gz)) segs.add(new int[]{gx, gz, gx, gz + 1});         // west
                if (!neighbourCovered.test(gx + 1, gz)) segs.add(new int[]{gx + 1, gz, gx + 1, gz + 1}); // east
            }
        }
        int[] flat = new int[segs.size() * 4];
        for (int i = 0; i < segs.size(); i++) {
            int[] s = segs.get(i);
            flat[i * 4] = s[0]; flat[i * 4 + 1] = s[1]; flat[i * 4 + 2] = s[2]; flat[i * 4 + 3] = s[3];
        }
        return flat;
    }

    /** Re-allocate the bitset onto a larger bounding box, preserving set columns. */
    private void reframe(int nMinX, int nMinZ, int nMaxX, int nMaxZ) {
        int nWidth = nMaxX - nMinX + 1, nDepth = nMaxZ - nMinZ + 1;
        BitSet nCells = new BitSet(nWidth * nDepth);
        for (int z = 0; z < depth; z++)
            for (int x = 0; x < width; x++)
                if (cells.get(x + z * width))
                    nCells.set((minX + x - nMinX) + (minZ + z - nMinZ) * nWidth);
        minX = nMinX; minZ = nMinZ; width = nWidth; depth = nDepth; cells = nCells;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("MinX", minX);
        t.putInt("MinZ", minZ);
        t.putInt("W", width);
        t.putInt("D", depth);
        t.putByteArray("Cells", cells.toByteArray());
        return t;
    }

    public static ClaimShape load(CompoundTag t) {
        int minX = t.getIntOr("MinX", 0);
        int minZ = t.getIntOr("MinZ", 0);
        int width = t.getIntOr("W", 0);
        int depth = t.getIntOr("D", 0);
        BitSet cells = BitSet.valueOf(t.getByteArray("Cells").orElse(new byte[0]));
        return new ClaimShape(minX, minZ, width, depth, cells);
    }
}

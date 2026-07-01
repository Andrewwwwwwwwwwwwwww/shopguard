package io.github.andrewwwwwwwwwwwwwww.shopguard;

/**
 * ShopGuard tunables. Defaults for now; file-backed loading comes with the config phase. Moderators
 * cap how much a player can claim so nobody fences off half the world.
 */
public final class Config {
    private Config() {}

    /** Maximum footprint (in blocks/columns) of a single claim. */
    public static int maxClaimArea = 10000;      // e.g. 100 x 100

    /** Maximum total footprint a single player may own across all their claims. */
    public static int maxTotalPerPlayer = 40000;
}

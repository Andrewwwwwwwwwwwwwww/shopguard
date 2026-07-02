package io.github.andrewwwwwwwwwwwwwww.shopguard;

import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.Claim;
import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.ClaimShape;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The golden-shovel claim tool.
 * <ul>
 *   <li>Right-click two corners → apply the current mode to that rectangle.</li>
 *   <li>Right-click the air → toggle between CLAIM (add area) and CARVE (remove area).</li>
 * </ul>
 * Adding a rectangle that touches any of your own claims merges them all into one claim (so touching
 * claims are truly one). The shovel does NOT hijack left-click, so you can still break blocks with it.
 */
public final class ClaimTool {
    private ClaimTool() {}

    private static final Map<UUID, Boolean> CARVE_MODE = new HashMap<>();
    private static final Map<UUID, BlockPos> PENDING = new HashMap<>();

    public static void register() {
        // Right-click a block: set a corner (applies the current mode on the second corner).
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (player.getItemInHand(hand).getItem() != Items.GOLDEN_SHOVEL) return InteractionResult.PASS;
            handleCorner(sp, hit.getBlockPos());
            return InteractionResult.SUCCESS; // consume — also cancels vanilla path-making
        });
        // Right-click the air: toggle CLAIM <-> CARVE mode.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (player.getItemInHand(hand).getItem() != Items.GOLDEN_SHOVEL) return InteractionResult.PASS;
            toggleMode(sp);
            return InteractionResult.SUCCESS;
        });
    }

    private static void toggleMode(ServerPlayer sp) {
        UUID uid = sp.getUUID();
        boolean carve = !CARVE_MODE.getOrDefault(uid, false);
        CARVE_MODE.put(uid, carve);
        PENDING.remove(uid);
        sp.sendSystemMessage(Component.literal("Shovel mode: " + (carve ? "CARVE (remove area)" : "CLAIM (add area)"))
                .withStyle(carve ? ChatFormatting.GOLD : ChatFormatting.GREEN));
    }

    private static void handleCorner(ServerPlayer sp, BlockPos pos) {
        UUID uid = sp.getUUID();
        boolean carve = CARVE_MODE.getOrDefault(uid, false);
        BlockPos first = PENDING.remove(uid);
        if (first == null) {
            PENDING.put(uid, pos.immutable());
            sp.sendSystemMessage(Component.literal(
                    (carve ? "Carve" : "Claim") + ": first corner set — right-click the opposite corner.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (carve) carve(sp, first, pos); else add(sp, first, pos);
    }

    private static String dim(ServerPlayer sp) {
        return sp.level().dimension().identifier().toString();
    }

    private static void add(ServerPlayer sp, BlockPos a, BlockPos b) {
        String dim = dim(sp);
        UUID uid = sp.getUUID();
        boolean op = ProtectionHandler.isOp(sp);

        ClaimShape merged = new ClaimShape();
        merged.addRect(a.getX(), a.getZ(), b.getX(), b.getZ());

        // Flood-merge with all of the player's claims (in this dimension) that touch the result.
        List<Claim> absorb = new ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Claim c : ShopGuard.STORE.all()) {
                if (!c.owner.equals(uid) || !c.dimension.equals(dim) || absorb.contains(c)) continue;
                if (merged.touches(c.shape)) {
                    merged.union(c.shape);
                    absorb.add(c);
                    changed = true;
                }
            }
        }

        Set<Long> absorbedIds = new HashSet<>();
        int absorbedCells = 0;
        for (Claim c : absorb) { absorbedIds.add(c.id); absorbedCells += c.shape.count(); }

        if (ShopGuard.STORE.overlapsOther(dim, merged, absorbedIds)) {
            error(sp, "That overlaps another player's claim.");
            return;
        }
        if (!ShopGuard.STORE.allowedByZones(dim, merged)) {
            error(sp, "Claims can only be made inside a claim zone here.");
            return;
        }
        if (merged.count() > ShopGuard.CONFIG.maxClaimArea) {
            error(sp, "Too big — max " + ShopGuard.CONFIG.maxClaimArea + " blocks per claim.");
            return;
        }
        int ownerTotal = ShopGuard.STORE.totalCellsOfOwner(uid) - absorbedCells + merged.count();
        if (!op && ownerTotal > ShopGuard.CONFIG.maxTotalPerPlayer) {
            error(sp, "That would exceed your total claim limit (" + ShopGuard.CONFIG.maxTotalPerPlayer + " blocks).");
            return;
        }

        boolean mergedExisting = !absorb.isEmpty();
        Claim result;
        if (mergedExisting) {
            result = absorb.get(0); // keep the first claim's id, absorb the rest
            for (int i = 1; i < absorb.size(); i++) ShopGuard.STORE.removeNoSave(absorb.get(i).id);
        } else {
            result = ShopGuard.STORE.newClaim(uid, sp.getName().getString(), dim);
        }
        result.shape = merged;
        ShopGuard.STORE.save();
        ClaimVisualizer.refresh(sp.level());
        ok(sp, (mergedExisting ? "Claim updated" : "Claim created") + " — " + result.shape.count() + " blocks.");
    }

    private static void carve(ServerPlayer sp, BlockPos a, BlockPos b) {
        String dim = dim(sp);
        UUID uid = sp.getUUID();
        boolean op = ProtectionHandler.isOp(sp);
        Claim target = ShopGuard.STORE.claimAt(dim, a.getX(), a.getZ());
        if (target == null || (!target.owner.equals(uid) && !op)) {
            error(sp, "Carve from inside your own claim (first corner must be claimed land).");
            return;
        }
        target.shape.removeRect(a.getX(), a.getZ(), b.getX(), b.getZ());
        if (target.shape.isEmpty()) {
            ShopGuard.STORE.remove(target.id);
            ok(sp, "Claim removed (carved to nothing).");
        } else {
            ShopGuard.STORE.save();
            ok(sp, "Carved — " + target.shape.count() + " blocks remain.");
        }
        ClaimVisualizer.refresh(sp.level());
    }

    private static void error(ServerPlayer sp, String msg) {
        sp.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
    }
    private static void ok(ServerPlayer sp, String msg) {
        sp.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GREEN));
    }
}

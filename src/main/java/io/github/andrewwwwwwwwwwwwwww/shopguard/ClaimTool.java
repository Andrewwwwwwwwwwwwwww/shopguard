package io.github.andrewwwwwwwwwwwwwww.shopguard;

import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.Claim;
import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.ClaimShape;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The golden-shovel claim tool. Right-click two opposite corners to <b>add</b> a rectangle (creates a
 * new claim, or extends the one you clicked inside); sneak + right-click two corners to <b>carve</b> a
 * rectangle back out. Consumes the click so the golden shovel no longer makes dirt paths.
 */
public final class ClaimTool {
    private ClaimTool() {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> LAST_ATTACK = new HashMap<>();

    private record Pending(BlockPos first, boolean carve) {}

    public static void register() {
        // Right-click two corners: add a rectangle to your claim.
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (player.getItemInHand(hand).getItem() != Items.GOLDEN_SHOVEL) return InteractionResult.PASS;
            handle(sp, hit.getBlockPos(), false);
            return InteractionResult.SUCCESS; // consume — also cancels vanilla path-making
        });
        // Left-click ("dig") two corners: carve a rectangle back out of your claim.
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (player.getItemInHand(hand).getItem() != Items.GOLDEN_SHOVEL) return InteractionResult.PASS;
            long now = sp.level().getGameTime();
            Long last = LAST_ATTACK.get(sp.getUUID());
            if (last == null || now - last >= 4) { // debounce a held left-click
                LAST_ATTACK.put(sp.getUUID(), now);
                handle(sp, pos, true);
            }
            return InteractionResult.FAIL; // the golden shovel is a claim tool — don't dig real blocks with it
        });
    }

    private static void handle(ServerPlayer sp, BlockPos pos, boolean carve) {
        UUID uid = sp.getUUID();
        Pending pend = PENDING.get(uid);
        if (pend == null || pend.carve() != carve) {
            PENDING.put(uid, new Pending(pos.immutable(), carve));
            sp.sendSystemMessage(Component.literal(carve
                    ? "Carve: first corner set — dig (left-click) the opposite corner."
                    : "Claim: first corner set — right-click the opposite corner.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        BlockPos a = pend.first();
        PENDING.remove(uid);
        if (carve) carve(sp, a, pos); else add(sp, a, pos);
    }

    private static String dim(ServerPlayer sp) {
        return sp.level().dimension().identifier().toString();
    }

    private static void add(ServerPlayer sp, BlockPos a, BlockPos b) {
        String dim = dim(sp);
        UUID uid = sp.getUUID();
        boolean op = ProtectionHandler.isOp(sp);
        Claim target = ShopGuard.STORE.claimAt(dim, a.getX(), a.getZ());
        if (target != null && !target.owner.equals(uid) && !op) {
            error(sp, "That corner is inside another player's claim.");
            return;
        }
        Claim editing = (target != null && (target.owner.equals(uid) || op)) ? target : null;

        ClaimShape sim = editing != null ? editing.shape.copy() : new ClaimShape();
        sim.addRect(a.getX(), a.getZ(), b.getX(), b.getZ());

        long excludeId = editing != null ? editing.id : -1L;
        if (ShopGuard.STORE.overlapsOther(dim, sim, excludeId)) { error(sp, "That overlaps another player's claim."); return; }
        if (!ShopGuard.STORE.allowedByZones(dim, sim)) { error(sp, "Claims can only be made inside a claim zone here."); return; }
        if (sim.count() > ShopGuard.CONFIG.maxClaimArea) { error(sp, "Too big — max " + ShopGuard.CONFIG.maxClaimArea + " blocks per claim."); return; }
        int ownerTotal = ShopGuard.STORE.totalCellsOfOwner(uid)
                - (editing != null ? editing.shape.count() : 0) + sim.count();
        if (!op && ownerTotal > ShopGuard.CONFIG.maxTotalPerPlayer) {
            error(sp, "That would exceed your total claim limit (" + ShopGuard.CONFIG.maxTotalPerPlayer + " blocks).");
            return;
        }

        boolean created = editing == null;
        if (created) {
            editing = ShopGuard.STORE.newClaim(uid, sp.getName().getString(), dim);
        }
        editing.shape.addRect(a.getX(), a.getZ(), b.getX(), b.getZ());
        ShopGuard.STORE.save();
        ok(sp, (created ? "Claim created" : "Claim extended") + " — " + editing.shape.count() + " blocks.");
    }

    private static void carve(ServerPlayer sp, BlockPos a, BlockPos b) {
        String dim = dim(sp);
        UUID uid = sp.getUUID();
        boolean op = ProtectionHandler.isOp(sp);
        Claim target = ShopGuard.STORE.claimAt(dim, a.getX(), a.getZ());
        if (target == null || (!target.owner.equals(uid) && !op)) {
            error(sp, "Sneak-carve from inside your own claim.");
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
    }

    private static void error(ServerPlayer sp, String msg) {
        sp.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
    }
    private static void ok(ServerPlayer sp, String msg) {
        sp.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GREEN));
    }
}

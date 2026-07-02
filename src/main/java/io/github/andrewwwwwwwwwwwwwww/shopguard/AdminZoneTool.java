package io.github.andrewwwwwwwwwwwwwww.shopguard;

import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.AdminZone;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ops-only admin zone tool (golden hoe) — works exactly like the claim shovel, but on admin zones:
 * <ul>
 *   <li>Right-click two corners → apply the current mode (ADD a rectangle / CARVE one out).</li>
 *   <li>Right-click the sky → toggle ADD ↔ CARVE mode.</li>
 *   <li>Corners raycast up to {@link ClaimTool#CORNER_RANGE} blocks, so far corners work by aiming.</li>
 * </ul>
 * Touching zones merge into one; carving a zone to nothing deletes it. Zone borders show while the
 * hoe is held (see {@link ClaimVisualizer}). Non-ops use the hoe as a normal hoe.
 */
public final class AdminZoneTool {
    private AdminZoneTool() {}

    private static final Map<UUID, Boolean> CARVE_MODE = new HashMap<>();
    private static final Map<UUID, BlockPos> PENDING = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (player.getItemInHand(hand).getItem() != Items.GOLDEN_HOE || !ProtectionHandler.isOp(sp)) {
                return InteractionResult.PASS;
            }
            if (!ClaimTool.debounce(sp, LAST_ACTION)) handleCorner(sp, hit.getBlockPos());
            return InteractionResult.SUCCESS; // consume — don't till farmland with the admin tool
        });
        // Out-of-reach clicks: a block in sight sets that corner; a true sky-aim toggles the mode.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (player.getItemInHand(hand).getItem() != Items.GOLDEN_HOE || !ProtectionHandler.isOp(sp)) {
                return InteractionResult.PASS;
            }
            if (!ClaimTool.debounce(sp, LAST_ACTION)) {
                HitResult hit = sp.pick(ClaimTool.CORNER_RANGE, 1.0f, false);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    handleCorner(sp, ((BlockHitResult) hit).getBlockPos());
                } else {
                    toggleMode(sp);
                }
            }
            return InteractionResult.SUCCESS;
        });
    }

    /** Forget a player's pending corner and mode (called on disconnect). */
    public static void clearPlayer(UUID uid) {
        CARVE_MODE.remove(uid);
        PENDING.remove(uid);
        LAST_ACTION.remove(uid);
    }

    private static void toggleMode(ServerPlayer sp) {
        UUID uid = sp.getUUID();
        boolean carve = !CARVE_MODE.getOrDefault(uid, false);
        CARVE_MODE.put(uid, carve);
        PENDING.remove(uid);
        sp.sendOverlayMessage(Component.literal("Zone tool mode: " + (carve ? "CARVE (remove area)" : "ADD (zone area)"))
                .withStyle(carve ? ChatFormatting.GOLD : ChatFormatting.DARK_RED));
    }

    private static void handleCorner(ServerPlayer sp, BlockPos pos) {
        UUID uid = sp.getUUID();
        boolean carve = CARVE_MODE.getOrDefault(uid, false);
        BlockPos first = PENDING.remove(uid);
        if (first == null) {
            PENDING.put(uid, pos.immutable());
            sp.sendOverlayMessage(Component.literal(
                    (carve ? "Zone carve" : "Zone") + ": first corner set — right-click the opposite corner.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        String dim = sp.level().dimension().identifier().toString();
        if (carve) {
            AdminZone z = ShopGuard.STORE.carveZone(dim, first.getX(), first.getZ(), pos.getX(), pos.getZ());
            if (z == null) {
                sp.sendSystemMessage(Component.literal("Carve from inside a zone (first corner must be zoned).")
                        .withStyle(ChatFormatting.RED));
            } else if (z.shape.isEmpty()) {
                sp.sendSystemMessage(Component.literal("Zone #" + z.id + " removed (carved to nothing).")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                sp.sendSystemMessage(Component.literal("Zone #" + z.id + " carved — " + z.shape.count() + " blocks remain.")
                        .withStyle(ChatFormatting.GREEN));
            }
        } else {
            AdminZone z = ShopGuard.STORE.addZone(dim, first.getX(), first.getZ(), pos.getX(), pos.getZ());
            sp.sendSystemMessage(Component.literal("Zone #" + z.id + " set — " + z.shape.count() + " blocks.")
                    .withStyle(ChatFormatting.GREEN));
        }
        ClaimVisualizer.refresh(sp.level());
    }
}

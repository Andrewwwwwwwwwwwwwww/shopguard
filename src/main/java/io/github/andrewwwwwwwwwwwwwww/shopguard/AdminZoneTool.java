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
 * Ops-only admin tool for defining claim zones by hand: hold a golden hoe and right-click two corners
 * to create a zone (the precise counterpart to {@code /claim zone add}). Non-ops use the hoe normally.
 */
public final class AdminZoneTool {
    private AdminZoneTool() {}

    private static final Map<UUID, BlockPos> PENDING = new HashMap<>();

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (player.getItemInHand(hand).getItem() != Items.GOLDEN_HOE || !ProtectionHandler.isOp(sp)) {
                return InteractionResult.PASS;
            }
            handle(sp, hit.getBlockPos());
            return InteractionResult.SUCCESS; // consume — don't till farmland with the admin tool
        });
        // Out-of-reach clicks: raycast so admins can set far corners of a big district by aiming.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (player.getItemInHand(hand).getItem() != Items.GOLDEN_HOE || !ProtectionHandler.isOp(sp)) {
                return InteractionResult.PASS;
            }
            HitResult hit = sp.pick(ClaimTool.CORNER_RANGE, 1.0f, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                handle(sp, ((BlockHitResult) hit).getBlockPos());
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS; // sky-aim with the hoe does nothing
        });
    }

    private static void handle(ServerPlayer sp, BlockPos pos) {
        UUID uid = sp.getUUID();
        BlockPos first = PENDING.remove(uid);
        if (first == null) {
            PENDING.put(uid, pos.immutable());
            sp.sendOverlayMessage(Component.literal("Zone: first corner set — right-click the opposite corner.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        String dim = sp.level().dimension().identifier().toString();
        AdminZone z = ShopGuard.STORE.addZone(dim, first.getX(), first.getZ(), pos.getX(), pos.getZ());
        sp.sendSystemMessage(Component.literal("Added claim zone #" + z.id + " ("
                + z.minX + "," + z.minZ + " to " + z.maxX + "," + z.maxZ + ").").withStyle(ChatFormatting.GREEN));
    }
}

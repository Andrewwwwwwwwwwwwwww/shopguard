package io.github.andrewwwwwwwwwwwwwww.shopguard;

import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.Claim;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Applies claim protection to the world. Non-owners inside someone's claim cannot break/place blocks
 * or open containers, and cannot touch item frames / armor stands / paintings — but they can still
 * walk through (doors, gates, trapdoors, buttons, levers, pressure plates) and trade with entities
 * (villager shops), so a claimed shopping district stays fully shoppable. Ops bypass everything.
 */
public final class ProtectionHandler {
    private ProtectionHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (!(player instanceof ServerPlayer sp)) return true;
            Claim claim = claimAt(sp, pos);
            if (claim == null || mayBuild(sp, claim)) return true;
            notifyBlocked(sp, claim);
            return false; // cancel the break
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            Claim claim = claimAt(sp, pos);

            // Interacting with a block that is itself inside a claim you can't build in.
            if (claim != null && !mayBuild(sp, claim)) {
                BlockState clicked = world.getBlockState(pos);
                if (isMovementInteract(clicked)) return InteractionResult.PASS;         // doors/gates/trapdoors/plates
                if (isRedstoneControl(clicked)) {                                       // buttons/levers
                    if (ShopGuard.CONFIG.allowRedstoneControls) return InteractionResult.PASS;
                    notifyBlocked(sp, claim);
                    return InteractionResult.FAIL;
                }
                if (world.getBlockEntity(pos) instanceof Container) {                   // chests/barrels/furnaces/etc.
                    notifyBlocked(sp, claim);
                    return InteractionResult.FAIL;
                }
                if (!player.getItemInHand(hand).isEmpty()) {                            // placement / bucket / flint / etc.
                    notifyBlocked(sp, claim);
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;                                          // empty-hand click on a plain block
            }

            // Placing something (block, bucket) whose target lands in an adjacent claim — even when the
            // clicked block is outside it (e.g. pouring a bucket across the border).
            if (!player.getItemInHand(hand).isEmpty()) {
                BlockPos placePos = pos.relative(hit.getDirection());
                Claim placeClaim = claimAt(sp, placePos);
                if (placeClaim != null && !mayBuild(sp, placeClaim)) {
                    notifyBlocked(sp, placeClaim);
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> guardEntity(player, world, entity));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> guardEntity(player, world, entity));
    }

    private static InteractionResult guardEntity(Player player, Level world, Entity entity) {
        if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!(entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof Painting)) {
            return InteractionResult.PASS; // villagers, animals, etc. stay interactive (shops work)
        }
        Claim claim = claimAt(sp, entity.blockPosition());
        if (claim == null || mayBuild(sp, claim)) return InteractionResult.PASS;
        notifyBlocked(sp, claim);
        return InteractionResult.FAIL;
    }

    // ---- helpers ----

    public static Claim claimAt(ServerPlayer sp, BlockPos pos) {
        String dim = sp.level().dimension().identifier().toString();
        return ShopGuard.STORE.claimAt(dim, pos.getX(), pos.getZ());
    }

    public static boolean isOp(ServerPlayer sp) {
        return Commands.LEVEL_GAMEMASTERS.check(sp.permissions());
    }

    private static boolean mayBuild(ServerPlayer sp, Claim claim) {
        return isOp(sp) || claim.mayBuild(sp.getUUID());
    }

    /** Blocks a visitor may use to move through a build — always allowed. */
    private static boolean isMovementInteract(BlockState s) {
        return s.getBlock() instanceof DoorBlock
            || s.getBlock() instanceof TrapDoorBlock
            || s.getBlock() instanceof FenceGateBlock
            || s.getBlock() instanceof BasePressurePlateBlock;
    }

    /** Redstone controls — allowed for visitors only if {@code allowRedstoneControls} is enabled. */
    private static boolean isRedstoneControl(BlockState s) {
        return s.getBlock() instanceof ButtonBlock
            || s.getBlock() instanceof LeverBlock;
    }

    private static void notifyBlocked(ServerPlayer sp, Claim claim) {
        // Overlay (action-bar) messages replace each other, so repeated blocked clicks won't spam chat.
        sp.sendOverlayMessage(Component.literal("Claimed by " + claim.ownerName).withStyle(ChatFormatting.RED));
    }
}

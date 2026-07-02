package io.github.andrewwwwwwwwwwwwwww.shopguard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import io.github.andrewwwwwwwwwwwwwww.shopguard.ClaimVisualizer;
import io.github.andrewwwwwwwwwwwwwww.shopguard.ProtectionHandler;
import io.github.andrewwwwwwwwwwwwwww.shopguard.ShopGuard;
import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.AdminZone;
import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.Claim;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * The {@code /claim} command tree, kept deliberately small:
 * <ul>
 *   <li>{@code /claim} — everything about your situation: the claim you're standing in, your claims,
 *       and your used/max totals.</li>
 *   <li>{@code /claim show} — toggle border visibility (zones dark red, your claims green, others orange).</li>
 *   <li>{@code /claim remove} — remove the claim you're standing in (owner; ops may remove anyone's).</li>
 *   <li>{@code /claim trust <player>} — toggle a player's build access on the claim you're standing in.</li>
 *   <li>{@code /claim zone add|list|remove} — ops: manage the zones players may claim in.</li>
 * </ul>
 */
public final class ShopGuardCommands {
    private ShopGuardCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var zone = Commands.literal("zone")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("add")
                        .then(Commands.argument("corner1", BlockPosArgument.blockPos())
                                .then(Commands.argument("corner2", BlockPosArgument.blockPos())
                                        .executes(ctx -> zoneAdd(ctx.getSource(),
                                                BlockPosArgument.getBlockPos(ctx, "corner1"),
                                                BlockPosArgument.getBlockPos(ctx, "corner2"))))))
                .then(Commands.literal("list").executes(ctx -> zoneList(ctx.getSource())))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", LongArgumentType.longArg())
                                .suggests((c, bld) -> SharedSuggestionProvider.suggest(
                                        ShopGuard.STORE.zones().stream().map(z -> String.valueOf(z.id)), bld))
                                .executes(ctx -> zoneRemove(ctx.getSource(), LongArgumentType.getLong(ctx, "id")))));

        dispatcher.register(Commands.literal("claim")
                .executes(ctx -> info(ctx.getSource()))
                .then(Commands.literal("show").executes(ctx -> show(ctx.getSource())))
                .then(Commands.literal("remove").executes(ctx -> remove(ctx.getSource())))
                .then(Commands.literal("trust")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> trustToggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(zone));
    }

    // ---- helpers ----

    private static ServerPlayer player(CommandSourceStack s) {
        return s.getEntity() instanceof ServerPlayer sp ? sp : null;
    }
    private static String dim(ServerPlayer sp) { return sp.level().dimension().identifier().toString(); }
    private static Claim standingClaim(ServerPlayer sp) {
        BlockPos p = sp.blockPosition();
        return ShopGuard.STORE.claimAt(dim(sp), p.getX(), p.getZ());
    }
    private static int notPlayer(CommandSourceStack s) {
        s.sendFailure(Component.literal("Run this as a player."));
        return 0;
    }

    // ---- /claim (bare): claim here + your claims + totals ----

    private static int info(CommandSourceStack s) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);

        Claim here = standingClaim(sp);
        if (here == null) {
            s.sendSuccess(() -> Component.literal("Here: unclaimed.").withStyle(ChatFormatting.GRAY), false);
        } else {
            s.sendSuccess(() -> Component.literal("Here: claim #" + here.id + " — " + here.ownerName
                    + ", " + here.shape.count() + " blocks.").withStyle(ChatFormatting.AQUA), false);
        }

        List<Claim> mine = ShopGuard.STORE.byOwner(sp.getUUID());
        if (mine.isEmpty()) {
            s.sendSuccess(() -> Component.literal("You have no claims.").withStyle(ChatFormatting.GRAY), false);
        } else {
            s.sendSuccess(() -> Component.literal("Your claims (" + mine.size() + "):")
                    .withStyle(ChatFormatting.AQUA), false);
            for (Claim c : mine) {
                s.sendSuccess(() -> Component.literal(" • #" + c.id + " — " + c.shape.count() + " blocks near "
                        + c.shape.minX() + ", " + c.shape.minZ() + " (" + c.dimension + ")")
                        .withStyle(ChatFormatting.GRAY), false);
            }
            int total = ShopGuard.STORE.totalCellsOfOwner(sp.getUUID());
            s.sendSuccess(() -> Component.literal("Total claimed: " + total + " / "
                    + ShopGuard.CONFIG.maxTotalPerPlayer + " blocks.").withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    // ---- /claim show: toggle border visibility ----

    private static int show(CommandSourceStack s) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);
        boolean on = ClaimVisualizer.toggleZoneViewer(sp.getUUID());
        if (on) {
            String hint = ShopGuard.STORE.zones().isEmpty()
                    ? "Borders ON — no zones are defined, so you can claim anywhere. Claims: green = yours, orange = others."
                    : "Borders ON — zones dark red, your claims green, others orange.";
            s.sendSuccess(() -> Component.literal(hint).withStyle(ChatFormatting.GREEN), false);
        } else {
            s.sendSuccess(() -> Component.literal("Borders OFF.").withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    // ---- /claim remove: owner (ops: anyone's) ----

    private static int remove(CommandSourceStack s) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);
        Claim c = standingClaim(sp);
        if (c == null) {
            s.sendFailure(Component.literal("Stand inside a claim to remove it."));
            return 0;
        }
        if (!c.owner.equals(sp.getUUID()) && !ProtectionHandler.isOp(sp)) {
            s.sendFailure(Component.literal("That claim isn't yours."));
            return 0;
        }
        ShopGuard.STORE.remove(c.id);
        ClaimVisualizer.refresh(sp.level());
        s.sendSuccess(() -> Component.literal("Removed claim #" + c.id + ".").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    // ---- /claim trust <player>: toggle ----

    private static int trustToggle(CommandSourceStack s, ServerPlayer target) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);
        Claim c = standingClaim(sp);
        if (c == null) {
            s.sendFailure(Component.literal("Stand inside your claim."));
            return 0;
        }
        if (!c.owner.equals(sp.getUUID()) && !ProtectionHandler.isOp(sp)) {
            s.sendFailure(Component.literal("That claim isn't yours."));
            return 0;
        }
        boolean added = c.trusted.add(target.getUUID());
        if (!added) c.trusted.remove(target.getUUID());
        ShopGuard.STORE.save();
        String name = target.getName().getString();
        s.sendSuccess(() -> Component.literal(name + (added ? " is now trusted" : " is no longer trusted")
                + " on claim #" + c.id + ".").withStyle(added ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        return 1;
    }

    // ---- /claim zone ... (ops) ----

    private static int zoneAdd(CommandSourceStack s, BlockPos c1, BlockPos c2) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);
        AdminZone z = ShopGuard.STORE.addZone(dim(sp), c1.getX(), c1.getZ(), c2.getX(), c2.getZ());
        ClaimVisualizer.refresh(sp.level());
        s.sendSuccess(() -> Component.literal("Zone #" + z.id + " set — " + z.shape.count() + " blocks.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int zoneList(CommandSourceStack s) {
        Collection<AdminZone> zones = ShopGuard.STORE.zones();
        if (zones.isEmpty()) {
            s.sendSuccess(() -> Component.literal("No claim zones — players may claim anywhere.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        s.sendSuccess(() -> Component.literal("Claim zones (" + zones.size() + "):").withStyle(ChatFormatting.AQUA), false);
        for (AdminZone z : zones) {
            s.sendSuccess(() -> Component.literal(" • #" + z.id + " — " + z.shape.count() + " blocks near "
                    + z.shape.minX() + ", " + z.shape.minZ() + " (" + z.dimension + ")")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int zoneRemove(CommandSourceStack s, long id) {
        if (!ShopGuard.STORE.removeZone(id)) {
            s.sendFailure(Component.literal("No zone #" + id + "."));
            return 0;
        }
        ServerPlayer sp = player(s);
        if (sp != null) ClaimVisualizer.refresh(sp.level());
        s.sendSuccess(() -> Component.literal("Removed claim zone #" + id + ".").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }
}

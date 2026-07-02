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

/** The {@code /claim} command tree: player info/list/remove/trust, plus op {@code zone} and {@code admin}. */
public final class ShopGuardCommands {
    private ShopGuardCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var zoneAddArgs = Commands.argument("corner1", BlockPosArgument.blockPos())
                .then(Commands.argument("corner2", BlockPosArgument.blockPos())
                        .executes(ctx -> zoneAdd(ctx.getSource(),
                                BlockPosArgument.getBlockPos(ctx, "corner1"),
                                BlockPosArgument.getBlockPos(ctx, "corner2"))));

        var zone = Commands.literal("zone")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("add").then(zoneAddArgs))
                .then(Commands.literal("list").executes(ctx -> zoneList(ctx.getSource())))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", LongArgumentType.longArg())
                                .suggests((c, bld) -> SharedSuggestionProvider.suggest(
                                        ShopGuard.STORE.zones().stream().map(z -> String.valueOf(z.id)), bld))
                                .executes(ctx -> zoneRemove(ctx.getSource(), LongArgumentType.getLong(ctx, "id")))));

        var admin = Commands.literal("admin")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("remove").executes(ctx -> remove(ctx.getSource(), true)));

        dispatcher.register(Commands.literal("claim")
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("zones").executes(ctx -> zonesToggle(ctx.getSource())))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("remove").executes(ctx -> remove(ctx.getSource(), false)))
                .then(Commands.literal("trust")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> trust(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true))))
                .then(Commands.literal("untrust")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> trust(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false))))
                .then(zone)
                .then(admin));
    }

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

    private static int info(CommandSourceStack s) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);
        Claim c = standingClaim(sp);
        if (c == null) {
            s.sendSuccess(() -> Component.literal("This spot is unclaimed.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        s.sendSuccess(() -> Component.literal(
                "Claim #" + c.id + " — owner " + c.ownerName + ", " + c.shape.count() + " blocks.")
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int list(CommandSourceStack s) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);
        List<Claim> mine = ShopGuard.STORE.byOwner(sp.getUUID());
        if (mine.isEmpty()) {
            s.sendSuccess(() -> Component.literal("You have no claims.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        s.sendSuccess(() -> Component.literal("Your claims (" + mine.size() + "):").withStyle(ChatFormatting.AQUA), false);
        for (Claim c : mine) {
            s.sendSuccess(() -> Component.literal(" • #" + c.id + " — " + c.shape.count() + " blocks near "
                    + c.shape.minX() + ", " + c.shape.minZ() + " (" + c.dimension + ")")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int remove(CommandSourceStack s, boolean admin) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);
        Claim c = standingClaim(sp);
        if (c == null) {
            s.sendFailure(Component.literal("Stand inside a claim to remove it."));
            return 0;
        }
        if (!admin && !c.owner.equals(sp.getUUID()) && !ProtectionHandler.isOp(sp)) {
            s.sendFailure(Component.literal("That claim isn't yours."));
            return 0;
        }
        ShopGuard.STORE.remove(c.id);
        ClaimVisualizer.refresh(sp.level());
        s.sendSuccess(() -> Component.literal("Removed claim #" + c.id + ".").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int trust(CommandSourceStack s, ServerPlayer target, boolean add) {
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
        if (add) c.trusted.add(target.getUUID()); else c.trusted.remove(target.getUUID());
        ShopGuard.STORE.save();
        String name = target.getName().getString();
        s.sendSuccess(() -> Component.literal((add ? "Trusted " : "Untrusted ") + name + " on claim #" + c.id + ".")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int zonesToggle(CommandSourceStack s) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);
        boolean on = ClaimVisualizer.toggleZoneViewer(sp.getUUID());
        if (on && ShopGuard.STORE.zones().isEmpty()) {
            s.sendSuccess(() -> Component.literal(
                    "Zone borders ON — no zones are defined yet, so you can claim anywhere.")
                    .withStyle(ChatFormatting.GRAY), false);
        } else {
            s.sendSuccess(() -> Component.literal("Zone borders " + (on ? "ON" : "OFF") + ".")
                    .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int zoneAdd(CommandSourceStack s, BlockPos c1, BlockPos c2) {
        ServerPlayer sp = player(s);
        if (sp == null) return notPlayer(s);
        AdminZone z = ShopGuard.STORE.addZone(dim(sp), c1.getX(), c1.getZ(), c2.getX(), c2.getZ());
        s.sendSuccess(() -> Component.literal("Added claim zone #" + z.id + " ("
                + z.minX + "," + z.minZ + " to " + z.maxX + "," + z.maxZ + ").")
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
            s.sendSuccess(() -> Component.literal(" • #" + z.id + " — " + z.minX + "," + z.minZ
                    + " to " + z.maxX + "," + z.maxZ + " (" + z.dimension + ")").withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int zoneRemove(CommandSourceStack s, long id) {
        if (ShopGuard.STORE.removeZone(id)) {
            s.sendSuccess(() -> Component.literal("Removed claim zone #" + id + ".").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        s.sendFailure(Component.literal("No zone #" + id + "."));
        return 0;
    }
}

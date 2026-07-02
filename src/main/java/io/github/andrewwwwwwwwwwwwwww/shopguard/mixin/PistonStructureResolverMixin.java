package io.github.andrewwwwwwwwwwwwwww.shopguard.mixin;

import io.github.andrewwwwwwwwwwwwwww.shopguard.ShopGuard;
import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.Claim;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Stops pistons from moving blocks across a claim boundary — a piston can't push or pull blocks into,
 * out of, or between claims (it can still move blocks freely within a single claim or in unclaimed
 * land). Cancels the move by making {@code resolve()} report the structure as unmovable.
 */
@Mixin(PistonStructureResolver.class)
public class PistonStructureResolverMixin {
    @Shadow @Final private Level level;

    @Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
    private void shopguard$protectClaimBorders(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || level.isClientSide()) return;
        PistonStructureResolver self = (PistonStructureResolver) (Object) this;
        String dim = level.dimension().identifier().toString();
        Direction dir = self.getPushDirection();
        for (BlockPos src : self.getToPush()) {
            if (claimAt(dim, src) != claimAt(dim, src.relative(dir))) {
                cir.setReturnValue(false);
                return;
            }
        }
        for (BlockPos doomed : self.getToDestroy()) {
            if (claimAt(dim, doomed) != null) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    private static Claim claimAt(String dim, BlockPos p) {
        return ShopGuard.STORE.claimAt(dim, p.getX(), p.getZ());
    }
}

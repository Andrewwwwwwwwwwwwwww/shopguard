package io.github.andrewwwwwwwwwwwwwww.shopguard.mixin;

import io.github.andrewwwwwwwwwwwwwww.shopguard.ShopGuard;
import io.github.andrewwwwwwwwwwwwwww.shopguard.claim.Claim;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops fluids (water/lava) from flowing across a claim border — a fluid can't spread into, out of, or
 * between claims, but flows freely within a single claim or in unclaimed land. {@code spreadTo} places
 * one spreading fluid block at {@code pos}, having come from {@code pos.relative(direction.opposite)};
 * if those two columns aren't the same claim, the flow is cancelled.
 */
@Mixin(FlowingFluid.class)
public abstract class FluidFlowMixin {

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void shopguard$protectClaims(LevelAccessor level, BlockPos pos, BlockState blockState,
                                         Direction direction, FluidState fluidState, CallbackInfo ci) {
        if (direction == null || !(level instanceof Level lvl) || lvl.isClientSide()) return;
        String dim = lvl.dimension().identifier().toString();
        Claim dest = ShopGuard.STORE.claimAt(dim, pos.getX(), pos.getZ());
        BlockPos from = pos.relative(direction.getOpposite());
        Claim src = ShopGuard.STORE.claimAt(dim, from.getX(), from.getZ());
        if (dest != src) ci.cancel();
    }
}

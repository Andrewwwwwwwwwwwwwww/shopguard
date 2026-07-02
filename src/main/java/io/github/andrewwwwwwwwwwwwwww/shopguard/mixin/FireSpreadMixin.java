package io.github.andrewwwwwwwwwwwwwww.shopguard.mixin;

import io.github.andrewwwwwwwwwwwwwww.shopguard.ShopGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops fire from consuming claimed blocks. {@code checkBurnOut} is what burns a block away (or spreads
 * fire onto it); cancelling it for claimed positions means fire can flicker up to a claim's edge but
 * can't eat into it.
 */
@Mixin(FireBlock.class)
public abstract class FireSpreadMixin {

    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void shopguard$protectClaims(Level level, BlockPos pos, int chance, RandomSource random,
                                         int age, CallbackInfo ci) {
        if (level.isClientSide()) return;
        String dim = level.dimension().identifier().toString();
        if (ShopGuard.STORE.claimAt(dim, pos.getX(), pos.getZ()) != null) ci.cancel();
    }
}

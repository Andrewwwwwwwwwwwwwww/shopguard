package io.github.andrewwwwwwwwwwwwwww.shopguard.mixin;

import io.github.andrewwwwwwwwwwwwwww.shopguard.ShopGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Protects claimed blocks from every explosion — TNT, creepers, ghast/wither blasts, end crystals,
 * beds and respawn anchors — since all of them run through {@link ServerExplosion}. Claimed blocks are
 * filtered out of the destroy list, so the explosion still hurts entities and breaks unclaimed blocks
 * around it; it just can't chew into a claim. (This also covers a creeper's block griefing.)
 */
@Mixin(ServerExplosion.class)
public abstract class ExplosionProtectionMixin {

    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void shopguard$protectClaims(CallbackInfoReturnable<List<BlockPos>> cir) {
        List<BlockPos> list = cir.getReturnValue();
        if (list == null || list.isEmpty()) return;
        ServerExplosion self = (ServerExplosion) (Object) this;
        String dim = self.level().dimension().identifier().toString();
        List<BlockPos> kept = new ArrayList<>(list.size());
        for (BlockPos p : list) {
            if (ShopGuard.STORE.claimAt(dim, p.getX(), p.getZ()) == null) kept.add(p);
        }
        if (kept.size() != list.size()) cir.setReturnValue(kept);
    }
}

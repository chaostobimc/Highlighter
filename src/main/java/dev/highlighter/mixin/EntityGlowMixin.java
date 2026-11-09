package dev.highlighter.mixin;

import dev.highlighter.HighlightManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes highlighted players glow.
 *
 * <p>On the client, {@code Entity#isCurrentlyGlowing()} simply returns the shared
 * "glowing" entity flag. Vanilla uses that value when it builds the render state
 * ({@code appearsGlowing}), which in turn selects the see-through outline render
 * type. Forcing the return value here gives us the exact vanilla outline —
 * including the through-walls part — without touching synced entity data, and it
 * is re-evaluated every frame, so toggling takes effect immediately.</p>
 */
@Mixin(Entity.class)
public abstract class EntityGlowMixin {
	@Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
	private void highlighter$forceGlow(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && HighlightManager.isHighlighted(player)) {
			cir.setReturnValue(true);
		}
	}
}

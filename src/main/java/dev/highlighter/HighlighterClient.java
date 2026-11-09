package dev.highlighter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint of the Highlighter mod.
 *
 * <p>Everything in this mod is client side: no packets are sent and the server
 * never learns about the highlighted players. The glow is produced by mixing
 * into {@code Entity#isCurrentlyGlowing()}, which is the method vanilla uses to
 * decide whether an entity gets the see-through outline.</p>
 */
public final class HighlighterClient implements ClientModInitializer {
	public static final String MOD_ID = "highlighter";
	public static final Logger LOGGER = LoggerFactory.getLogger("Highlighter");

	@Override
	public void onInitializeClient() {
		HighlightManager.init(HighlightConfig.load());
		HighlightCommand.register();
		ClientTickEvents.END_CLIENT_TICK.register(client -> HighlightManager.tick());

		LOGGER.info("[HL] Highlighter ready ({} saved player(s), near={}, radius={})",
				HighlightManager.config().users.size(),
				HighlightManager.config().near,
				HighlightManager.config().radius);
	}
}

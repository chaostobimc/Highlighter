package dev.highlighter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Builds and prints the chat feedback.
 *
 * <p>Format: {@code [HL] text} — the brackets are gray, {@code HL} is yellow and
 * the actual message is red.</p>
 */
public final class HighlightMessages {
	private HighlightMessages() {
	}

	/** {@code [HL] text} with the brackets in gray, "HL" in yellow and the rest in red. */
	public static MutableComponent prefixed(String text) {
		return Component.literal("[")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal("HL").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(text).withStyle(ChatFormatting.RED));
	}

	public static void send(String text) {
		send(prefixed(text));
	}

	/** Prints a message into the chat, client side only. */
	public static void send(Component message) {
		Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
	}
}

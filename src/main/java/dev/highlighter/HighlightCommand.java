package dev.highlighter;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers the client side {@code /highlight} command.
 *
 * <ul>
 *     <li>{@code /highlight user <Name>} – toggle a single player</li>
 *     <li>{@code /highlight near} – toggle "everyone around me"</li>
 *     <li>{@code /highlight list} – show the current picks</li>
 *     <li>{@code /highlight clear} – drop all picks</li>
 *     <li>{@code /highlight radius <Blöcke>} – change the near radius</li>
 * </ul>
 */
public final class HighlightCommand {
	private HighlightCommand() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				ClientCommands.literal("highlight")
						// Bare /highlight: show the current state plus a usage hint.
						.executes(context -> {
							showStatus();
							return 1;
						})
						.then(ClientCommands.literal("user")
								.then(ClientCommands.argument("name", StringArgumentType.word())
										.suggests((context, builder) -> {
											for (String name : knownNames()) {
												builder.suggest(name);
											}

											return builder.buildFuture();
										})
										.executes(context -> toggleUser(StringArgumentType.getString(context, "name")))))
						.then(ClientCommands.literal("near")
								.executes(context -> toggleNear()))
						.then(ClientCommands.literal("list")
								.executes(context -> {
									listUsers();
									return 1;
								}))
						.then(ClientCommands.literal("clear")
								.executes(context -> {
									HighlightManager.clearUsers();
									HighlightMessages.send("Alle Hervorhebungen entfernt.");
									return 1;
								}))
						.then(ClientCommands.literal("radius")
								.then(ClientCommands.argument("blocks", IntegerArgumentType.integer(1, 512))
										.executes(context -> {
											HighlightManager.setRadius(IntegerArgumentType.getInteger(context, "blocks"));
											HighlightMessages.send("Umkreis für 'near' auf "
													+ HighlightManager.radiusText() + " Blöcke gesetzt.");
											return 1;
										})))
		));
	}

	private static int toggleUser(String name) {
		boolean highlighted = HighlightManager.toggleUser(name);

		if (highlighted) {
			HighlightMessages.send(name.trim() + " wird hervorgehoben.");
		} else {
			HighlightMessages.send(name.trim() + " wird nicht mehr hervorgehoben.");
		}

		return 1;
	}

	private static int toggleNear() {
		boolean near = HighlightManager.toggleNear();

		if (near) {
			HighlightMessages.send("Near an – alle Spieler im Umkreis von "
					+ HighlightManager.radiusText() + " Blöcken leuchten.");
		} else {
			HighlightMessages.send("Near aus.");
		}

		return 1;
	}

	private static void listUsers() {
		List<String> users = HighlightManager.config().users;

		if (users.isEmpty()) {
			HighlightMessages.send("Keine Spieler hervorgehoben.");
			return;
		}

		HighlightMessages.send("Hervorgehoben: " + String.join(", ", users) + ".");
	}

	private static void showStatus() {
		HighlightMessages.send("Near: " + (HighlightManager.config().near
				? "an (" + HighlightManager.radiusText() + " Blöcke)"
				: "aus") + ".");

		HighlightMessages.send("Nutzung: /highlight user <Name> | /highlight near | "
				+ "/highlight list | /highlight clear | /highlight radius <Blöcke>");
	}

	/** Online player names plus everything that is already highlighted, for tab completion. */
	private static List<String> knownNames() {
		List<String> names = new ArrayList<>(HighlightManager.config().users);
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;

		if (level != null) {
			for (AbstractClientPlayer player : level.players()) {
				String name = player.getName().getString();

				if (!names.contains(name)) {
					names.add(name);
				}
			}
		}

		return names;
	}
}

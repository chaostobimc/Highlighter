package dev.highlighter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Holds the highlight rules and keeps them in sync with the world.
 *
 * <p>{@link #isHighlighted(Player)} is queried from the render path (through the
 * {@code Entity#isCurrentlyGlowing()} mixin), so it has to be cheap and must
 * never touch anything that is only safe during a tick. {@link #tick()} runs ten
 * times per second and is only responsible for the "X ist in der Nähe." chat
 * feedback.</p>
 */
public final class HighlightManager {
	private static HighlightConfig config = new HighlightConfig();

	/** UUIDs of the players we already announced as "in der Nähe" (in memory only). */
	private static final Set<UUID> announcedNear = new HashSet<>();

	private static int tickCounter;

	private HighlightManager() {
	}

	static void init(HighlightConfig loaded) {
		config = loaded;
		announcedNear.clear();
	}

	public static HighlightConfig config() {
		return config;
	}

	/**
	 * Whether the given player should glow right now: either explicitly added with
	 * {@code /highlight user} or — while {@code /highlight near} is active — within
	 * the configured radius of the local player.
	 */
	public static boolean isHighlighted(Player player) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer self = minecraft.player;

		if (self == null || player == null || player == self) {
			return false;
		}

		if (config.users.isEmpty() && !config.near) {
			return false;
		}

		if (findUser(player.getName().getString()) != null) {
			return true;
		}

		return config.near && self.distanceToSqr(player) <= config.radius * config.radius;
	}

	/** @return the stored spelling of {@code name}, or {@code null} if that player is not highlighted. */
	static String findUser(String name) {
		if (name == null || name.isEmpty()) {
			return null;
		}

		for (String stored : config.users) {
			if (stored.equalsIgnoreCase(name)) {
				return stored;
			}
		}

		return null;
	}

	/** Adds or removes a player and persists the change. @return {@code true} if the player is now highlighted. */
	static boolean toggleUser(String name) {
		String stored = findUser(name);

		if (stored != null) {
			config.users.remove(stored);
			config.save();
			return false;
		}

		String cleaned = name.trim();

		if (cleaned.isEmpty()) {
			return false;
		}

		config.users.add(cleaned);
		config.save();
		return true;
	}

	/** Turns "near" on or off and persists the change. @return {@code true} if near mode is now active. */
	static boolean toggleNear() {
		config.near = !config.near;
		announcedNear.clear();
		config.save();
		return config.near;
	}

	static void clearUsers() {
		config.users.clear();
		config.save();
	}

	static void setRadius(double radius) {
		config.radius = radius;
		announcedNear.clear();
		config.save();
	}

	static String radiusText() {
		return Integer.toString((int) Math.round(config.radius));
	}

	/** Called at the end of every client tick; does the chat announcements. */
	static void tick() {
		if (++tickCounter % 10 != 0) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		LocalPlayer self = minecraft.player;

		if (level == null || self == null || !config.near) {
			announcedNear.clear();
			return;
		}

		double radiusSqr = config.radius * config.radius;
		Set<UUID> stillNear = new HashSet<>();
		List<String> newcomers = new ArrayList<>();

		for (AbstractClientPlayer other : level.players()) {
			if (other == self || other.isRemoved()) {
				continue;
			}

			if (self.distanceToSqr(other) > radiusSqr) {
				continue;
			}

			UUID id = other.getUUID();
			stillNear.add(id);

			if (!announcedNear.contains(id)) {
				newcomers.add(other.getName().getString());
			}
		}

		announcedNear.clear();
		announcedNear.addAll(stillNear);

		for (String name : newcomers) {
			HighlightMessages.send(name + " ist in der Nähe.");
		}
	}
}

package dev.highlighter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The persisted state of the mod, stored as {@code config/highlighter.json}.
 *
 * <p>It survives restarts, so whoever was highlighted stays highlighted the next
 * time the game starts. Written on every change, read once at mod init.</p>
 */
public final class HighlightConfig {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private static final double DEFAULT_RADIUS = 64.0;

	/** Player names added with {@code /highlight user <name>}, in the casing they were typed in. */
	public List<String> users = new ArrayList<>();

	/** Whether {@code /highlight near} is currently active. */
	public boolean near = false;

	/** Radius in blocks used by {@code /highlight near}. */
	public double radius = DEFAULT_RADIUS;

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("highlighter.json");
	}

	public static HighlightConfig load() {
		Path path = path();

		if (!Files.isRegularFile(path)) {
			return new HighlightConfig();
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			HighlightConfig loaded = GSON.fromJson(reader, HighlightConfig.class);

			if (loaded == null) {
				return new HighlightConfig();
			}

			if (loaded.users == null) {
				loaded.users = new ArrayList<>();
			}

			if (!(loaded.radius > 0.0D) || Double.isNaN(loaded.radius) || Double.isInfinite(loaded.radius)) {
				loaded.radius = DEFAULT_RADIUS;
			}

			return loaded;
		} catch (IOException | RuntimeException e) {
			HighlighterClient.LOGGER.warn("[HL] Could not read {}, starting with a fresh state", path, e);
			return new HighlightConfig();
		}
	}

	public void save() {
		Path path = path();

		try {
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException | RuntimeException e) {
			HighlighterClient.LOGGER.warn("[HL] Could not write {}", path, e);
		}
	}
}

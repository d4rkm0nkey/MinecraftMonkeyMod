package me.teajay.cheeky.monkeys;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import me.teajay.cheeky.monkeys.platform.Platform;

/// Settings in `config/cheeky_monkeys.json`, read once at startup. Missing options are filled in with their defaults
/// and written back, so the file always lists everything there is to change.
public final class CheekyMonkeysConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CheekyMonkeysConfig instance = new CheekyMonkeysConfig();

    /// Wild monkeys sneak up on players and steal from their pockets
    public boolean pickpocketing = true;
    /// Most items a monkey takes from one stack
    public int maxItemsStolen = 4;
    /// Thieves hide their loot in nests up in the trees; otherwise they just hang on to it
    public boolean stashNests = true;
    /// Monkeys race for bananas lying on the ground
    public boolean grabThrownBananas = true;
    /// Pets screech at monsters coming for their owner and make them glow
    public boolean petLookout = true;
    /// Pets snatch weapons out of monsters' hands
    public boolean petDisarming = true;

    public static CheekyMonkeysConfig get() {
        return instance;
    }

    static void load() {
        Path file = Platform.INSTANCE.configDirectory().resolve(CheekyMonkeys.MOD_ID + ".json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                CheekyMonkeysConfig loaded = GSON.fromJson(reader, CheekyMonkeysConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException | JsonParseException e) {
                // Leave a broken file alone so the player can fix it
                CheekyMonkeys.LOGGER.warn("Couldn't read {}, using the default settings", file, e);
                return;
            }
        }

        instance.maxItemsStolen = Math.max(1, instance.maxItemsStolen);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            CheekyMonkeys.LOGGER.warn("Couldn't write {}", file, e);
        }
    }
}

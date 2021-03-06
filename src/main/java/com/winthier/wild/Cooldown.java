package com.winthier.wild;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

@RequiredArgsConstructor
public class Cooldown
{
    final WildPlugin plugin;
    private YamlConfiguration config = null;
    static final String SAVE_FILE = "cooldowns.yml";
    static final long COOLDOWN_SECONDS_SMALL = 60;
    static final long COOLDOWN_SECONDS_BIG = 60 * 60 * 24;
    static final long FREE_SECONDS = 60 * 10;

    File saveFile()
    {
        return new File(plugin.getDataFolder(), SAVE_FILE);
    }

    void reload()
    {
        config = null;
    }

    YamlConfiguration getConfig()
    {
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(saveFile());
            // Delete all entries older than a day.
            long min = System.currentTimeMillis() - 1000 * 60 * 60 * 24;
            for (String key : config.getKeys(false)) {
                long cd = config.getLong(key);
                if (cd < min) config.set(key, null);
            }
        }
        return config;
    }

    private final void save()
    {
        try {
            getConfig().save(saveFile());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * @return null if player is off cooldown
     * @return a meaningful message if the player is on cooldown
     */
    String cooldownMessage(UUID uuid)
    {
        String key = uuid.toString();
        if (!getConfig().isLong(key)) return null;
        final long lastUse = getConfig().getLong(uuid.toString());
        final long now = System.currentTimeMillis();
        final long interval = now - lastUse;
        final long seconds = interval / 1000;
        long cooldownSecondsSmall = plugin.getConfig().getLong("wild.SmallCooldown", COOLDOWN_SECONDS_SMALL);
        if (seconds < cooldownSecondsSmall) {
            // If they waited less than 10 seconds, tell them to wait a bit longer
            long wait = cooldownSecondsSmall - seconds;
            if (wait > 1) return String.format("Please wait %d more seconds", (int)wait);
            return "Please wait one more second";
        }
        // If they waited more than 12 hours, let them go
        long cooldownSecondsBig = plugin.getConfig().getLong("wild.Cooldown", COOLDOWN_SECONDS_BIG);
        if (seconds > cooldownSecondsBig) return null;
        // If they waited more than 10 minutes, make them wait the full cooldown
        long freeSeconds = getConfig().getLong("wild.FreeSeconds", FREE_SECONDS);
        if (seconds > freeSeconds) {
            final long secondsLeft = cooldownSecondsBig - seconds;
            final long minutesLeft = secondsLeft / 60;
            final long hoursLeft = minutesLeft / 60;
            return String.format("Please wait %02d:%02d more hours", (int)hoursLeft, (int)minutesLeft % 60);
        }
        // Anything in between, let them go
        return null;
    }

    void setCooldownNow(UUID uuid)
    {
        getConfig().set(uuid.toString(), System.currentTimeMillis());
        save();
    }
}

package com.gamertee.elytra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ElytraCooldown extends JavaPlugin implements Listener {

    private int flight, cooldown;
    private String msgRemaining, msgCooldown;
    private final Map<UUID, long[]> data = new HashMap<>(); // {secondsFlown, cooldownEndMillis}

    @Override
    public void onEnable() {
        saveDefaultConfig();
        flight = getConfig().getInt("flight-time", 60);
        cooldown = getConfig().getInt("cooldown-time", 300);
        msgRemaining = getConfig().getString("messages.remaining", "&aElytra &e{time} &aremaining");
        msgCooldown = getConfig().getString("messages.cooldown", "&cElytra on Cooldown for &e{time}");
        Bukkit.getPluginManager().registerEvents(this, this);

        // Runs once per second, only counts players who are currently gliding
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.isGliding()) continue;
                long[] d = data.computeIfAbsent(p.getUniqueId(), k -> new long[2]);
                if (++d[0] >= flight) {
                    d[0] = 0;
                    d[1] = System.currentTimeMillis() + cooldown * 1000L;
                    p.setGliding(false);
                } else {
                    p.sendActionBar(msg(msgRemaining, time((int) (flight - d[0]))));
                }
            }
        }, 20L, 20L);
    }

    @EventHandler
    public void onGlide(EntityToggleGlideEvent e) {
        if (!e.isGliding() || !(e.getEntity() instanceof Player p)) return;
        long[] d = data.get(p.getUniqueId());
        if (d == null) return;
        long left = d[1] - System.currentTimeMillis();
        if (left > 0) {
            e.setCancelled(true);
            p.sendActionBar(msg(msgCooldown, time((int) Math.ceil(left / 1000.0))));
        }
    }

    private Component msg(String m, String t) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(m.replace("{time}", t));
    }

    private String time(int s) {
        return (s >= 60 ? s / 60 + "m " : "") + s % 60 + "s";
    }
}
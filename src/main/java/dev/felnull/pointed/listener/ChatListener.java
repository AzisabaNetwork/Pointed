package dev.felnull.pointed.listener;

import dev.felnull.pointed.Pointed;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final Pointed plugin;
    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage();

        if (!plugin.getChatReader().isRegistered(p)) {
            return;
        }

        e.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getChatReader().onChat(p, Component.text(msg)));

    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.getChatReader().unregisterNextChat(p);
    }
}

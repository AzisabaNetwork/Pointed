package dev.felnull.pointed.core.util;

import dev.felnull.bettergui.core.InventoryGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatReader {
    private final Map<UUID, ChatContentType> contentTypes = new HashMap<>();

    public void registerNextChat(Player p, ChatContentType type) {
        contentTypes.put(p.getUniqueId(), type);
        p.closeInventory();
    }

    public void unregisterNextChat(Player p) {
        contentTypes.remove(p.getUniqueId());
    }

    public boolean isRegistered(Player p) {
        return contentTypes.containsKey(p.getUniqueId());
    }

    public void onChat(Player p, Component msg) {
        if (!contentTypes.containsKey(p.getUniqueId())) {
            return;
        }
        ChatContentType type = contentTypes.get(p.getUniqueId());
        InventoryGUI gui = new InventoryGUI(p);

        switch (type) {
            //ChatContentTypeがDisplay_Nameの場合の処理

        }

        unregisterNextChat(p);
    }
}

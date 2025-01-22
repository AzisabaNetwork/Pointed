package dev.felnull.pointed.util;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.gui.page.RewardSettingsGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatReader {
    private final Map<UUID, ChatContentType> contentTypes = new HashMap<>();
    private final Map<UUID, RewardData> rewardDataMap = new HashMap<>();

    public void registerNextChat(Player p, ChatContentType type, RewardData rewardData) {
        contentTypes.put(p.getUniqueId(), type);
        rewardDataMap.put(p.getUniqueId(), rewardData);
        p.closeInventory();
    }

    public void unregisterNextChat(Player p) {
        contentTypes.remove(p.getUniqueId());
        rewardDataMap.remove(p.getUniqueId());
    }

    public boolean isRegistered(Player p) {
        return contentTypes.containsKey(p.getUniqueId());
    }

    public void onChat(Player p, Component msg) {
        if (!contentTypes.containsKey(p.getUniqueId())) {
            return;
        }
        ChatContentType type = contentTypes.get(p.getUniqueId());
        RewardData rewardData = rewardDataMap.get(p.getUniqueId());
        InventoryGUI gui = new InventoryGUI(p);

        switch (type) {
            //ChatContentTypeがDisplay_Nameの場合の処理
            case DISPLAY_NAME:
                rewardData.displayName = componentToString(msg);
                gui.openPage(new RewardSettingsGUI(gui));
                break;
            case REWARD_ID:
                int id = Integer.parseInt(componentToString(msg).replaceAll("[^0-9]", ""));
                if(id >= 6*9 || id < 0){
                    p.sendMessage("IDは0以上54未満で設定してください(インベントリの場所に対応した値です)");
                    gui.openPage(new RewardSettingsGUI(gui));
                    break;
                }
                rewardData.rewardID = id;
                gui.openPage(new RewardSettingsGUI(gui));
                break;
            case NEED_POINT:
                int np = Integer.parseInt(componentToString(msg).replaceAll("[^0-9]", ""));
                if(np < 0){
                    p.sendMessage("必要ポイント数は0以上の値で設定してください");
                    gui.openPage(new RewardSettingsGUI(gui));
                    break;
                }
                rewardData.needPoint = np;
                gui.openPage(new RewardSettingsGUI(gui));
                break;
            case NEED_MIN_POINT:
                int nmp = Integer.parseInt(componentToString(msg).replaceAll("[^0-9]", ""));
                if(nmp < 0){
                    p.sendMessage("最低必要ポイント数は0以上の値で設定してください");
                    gui.openPage(new RewardSettingsGUI(gui));
                    break;
                }
                rewardData.needMinPoint = nmp;
                gui.openPage(new RewardSettingsGUI(gui));
                break;
        }

        unregisterNextChat(p);
    }

    //ComponentMessageをStringに変換するメソッド
    public String componentToString(Component msg) {
        return ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.legacyAmpersand().serialize(msg));
    }
}

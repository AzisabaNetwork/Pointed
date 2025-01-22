package dev.felnull.pointed.listener;

import dev.felnull.bettergui.listener.GUIClickListener;
import dev.felnull.pointed.gui.page.SetRewardItemPage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class SetRewardItemPageListener extends GUIClickListener {
    private final SetRewardItemPage page;

    public SetRewardItemPageListener(SetRewardItemPage page){
        super(page);
        this.page = page;
    }

    @Override
    public void onClick(InventoryClickEvent e) {

    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTopInventory() != page.inventory) {
            return;
        }
        page.close();
    }

}

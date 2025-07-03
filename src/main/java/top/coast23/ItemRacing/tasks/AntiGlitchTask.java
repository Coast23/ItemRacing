package top.coast23.ItemRacing.tasks;

import top.coast23.ItemRacing.ItemRacing;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class AntiGlitchTask extends BukkitRunnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta()) {
                    if (item.getItemMeta().getPersistentDataContainer().has(ItemRacing.GUI_ITEM_KEY, PersistentDataType.BYTE)) {
                        player.getInventory().remove(item);
                    }
                }
            }
        }
    }
}
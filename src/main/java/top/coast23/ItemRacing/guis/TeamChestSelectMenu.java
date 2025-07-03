package top.coast23.ItemRacing.guis;

import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.models.Team;
import top.coast23.ItemRacing.ItemRacing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class TeamChestSelectMenu {

    public static final Component MENU_TITLE = Component.text("选择一个团队箱子", NamedTextColor.GOLD);

    private final GameManager gameManager;

    public TeamChestSelectMenu(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        Optional<Team> playerTeamOpt = gameManager.getTeamManager().getPlayerTeam(player);
        if (playerTeamOpt.isEmpty()) {
            player.sendMessage(Component.text("你不在任何队伍中！", NamedTextColor.RED));
            return;
        }

        int chestCount = gameManager.getConfigManager().getTeamChestAmount();
        int menuSize = Math.max(9, ((chestCount - 1) / 9 + 1) * 9);
        Inventory menu = Bukkit.createInventory(null, menuSize, MENU_TITLE);

        for (int i = 0; i < chestCount; i++) {
            ItemStack chestItem = new ItemStack(Material.CHEST);
            ItemMeta meta = chestItem.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("团队箱子 #" + (i + 1), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                meta.getPersistentDataContainer().set(ItemRacing.GUI_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                chestItem.setItemMeta(meta);
            }
            menu.setItem(i, chestItem);
        }
        player.openInventory(menu);
    }
}

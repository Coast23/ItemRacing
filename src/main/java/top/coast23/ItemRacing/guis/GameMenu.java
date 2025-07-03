package top.coast23.ItemRacing.guis;

import top.coast23.ItemRacing.managers.GameManager;
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

import java.util.List;

public class GameMenu {

    public static final Component MENU_TITLE = Component.text("游戏菜单", NamedTextColor.DARK_PURPLE);
    private final GameManager gameManager;

    public GameMenu(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, MENU_TITLE);

        // --- 创建按钮 (采用对称布局) ---

        // 按钮1: 团队箱子 (槽位 0)
        menu.setItem(0, createButton(Material.CHEST, Component.text("团队箱子", NamedTextColor.GOLD), List.of(Component.text("打开你所在队伍的共享储物箱。", NamedTextColor.GRAY), Component.text("关联命令: /chest", NamedTextColor.AQUA))));

        // 按钮2: 路径点管理 (槽位 2)
        menu.setItem(2, createButton(Material.FILLED_MAP, Component.text("路径点管理", NamedTextColor.BLUE), List.of(Component.text("设置、传送或删除你的路径点。", NamedTextColor.GRAY), Component.text("关联命令: /wp", NamedTextColor.AQUA))));

        // 按钮3: 申请重摇任务 (槽位 4)
        menu.setItem(4, createButton(Material.TOTEM_OF_UNDYING, Component.text("申请重摇任务", NamedTextColor.YELLOW), List.of(Component.text("向你的队友发起投票，更换任务列表。", NamedTextColor.GRAY), Component.text("关联命令: /roll", NamedTextColor.AQUA))));

        // 按钮4: 购买定位权限 (槽位 6)
        int locateCost = gameManager.getConfigManager().getLocateCommandCost();
        menu.setItem(6, createButton(
                Material.COMPASS,
                Component.text("购买定位权限", NamedTextColor.LIGHT_PURPLE),
                List.of(
                        Component.text("消耗 " + locateCost + " 积分以获得 /locate 命令权限。", NamedTextColor.GRAY),
                        Component.text("关联命令: /locate", NamedTextColor.AQUA))
        ));

        // 按钮5: 随机传送 (槽位 8)
        menu.setItem(8, createButton(
                Material.ENDER_PEARL,
                Component.text("随机传送", NamedTextColor.DARK_PURPLE),
                List.of(
                        Component.text("立即进行一次安全的随机传送。", NamedTextColor.GRAY),
                        Component.text("关联命令: /tpr", NamedTextColor.AQUA))
        ));

        player.openInventory(menu);
    }

    private ItemStack createButton(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());

            // 为所有GUI按钮添加一个特殊的NBT标签
            meta.getPersistentDataContainer().set(ItemRacing.GUI_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES); // 顺便隐藏物品属性

            item.setItemMeta(meta);
        }
        return item;
    }
}

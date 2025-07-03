package top.coast23.ItemRacing.guis;

import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.GuiManager;
import top.coast23.ItemRacing.managers.LobbyManager;
import top.coast23.ItemRacing.managers.TeamManager;
import top.coast23.ItemRacing.models.Team;
import top.coast23.ItemRacing.ItemRacing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

public class LobbyMenu {

    public static final Component MENU_TITLE = Component.text("游戏大厅", NamedTextColor.DARK_AQUA);
    private final GuiManager guiManager;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final LobbyManager lobbyManager;

    public LobbyMenu(GuiManager guiManager, GameManager gameManager, TeamManager teamManager, LobbyManager lobbyManager) {
        this.guiManager = guiManager;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.lobbyManager = lobbyManager;
    }

    public void open(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, MENU_TITLE);

        // --- 创建按钮 ---
        // [改造] 采用更美观的对称布局

        // 按钮1: 队伍选择 (槽位 1)
        Optional<Team> playerTeamOpt = teamManager.getPlayerTeam(player);
        DyeColor bannerColor = playerTeamOpt.map(team -> {
            try {
                return DyeColor.valueOf(NamedTextColor.nearestTo(team.getColor()).toString().toUpperCase());
            } catch (Exception ignored) {
                return DyeColor.WHITE;
            }
        }).orElse(DyeColor.WHITE);
        Material bannerMaterial = Material.getMaterial(bannerColor.name() + "_BANNER");
        if (bannerMaterial == null) bannerMaterial = Material.WHITE_BANNER;
        menu.setItem(1, createButton(bannerMaterial, Component.text("选择队伍", NamedTextColor.GREEN), List.of(Component.text("点击这里加入或切换队伍。", NamedTextColor.GRAY))));

        // 按钮2: 游戏设置 (槽位 3)
        menu.setItem(3, createButton(Material.COMPARATOR, Component.text("游戏设置", NamedTextColor.YELLOW), List.of(Component.text("所有玩家均可修改本局游戏设置。", NamedTextColor.GRAY))));

        // 按钮3: 准备 (槽位 5)
        boolean isReady = lobbyManager.isPlayerReady(player);
        menu.setItem(5, createButton(
                isReady ? Material.LIME_DYE : Material.GRAY_DYE,
                isReady ? Component.text("准备就绪", NamedTextColor.GREEN) : Component.text("点击准备", NamedTextColor.GRAY),
                List.of(Component.text("当所有玩家准备后，即可开始游戏。", NamedTextColor.GRAY))
        ));

        // 按钮4: 开始游戏 (槽位 7)
        menu.setItem(7, createButton(Material.EMERALD_BLOCK, Component.text("开始游戏", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(Component.text("所有玩家准备后，点击即可开始！", NamedTextColor.GREEN))));

        player.openInventory(menu);
    }

    private ItemStack createButton(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());

            // [新增] 为所有GUI按钮添加一个特殊的NBT标签
            meta.getPersistentDataContainer().set(ItemRacing.GUI_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES); // 顺便隐藏物品属性

            item.setItemMeta(meta);
        }
        return item;
    }
}

package top.coast23.ItemRacing.guis;

import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class WaypointMenu {

    public static final Component MENU_TITLE = Component.text("路径点管理", NamedTextColor.BLUE);
    private final GameManager gameManager;

    public WaypointMenu(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        Optional<Team> playerTeamOpt = gameManager.getTeamManager().getPlayerTeam(player);
        if (playerTeamOpt.isEmpty()) {
            player.sendMessage(Component.text("你不在任何队伍中！", NamedTextColor.RED));
            return;
        }
        Team playerTeam = playerTeamOpt.get();

        int waypointCount = gameManager.getConfigManager().getTeamWaypointAmount();
        int menuSize = Math.max(9, ((waypointCount - 1) / 9 + 1) * 9);
        Inventory menu = Bukkit.createInventory(null, menuSize, MENU_TITLE);

        for (int i = 0; i < waypointCount; i++) {
            menu.setItem(i, createWaypointButton(playerTeam, i + 1));
        }
        player.openInventory(menu);
    }

    private ItemStack createWaypointButton(Team team, int index) {
        Map<Integer, Location> waypoints = gameManager.getTeamWaypoints().getOrDefault(team.getId(), Collections.emptyMap());
        Location loc = waypoints.get(index);

        NamespacedKey key = new NamespacedKey(ItemRacing.getInstance(), "waypoint_index");

        if (loc != null) {
            Material icon = getIconForLocation(team, index, loc);
            Biome biome = loc.getBlock().getBiome();

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("路径点 #" + index, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("坐标: ", NamedTextColor.GRAY).append(Component.text(String.format("%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), NamedTextColor.AQUA)));
                lore.add(Component.text("群系: ", NamedTextColor.GRAY).append(Component.text(formatBiomeName(biome), NamedTextColor.AQUA)));
                lore.add(Component.empty());
                lore.add(Component.text("» 左键传送", NamedTextColor.YELLOW));
                lore.add(Component.text("» 右键删除", NamedTextColor.RED));
                meta.lore(lore.stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, index);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }
            return item;
        } else {
            ItemStack item = new ItemStack(Material.MAP);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("路径点 #" + index + " (空)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(Component.text("» 点击以你当前位置设置", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, index);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }
            return item;
        }
    }

    /**
     * [改造] 获取一个地点的图标，添加了对非物品方块的安全检查。
     */
    private Material getIconForLocation(Team team, int index, Location loc) {
        Map<Integer, Material> iconCache = gameManager.getTeamWaypointIcons().getOrDefault(team.getId(), Collections.emptyMap());
        if (iconCache.containsKey(index)) {
            return iconCache.get(index);
        }

        Block currentBlock = loc.getBlock();
        while (currentBlock.isEmpty() && currentBlock.getY() > loc.getWorld().getMinHeight()) {
            currentBlock = currentBlock.getRelative(0, -1, 0);
        }

        Material icon = currentBlock.getType();

        // [核心修复] 检查找到的方块是否可以作为物品存在，如果不行，则使用默认图标。
        if (!icon.isItem()) {
            icon = switch (loc.getWorld().getEnvironment()) {
                case NORMAL -> Material.GRASS_BLOCK;
                case NETHER -> Material.NETHERRACK;
                case THE_END -> Material.END_STONE;
                default -> Material.FILLED_MAP;
            };
        }

        gameManager.setTeamWaypointIcon(team, index, icon);
        return icon;
    }

    private String formatBiomeName(Biome biome) {
        String name = biome.getKey().getKey();
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}

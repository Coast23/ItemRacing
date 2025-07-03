package top.coast23.ItemRacing.guis;

import org.bukkit.inventory.meta.Damageable;
import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.managers.GameSettingsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameSettingsMenu {

    public static final Component MENU_TITLE = Component.text("游戏设置", NamedTextColor.RED);
    private final GameSettingsManager gameSettings;

    public GameSettingsMenu(GameSettingsManager gameSettings) {
        this.gameSettings = gameSettings;
    }

    public void open(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, MENU_TITLE);

        menu.setItem(1, createButton(Material.CHEST, "任务物品数量", gameSettings.getItemsToCollect(), "» 点击后在聊天框输入数字"));
        menu.setItem(3, createGameModeButton());
        menu.setItem(5, createToolTypeButton());
        menu.setItem(7, createElytraTypeButton());

        menu.setItem(10, createFoodTypeButton());
        menu.setItem(12, createLevelButton(Material.SUGAR, "速度等级", gameSettings.getSpeedLevel()));
        menu.setItem(14, createLevelButton(Material.GLOWSTONE_DUST, "急迫等级", gameSettings.getHasteLevel()));
        menu.setItem(16, createLevelButton(Material.REDSTONE, "抗性提升等级", gameSettings.getResistLevel()));

        menu.setItem(19, createToggleButton(gameSettings.isGiveMendingBook(), "经验修补"));
        menu.setItem(21, createToggleButton(gameSettings.isGiveSilktouchBook(), "精准采集"));
        menu.setItem(23, createToggleButton(gameSettings.isGiveFortuneBook(), "时运"));
        menu.setItem(25, createToggleButton(gameSettings.isGiveLootingBook(), "抢夺"));

        player.openInventory(menu);
    }

    // --- 辅助方法 ---

    private ItemStack createGameModeButton() {
        GameSettingsManager.GameMode currentMode = gameSettings.getGameMode();
        Material icon = (currentMode == GameSettingsManager.GameMode.CLASSIC) ? Material.WRITABLE_BOOK : Material.BOOK;
        return createCycleButton(icon, "游戏模式 [无法更改]", currentMode.toString());
    }

    private ItemStack createToolTypeButton() {
        GameSettingsManager.ToolType currentType = gameSettings.getToolType();
        Material icon = switch (currentType) {
            case NONE -> Material.STICK;
            case STONE -> Material.STONE_PICKAXE;
            case IRON -> Material.IRON_PICKAXE;
        };
        return createCycleButton(icon, "开局工具", currentType.toString());
    }

    private ItemStack createElytraTypeButton() {
        GameSettingsManager.ElytraType currentType = gameSettings.getElytraType();
        ItemStack item;
        switch (currentType) {
            case NONE:
                item = new ItemStack(Material.PHANTOM_MEMBRANE);
                break;
            case BROKEN:
                item = new ItemStack(Material.ELYTRA);
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof Damageable) {
                    // 使用现代的 setDamage 方法，设置伤害值为 最大耐久-1，使其只剩1点耐久
                    ((Damageable) meta).setDamage(Material.ELYTRA.getMaxDurability() - 1);
                    item.setItemMeta(meta);
                }
                break;
            case NORMAL:
            default:
                item = new ItemStack(Material.ELYTRA);
                break;
        }
        return createCycleButton(item, "开局鞘翅", currentType.toString());
    }

    private ItemStack createFoodTypeButton() {
        GameSettingsManager.FoodType currentType = gameSettings.getFoodType();
        Material icon = switch (currentType) {
            case NONE -> Material.BREAD;
            case GOLDEN_CARROT -> Material.GOLDEN_CARROT;
            case SATURATION -> Material.COOKED_BEEF;
        };
        return createCycleButton(icon, "开局食物", currentType.toString());
    }

    private ItemStack createCycleButton(ItemStack item, String name, String status) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("当前: ", NamedTextColor.GRAY).append(Component.text(status, NamedTextColor.WHITE)));
            lore.add(Component.empty());
            lore.add(Component.text("» 点击切换", NamedTextColor.YELLOW));
            return createBaseButton(item, Component.text(name, NamedTextColor.AQUA), lore);
        }
        return item;
    }

    private ItemStack createCycleButton(Material material, String name, String status) {
        return createCycleButton(new ItemStack(material), name, status);
    }

    private ItemStack createToggleButton(boolean enabled, String name) {
        List<Component> description = new ArrayList<>();
        description.add(Component.text("当前状态: ", NamedTextColor.GRAY).append(enabled ? Component.text("开启", NamedTextColor.GREEN) : Component.text("关闭", NamedTextColor.RED)));
        description.add(Component.empty());
        description.add(Component.text("» 点击切换", NamedTextColor.YELLOW));

        ItemStack item = createBaseButton(Material.ENCHANTED_BOOK, Component.text(name, NamedTextColor.AQUA), description);
        if (enabled) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack createLevelButton(Material material, String name, int level) {
        ItemStack item = createButton(material, name, level, "» 左键增加 | 右键减少");
        item.setAmount(Math.max(1, level));
        return item;
    }

    private ItemStack createButton(Material material, String name, int currentValue, String... lore) {
        List<Component> description = new ArrayList<>();
        description.add(Component.text("当前: ", NamedTextColor.GRAY).append(Component.text(currentValue, NamedTextColor.WHITE)));
        description.add(Component.empty());
        for (String line : lore) {
            description.add(Component.text(line, NamedTextColor.YELLOW));
        }
        return createBaseButton(material, Component.text(name, NamedTextColor.AQUA), description);
    }

    private ItemStack createBaseButton(Material material, Component name, List<Component> lore) {
        return createBaseButton(new ItemStack(material), name, lore);
    }

    private ItemStack createBaseButton(ItemStack item, Component name, List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).collect(Collectors.toList()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(ItemRacing.GUI_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}

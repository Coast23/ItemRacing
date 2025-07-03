package top.coast23.ItemRacing.guis;

import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.managers.TeamManager;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class TeamSelectMenu {

    public static final Component MENU_TITLE = Component.text("选择你的队伍", NamedTextColor.DARK_GREEN);

    private final TeamManager teamManager;

    public TeamSelectMenu(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void open(Player player) {
        Collection<Team> allTeams = teamManager.getAllTeams();
        int menuSize = Math.max(9, ((allTeams.size() - 1) / 9 + 1) * 9);
        Inventory menu = Bukkit.createInventory(null, menuSize, MENU_TITLE);

        int slot = 0;
        for (Team team : allTeams) {
            ItemStack teamBanner = createTeamBanner(team, player);
            menu.setItem(slot, teamBanner);
            slot++;
        }

        player.openInventory(menu);
    }

    /**
     * 创建一个代表队伍的旗帜图标。
     */
    private ItemStack createTeamBanner(Team team, Player viewer) {
        DyeColor dyeColor = DyeColor.WHITE;
        try {
            dyeColor = DyeColor.valueOf(NamedTextColor.nearestTo(team.getColor()).toString().toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        Material bannerMaterial = Material.getMaterial(dyeColor.name() + "_BANNER");
        if (bannerMaterial == null) bannerMaterial = Material.WHITE_BANNER;

        ItemStack banner = new ItemStack(bannerMaterial);
        ItemMeta meta = banner.getItemMeta();

        if (meta != null) {
            NamespacedKey key = new NamespacedKey(ItemRacing.getInstance(), "team_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, team.getId());

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("当前成员: ", NamedTextColor.GRAY).append(Component.text(team.getSize(), NamedTextColor.WHITE)));
            lore.add(Component.empty());

            Optional<Team> playerTeamOpt = teamManager.getPlayerTeam(viewer);
            if (playerTeamOpt.isPresent() && playerTeamOpt.get().equals(team)) {
                // 将 Enchantment.DURABILITY 替换为现代版本的 Enchantment.UNBREAKING
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                lore.add(Component.text("✔ 你已在该队伍中", NamedTextColor.GREEN));
            } else {
                lore.add(Component.text("» 点击加入此队伍", NamedTextColor.YELLOW));
            }
            meta.getPersistentDataContainer().set(ItemRacing.GUI_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            meta.displayName(Component.text(team.getDisplayName(), team.getColor()).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            banner.setItemMeta(meta);
        }
        return banner;
    }
}

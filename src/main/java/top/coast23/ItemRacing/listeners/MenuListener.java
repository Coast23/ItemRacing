package top.coast23.ItemRacing.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.guis.GameMenu;
import top.coast23.ItemRacing.guis.GameSettingsMenu;
import top.coast23.ItemRacing.guis.LobbyMenu;
import top.coast23.ItemRacing.guis.TeamChestSelectMenu;
import top.coast23.ItemRacing.guis.TeamSelectMenu;
import top.coast23.ItemRacing.guis.WaypointMenu;
import top.coast23.ItemRacing.managers.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Collections;
import java.util.Objects;

/**
 * [全面重构] 负责监听所有自定义GUI菜单中的点击事件。
 */
public class MenuListener implements Listener {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final LobbyManager lobbyManager;
    private final GuiManager guiManager;
    private final GameSettingsManager gameSettings;
    private final ScoreboardManager scoreboardManager;
    private final BroadcastManager broadcastManager;

    public MenuListener(GameManager gameManager, TeamManager teamManager, LobbyManager lobbyManager, GuiManager guiManager, GameSettingsManager gameSettings, ScoreboardManager scoreboardManager, BroadcastManager broadcastManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.lobbyManager = lobbyManager;
        this.guiManager = guiManager;
        this.gameSettings = gameSettings;
        this.scoreboardManager = scoreboardManager;
        this.broadcastManager = broadcastManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component menuTitle = event.getView().title();

        // --- 统一的取消逻辑 ---
        if (isCustomMenu(menuTitle)) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            }
        }

        // 修复SHIFT + 点击物品时会触发选项的 BUG.
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        // 检查被点击的物品栏是否是上面的菜单 (GUI)。
        // 如果不是 (例如，玩家点击了自己的背包)，就直接返回，不处理后续的按钮逻辑。
        if (clickedInventory == null || !clickedInventory.equals(topInventory)) {
            return;
        }

        // --- 具体菜单的点击事件处理 ---
        if (event.getCurrentItem() == null) return;

        if (menuTitle.equals(LobbyMenu.MENU_TITLE)) {
            handleLobbyMenuClick(player, event.getSlot());
        } else if (menuTitle.equals(TeamSelectMenu.MENU_TITLE)) {
            handleTeamSelectMenuClick(player, event.getCurrentItem());
        } else if (menuTitle.equals(GameSettingsMenu.MENU_TITLE)) {
            handleGameSettingsMenuClick(player, event.getSlot(), event.getClick());
        } else if (menuTitle.equals(GameMenu.MENU_TITLE)) {
            handleGameMenuClick(player, event.getSlot());
        } else if (menuTitle.equals(TeamChestSelectMenu.MENU_TITLE)) {
            handleTeamChestSelectMenuClick(player, event.getSlot());
        } else if (menuTitle.equals(WaypointMenu.MENU_TITLE)) {
            handleWaypointMenuClick(player, event.getCurrentItem(), event.getClick());
        }
    }

    private boolean isCustomMenu(Component title) {
        return title.equals(LobbyMenu.MENU_TITLE) || title.equals(GameMenu.MENU_TITLE) ||
                title.equals(TeamSelectMenu.MENU_TITLE) || title.equals(GameSettingsMenu.MENU_TITLE) ||
                title.equals(TeamChestSelectMenu.MENU_TITLE) || title.equals(WaypointMenu.MENU_TITLE);
    }

    private void handleLobbyMenuClick(Player player, int slot) {
        switch (slot) {
            case 1: new TeamSelectMenu(teamManager).open(player); break;
            case 3: new GameSettingsMenu(gameSettings).open(player); break;
            case 5:
                if (teamManager.getPlayerTeam(player).isEmpty()) {
                    player.sendMessage(Component.text("请先加入队伍！", NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }
                lobbyManager.togglePlayerReady(player);
                broadcastManager.playerReadyStatusChanged(player, lobbyManager.isPlayerReady(player));
                new LobbyMenu(guiManager, gameManager, teamManager, lobbyManager).open(player);
                break;
            case 7:
                gameManager.attemptToStartGame(player);
                player.closeInventory();
                break;
        }
    }

    private void handleTeamSelectMenuClick(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(ItemRacing.getInstance(), "team_id");
        String teamId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (teamId != null) {
            teamManager.getTeamById(teamId).ifPresent(clickedTeam -> {
                if (teamManager.addPlayerToTeam(player, clickedTeam.getId())) {
                    player.playerListName(Component.text(player.getName(), clickedTeam.getColor()));
                //    player.sendMessage(Component.text("成功加入 ", NamedTextColor.GREEN)
                //            .append(Component.text(clickedTeam.getDisplayName(), clickedTeam.getColor())));
                    Bukkit.broadcast(Component.text(player.getName(), NamedTextColor.AQUA)
                            .append(Component.text(" 加入了 ", NamedTextColor.GREEN))
                            .append(Component.text(clickedTeam.getDisplayName(), clickedTeam.getColor()))
                            .append(Component.text(" !", NamedTextColor.GREEN))
                    );
                } else {
                    player.sendMessage(Component.text("加入队伍失败！", NamedTextColor.RED));
                }
                new LobbyMenu(guiManager, gameManager, teamManager, lobbyManager).open(player);
            });
        }
    }

    private void handleGameSettingsMenuClick(Player player, int slot, ClickType click) {
        boolean refresh = true;
        String settingName = "";
        String newValue = "";
        String preValue = "";

        switch (slot) {
            // --- Row 2 ---
            case 1: // 任务物品数量
                player.closeInventory();
                ChatListener.requestItemAmountInput(player);
                refresh = false;
                break;
            case 3: // 游戏模式
                /* 太难实现, 我决定废弃 */
            //    gameSettings.toggleGameMode();
            //    settingName = "游戏模式";
            //    newValue = gameSettings.getGameMode().toString();
                break;
            case 5: // 开局工具
                preValue = gameSettings.getToolType().toString();
                gameSettings.cycleToolType();
                settingName = "工具";
                newValue = gameSettings.getToolType().toString();
                break;
            case 7: // 开局鞘翅
                preValue = gameSettings.getToolType().toString();
                gameSettings.cycleElytraType();
                settingName = "鞘翅";
                newValue = gameSettings.getElytraType().toString();
                break;
            // --- Row 3 ---
            case 10: // 开局食物
                preValue = gameSettings.getFoodType().toString();
                gameSettings.cycleFoodType();
                settingName = "食物";
                newValue = gameSettings.getFoodType().toString();
                break;
            case 12: // 速度等级
                preValue = String.valueOf(gameSettings.getSpeedLevel());
                if (click.isLeftClick()) gameSettings.setSpeedLevel(gameSettings.getSpeedLevel() + 1);
                else if (click.isRightClick()) gameSettings.setSpeedLevel(gameSettings.getSpeedLevel() - 1);
                settingName = "速度效果";
                newValue = String.valueOf(gameSettings.getSpeedLevel());
                break;
            case 14: // 急迫等级
                preValue = String.valueOf(gameSettings.getHasteLevel());
                if (click.isLeftClick()) gameSettings.setHasteLevel(gameSettings.getHasteLevel() + 1);
                else if (click.isRightClick()) gameSettings.setHasteLevel(gameSettings.getHasteLevel() - 1);
                settingName = "急迫效果";
                newValue = String.valueOf(gameSettings.getHasteLevel());
                break;
            case 16:
                preValue = String.valueOf(gameSettings.getResistLevel());
                if(click.isLeftClick()) gameSettings.setResistLevel(gameSettings.getResistLevel() + 1);
                else if(click.isRightClick()) gameSettings.setResistLevel(gameSettings.getResistLevel() - 1);
                settingName = "抗性提升";
                newValue = String.valueOf(gameSettings.getResistLevel());
                break;
                // --- Row 4 ---
            case 19:
                preValue = gameSettings.isGiveMendingBook() ? "是" : "否";
                gameSettings.toggleGiveMendingBook();
                settingName = "给予 [经验修补]";
                newValue = gameSettings.isGiveMendingBook() ? "是" : "否";
                break;
            case 21:
                preValue = gameSettings.isGiveSilktouchBook() ? "是" : "否";
                gameSettings.toggleGiveSilktouchBook();
                settingName = "给予 [精准采集]";
                newValue = gameSettings.isGiveSilktouchBook() ? "是" : "否";
                break;
            case 23:
                preValue = gameSettings.isGiveFortuneBook() ? "是" : "否";
                gameSettings.toggleGiveFortuneBook();
                settingName = "给予 [时运]";
                newValue = gameSettings.isGiveFortuneBook() ? "是" : "否";
                break;
            case 25:
                preValue = gameSettings.isGiveLootingBook() ? "是" : "否";
                gameSettings.toggleGiveLootingBook();
                settingName = "给予 [抢夺]";
                newValue = gameSettings.isGiveLootingBook() ? "是" : "否";
                break;
            default:
                refresh = false;
                break;
        }

        if (refresh && !Objects.equals(preValue, newValue)) {
            broadcastManager.settingChanged(player, settingName, newValue);
            guiManager.openGameSettingsMenu(player);
            scoreboardManager.forceUpdateAllScoreboards();
        }
    }

    private void handleGameMenuClick(Player player, int slot) {
        // [修复] 为所有按钮添加关闭菜单的逻辑，并实现/locate
        switch (slot) {
            case 0: // 团队箱子
                player.performCommand("chest");
                // chest 命令会打开另一个GUI，所以这里不需要关闭
                return;
            case 2: // 路径点
                player.performCommand("wp");
                // wp 命令会打开另一个GUI
                return;
            case 4: // 申请重摇
                player.performCommand("roll");
                break;
            case 6: // 购买定位权限
                gameManager.purchaseLocateCharge(player);
                break;
            case 8: // 随机传送
                player.performCommand("tpr");
                break;
            default: // 点击其他地方
                return;
        }
        // 对于执行完指令就需要看聊天框反馈的功能，自动关闭菜单
        player.closeInventory();
    }

    private void handleTeamChestSelectMenuClick(Player player, int chestIndex) {
        teamManager.getPlayerTeam(player).ifPresent(team -> {
            List<Inventory> chests = gameManager.getTeamChests().get(team.getId());
            if (chests != null && chestIndex >= 0 && chestIndex < chests.size()) {
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f);
                player.openInventory(chests.get(chestIndex));
            }
        });
    }

    private void handleWaypointMenuClick(Player player, ItemStack clickedItem, ClickType clickType) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(ItemRacing.getInstance(), "waypoint_index");
        Integer index = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        if (index == null) return;

        teamManager.getPlayerTeam(player).ifPresent(team -> {
            if (clickType.isLeftClick()) {
                Location loc = gameManager.getTeamWaypoints().getOrDefault(team.getId(), Collections.emptyMap()).get(index);
                if (loc != null) {
                    player.teleport(loc);
                //    player.sendMessage("已将你传送至路径点 #" + index);
                    player.sendMessage(Component.text("已将你传送至路径点 ", NamedTextColor.GREEN)
                            .append(Component.text("#" + index, NamedTextColor.AQUA))
                    );
                    player.closeInventory(); // 传送后关闭菜单
                } else {
                    gameManager.setTeamWaypoint(team, index, player.getLocation());
                //    player.sendMessage("已将当前位置设为路径点 #" + index);
                    player.sendMessage(Component.text("已将当前位置设为路径点 ", NamedTextColor.GREEN)
                            .append(Component.text("#" + index, NamedTextColor.AQUA))
                    );
                    guiManager.openWaypointMenu(player); // 刷新菜单
                }
            } else if (clickType.isRightClick()) {
                gameManager.removeTeamWaypoint(team, index);
                gameManager.removeTeamWaypointIcon(team, index);
            //    player.sendMessage("已删除路径点 #" + index);
                player.sendMessage(Component.text("已删除路径点 ",  NamedTextColor.RED)
                        .append(Component.text("#" + index, NamedTextColor.AQUA))
                );
                guiManager.openWaypointMenu(player); // 刷新菜单
            }
        });
    }
}

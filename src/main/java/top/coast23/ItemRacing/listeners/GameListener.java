package top.coast23.ItemRacing.listeners;

import net.kyori.adventure.text.format.TextDecoration;
import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.managers.ConfigManager;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.GameSettingsManager;
import top.coast23.ItemRacing.managers.GuiManager;
import top.coast23.ItemRacing.managers.ScoreboardManager;
import top.coast23.ItemRacing.managers.BossBarManager;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;



/**
 * 负责监听与核心游戏流程相关的玩家事件。
 */
public class GameListener implements Listener {

    private final GameManager gameManager;
    private final ScoreboardManager scoreboardManager;
    private final GuiManager guiManager;
    private final BossBarManager bossBarManager;
    private final ItemRacing plugin;

    // [修复] 构造函数现在接收4个管理器实例
    public GameListener(GameManager gameManager, ScoreboardManager scoreboardManager, GuiManager guiManager, BossBarManager bossBarManager) {
        this.gameManager = gameManager;
        this.scoreboardManager = scoreboardManager;
        this.guiManager = guiManager;
        this.bossBarManager = bossBarManager;
        this.plugin = ItemRacing.getInstance();
    }

    public static void unlockAllRecipes(Player player) {
        // 创建一个列表来存储所有配方的NamespacedKey
        Collection<NamespacedKey> keys = new ArrayList<>();

        // 获取服务器所有配方的迭代器
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();

        // 遍历所有配方
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            // 检查配方是否是Keyed类型，从而可以获取NamespacedKey
            if (recipe instanceof Keyed) {
                // 将配方的Key添加到列表中
                keys.add(((Keyed) recipe).getKey());
            }
        }

        // 使用性能更高的方法，一次性为玩家解锁所有配方
        player.discoverRecipes(keys);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(gameManager.getConfigManager().isUnlockAllRecipes()) unlockAllRecipes(player);
        scoreboardManager.setupPlayer(player);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Optional<Team> playerTeam = gameManager.getTeamManager().getPlayerTeam(player);

            if (playerTeam.isPresent()) {
                // 无论在哪个阶段，只要玩家在队伍里，就设置他的Tab列表名
                player.playerListName(Component.text(player.getName(), playerTeam.get().getColor()));
            }

            if (gameManager.getCurrentState() == GameManager.GameState.LOBBY) {

                player.setGameMode(GameMode.ADVENTURE);
                player.sendMessage(Component.text("欢迎来到 ItemRacing !, 请按 ",  NamedTextColor.GREEN)
                        .append(Component.text("SHIFT+副手", NamedTextColor.AQUA))
                        .append(Component.text(" 打开选队与设置界面! 在游戏的不同阶段输入命令 ", NamedTextColor.GREEN))
                        .append(Component.text("/help ", NamedTextColor.AQUA))
                        .append(Component.text("以查看当前阶段可使用的命令列表!"))
                );

                ConfigManager configManager = ItemRacing.getInstance().getConfigManager();


                player.sendMessage(Component.text("----- 当前游戏规则 -----", NamedTextColor.GOLD, TextDecoration.BOLD));
                player.sendMessage(Component.text("完成每个物品得分: ",  NamedTextColor.YELLOW)
                        .append(Component.text(configManager.getScorePerItem(), NamedTextColor.GREEN))
                );
                player.sendMessage(Component.text("随机传送花费: ", NamedTextColor.YELLOW)
                        .append(Component.text(configManager.getRandomTeleportCost(), NamedTextColor.RED))
                        .append(Component.text("   免费次数: ",  NamedTextColor.YELLOW))
                        .append(Component.text(configManager.getInitialTprUses(), NamedTextColor.GREEN))
                );
                player.sendMessage(Component.text("定位花费: ",  NamedTextColor.YELLOW)
                        .append(Component.text(configManager.getRandomTeleportCost(), NamedTextColor.RED))
                        .append(Component.text("       免费次数: ", NamedTextColor.YELLOW))
                        .append(Component.text(configManager.getInitialLocateUses(), NamedTextColor.GREEN))
                );
                player.sendMessage(Component.text("队伍箱子数: ",  NamedTextColor.AQUA)
                        .append(Component.text(configManager.getTeamChestAmount(), NamedTextColor.GREEN))
                        .append(Component.text("   路径点数: ",   NamedTextColor.AQUA))
                        .append(Component.text(configManager.getTeamWaypointAmount(), NamedTextColor.GREEN))
                );
                player.sendMessage(Component.text("任务队列大小: ", NamedTextColor.AQUA)
                        .append(Component.text(configManager.getTaskQueueSize(), NamedTextColor.GREEN))
                        .append(Component.text("   重摇次数上限: ",   NamedTextColor.AQUA))
                        .append(Component.text(configManager.getMaxRollsPerTeam(), NamedTextColor.GREEN))
                );

            //    player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            } else if (gameManager.getCurrentState() == GameManager.GameState.ACTIVE) {
                if (playerTeam.isPresent()) {
                    bossBarManager.showBarToPlayer(player, playerTeam.get());
                    player.sendMessage(Component.text("欢迎回来！你仍在 ", NamedTextColor.YELLOW)
                            .append(Component.text(playerTeam.get().getDisplayName(), playerTeam.get().getColor())));
                } else {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage(Component.text("游戏正在进行中，你已进入观察者模式。", NamedTextColor.GRAY));
                }
            }
        }, 2L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        scoreboardManager.removePlayer(player);

        // [改造] 只有在游戏进行中时，玩家退出才需要更新重摇状态
        if (gameManager.getCurrentState() == GameManager.GameState.ACTIVE) {
            gameManager.getTeamManager().getPlayerTeam(player).ifPresent(team -> {
                Set<UUID> requesters = gameManager.getTeamRollRequesters().get(team.getId());
                if (requesters != null && requesters.remove(player.getUniqueId())) {
                    if(gameManager.getGameSettings().getGameMode() == GameSettingsManager.GameMode.CLASSIC)
                        gameManager.checkTeamRollStatus(team);// 更新BossBar
                    else if(gameManager.getGameSettings().getGameMode() == GameSettingsManager.GameMode.RACING)
                        gameManager.checkGlobalRollStatus();
                }
                team.broadcast(Component.text("你的队友 " + player.getName() + " 已下线。", NamedTextColor.YELLOW));
            });
        }
        if (gameManager.getCurrentState() == GameManager.GameState.LOBBY) {
            // 退出组队
            player.performCommand("itemracing:leave");
        }
    }

    /**
     * 当玩家按下F键（切换副手）且潜行时，作为打开菜单的快捷方式。
     */
    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        // 只有在游戏准备或进行中才响应
        if (gameManager.getCurrentState() == GameManager.GameState.LOBBY || gameManager.getCurrentState() == GameManager.GameState.ACTIVE) {
            if (event.getPlayer().isSneaking()) {
                event.setCancelled(true);
                // 调用 GuiManager 打开合适的菜单
                guiManager.openMenu(event.getPlayer());
            }
        }
    }

    /**
     * 当玩家重生时触发，给予短暂的保护效果。
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // 只有在游戏进行中时才给予效果
        if (gameManager.getCurrentState() == GameManager.GameState.ACTIVE) {
        //    player.sendMessage(Component.text("你获得了重生保护效果！", NamedTextColor.AQUA));

            // 使用 Bukkit 调度器延迟应用效果，确保玩家已完全重生
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 2)); // 10秒的抗性提升 III
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1)); // 5秒的生命恢复 II
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0)); // 10秒的抗火
            }, 1L); // 延迟1个游戏刻
        }
    }
}

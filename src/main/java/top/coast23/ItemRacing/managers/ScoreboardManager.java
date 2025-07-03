package top.coast23.ItemRacing.managers;

import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 负责为所有玩家创建、更新和管理计分板。
 */
public class ScoreboardManager {

    private final ItemRacing plugin;
    private GameManager gameManager; // [改造] 初始时可以为null
    private final PlayerSettings playerSettings;

    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, Integer> rotationIndex = new HashMap<>();
    private BukkitTask updateTask;

    public ScoreboardManager(ItemRacing plugin, GameManager gameManager, PlayerSettings playerSettings) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.playerSettings = playerSettings;
    }

    /**
     * [新增] 用于解决循环依赖。在GameManager被创建后，通过此方法注入。
     * @param gameManager 游戏管理器实例
     */
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void startUpdater() {
        this.updateTask = new BukkitRunnable() {
            private int timer = 0;
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboardFor(player, timer);
                }
                timer++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    /**
     * [新增] 强制刷新所有在线玩家的计分板，用于即时反馈。
     */
    public void forceUpdateAllScoreboards() {
        // 在主线程的下一个tick执行，确保所有数据都已更新
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboardFor(player, -1); // timer为-1表示非周期性强制更新
                }
            }
        }.runTask(plugin);
    }

    /**
     * 停止计分板更新任务。
     */
    public void stopUpdater() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    /**
     * 为一个新加入的玩家设置计分板。
     * @param player 新玩家
     */
    public void setupPlayer(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("ItemRacing", "dummy", Component.text("ItemRacing", NamedTextColor.GOLD, TextDecoration.BOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < 15; i++) {
            board.registerNewTeam("line" + i).addEntry("§" + Integer.toHexString(i));
        }

        player.setScoreboard(board);
        playerScoreboards.put(player.getUniqueId(), board);
        playerSettings.setRankingStyle(player); // 默认设置为排行榜模式
    }

    /**
     * 移除一个玩家的计分板（例如当他们退出时）。
     * @param player 要移除计分板的玩家
     */
    public void removePlayer(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        playerScoreboards.remove(player.getUniqueId());
        playerSettings.clearSettingsFor(player);
        rotationIndex.remove(player.getUniqueId());
    }

    /**
     * 更新指定玩家的计分板内容。
     * @param player 要更新的玩家
     * @param timer 全局计时器，用于滚动模式
     */
    private void updateScoreboardFor(Player player, int timer) {
        if (gameManager == null) return;

        Scoreboard board = playerScoreboards.get(player.getUniqueId());
        if (board == null) return;

        Objective objective = board.getObjective("ItemRacing");
        if (objective == null) return;

        List<Component> lines = new ArrayList<>();

        // 根据游戏状态渲染不同的计分板
        switch(gameManager.getCurrentState()) {
            case LOBBY:
                renderLobbyView(lines);
                break;
            case ACTIVE:
                renderActiveGameView(player, lines, timer);
                break;
            case ENDED:
                renderEndedView(lines);
                break;
        }

        // --- 渲染逻辑 ---
        // 先清空所有行
        for(int i=0; i<15; i++) {
            org.bukkit.scoreboard.Team lineTeam = board.getTeam("line" + i);
            if(lineTeam != null) {
                lineTeam.prefix(Component.empty());
            }
        }

        // 从上到下渲染新行
        for (int i = 0; i < lines.size(); i++) {
            if (i >= 15) break; // 防止行数超标
            org.bukkit.scoreboard.Team lineTeam = board.getTeam("line" + (14 - i));
            if (lineTeam != null) {
                lineTeam.prefix(lines.get(i));
                objective.getScore("§" + Integer.toHexString(14 - i)).setScore(15 - i);
            }
        }

        /*
        // --- 渲染逻辑 ---
        for (int i = 0; i < 15; i++) {
            org.bukkit.scoreboard.Team lineTeam = board.getTeam("line" + i);
            if (lineTeam != null) {
                if (i < lines.size()) {
                    lineTeam.prefix(lines.get(lines.size() - 1 - i));
                    objective.getScore("§" + Integer.toHexString(i)).setScore(i);
                } else {
                    board.resetScores("§" + Integer.toHexString(i));
                }
            }
        }
        * */
    }

    private void renderLobbyView(List<Component> lines) {
        GameSettingsManager settings = gameManager.getGameSettings();
        lines.add(Component.empty());
        lines.add(Component.text("当前游戏设置:", NamedTextColor.GOLD));
        lines.add(Component.text("  目标个数: ", NamedTextColor.YELLOW).append(Component.text(settings.getItemsToCollect(), NamedTextColor.GREEN)));
        lines.add(Component.text("  工具: ", NamedTextColor.YELLOW).append(Component.text(settings.getToolType().toString(), NamedTextColor.GREEN)));
        lines.add(Component.text("  鞘翅: ", NamedTextColor.YELLOW).append(Component.text(settings.getElytraType().toString(), NamedTextColor.GREEN)));
        lines.add(Component.text("  食物: ", NamedTextColor.YELLOW).append(Component.text(settings.getFoodType().toString(), NamedTextColor.GREEN)));
        lines.add(Component.text("  速度/急迫/抗性提升: ", NamedTextColor.YELLOW).append(Component.text(settings.getSpeedLevel() + "/" + settings.getHasteLevel() + "/" + settings.getResistLevel(), NamedTextColor.GREEN)));
        lines.add(Component.empty());
        lines.add(Component.text("在线玩家: " + Bukkit.getOnlinePlayers().size(), NamedTextColor.YELLOW));
    }

    private void renderActiveGameView(Player player, List<Component> lines, int timer) {
        // 显示游戏已进行的时间
        long elapsedSeconds = (System.currentTimeMillis() - gameManager.getGameStartTime()) / 1000;
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;
        lines.add(Component.text("  游戏已进行: ", NamedTextColor.YELLOW)
                .append(Component.text(String.format("%02d:%02d:%02d", hours, minutes, seconds), NamedTextColor.WHITE)));

        // 显示玩家自己队伍的信息
        Optional<Team> playerTeamOpt = gameManager.getTeamManager().getPlayerTeam(player);
        if (playerTeamOpt.isPresent()) {
            Team playerTeam = playerTeamOpt.get();
            int score = gameManager.getTeamScores().getOrDefault(playerTeam.getId(), 0);
            int totalItems = gameManager.getTeamTotalItems().getOrDefault(playerTeam.getId(), 0);
            int remainingItems = gameManager.getTeamRequiredItems().getOrDefault(playerTeam.getId(), List.of()).size();
            int completedItems = totalItems - remainingItems;
            int locateUses = gameManager.getTeamLocateUses().getOrDefault(playerTeam.getId(), 0);

            lines.add(Component.text("» ", NamedTextColor.GRAY)
                    .append(Component.text(playerTeam.getDisplayName(), playerTeam.getColor()))
                    .append(Component.text(" "))
                    .append(Component.text(score, NamedTextColor.YELLOW))
                    .append(Component.text("分 ", NamedTextColor.YELLOW))
                    .append(Component.text(" (" + completedItems + "/" + totalItems + ")", NamedTextColor.AQUA)));

            int maxRolls = gameManager.getConfigManager().getMaxRollsPerTeam();
            int rollsUsed = gameManager.getTeamRollsUsed().getOrDefault(playerTeam.getId(), 0);
            int requesters = gameManager.getTeamRollRequesters().getOrDefault(playerTeam.getId(), Set.of()).size();
            int tprLeft = gameManager.getTeamTprUses().getOrDefault(playerTeam.getId(), 0);
            long onlineMembers = playerTeam.getMembers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).count();

            lines.add(Component.text("» ", NamedTextColor.GRAY)
                    .append(Component.text("定位: ", locateUses > 0 ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.text(locateUses, NamedTextColor.GOLD))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(Component.text("传送: ", tprLeft > 0 ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.text(tprLeft, NamedTextColor.GOLD))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(Component.text("重摇: ", maxRolls - rollsUsed > 0 ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.text(maxRolls - rollsUsed, NamedTextColor.GOLD))
            );

        //    List<Material> taskQueue = gameManager.getTaskQueueFor(playerTeam);
            List<Material> taskQueue = gameManager.getCurrentItems(playerTeam);
        //    if (!taskQueue.isEmpty()) {
        //        lines.add(Component.text(" ")); // 在任务列表前加一个空行
        //    }
            for (int i = 0; i < taskQueue.size(); i++) {
                Material item = taskQueue.get(i);
                lines.add(Component.text("[" + (i + 1) + "] ", NamedTextColor.YELLOW)
                        .append(Component.translatable(item).color(NamedTextColor.AQUA)));
            }
        } else {
            lines.add(Component.text("你不在任何队伍中", NamedTextColor.GRAY));
        }

        // 根据玩家偏好渲染下方视图
        PlayerSettings.PlayerDisplaySettings settings = playerSettings.getSettingsFor(player);
        switch (settings.style()) {
            case RANKING: renderRankingView(lines); break;
            case WATCH_TEAM: renderWatchTeamView(player, lines, settings.watchedTeamId()); break;
            case ROTATING: renderRotatingView(lines, player, settings.rotationSeconds(), timer); break;
        }
    }

    private void renderRankingView(List<Component> lines) {
        lines.add(Component.empty());
        lines.add(Component.text("» 排行榜", NamedTextColor.GOLD));

        List<Team> sortedTeams = gameManager.getTeamManager().getAllTeams().stream()
                .sorted(Comparator.comparingInt((Team team) -> {
                    int total = gameManager.getTeamTotalItems().getOrDefault(team.getId(), 0);
                    int remaining = gameManager.getTeamRequiredItems().getOrDefault(team.getId(), Collections.emptyList()).size();
                    return total - remaining;
                }).reversed())
                .toList();

        int rank = 1;
        for (Team team : sortedTeams) {
            int total = gameManager.getTeamTotalItems().getOrDefault(team.getId(), 0);
            int remaining = gameManager.getTeamRequiredItems().getOrDefault(team.getId(), Collections.emptyList()).size();
            int completed = total - remaining;

            Component line = Component.text(rank + ". ", NamedTextColor.GRAY)
                    .append(Component.text(team.getDisplayName(), team.getColor()))
                    .append(Component.text(" - " + completed + "/" + total, NamedTextColor.WHITE));
            lines.add(line);
            rank++;
        }
    }

    private void renderWatchTeamView(Player player, List<Component> lines, String targetTeamId) {
        lines.add(Component.empty());

        Optional<Team> playerTeamOpt = gameManager.getTeamManager().getPlayerTeam(player);
        if (playerTeamOpt.isPresent() && playerTeamOpt.get().getId().equals(targetTeamId)) {
            lines.add(Component.text("» 无法监视自己的队伍", NamedTextColor.RED));
            return;
        }

        if (targetTeamId == null) { lines.add(Component.text("» 未选择监视队伍", NamedTextColor.RED)); return; }
        Optional<Team> targetTeamOpt = gameManager.getTeamManager().getTeamById(targetTeamId);
        if (targetTeamOpt.isEmpty()) { lines.add(Component.text("» 监视的队伍不存在", NamedTextColor.RED)); return; }

        Team targetTeam = targetTeamOpt.get();
        int score = gameManager.getTeamScores().getOrDefault(targetTeam.getId(), 0);
        int totalItems = gameManager.getTeamTotalItems().getOrDefault(targetTeam.getId(), 0);
        int remainingItems = gameManager.getTeamRequiredItems().getOrDefault(targetTeam.getId(), Collections.emptyList()).size();
        int completedItems = totalItems - remainingItems;

        lines.add(Component.text("» 监视中: ", NamedTextColor.YELLOW)
                .append(Component.text(targetTeam.getDisplayName(), targetTeam.getColor()))
                .append(Component.text(" "))
                .append(Component.text(score, NamedTextColor.YELLOW))
                .append(Component.text("分 ", NamedTextColor.YELLOW))
                .append(Component.text(" (" + completedItems + "/" + totalItems + ")", NamedTextColor.AQUA)));

    //    List<Material> taskQueue = gameManager.getTaskQueueFor(targetTeam);
        List<Material> taskQueue = gameManager.getCurrentItems(targetTeam);
        for (int i = 0; i < taskQueue.size(); i++) {
            Material item = taskQueue.get(i);
            lines.add(Component.text("[" + (i + 1) + "] ", NamedTextColor.DARK_GRAY).append(Component.translatable(item).color(NamedTextColor.GRAY)));
        }
    }

    private void _renderRotatingView(List<Component> lines, Player player, int seconds, int timer) {
        lines.add(Component.empty());

        List<Team> otherTeams = gameManager.getTeamManager().getAllTeams().stream()
                .filter(team -> !team.isMember(player))
                .collect(Collectors.toList());

        if (otherTeams.isEmpty()) { lines.add(Component.text("» 没有其他队伍", NamedTextColor.YELLOW)); return; }

        // 我的魔改
    //    long gameTime = (System.currentTimeMillis() - gameManager.getGameStartTime()) / 1000;
    //    int totalIntervalsPassed = (int) (elapsedSeconds / seconds);
        int currentRotationIndex = rotationIndex.getOrDefault(player.getUniqueId(), 0);
        // 每当计时器达到间隔秒数的倍数时，才增加索引
        if (timer > 0 && timer % seconds == 0) {
            currentRotationIndex = (currentRotationIndex + 1) % otherTeams.size();
            rotationIndex.put(player.getUniqueId(), currentRotationIndex);
        }
        Team teamToDisplay = otherTeams.get(currentRotationIndex);
        int score = gameManager.getTeamScores().getOrDefault(teamToDisplay.getId(), 0);
        int totalItems = gameManager.getTeamTotalItems().getOrDefault(teamToDisplay.getId(), 0);
        int remainingItems = gameManager.getTeamRequiredItems().getOrDefault(teamToDisplay.getId(), Collections.emptyList()).size();
        int completedItems = totalItems - remainingItems;

        lines.add(Component.text("» 滚动中: ", NamedTextColor.YELLOW)
                .append(Component.text(teamToDisplay.getDisplayName(), teamToDisplay.getColor()))
                .append(Component.text(" "))
                .append(Component.text(score, NamedTextColor.YELLOW))
                .append(Component.text("分 ", NamedTextColor.YELLOW))
                .append(Component.text(" (" + completedItems + "/" + totalItems + ")", NamedTextColor.AQUA)));

    //    List<Material> taskQueue = gameManager.getTaskQueueFor(teamToDisplay);
        List<Material> taskQueue = gameManager.getCurrentItems(teamToDisplay);
        for (int i = 0; i < taskQueue.size(); i++) {
            Material item = taskQueue.get(i);
            lines.add(Component.text("[" + (i + 1) + "] ", NamedTextColor.DARK_GRAY).append(Component.translatable(item).color(NamedTextColor.GRAY)));
        }
    }
    private void renderRotatingView(List<Component> lines, Player player, int seconds, int timer) {
        lines.add(Component.empty());

        List<Team> otherTeams = gameManager.getTeamManager().getAllTeams().stream()
                .filter(team -> !team.isMember(player))
                .collect(Collectors.toList());

        if (otherTeams.isEmpty()) { lines.add(Component.text("» 没有其他队伍", NamedTextColor.YELLOW)); return; }

        long gameStartTime = gameManager.getGameStartTime();
        long elapsedSeconds = (System.currentTimeMillis() - gameStartTime) / 1000;
        //    int totalIntervalsPassed = (int) (elapsedSeconds / seconds);
        int currentRotationIndex = (int) (elapsedSeconds / seconds) % otherTeams.size();

        // 3. 使用计算出的索引来显示队伍信息
        Team teamToDisplay = otherTeams.get(currentRotationIndex);
        int score = gameManager.getTeamScores().getOrDefault(teamToDisplay.getId(), 0);
        int totalItems = gameManager.getTeamTotalItems().getOrDefault(teamToDisplay.getId(), 0);
        int remainingItems = gameManager.getTeamRequiredItems().getOrDefault(teamToDisplay.getId(), Collections.emptyList()).size();
        int completedItems = totalItems - remainingItems;

        lines.add(Component.text("» 滚动中: ", NamedTextColor.YELLOW)
                .append(Component.text(teamToDisplay.getDisplayName(), teamToDisplay.getColor()))
                .append(Component.text(" "))
                .append(Component.text(score, NamedTextColor.YELLOW))
                .append(Component.text("分 ", NamedTextColor.YELLOW))
                .append(Component.text(" (" + completedItems + "/" + totalItems + ")", NamedTextColor.AQUA)));

        List<Material> taskQueue = gameManager.getCurrentItems(teamToDisplay);
        for (int i = 0; i < taskQueue.size(); i++) {
            Material item = taskQueue.get(i);
            lines.add(Component.text("[" + (i + 1) + "] ", NamedTextColor.DARK_GRAY).append(Component.translatable(item).color(NamedTextColor.GRAY)));
        }
    }
    private void renderEndedView(List<Component> lines) {
        lines.add(Component.empty());
        lines.add(Component.text("游戏已结束！", NamedTextColor.GOLD, TextDecoration.BOLD));
        lines.add(Component.empty());
        lines.add(Component.text("感谢游玩！", NamedTextColor.YELLOW));
    }
}

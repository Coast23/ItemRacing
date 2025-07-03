package top.coast23.ItemRacing.managers;

import net.kyori.adventure.text.format.TextDecoration;
import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;


import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏的核心管理器，负责驱动整个游戏流程、状态和规则。
 */
public class GameManager {

    // --- 游戏状态枚举 ---
    public enum GameState { LOBBY, ACTIVE, ENDED }

    // --- 核心管理器实例 ---
    private final ItemRacing plugin;
    private final TeamManager teamManager;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final LobbyManager lobbyManager;
    private final BossBarManager bossBarManager;
    private final ScoreboardManager scoreboardManager;

    // --- 游戏状态与数据 ---
    private GameState currentState;
    private BukkitTask gameLoopTask;
    private BukkitTask preGameLoopTask;
    private long gameStartTime;

    private final Map<String, List<Material>> teamRequiredItems = new HashMap<>();
    private final Map<String, Integer> teamScores = new HashMap<>();
    private final Map<String, Integer> teamTotalItems = new HashMap<>();
    private final Map<String, Integer> teamLocateUses = new HashMap<>();
    private final Map<UUID, Integer> playerContributions = new HashMap<>();
    private final Map<String, Integer> teamChestContributions = new HashMap<>();
    private final Map<String, List<Inventory>> teamChests = new HashMap<>();
    private final Map<String, Integer> teamRollsUsed = new HashMap<>();
    private final Map<String, Set<UUID>> teamRollRequesters = new HashMap<>();
    private final Map<String, Map<Integer, Location>> teamWaypoints = new HashMap<>();
    private final Map<String, Map<Integer, Material>> teamWaypointIcons = new HashMap<>();
    private final Map<UUID, PermissionAttachment> locatePermissions = new HashMap<>();
    private final GameSettingsManager gameSettings;
    private final Map<String, Integer> teamTprUses = new HashMap<>();
    private final Set<UUID> globalRollRequesters = new HashSet<>();

    // --- 局内游戏设置 ---
    private int itemsToCollect;
    private int chestAmount;
    private int waypointAmount;

    public GameManager(ItemRacing plugin, TeamManager teamManager, ConfigManager configManager, ItemManager itemManager, LobbyManager lobbyManager, BossBarManager bossBarManager, GameSettingsManager gameSettings, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.configManager = configManager;
        this.itemManager = itemManager;
        this.lobbyManager = lobbyManager;
        this.bossBarManager = bossBarManager;
        this.gameSettings = gameSettings;
        this.scoreboardManager = scoreboardManager;
        this.currentState = GameState.LOBBY;
        this.itemsToCollect = gameSettings.getItemsToCollect();
        this.chestAmount = gameSettings.getTeamChestAmount();
        this.waypointAmount = gameSettings.getTeamWaypointAmount();
    }

    public void attemptToStartGame(Player starter) {
        if (this.currentState != GameState.LOBBY) {
            starter.sendMessage(Component.text("游戏已在进行中或已结束，无法重复开始。", NamedTextColor.RED));
            return;
        }

        boolean anyPlayerInTeams = teamManager.getAllTeams().stream().anyMatch(team -> team.getSize() > 0);
        if (!anyPlayerInTeams) {
            starter.sendMessage(Component.text("无法开始游戏：没有任何玩家加入队伍！", NamedTextColor.RED));
            return;
        }

        Set<Player> playersInTeams = teamManager.getAllTeams().stream()
                .flatMap(team -> team.getMembers().stream())
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Player> readyPlayers = lobbyManager.getOnlineReadyPlayers();

        if (!readyPlayers.containsAll(playersInTeams)) {
            List<String> unreadyPlayerNames = playersInTeams.stream()
                    .filter(p -> !readyPlayers.contains(p))
                    .map(Player::getName)
                    .toList();

            starter.sendMessage(Component.text("无法开始游戏：以下玩家未准备！", NamedTextColor.RED));
            starter.sendMessage(Component.text(String.join(", ", unreadyPlayerNames), NamedTextColor.YELLOW));
            return;
        }

        plugin.getLogger().info("游戏即将开始...");
        startGame();
    }

    private void startGame() {
        // 1. 停止游戏大厅循环，更新游戏状态和时间
        if (preGameLoopTask != null) preGameLoopTask.cancel();
        this.currentState = GameState.ACTIVE;
        this.gameStartTime = System.currentTimeMillis();
        lobbyManager.reset();

        // 关闭所有玩家菜单
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
        }

        // 2. 获取本局游戏的核心设置
        int itemsToCollect = gameSettings.getItemsToCollect();

        // 3. 根据游戏模式决定任务列表的分配方式
        if (gameSettings.getGameMode() == GameSettingsManager.GameMode.RACING) {
            // 竞速模式：所有队伍共享一份任务列表
            // 竞速模式已被废弃, 我不会实现它
            List<Material> sharedItems = itemManager.generateRequiredItems(itemsToCollect);
            plugin.getLogger().info("竞速模式已启动，共享任务列表生成。");
            for (Team team : teamManager.getAllTeams()) {
                // 每个队伍都获得这份列表的一个独立副本
                teamRequiredItems.put(team.getId(), new ArrayList<>(sharedItems));
                initializeTeamData(team, sharedItems.size());
            }
        } else {
            // 经典模式：每个队伍独立生成任务列表
            plugin.getLogger().info("经典模式已启动，为每个队伍独立生成任务。");
            for (Team team : teamManager.getAllTeams()) {
                List<Material> items = itemManager.generateRequiredItems(itemsToCollect);
                teamRequiredItems.put(team.getId(), items);
                initializeTeamData(team, items.size());
            }
        }

        // 4. 初始化所有在线玩家的状态
        playerContributions.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Optional<Team> teamOpt = teamManager.getPlayerTeam(player);
            if (teamOpt.isPresent()) {
                initPlayer(player); // 初始化在队伍中的玩家
            } else {
                player.setGameMode(GameMode.SPECTATOR); // 其他人设为观察者
                player.sendMessage(Component.text("游戏开始，你已进入观察者模式。", NamedTextColor.GRAY));
            }
        }

        // 5. 设置世界属性
        World world = Bukkit.getWorlds().get(0);
        world.getWorldBorder().setSize(59999968);
        world.setDifficulty(Difficulty.EASY);
        world.setTime(1000);
        world.getEntitiesByClass(Item.class).forEach(Entity::remove); // 清理掉落物

        // 6. 启动游戏主循环并广播消息
        this.gameLoopTask = new GameLoop().runTaskTimer(plugin, 0L, 5L);
        Bukkit.broadcast(Component.text("ItemRacing 游戏开始！", NamedTextColor.GOLD));
    }

    /**
     * 一个辅助方法，用于初始化单个队伍的游戏数据。
     */
    private void initializeTeamData(Team team, int totalItems) {
        int chestAmount = gameSettings.getTeamChestAmount();
        int initialLocateUses = configManager.getInitialLocateUses();
        int initialTprUses = configManager.getInitialTprUses();

        teamTotalItems.put(team.getId(), totalItems);
        teamScores.put(team.getId(), 0);
        teamLocateUses.put(team.getId(), initialLocateUses);
        teamTprUses.put(team.getId(), initialTprUses);
        teamRollsUsed.put(team.getId(), 0);
        teamRollRequesters.put(team.getId(), new HashSet<>());

        List<Inventory> chests = new ArrayList<>();
        for (int i = 0; i < chestAmount; i++) {
            Component chestTitle = Component.text(team.getDisplayName(), team.getColor())
                    .append(Component.text(" 的箱子 #" + (i + 1), NamedTextColor.GRAY));
            chests.add(Bukkit.createInventory(null, 54, chestTitle));
        }
        teamChests.put(team.getId(), chests);
    }

    public void initPlayer(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(10.0f);
        player.setExp(0);
        player.setLevel(0);
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);

        // 效果
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        executeSafeTeleport(player, true); // 随机传送
        // 随机传送后会分发物品
    //    player.sendMessage(Component.text("你的游戏开始了！", NamedTextColor.GREEN));
    }

    private void applyGameEffects(Player player){
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));

        if (gameSettings.getSpeedLevel() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, gameSettings.getSpeedLevel() - 1, false, false));
        }
        if (gameSettings.getHasteLevel() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, -1, gameSettings.getHasteLevel() - 1, false, false));
        }
        if (gameSettings.getResistLevel() > 0){
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, gameSettings.getResistLevel() - 1, false, false));
        }
        if (gameSettings.getFoodType() == GameSettingsManager.FoodType.SATURATION) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
        }
    }

    private void giveStartingKit(Player player){
        // 工具
        switch (gameSettings.getToolType()) {
            case STONE:
                player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
                player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
                player.getInventory().addItem(new ItemStack(Material.STONE_SHOVEL));
                break;
            case IRON:
                player.getInventory().addItem(new ItemStack(Material.IRON_PICKAXE));
                player.getInventory().addItem(new ItemStack(Material.IRON_AXE));
                player.getInventory().addItem(new ItemStack(Material.IRON_SHOVEL));
                break;
        }

        // 鞘翅
        switch (gameSettings.getElytraType()) {
            case BROKEN:
                ItemStack brokenElytra = new ItemStack(Material.ELYTRA);
                ItemMeta meta = brokenElytra.getItemMeta();
                if (meta instanceof Damageable) {
                    // [修复] 同步使用现代的 setDamage 方法
                    ((Damageable) meta).setDamage(Material.ELYTRA.getMaxDurability() - 1);
                    brokenElytra.setItemMeta(meta);
                }
                player.getInventory().addItem(brokenElytra);
                break;
            case NORMAL:
                player.getInventory().addItem(new ItemStack(Material.ELYTRA));
                break;
            default: // NONE
                break;
        }

        // 附魔书
        if (gameSettings.isGiveMendingBook()) player.getInventory().addItem(createEnchantedBook(Enchantment.MENDING, 1));
        if (gameSettings.isGiveSilktouchBook()) player.getInventory().addItem(createEnchantedBook(Enchantment.SILK_TOUCH, 1));
        if (gameSettings.isGiveFortuneBook()) player.getInventory().addItem(createEnchantedBook(Enchantment.FORTUNE, 3));
        if (gameSettings.isGiveLootingBook()) player.getInventory().addItem(createEnchantedBook(Enchantment.LOOTING, 3));
        // 金胡萝卜
        if (gameSettings.getFoodType() == GameSettingsManager.FoodType.GOLDEN_CARROT) {
            player.getInventory().addItem(new ItemStack(Material.GOLDEN_CARROT, 64));
        }
    }

    private ItemStack createEnchantedBook(Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(enchantment, level, true);
            book.setItemMeta(meta);
        }
        return book;
    }

    public List<Material> getCurrentItems(Team team){
        return teamRequiredItems.get(team.getId()).subList(0, Math.min(configManager.getTaskQueueSize(), teamRequiredItems.get(team.getId()).size()));
    }

    public void handleTaskCompletion(Team team, Material item, Player completer) {
        if (scoreboardManager != null) {
            scoreboardManager.forceUpdateAllScoreboards();
        }

    //    List<Material> requiredItems = teamRequiredItems.get(team.getId());
        List<Material> requiredItems = getCurrentItems(team);
        if (requiredItems == null || !requiredItems.remove(item)) return;

        int currentScore = teamScores.getOrDefault(team.getId(), 0);
        teamScores.put(team.getId(), currentScore + configManager.getScorePerItem());

        // 根据完成者是玩家还是箱子，记录到不同的贡献Map中
        if (completer != null) {
            playerContributions.put(completer.getUniqueId(), playerContributions.getOrDefault(completer.getUniqueId(), 0) + 1);
        } else {
            teamChestContributions.put(team.getId(), teamChestContributions.getOrDefault(team.getId(), 0) + 1);
        }

        Component sourceComponent;
        if (completer != null) {
            sourceComponent = Component.text("(来自", NamedTextColor.WHITE)
                    .append(completer.displayName().color(team.getColor()))
                    .append(Component.text(")", NamedTextColor.WHITE));
        } else {
            sourceComponent = Component.text("(来自", NamedTextColor.WHITE)
                    .append(Component.text(team.getDisplayName() + "箱子", team.getColor()))
                    .append(Component.text(")", NamedTextColor.WHITE));
        }

        Component message = Component.text(team.getDisplayName(), team.getColor())
                .append(Component.text("收集了 ", NamedTextColor.WHITE))
                .append(Component.translatable(item).color(NamedTextColor.AQUA))
                .append(Component.text(" ", NamedTextColor.WHITE))
                .append(sourceComponent);
        Bukkit.broadcast(message);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        if(teamRequiredItems.get(team.getId()).isEmpty()){
            endGame(team);
        }
    }

    public void endGame(Team winningTeam) {
        if (this.currentState != GameState.ACTIVE) return;
        this.currentState = GameState.ENDED;

        if (gameLoopTask != null && !gameLoopTask.isCancelled()) {
            gameLoopTask.cancel();
        }

        // 游戏结束后，将所有玩家设为观察者模式
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setGameMode(GameMode.SPECTATOR);
        }
        if (winningTeam != null) {
            // [修复] 使用现代的 showTitle API
            Component titleComponent = Component.text(winningTeam.getDisplayName(), winningTeam.getColor());
            Component subtitleComponent = Component.text("取得了胜利！", NamedTextColor.GOLD);

            // 定义标题的显示时间：淡入10 ticks, 停留70 ticks, 淡出20 ticks
            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000));
            Title finalTitle = Title.title(titleComponent, subtitleComponent, times);

            for(Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(finalTitle);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        } else {
            Bukkit.broadcast(Component.text("游戏结束！", NamedTextColor.YELLOW));
        }
        showRanking();
        if (scoreboardManager != null) {
            scoreboardManager.forceUpdateAllScoreboards();
        }
        long elapsedSeconds = (System.currentTimeMillis() - getGameStartTime()) / 1000;
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;
        Bukkit.broadcast(Component.text("本局游戏用时: ",  NamedTextColor.GREEN)
                .append(Component.text(String.format("%02d:%02d:%02d", hours, minutes, seconds), NamedTextColor.AQUA))
        );

    }

    private void showRanking() {
        // 使用一个辅助内部类来统一存储排名条目，方便排序
        record RankEntry(Component displayName, int count) {}

        List<RankEntry> entries = new ArrayList<>();

        // 1. 添加玩家贡献
        playerContributions.forEach((uuid, count) -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String name = player.getName() != null ? player.getName() : "离线玩家"; // 提供一个更明确的离线提示

            // 查找玩家所在的队伍以获取颜色
            Optional<Team> teamOpt = teamManager.getAllTeams().stream()
                    .filter(team -> team.getMembers().contains(uuid))
                    .findFirst();

            Component coloredName = teamOpt.isPresent() ?
                    Component.text(name, teamOpt.get().getColor()) :
                    Component.text(name, NamedTextColor.WHITE);

            entries.add(new RankEntry(coloredName, count));
        });

        // 2. 添加团队箱子贡献
        teamChestContributions.forEach((teamId, count) -> {
            teamManager.getTeamById(teamId).ifPresent(team -> {
                Component chestName = Component.text("[", NamedTextColor.GRAY)
                        .append(Component.text(team.getDisplayName(), team.getColor()))
                        .append(Component.text("的箱子]", NamedTextColor.GRAY));
                entries.add(new RankEntry(chestName, count));
            });
        });

        // 3. 按贡献值（完成的物品数）从高到低对所有条目进行排序
        entries.sort(Comparator.comparingInt(RankEntry::count).reversed());

        // 4. 广播最终排名
        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("----- 贡献排名 -----", NamedTextColor.GOLD, TextDecoration.BOLD));
        Bukkit.broadcast(Component.text(""));

        int rank = 1;
        for (RankEntry entry : entries) {
            Component rankLine = Component.text("  " + rank + ". ", NamedTextColor.GRAY)
                    .append(entry.displayName())
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(entry.count(), NamedTextColor.AQUA));
            Bukkit.broadcast(rankLine);
            rank++;
        }
        Bukkit.broadcast(Component.text(""));
    //    Bukkit.broadcast(Component.text("-------------------------", NamedTextColor.GOLD, TextDecoration.BOLD));
    }

    public void resetGame() {
        if (this.currentState == GameState.ACTIVE) {
            endGame(null);
        }

        this.currentState = GameState.LOBBY;
        teamRequiredItems.clear();
        teamScores.clear();
        teamTotalItems.clear();
        teamLocateUses.clear();
        playerContributions.clear();
        teamChests.clear();
        teamWaypoints.clear();
        teamWaypointIcons.clear(); // [新增] 在重置游戏时清空图标缓存
        lobbyManager.reset();
        teamManager.getAllTeams().forEach(bossBarManager::removeRollBar);
        plugin.getLogger().info("游戏已重置，返回大厅准备阶段。");
    }


    public boolean handleRandomTeleportCommand(Player player) {
        Optional<Team> teamOpt = teamManager.getPlayerTeam(player);
        if (teamOpt.isEmpty()) {
            player.sendMessage(Component.text("你不在任何队伍中，无法使用此功能。", NamedTextColor.RED));
            return false;
        }

        Team team = teamOpt.get();
        int freeUses = teamTprUses.getOrDefault(team.getId(), 0);

        if (freeUses > 0) {
            teamTprUses.put(team.getId(), freeUses - 1);

            team.broadcast(Component.text(player.getName(), team.getColor())
                    .append(Component.text("使用了免费随机传送，剩余 " + (freeUses - 1) + " 次。", NamedTextColor.YELLOW))
            );
            executeSafeTeleport(player, false); // 执行传送
            if (scoreboardManager != null) scoreboardManager.forceUpdateAllScoreboards();
            return true;
        }

        int cost = configManager.getRandomTeleportCost();
        int currentScore = teamScores.getOrDefault(team.getId(), 0);

        if (currentScore < cost) {
            player.sendMessage(Component.text("分数不足！随机传送需要 " + cost + " 分。", NamedTextColor.RED));
            return false;
        }

        teamScores.put(team.getId(), currentScore - cost);
        Bukkit.broadcast(Component.text(player.getName(), team.getColor())
                .append(Component.text("进行了随机传送，队伍积分 ", NamedTextColor.YELLOW))
                .append(Component.text( "-" + cost,  NamedTextColor.AQUA))
        );
    //    player.sendMessage(Component.text("消耗 " + cost + " 积分...", NamedTextColor.YELLOW));
        if (scoreboardManager != null) scoreboardManager.forceUpdateAllScoreboards();
        executeSafeTeleport(player, false); // 执行传送
        return true;
    }

    /**
     * [新增] 真正执行安全随机传送的独立方法。
     * @param player 要传送的玩家
     * @param isInitial 是否是开局的第一次传送
     */
    private void executeSafeTeleport(Player player, boolean isInitial) {
        if (!isInitial) {
            player.sendMessage(Component.text("正在为你寻找一个安全的传送点...", NamedTextColor.GRAY));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Random random = new Random();
                World world = Bukkit.getWorlds().get(0);
                if (world == null) { /* ...U should never reach this... */ return; }

                Location safeLocation = null;
                int attempts = 0;
                while (attempts < 128) {
                    double x = world.getSpawnLocation().getX() + random.nextInt(20000) - 10000;
                    double z = world.getSpawnLocation().getZ() + random.nextInt(20000) - 10000;
                    Block highestBlock = world.getHighestBlockAt((int)x, (int)z, HeightMap.MOTION_BLOCKING_NO_LEAVES);

                    if (isSafeSurface(highestBlock.getType())) {
                        safeLocation = highestBlock.getLocation().add(0.5, 1.5, 0.5); // 增加Y坐标以防卡住
                        break;
                    }
                    attempts++;
                }

                final Location finalLocation = safeLocation;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (finalLocation != null) {
                            player.teleportAsync(finalLocation);
                            Component coords = Component.text("(")
                                    .append(Component.text((int) finalLocation.getX(), NamedTextColor.AQUA))
                                    .append(Component.text(", ", NamedTextColor.GRAY))
                                    .append(Component.text((int) finalLocation.getY(), NamedTextColor.AQUA))
                                    .append(Component.text(", ", NamedTextColor.GRAY))
                                    .append(Component.text((int) finalLocation.getZ(), NamedTextColor.AQUA))
                                    .append(Component.text(")"));
                            player.sendMessage(Component.text("已将你随机传送到 ", NamedTextColor.LIGHT_PURPLE).append(coords));
                            if(isInitial) giveStartingKit(player); // 分发初始物品
                        } else {
                            player.sendMessage(Component.text("传送失败，未能找到一个安全的地点。", NamedTextColor.RED));
                            // 如果是非开局传送失败，返还积分
                            if (!isInitial) {
                                Optional<Team> teamOpt = teamManager.getPlayerTeam(player);
                                if (teamOpt.isPresent()) {
                                    int cost = configManager.getRandomTeleportCost();
                                    teamScores.put(teamOpt.get().getId(), teamScores.getOrDefault(teamOpt.get().getId(), 0) + cost);
                                    player.sendMessage(Component.text("已返还 " + cost + " 积分。", NamedTextColor.GREEN));
                                }
                            }
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private boolean isSafeSurface(Material material) {
        return !material.isAir() &&
                !material.equals(Material.WATER) &&
                !material.equals(Material.LAVA) &&
                !material.equals(Material.CACTUS) &&
                !material.equals(Material.MAGMA_BLOCK) &&
                !material.equals(Material.FIRE) &&
                !material.equals(Material.SOUL_FIRE);
    }

    public void requestRoll(Player player) {
        Optional<Team> teamOpt = teamManager.getPlayerTeam(player);
        if (teamOpt.isEmpty()) {
            player.sendMessage(Component.text("你不在任何队伍中，无法发起重摇。", NamedTextColor.RED));
            return;
        }
        if (gameSettings.getGameMode() == GameSettingsManager.GameMode.RACING) {
            handleGlobalRollRequest(player);
        } else {
            handleTeamRollRequest(player);
        }

        Team team = teamOpt.get();
    }

    /**
     * 处理竞速模式下的全局重摇请求。
     * @param player 发起请求的玩家
     */
    private void handleGlobalRollRequest(Player player) {
        // 首先，确保发起请求的玩家是在某个队伍里的，防止旁观者发起投票
        if (teamManager.getPlayerTeam(player).isEmpty()) {
            player.sendMessage(Component.text("只有在队伍中的玩家才能发起全局重摇。", NamedTextColor.RED));
            return;
        }

        // 检查玩家是否已经投过票
        if (globalRollRequesters.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("你已经申请过全局重摇了。", NamedTextColor.YELLOW));
            return;
        }

        // 将玩家加入到全局投票者列表中
        globalRollRequesters.add(player.getUniqueId());

        // 向服务器内的所有玩家广播这条申请信息
        Bukkit.broadcast(Component.text("玩家 " + player.getName() + " 申请全局重摇任务！", NamedTextColor.AQUA));

        // 调用检查方法，更新Boss Bar并判断是否所有人都已同意
        checkGlobalRollStatus();
    }

    private void handleTeamRollRequest(Player player) {
        Optional<Team> teamOpt = teamManager.getPlayerTeam(player);
        if (teamOpt.isEmpty()) { return; }

        Team team = teamOpt.get();
        String teamId = team.getId();
        int maxRolls = configManager.getMaxRollsPerTeam();
        int rollsUsed = teamRollsUsed.getOrDefault(teamId, 0);

        if (rollsUsed >= maxRolls) {
            team.broadcast(Component.text("本队重摇次数已用尽！", NamedTextColor.RED));
            return;
        }

        Set<UUID> requesters = teamRollRequesters.get(teamId);
        if (requesters.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("你已经申请过重摇了。", NamedTextColor.YELLOW));
            return;
        }
        requesters.add(player.getUniqueId());
        checkTeamRollStatus(team);
    }

    /**
     * [新增] 检查一个队伍的重摇状态，如果所有人都同意，则执行重摇。
     * @param team 要检查的队伍
     */
    public void checkTeamRollStatus(Team team) {
        String teamId = team.getId();
        Set<UUID> requesters = teamRollRequesters.get(teamId);
        List<Player> onlineMembers = team.getMembers().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline()).toList();

        if (onlineMembers.isEmpty()) return;

        bossBarManager.updateRollProgress(team, requesters, onlineMembers.size());

        Set<UUID> onlineMemberIds = onlineMembers.stream().map(Player::getUniqueId).collect(Collectors.toSet());
        if (requesters.containsAll(onlineMemberIds)) {
            int rollsUsed = teamRollsUsed.get(teamId) + 1;
            teamRollsUsed.put(teamId, rollsUsed);

            int leftItems = teamRequiredItems.get(teamId).size();
            List<Material> newItems = itemManager.generateRequiredItems(leftItems);
        //    teamRequiredItems.get(teamId).clear();
            teamRequiredItems.put(teamId, new ArrayList<>(newItems));

            requesters.clear();
            bossBarManager.removeRollBar(team);
            Bukkit.broadcast(Component.text(team.getDisplayName(), team.getColor())
                    .append(Component.text("轮换了物品！", NamedTextColor.GOLD))
            );
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    /**
     * [补全] 检查全局重摇的状态。
     */
    public void checkGlobalRollStatus() {
        List<Player> allPlayersInGame = teamManager.getAllTeams().stream()
                .flatMap(team -> team.getMembers().stream())
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .toList();

        if (allPlayersInGame.isEmpty()) {
            bossBarManager.removeAllGlobalBars();
            return;
        }

        bossBarManager.updateGlobalRollProgress(allPlayersInGame, globalRollRequesters);

        Set<UUID> allPlayerIds = allPlayersInGame.stream().map(Player::getUniqueId).collect(Collectors.toSet());
        if (globalRollRequesters.containsAll(allPlayerIds)) {
            int remainingCount = teamRequiredItems.values().stream().findFirst().map(List::size).orElse(0);
            if (remainingCount == 0) return;

            List<Material> newGlobalTasks = itemManager.generateRequiredItems(remainingCount);

            for (Team team : teamManager.getAllTeams()) {
                teamRequiredItems.put(team.getId(), new ArrayList<>(newGlobalTasks));
            //    replenishTaskQueue(team);
            }

            globalRollRequesters.clear();
            bossBarManager.removeAllGlobalBars();
            Bukkit.broadcast(Component.text("所有玩家同意，任务列表已重摇！", NamedTextColor.GOLD));
        }
    }

    /**
     * [恢复] 设置或更新一个队伍的路径点。
     * @param team 要设置路径点的队伍
     * @param index 路径点的索引 (1, 2, 3...)
     * @param location 路径点的位置
     */
    public void setTeamWaypoint(Team team, int index, Location location) {
        teamWaypoints.computeIfAbsent(team.getId(), k -> new HashMap<>()).put(index, location);
    }

    public void removeTeamWaypoint(Team team, int index) {
        teamWaypoints.getOrDefault(team.getId(), new HashMap<>()).remove(index);
    }

    public void setTeamWaypointIcon(Team team, int index, Material icon) {
        teamWaypointIcons.computeIfAbsent(team.getId(), k -> new HashMap<>()).put(index, icon);
    }

    /**
     * [新增] 移除一个路径点的图标缓存。
     */
    public void removeTeamWaypointIcon(Team team, int index) {
        teamWaypointIcons.getOrDefault(team.getId(), new HashMap<>()).remove(index);
    }

    /**
     * [新增] 处理购买和使用定位权限的逻辑。
     */
    public void purchaseLocateCharge(Player player) {
        Optional<Team> teamOpt = teamManager.getPlayerTeam(player);
        if (teamOpt.isEmpty()) {
            player.sendMessage(Component.text("你不在任何队伍中。", NamedTextColor.RED));
            return;
        }
        Team team = teamOpt.get();
        String teamId = team.getId();
        int score = teamScores.getOrDefault(teamId, 0);
        int cost = configManager.getLocateCommandCost();

        if (score < cost) {
            player.sendMessage(Component.text("分数不足！需要 " + cost + " 分来购买定位权限。", NamedTextColor.RED));
            return;
        }

        teamScores.put(teamId, score - cost);
        teamLocateUses.put(teamId, teamLocateUses.getOrDefault(teamId, 0) + 1);

        Bukkit.broadcast(Component.text(player.getName(), team.getColor())
                .append(Component.text("购买了一次定位次数! 积分 ", NamedTextColor.YELLOW))
                .append(Component.text("-" + cost, NamedTextColor.AQUA))
        );

        // team.broadcast(Component.text("队伍花费 " + cost + " 积分为 ", NamedTextColor.YELLOW)
        //        .append(player.displayName().color(NamedTextColor.WHITE))
        //        .append(Component.text(" 购买了一次定位次数！", NamedTextColor.YELLOW)));

        if (scoreboardManager != null) scoreboardManager.forceUpdateAllScoreboards();
        }

    public void useLocate(Player player, String type, String name) {
        Optional<Team> teamOpt = teamManager.getPlayerTeam(player);
        if (teamOpt.isEmpty()) {
            player.sendMessage(Component.text("你不在任何队伍中。", NamedTextColor.RED));
            return;
        }

        Team team = teamOpt.get();
        String teamId = team.getId();
        int usesLeft = teamLocateUses.getOrDefault(teamId, 0);

        if (usesLeft <= 0) {
            player.sendMessage(Component.text("你的队伍没有定位次数了！请先购买。", NamedTextColor.RED));
            return;
        }

        teamLocateUses.put(teamId, usesLeft - 1);
        team.broadcast(Component.text(player.getName(), team.getColor())
                .append(Component.text("消耗了一次定位次数，剩余 " + (usesLeft - 1) + " 次。", NamedTextColor.YELLOW))
        );
        if (scoreboardManager != null) scoreboardManager.forceUpdateAllScoreboards();

        PermissionAttachment attachment = null;
        try {
            attachment = player.addAttachment(ItemRacing.getInstance());
            attachment.setPermission("minecraft.command.locate", true);
            player.performCommand("minecraft:locate " + type + " " + name);
        }finally {
            if (attachment != null) player.removeAttachment(attachment);
        }
    }

    // --- Getters and Setters ---
    public int getItemsToCollect() { return itemsToCollect; }
    public void setItemsToCollect(int itemsToCollect) { this.itemsToCollect = itemsToCollect; }
    public int getTeamChestAmount() { return chestAmount; }
    public int getWaypointAmount() { return waypointAmount;}
    public void setTeamChestAmount(int teamChestAmount) { this.chestAmount = teamChestAmount; }
    public GameState getCurrentState() { return currentState; }
    public TeamManager getTeamManager() { return teamManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public ItemManager getItemManager() { return itemManager; }
    public LobbyManager getLobbyManager() { return lobbyManager; }
    public Map<String, Integer> getTeamScores() { return teamScores; }
    public Map<String, List<Inventory>> getTeamChests() { return teamChests; }
    public Map<String, Integer> getTeamTotalItems() { return teamTotalItems; }
    public Map<String, Integer> getTeamLocateUses() { return teamLocateUses; }
    public Map<String, List<Material>> getTeamRequiredItems() { return teamRequiredItems; }
    public Map<String, Set<UUID>> getTeamRollRequesters() { return teamRollRequesters; }
    public Map<String, Integer> getTeamRollsUsed() { return teamRollsUsed; }
    public Map<String, Map<Integer, Location>> getTeamWaypoints() { return teamWaypoints; }
    public Map<String, Map<Integer, Material>> getTeamWaypointIcons() { return teamWaypointIcons; }
    public GameSettingsManager getGameSettings() { return gameSettings; }
    public Map<String, Integer> getTeamTprUses(){ return teamTprUses;}

    public long getGameStartTime() {
        return gameStartTime;
    }

    // 这两个 Task 其实应该放到 tasks 中, 但我还是在 GameManager 里保留了它们
    private class GameLoop extends BukkitRunnable {
        @Override
        public void run() {
            if (currentState != GameState.ACTIVE) {
                this.cancel();
                return;
            }

            // 阶段一: 为所有游戏内玩家持续应用效果
            for (Team team : teamManager.getAllTeams()) {
                for (UUID memberUUID : team.getMembers()) {
                    Player player = Bukkit.getPlayer(memberUUID);
                    if (player != null && player.isOnline()) {
                        applyGameEffects(player);
                    }
                }
            }

            // 阶段二: 检查所有队伍的任务完成情况
            for (Team team : teamManager.getAllTeams()) {
                // [修复] 我们只检查当前任务队列中的物品，而不是所有未完成的物品。
                // 这样可以防止玩家通过“运气”完成了一个尚未在计分板上显示的任务。
                List<Material> tasksToCheck = new ArrayList<>(getCurrentItems(team));

                for (Material requiredItem : tasksToCheck) {
                    // 检查玩家背包
                    Optional<Player> completerOpt = team.getMembers().stream()
                            .map(Bukkit::getPlayer)
                            .filter(p -> p != null && p.isOnline() && p.getInventory().contains(requiredItem))
                            .findFirst();

                    if (completerOpt.isPresent()) {
                        handleTaskCompletion(team, requiredItem, completerOpt.get());
                        // [重要] 完成一个后，跳出当前队伍的检查，防止一轮检查多个导致逻辑混乱
                        break;
                    }

                    // 如果玩家背包没有，再检查团队箱子
                    List<Inventory> chests = teamChests.get(team.getId());
                    if (chests != null) {
                        boolean foundInChest = false;
                        for (Inventory chest : chests) {
                            if (chest.contains(requiredItem)) {
                                handleTaskCompletion(team, requiredItem, null);
                                foundInChest = true;
                                break; // 找到一个就够了，跳出箱子循环
                            }
                        }
                        if (foundInChest) {
                            break; // 这个物品已在箱子中完成，跳出当前队伍的检查
                        }
                    }
                }
            }
        }
    }
    public void startPreGameLoop() {
        // Init world settings
        Bukkit.getScheduler().runTaskLater(ItemRacing.getInstance(), () -> {
            World world = Bukkit.getWorlds().get(0);
            world.setDifficulty(Difficulty.PEACEFUL);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            Bukkit.getWorlds().get(1).setGameRule(GameRule.KEEP_INVENTORY, true);
            Bukkit.getWorlds().get(2).setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setTime(1000);
        }, 5);

        World world = Bukkit.getWorlds().get(0);
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(32); // 设置小边界

        this.preGameLoopTask = new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Bukkit.getOnlinePlayers()) {
                    if (getCurrentState() != GameState.LOBBY) {
                        this.cancel();
                        return;
                    }
                    player.setGameMode(GameMode.ADVENTURE);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 255, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1, false, false));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}

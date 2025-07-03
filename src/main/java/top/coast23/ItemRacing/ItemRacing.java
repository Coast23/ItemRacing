package top.coast23.ItemRacing;

import top.coast23.ItemRacing.commands.*;
import top.coast23.ItemRacing.listeners.ChatListener;
import top.coast23.ItemRacing.listeners.GameListener;
import top.coast23.ItemRacing.listeners.MenuListener;
import top.coast23.ItemRacing.managers.*;
import top.coast23.ItemRacing.tasks.AntiGlitchTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import java.util.Objects;

public final class ItemRacing extends JavaPlugin {

    // --- 管理器实例 ---
    private static ItemRacing instance;
    private ConfigManager configManager;
    private TeamManager teamManager;
    private ItemManager itemManager;
    private GameSettingsManager gameSettings;
    private LobbyManager lobbyManager;
    private BossBarManager bossBarManager;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;
    private GuiManager guiManager;
    private BroadcastManager broadcastManager;
    private PlayerSettings playerSettings;
    public static NamespacedKey GUI_ITEM_KEY;

    @Override
    public void onEnable() {
        instance = this;
        GUI_ITEM_KEY = new NamespacedKey(this, "gui_item");

        configManager = new ConfigManager(this);
        teamManager = new TeamManager();
        itemManager = new ItemManager(this);
        playerSettings = new PlayerSettings();
        lobbyManager = new LobbyManager();
        bossBarManager = new BossBarManager();

        configManager.loadConfig();

        gameSettings = new GameSettingsManager(configManager);

        scoreboardManager = new ScoreboardManager(this, null, playerSettings);
        gameManager = new GameManager(this, teamManager, configManager, itemManager, lobbyManager, bossBarManager, gameSettings, scoreboardManager);
        scoreboardManager.setGameManager(gameManager); // 回填GameManager实例

        guiManager = new GuiManager(gameManager, teamManager, lobbyManager, gameSettings);

        broadcastManager = new BroadcastManager();

        itemManager.loadSurvivalItemPool();
        configManager.loadTeams(teamManager);


        registerCommands();
        registerListeners();

        gameManager.startPreGameLoop();

        scoreboardManager.startUpdater();
        new AntiGlitchTask().runTaskTimer(this, 0L, 2L);

        getLogger().info("========================================");
        getLogger().info("             ItemRacing v1.0            ");
        getLogger().info("              插件已成功启动！");
        getLogger().info("========================================");
    }

    private void registerCommands() {
        JoinCommand joinCommand = new JoinCommand(teamManager, gameManager);
        Objects.requireNonNull(getCommand("join")).setExecutor(joinCommand);
        Objects.requireNonNull(getCommand("join")).setTabCompleter(joinCommand);

        Objects.requireNonNull(getCommand("leave")).setExecutor(new LeaveCommand(teamManager, gameManager));
        Objects.requireNonNull(getCommand("leave")).setTabCompleter(new LeaveCommand(teamManager, gameManager));

        Objects.requireNonNull(getCommand("start")).setExecutor(new StartCommand(gameManager));
        Objects.requireNonNull(getCommand("start")).setTabCompleter(new StartCommand(gameManager));

        TprCommand tprCommand = new TprCommand(gameManager);
        Objects.requireNonNull(getCommand("tpr")).setExecutor(tprCommand);
        Objects.requireNonNull(getCommand("tpr")).setTabCompleter(tprCommand);

        Objects.requireNonNull(getCommand("chest")).setExecutor(new ChestCommand(guiManager, gameManager));
        Objects.requireNonNull(getCommand("chest")).setTabCompleter(new ChestCommand(guiManager, gameManager));

        Objects.requireNonNull(getCommand("roll")).setExecutor(new RollCommand(gameManager));
        Objects.requireNonNull(getCommand("roll")).setTabCompleter(new RollCommand(gameManager));

        Objects.requireNonNull(getCommand("wp")).setExecutor(new WaypointCommand(gameManager, guiManager));
        Objects.requireNonNull(getCommand("wp")).setTabCompleter(new WaypointCommand(gameManager, guiManager));

        Objects.requireNonNull(getCommand("locate")).setExecutor(new LocateCommand(gameManager));

        TpCommand tpCommand = new TpCommand(teamManager, gameManager);
        Objects.requireNonNull(getCommand("tp")).setExecutor(tpCommand);
        Objects.requireNonNull(getCommand("tp")).setTabCompleter(tpCommand);

        PcCommand pcCommand = new PcCommand(teamManager, gameManager);
        Objects.requireNonNull(getCommand("pc")).setExecutor(pcCommand);
        Objects.requireNonNull(getCommand("pc")).setTabCompleter(pcCommand);

        ScoreboardCommand scoreboardCommandHandler = new ScoreboardCommand(playerSettings, teamManager);
        PluginCommand sbCommand = Objects.requireNonNull(getCommand("scoreboard"));
        sbCommand.setExecutor(scoreboardCommandHandler);
        sbCommand.setTabCompleter(scoreboardCommandHandler);

        HelpCommand helpCommand = new HelpCommand(gameManager);
        Objects.requireNonNull(getCommand("help")).setExecutor(helpCommand);
        Objects.requireNonNull(getCommand("help")).setTabCompleter(helpCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GameListener(gameManager, scoreboardManager, guiManager, bossBarManager), this);
        getServer().getPluginManager().registerEvents(new MenuListener(gameManager, teamManager, lobbyManager, guiManager, gameSettings, scoreboardManager, broadcastManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(gameManager, gameSettings, scoreboardManager, broadcastManager), this);
    }

    @Override
    public void onDisable() {
        // 停止计分板的周期性更新任务
        if (scoreboardManager != null) {
            scoreboardManager.stopUpdater();
        }

        // 停止所有由Bukkit调度器安排的任务，确保插件干净地关闭
        getServer().getScheduler().cancelTasks(this);

        getLogger().info("ItemRacing 插件已卸载。");
    }

    // --- Getters ---
    public static ItemRacing getInstance() { return instance; }

    public ConfigManager getConfigManager() { return configManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public ItemManager getItemManager() { return itemManager; }
    public GameSettingsManager getGameSettings() { return gameSettings; }
    public LobbyManager getLobbyManager() { return lobbyManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public GameManager getGameManager() { return gameManager; }
    public PlayerSettings getPlayerSettings() { return playerSettings; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public GuiManager getGuiManager() { return guiManager; }
}

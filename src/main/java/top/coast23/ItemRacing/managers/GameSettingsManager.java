package top.coast23.ItemRacing.managers;

import top.coast23.ItemRacing.ItemRacing;

/**
 * 负责管理一局游戏的具体设置。
 * 这些设置在游戏大厅阶段可以被玩家修改。
 */
public class GameSettingsManager {

    private final ConfigManager configManager;

    public enum GameMode {
        CLASSIC,
        RACING
    }

    public enum ToolType{
        NONE,
        STONE,
        IRON
    }

    public enum ElytraType{
        NONE,
        BROKEN,
        NORMAL
    }

    public enum FoodType{
        NONE,
        GOLDEN_CARROT,
        SATURATION
    }

    // --- 可变的游戏设置 ---
    private int itemsToCollect;
    private int teamChestAmount;
    private int teamWaypointAmount;
    private int maxRollsPerTeam;

    private int speedLevel;
    private int hasteLevel;
    private int resistLevel;

    private GameMode gameMode;
    private ToolType toolType;
    private ElytraType elytraType;
    private FoodType foodType;

    private boolean giveMendingBook;
    private boolean giveSilktouchBook;
    private boolean giveFortuneBook;
    private boolean giveLootingBook;


    public GameSettingsManager(ConfigManager configManager) {
        this.configManager = configManager;
        // 使用配置文件中的值作为初始默认值
        resetToDefaults();
    }

    /**
     * 将所有设置重置为 config.yml 中定义的默认值。
     */
    public void resetToDefaults() {
        this.itemsToCollect = configManager.getTotalItemsToCollect();
        this.teamChestAmount = configManager.getTeamChestAmount();
        this.teamWaypointAmount = configManager.getTeamWaypointAmount();
        this.maxRollsPerTeam = configManager.getMaxRollsPerTeam();

        this.speedLevel = 2;
        this.hasteLevel = 2;
        this.resistLevel = 2;
        this.gameMode = GameMode.CLASSIC;
        this.elytraType = ElytraType.NORMAL;
        this.toolType = ToolType.IRON;
        this.foodType = FoodType.GOLDEN_CARROT;

        this.giveMendingBook = true;
        this.giveFortuneBook = false;
        this.giveSilktouchBook = true;
        this.giveLootingBook = false;
    }

    public int getItemsToCollect() {
        return itemsToCollect;
    }

    public void setItemsToCollect(int itemsToCollect) {
        int totalItems = ItemRacing.getInstance().getItemManager().getTotalItemAmount();
        this.itemsToCollect = Math.min(Math.max(1, itemsToCollect), totalItems);
    }

    public GameMode getGameMode() { return gameMode; }
    public void toggleGameMode() {
        this.gameMode = (this.gameMode == GameMode.CLASSIC) ? GameMode.RACING : GameMode.CLASSIC;
    }

    public int getSpeedLevel() { return speedLevel; }
    public void setSpeedLevel(int level) { this.speedLevel = Math.min(Math.max(0, level), 5); } // 速度等级为 0~5

    public int getHasteLevel() { return hasteLevel; }
    public void setHasteLevel(int level) { this.hasteLevel = Math.min(Math.max(0, level), 5); } // 急迫等级为 0~5
    public int getResistLevel() { return resistLevel; }
    public void setResistLevel(int level) {this.resistLevel = Math.min(Math.max(0, level), 5); }

    public ToolType getToolType() { return toolType; }
    public void cycleToolType() {
        ToolType[] types = ToolType.values();
        this.toolType = types[(this.toolType.ordinal() + 1) % types.length];
    }

    public ElytraType getElytraType() { return elytraType; }
    public void cycleElytraType() {
        ElytraType[] types = ElytraType.values();
        this.elytraType = types[(this.elytraType.ordinal() + 1) % types.length];
    }

    public FoodType getFoodType() { return foodType; }
    public void cycleFoodType() {
        FoodType[] types = FoodType.values();
        this.foodType = types[(this.foodType.ordinal() + 1) % types.length];
    }

    public int getTeamChestAmount() {
        return teamChestAmount;
    }
    public int getMaxRollsPerTeam() { return maxRollsPerTeam; }
    public int getTeamWaypointAmount(){
        return teamWaypointAmount;
    }

    public boolean isGiveMendingBook() { return giveMendingBook; }
    public void toggleGiveMendingBook() { this.giveMendingBook = !this.giveMendingBook; }

    public boolean isGiveSilktouchBook() { return giveSilktouchBook; }
    public void toggleGiveSilktouchBook() { this.giveSilktouchBook = !this.giveSilktouchBook; }

    public boolean isGiveFortuneBook() { return giveFortuneBook; }
    public void toggleGiveFortuneBook() { this.giveFortuneBook = !this.giveFortuneBook; }

    public boolean isGiveLootingBook() { return giveLootingBook; }
    public void toggleGiveLootingBook() { this.giveLootingBook = !this.giveLootingBook; }
}

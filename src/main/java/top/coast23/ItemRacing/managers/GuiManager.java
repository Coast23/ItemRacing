package top.coast23.ItemRacing.managers;
import org.bukkit.Sound;
import top.coast23.ItemRacing.guis.*;
import org.bukkit.entity.Player;

/**
 * 负责管理和打开所有GUI菜单。
 */
public class GuiManager {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final LobbyManager lobbyManager;
    private final GameSettingsManager gameSettings;

    public GuiManager(GameManager gameManager, TeamManager teamManager, LobbyManager lobbyManager, GameSettingsManager gameSettings) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.lobbyManager = lobbyManager;
        this.gameSettings = gameSettings;
    }

    /**
     * 为玩家打开合适的菜单。
     */
    public void openMenu(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        if (gameManager.getCurrentState() == GameManager.GameState.LOBBY) {
            new LobbyMenu(this, gameManager, teamManager, lobbyManager).open(player);
        } else if (gameManager.getCurrentState() == GameManager.GameState.ACTIVE) {
            new GameMenu(gameManager).open(player);
        }
    }

    /**
     * [新增] 一个专门负责打开团队箱子选择菜单的方法。
     * @param player 要为其打开菜单的玩家。
     */
    public void openTeamChestSelectMenu(Player player) {
        new TeamChestSelectMenu(gameManager).open(player);
    }
    public void openGameSettingsMenu(Player player){new GameSettingsMenu(gameSettings).open(player);}
    public void openWaypointMenu(Player player) {
        new WaypointMenu(gameManager).open(player);
    }
}

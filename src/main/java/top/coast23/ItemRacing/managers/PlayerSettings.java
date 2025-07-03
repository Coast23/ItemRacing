package top.coast23.ItemRacing.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 负责管理所有玩家的个人设置，例如他们偏好的计分板样式。
 */
public class PlayerSettings {

    /**
     * 定义所有可用的计分板显示样式。
     */
    public enum ScoreboardStyle {
        /** 显示所有队伍的实时排名。 */
        RANKING,
        /** 持续关注一个特定队伍的详细任务列表。 */
        WATCH_TEAM,
        /** 循环展示所有其他队伍的任务列表。 */
        ROTATING
    }

    /**
     * 一个内部记录类，用于封装单个玩家的所有显示设置。
     * @param style           玩家选择的样式。
     * @param watchedTeamId   要监视的队伍ID (仅在WATCH_TEAM模式下有效)。
     * @param rotationSeconds 滚动显示的间隔秒数 (仅在ROTATING模式下有效)。
     */
    public record PlayerDisplaySettings(
            ScoreboardStyle style,
            String watchedTeamId,
            int rotationSeconds
    ) {}

    // 使用 Map 来存储每个玩家（通过UUID）的个人设置。
    private final Map<UUID, PlayerDisplaySettings> playerSettingsMap = new HashMap<>();

    /**
     * 获取指定玩家的显示设置。
     * 如果玩家没有设置过，会返回一个默认设置（排行榜模式）。
     * @param player 要查询的玩家。
     * @return 玩家的显示设置。
     */
    public PlayerDisplaySettings getSettingsFor(Player player) {
        // getOrDefault 确保即使玩家是第一次查询，我们也能返回一个安全的默认值。
        return playerSettingsMap.getOrDefault(player.getUniqueId(),
                new PlayerDisplaySettings(ScoreboardStyle.ROTATING, null, 5));
    }

    /**
     * 为玩家设置计分板为“排行榜”模式。
     * @param player 要设置的玩家。
     */
    public void setRankingStyle(Player player) {
        // 创建一个新的设置对象，只保留样式信息。
        PlayerDisplaySettings newSettings = new PlayerDisplaySettings(ScoreboardStyle.RANKING, null, 5);
        playerSettingsMap.put(player.getUniqueId(), newSettings);
    }

    /**
     * 为玩家设置计分板为“监视队伍”模式。
     * @param player       要设置的玩家。
     * @param targetTeamId 要监视的队伍ID。
     */
    public void setWatchTeamStyle(Player player, String targetTeamId) {
        PlayerDisplaySettings newSettings = new PlayerDisplaySettings(ScoreboardStyle.WATCH_TEAM, targetTeamId, 5);
        playerSettingsMap.put(player.getUniqueId(), newSettings);
    }

    /**
     * 为玩家设置计分板为“滚动显示”模式。
     * @param player  要设置的玩家。
     * @param seconds 滚动的间隔秒数。
     */
    public void setRotatingStyle(Player player, int seconds) {
        int validSeconds = Math.max(1, seconds); // 确保间隔时间至少为1秒
        PlayerDisplaySettings newSettings = new PlayerDisplaySettings(ScoreboardStyle.ROTATING, null, validSeconds);
        playerSettingsMap.put(player.getUniqueId(), newSettings);
    }

    /**
     * 当玩家退出服务器时，清除他们的设置以释放内存。
     * @param player 退出的玩家。
     */
    public void clearSettingsFor(Player player) {
        playerSettingsMap.remove(player.getUniqueId());
    }
}

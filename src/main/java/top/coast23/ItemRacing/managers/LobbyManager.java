package top.coast23.ItemRacing.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 专门负责管理游戏大厅（准备阶段）的逻辑，特别是玩家的准备状态。
 */
public class LobbyManager {

    // 使用一个Set来存储所有已准备玩家的UUID，以实现快速的增、删、查。
    private final Set<UUID> readyPlayers = new HashSet<>();

    /**
     * 切换一个玩家的准备状态。
     * 如果玩家已准备，则将其设为未准备，反之亦然。
     * @param player 要切换状态的玩家。
     */
    public void togglePlayerReady(Player player) {
        if (isPlayerReady(player)) {
            readyPlayers.remove(player.getUniqueId());
        } else {
            readyPlayers.add(player.getUniqueId());
        }
    }

    /**
     * 检查一个玩家是否已准备。
     * @param player 要检查的玩家。
     * @return 如果玩家已准备，则返回 true。
     */
    public boolean isPlayerReady(Player player) {
        return readyPlayers.contains(player.getUniqueId());
    }

    /**
     * 获取当前已准备的玩家数量。
     * @return 已准备的玩家数。
     */
    public int getReadyPlayerCount() {
        return readyPlayers.size();
    }

    /**
     * 获取所有在线的、已准备的玩家。
     * @return 一个包含在线且已准备玩家的Set。
     */
    public Set<Player> getOnlineReadyPlayers() {
        return readyPlayers.stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .collect(Collectors.toSet());
    }

    /**
     * 在游戏开始或重置时，清空所有玩家的准备状态。
     */
    public void reset() {
        readyPlayers.clear();
    }
}

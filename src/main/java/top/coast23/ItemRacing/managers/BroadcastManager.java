package top.coast23.ItemRacing.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 负责发送所有格式化的全局游戏广播。
 * AI 后来加的, 前面写的广播我没有整合到这个类里, 懒.
 */
public class BroadcastManager {

    /**
     * 广播玩家准备状态的变更。
     * @param player 状态变更的玩家
     * @param isReady 玩家是否已准备
     */
    public void playerReadyStatusChanged(Player player, boolean isReady) {
        Component message = Component.text(player.getName(), NamedTextColor.AQUA)
                .append(isReady ?
                        Component.text(" 已准备！", NamedTextColor.GREEN) :
                        Component.text(" 已取消准备。", NamedTextColor.RED));
        Bukkit.broadcast(message);


    }

    /**
     * 广播游戏设置的变更。
     * @param player      执行操作的玩家
     * @param settingName 被修改的设置项名称
     * @param newValue    设置项的新值
     */
    public void settingChanged(Player player, String settingName, String newValue) {
        Component message = Component.text("[", NamedTextColor.GRAY)
                .append(player.displayName().color(NamedTextColor.WHITE))
                .append(Component.text("] 将 ", NamedTextColor.GRAY))
                .append(Component.text(settingName, NamedTextColor.AQUA))
                .append(Component.text(" 修改为 ", NamedTextColor.GRAY))
                .append(Component.text(newValue, NamedTextColor.YELLOW));
        Bukkit.broadcast(message);
    }
}

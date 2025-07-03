package top.coast23.ItemRacing.managers;

import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 负责管理所有与Boss Bar相关的显示逻辑，其实只有重摇投票进度。
 */
public class BossBarManager {

    private final Map<String, BossBar> teamRollBars = new HashMap<>();
    private BossBar globalRollBar;

    /**
     * 为一个队伍更新或创建重摇投票进度条。
     */
    public void updateRollProgress(Team team, Set<UUID> requesters, int onlineMembers) {
        if (onlineMembers == 0) {
            removeRollBar(team);
            return;
        }

        String teamId = team.getId();
        BossBar bossBar = teamRollBars.get(teamId);

        if (bossBar == null) {
            Component title = Component.text("重摇投票中: " + requesters.size() + "/" + onlineMembers, NamedTextColor.AQUA);
            BarColor barColor = getBarColor(team.getColor());
            bossBar = Bukkit.createBossBar(title.toString(), barColor, BarStyle.SOLID);
            teamRollBars.put(teamId, bossBar);
        }

        bossBar.setTitle("重摇投票中: " + requesters.size() + "/" + onlineMembers);
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) requesters.size() / onlineMembers)));

        for (UUID memberId : team.getMembers()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null && p.isOnline()) {
                bossBar.addPlayer(p);
            }
        }
    }

    /**
     * 移除一个队伍的重摇 Boss Bar。
     */
    public void removeRollBar(Team team) {
        BossBar bossBar = teamRollBars.remove(team.getId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    /**
     * [补全] 为所有指定玩家更新或创建全局重摇进度条。
     */
    public void updateGlobalRollProgress(List<Player> playersToShow, Set<UUID> requesters) {
        if (playersToShow.isEmpty()) {
            removeAllGlobalBars();
            return;
        }

        if (globalRollBar == null) {
            globalRollBar = Bukkit.createBossBar("全局重摇投票", BarColor.WHITE, BarStyle.SOLID);
        }

        globalRollBar.setTitle("全局重摇投票: " + requesters.size() + "/" + playersToShow.size());
        globalRollBar.setProgress(Math.max(0.0, Math.min(1.0, (double) requesters.size() / playersToShow.size())));

        globalRollBar.removeAll();
        playersToShow.forEach(globalRollBar::addPlayer);
    }

    /**
     * [补全] 移除全局重摇 Boss Bar。
     */
    public void removeAllGlobalBars() {
        if (globalRollBar != null) {
            globalRollBar.removeAll();
            globalRollBar = null;
        }
    }

    public void showBarToPlayer(Player player, Team team) {
        BossBar bossBar = teamRollBars.get(team.getId());
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    private BarColor getBarColor(net.kyori.adventure.text.format.TextColor textColor) {
        String colorName = NamedTextColor.nearestTo(textColor).toString().toUpperCase();
        return switch (colorName) {
            case "RED", "DARK_RED" -> BarColor.RED;
            case "BLUE", "DARK_BLUE", "AQUA", "DARK_AQUA" -> BarColor.BLUE;
            case "GREEN", "DARK_GREEN" -> BarColor.GREEN;
            case "YELLOW", "GOLD" -> BarColor.YELLOW;
            case "LIGHT_PURPLE", "DARK_PURPLE" -> BarColor.PURPLE;
            case "WHITE" -> BarColor.WHITE;
            default -> BarColor.PINK;
        };
    }
}

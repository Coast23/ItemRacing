package top.coast23.ItemRacing.commands;

import org.jetbrains.annotations.Nullable;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.TeamManager;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * 负责处理 /pc 队内聊天指令。
 */
public class PcCommand implements CommandExecutor, TabCompleter {

    private final TeamManager teamManager;
    private final GameManager gameManager;

    public PcCommand(TeamManager teamManager, GameManager gameManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("这个指令只能由玩家执行。");
            return true;
        }
        if(gameManager.getCurrentState() != GameManager.GameState.LOBBY && gameManager.getCurrentState() != GameManager.GameState.ACTIVE) {
            player.sendMessage(Component.text("无法在当前阶段使用该命令！", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("用法: /pc <消息>", NamedTextColor.RED));
            return true;
        }

        Optional<Team> teamOpt = teamManager.getPlayerTeam(player);
        if (teamOpt.isEmpty()) {
            player.sendMessage(Component.text("你不在任何队伍中，无法使用队伍聊天。", NamedTextColor.RED));
            return true;
        }

        Team team = teamOpt.get();
        String messageContent = String.join(" ", args);

        // 构建聊天消息
        Component teamMessage = Component.text("[队伍] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(player.displayName().color(team.getColor()))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(messageContent, NamedTextColor.WHITE));

        // 向队伍广播
        team.broadcast(teamMessage);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 禁用此命令的Tab补全
        return List.of();
    }
}
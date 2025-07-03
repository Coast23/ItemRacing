package top.coast23.ItemRacing.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.Nullable;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.TeamManager;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class LeaveCommand implements CommandExecutor, TabCompleter {
    private final TeamManager teamManager;
    private final GameManager gameManager;

    public LeaveCommand(TeamManager teamManager, GameManager gameManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("这个指令只能由玩家执行。");
            return true;
        }
        if(gameManager.getCurrentState() != GameManager.GameState.LOBBY) {
            player.sendMessage(Component.text("无法在当前阶段使用该命令！", NamedTextColor.RED));
            return true;
        }
        Optional<Team> currentTeam = teamManager.getPlayerTeam(player);
        if (currentTeam.isPresent()) {
            Team team = currentTeam.get();
            teamManager.removePlayerFromCurrentTeam(player);
            player.playerListName(Component.text(player.getName()));
        //    player.sendMessage(Component.text("你已成功离开 " + currentTeam.get().getDisplayName() + "。", NamedTextColor.GREEN));
            Bukkit.broadcast(Component.text(player.getName(), NamedTextColor.AQUA)
                    .append(Component.text(" 离开了 ", NamedTextColor.RED))
                    .append(Component.text(team.getDisplayName(), team.getColor()))
                    .append(Component.text(" !", NamedTextColor.RED))
            );
            if(gameManager.getLobbyManager().isPlayerReady(player)) {
                gameManager.getLobbyManager().togglePlayerReady(player);
            }

        } else {
            player.sendMessage(Component.text("你不在任何队伍中。", NamedTextColor.YELLOW));
        }
        return true;
    }
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 禁用此命令的Tab补全
        return List.of();
    }
}
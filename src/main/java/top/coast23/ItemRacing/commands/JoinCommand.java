package top.coast23.ItemRacing.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.TeamManager;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JoinCommand implements CommandExecutor, TabCompleter {
    private final TeamManager teamManager;
    private final GameManager gameManager;

    public JoinCommand(TeamManager teamManager, GameManager gameManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("这个指令只能由玩家执行。");
            return true;
        }

        Optional<Team> playerTeam = gameManager.getTeamManager().getPlayerTeam(player);

        if (gameManager.getCurrentState() != GameManager.GameState.LOBBY && playerTeam.isPresent()) {
            player.sendMessage(Component.text("不允许在游戏中更换队伍！", NamedTextColor.RED));
            return true;
        }
        if(gameManager.getCurrentState() != GameManager.GameState.LOBBY && gameManager.getCurrentState() != GameManager.GameState.ACTIVE) {
            player.sendMessage(Component.text("无法在当前阶段使用该命令！", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Component.text("用法: /join <队伍ID>", NamedTextColor.RED));
            return true;
        }
        String teamId = args[0].toLowerCase();
        if (teamManager.addPlayerToTeam(player, teamId)) {
            Team joinedTeam = teamManager.getTeamById(teamId).get();
            player.playerListName(Component.text(player.getName(), joinedTeam.getColor()));
        //    player.sendMessage(Component.text("成功加入 " + joinedTeam.getDisplayName() + " !", joinedTeam.getColor()));

            Bukkit.broadcast(Component.text(player.getName(), NamedTextColor.AQUA)
                    .append(Component.text(" 加入了 ", NamedTextColor.GREEN))
                    .append(Component.text(joinedTeam.getDisplayName(), joinedTeam.getColor()))
                    .append(Component.text(" !", NamedTextColor.GREEN))
            );

            if(player.getGameMode() == GameMode.SPECTATOR) {
                gameManager.initPlayer(player);
            }
        } else {
            player.sendMessage(Component.text("加入队伍失败！队伍ID不存在。", NamedTextColor.RED));
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return teamManager.getAllTeams().stream()
                    .map(Team::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of(); // 其他情况不提供补全
    }
}
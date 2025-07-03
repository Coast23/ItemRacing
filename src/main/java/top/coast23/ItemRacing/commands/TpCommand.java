package top.coast23.ItemRacing.commands;

import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.TeamManager;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

public class TpCommand implements CommandExecutor, TabCompleter {
    private final TeamManager teamManager;
    private final GameManager gameManager;

    public TpCommand(TeamManager teamManager, GameManager gameManager) {
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("这个指令只能由玩家执行。");
            return true;
        }
        if (gameManager.getCurrentState() != GameManager.GameState.ACTIVE) {
            player.sendMessage(Component.text("无法在当前阶段使用该命令！", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Component.text("用法: /tp <队友名>", NamedTextColor.RED));
            return true;
        }

        Optional<Team> teamOpt = teamManager.getPlayerTeam(player);
        if (teamOpt.isEmpty()) {
            player.sendMessage(Component.text("你不在任何队伍中。", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("玩家 " + args[0] + " 不在线。", NamedTextColor.RED));
            return true;
        }

        if (!teamOpt.get().isMember(target)) {
            player.sendMessage(Component.text(target.getName() + " 不是你的队友！", NamedTextColor.RED));
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage(Component.text("你不能传送给自己。", NamedTextColor.YELLOW));
            return true;
        }

        player.teleport(target);
        player.sendMessage(Component.text("已将你传送至 " + target.getName() + " 的位置。", NamedTextColor.GREEN));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return teamManager.getPlayerTeam(player)
                    .map(team -> team.getMembers().stream()
                            .map(Bukkit::getPlayer)
                            .filter(p -> p != null && !p.equals(player))
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList()))
                    .orElse(List.of());
        }
        return List.of();
    }
}

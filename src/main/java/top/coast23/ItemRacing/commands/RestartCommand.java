package top.coast23.ItemRacing.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.Nullable;
import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RestartCommand implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;
    public RestartCommand(GameManager gameManager) { this.gameManager = gameManager; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            // 终端可直接重启
            scheduleRestart();
            return true;
        }
        if (gameManager.getCurrentState() != GameManager.GameState.ENDED) {
            player.sendMessage(Component.text("无法在当前阶段使用该命令！", NamedTextColor.RED));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            Bukkit.broadcast(Component.text("------------------------------------------", NamedTextColor.GOLD));
            Bukkit.broadcast(player.displayName()
                    .append(Component.text(" 执行了 restart 命令! 服务器即将重启!", NamedTextColor.RED))
            );
            Bukkit.broadcast(Component.text("------------------------------------------", NamedTextColor.GOLD));
            scheduleRestart();
            return true;
        }
        else{
            player.sendMessage(Component.text("警告: 此命令将重启服务器! 请输入 ", NamedTextColor.RED)
                    .append(Component.text("/itemracing:restart confirm", NamedTextColor.YELLOW))
                    .append(Component.text(" 确认执行该操作!", NamedTextColor.RED))
            );
            return true;
        }

    }

    private void scheduleRestart() {
        // 3 秒后重启
        Bukkit.getScheduler().runTaskLater(ItemRacing.getInstance(), () -> {
            Bukkit.getServer().restart();
        }, 20L * 5);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 禁用此命令的Tab补全
        return List.of();
    }
}
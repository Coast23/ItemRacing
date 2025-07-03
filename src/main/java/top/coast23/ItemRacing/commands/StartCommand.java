package top.coast23.ItemRacing.commands;

import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.Nullable;
import top.coast23.ItemRacing.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StartCommand implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;
    public StartCommand(GameManager gameManager) { this.gameManager = gameManager; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("这个指令只能由玩家执行。");
            return true;
        }
        if (gameManager.getCurrentState() != GameManager.GameState.LOBBY) {
            player.sendMessage(Component.text("无法在当前阶段使用该命令！", NamedTextColor.RED));
            return true;
        }
        gameManager.attemptToStartGame(player);
        return true;
    }
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 禁用此命令的Tab补全
        return List.of();
    }
}
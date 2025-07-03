package top.coast23.ItemRacing.commands;

import org.jetbrains.annotations.Nullable;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.GuiManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WaypointCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;
    private final GuiManager guiManager;

    public WaypointCommand(GameManager gameManager, GuiManager guiManager) {
        this.gameManager = gameManager;
        this.guiManager = guiManager;
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

        // 无论玩家输入什么参数，都直接打开GUI菜单，让玩家在菜单中进行操作。
        guiManager.openWaypointMenu(player);
        return true;
    }
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 禁用此命令的Tab补全
        return List.of();
    }
}

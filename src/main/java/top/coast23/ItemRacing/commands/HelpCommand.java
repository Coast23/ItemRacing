package top.coast23.ItemRacing.commands;

import top.coast23.ItemRacing.managers.GameManager;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HelpCommand implements CommandExecutor, TabCompleter {

    private GameManager gameManager;

    public HelpCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("这个指令只能由玩家执行。");
            return true;
        }
        player.sendMessage(Component.text("----- ItemRacing 命令列表 -----", NamedTextColor.BLUE, TextDecoration.BOLD));

        if (gameManager.getCurrentState() == GameManager.GameState.LOBBY){
            player.sendMessage(createHelpLine("/join <队伍名>", "加入队伍"));
            player.sendMessage(createHelpLine("/leave", "离开队伍"));
            player.sendMessage(createHelpLine("/start", "开始游戏"));
        }

        else if(gameManager.getCurrentState() == GameManager.GameState.ACTIVE) {
            player.sendMessage(createHelpLine("/locate", "定位群系或结构"));
            player.sendMessage(createHelpLine("/chest <num>", "打开第 num 个队伍箱子"));
            player.sendMessage(createHelpLine("/box <num>", "同 /chest"));
            player.sendMessage(createHelpLine("/wp", "打开路径点菜单"));
            player.sendMessage(createHelpLine("/tpr", "随机传送"));
            player.sendMessage(createHelpLine("/tp <player>", "传送到队友"));
            player.sendMessage(createHelpLine("/pc <msg>", "队伍聊天"));
            player.sendMessage(createHelpLine("/teammsg <msg>", "同 /pc"));
            player.sendMessage(createHelpLine("/wp", "打开路径点菜单"));
            player.sendMessage(createHelpLine("/roll", "申请重摇任务"));
            player.sendMessage(createHelpLine("/sb <ranking | watch | rotate>", "切换计分板样式"));
        }
        else if(gameManager.getCurrentState() == GameManager.GameState.ENDED){
            player.sendMessage(createHelpLine("/itemracing:restart", "重启服务器"));
        }
        return true;
    }


    /**
     * 一个辅助方法，用于创建格式化的帮助行
     * @param cmd 命令示例
     * @param desc 命令描述
     * @return 格式化后的Component
     */
    private Component createHelpLine(String cmd, String desc) {
        return Component.text("» ", NamedTextColor.DARK_GRAY)
                .append(Component.text(cmd, NamedTextColor.GOLD))
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text(desc, NamedTextColor.WHITE));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}

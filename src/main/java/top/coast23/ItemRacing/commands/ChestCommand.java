package top.coast23.ItemRacing.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.GuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.coast23.ItemRacing.managers.TeamManager;

import java.util.List;

public class ChestCommand implements CommandExecutor, TabCompleter {
    private final GuiManager guiManager;
    private final GameManager gameManager;
    public ChestCommand(GuiManager guiManager, GameManager gameManager) {
        this.guiManager = guiManager;
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
        if(args.length == 0) {
            guiManager.openTeamChestSelectMenu(player);
        }
        else if(args.length == 1) {
            try{
                int chestIndex = Integer.parseInt(args[0]) - 1;
                TeamManager teamManager = gameManager.getTeamManager();
                teamManager.getPlayerTeam(player).ifPresent(team -> {
                    List<Inventory> chests = gameManager.getTeamChests().get(team.getId());
                    if (chests != null && chestIndex >= 0 && chestIndex < chests.size()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.0f);
                        player.openInventory(chests.get(chestIndex));
                    }
                    else{
                        player.sendMessage(Component.text("箱子序号范围必须是 1~" + chests.size() + " !", NamedTextColor.RED));
                    }
                });

            }catch(Exception e){
                player.sendMessage(Component.text("用法: /chest <箱子序号>", NamedTextColor.RED));
            }
            return true;
        }
        else player.sendMessage(Component.text("用法: /chest <箱子序号>", NamedTextColor.RED));
        return true;
    }
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 禁用此命令的Tab补全
        return List.of();
    }
}
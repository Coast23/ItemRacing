package top.coast23.ItemRacing.commands;

import top.coast23.ItemRacing.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocateCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;

    public LocateCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("这个指令只能由玩家执行。");
            return true;
        }
        if(gameManager.getCurrentState() != GameManager.GameState.ACTIVE) {
            player.sendMessage(Component.text("无法在当前阶段使用该命令！", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /locate <biome | structure> <名称>", NamedTextColor.RED));
            return true;
        }
        String type = args[0].toLowerCase();
        if (!type.equals("biome") && !type.equals("structure")) {
            player.sendMessage(Component.text("只能定位 'biome' 或 'structure'。", NamedTextColor.RED));
            return true;
        }

        // 将参数数组拼接成一个字符串，以支持带空格的名称
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        gameManager.useLocate(player, type, name);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("biome", "structure")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            String type = args[0].toLowerCase();
            String currentInput = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
            /*
            if (type.equals("biome")) {
                // [修复] 使用 Registry.BIOME.getKey(biome).asString() 来替代已弃用的方法
                return Registry.BIOME.stream()
                        .map(biome -> Registry.BIOME.getKey(biome).asString())
                        .filter(key -> key.startsWith(currentInput))
                        .collect(Collectors.toList());
            } else if (type.equals("structure")) {
                // [修复] 使用 Registry.STRUCTURE.getKey(structure).asString() 来替代已弃用的方法
                return Registry.STRUCTURE.stream()
                        .map(structure -> Registry.STRUCTURE.getKey(structure).asString())
                        .filter(key -> key.startsWith(currentInput))
                        .collect(Collectors.toList());
            }*/
            if (type.equals("biome")) {
                return Registry.BIOME.stream()
                        .map(biome -> Registry.BIOME.getKey(biome).getKey()) // 获取 "plains", "forest" 等
                        .filter(key -> key.startsWith(currentInput))
                        .collect(Collectors.toList());
            } else if (type.equals("structure")) {
                return Registry.STRUCTURE.stream()
                        .map(structure -> Registry.STRUCTURE.getKey(structure).getKey()) // 获取 "village", "stronghold" 等
                        .filter(key -> key.startsWith(currentInput))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }
}

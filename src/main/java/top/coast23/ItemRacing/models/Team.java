package top.coast23.ItemRacing.models;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 代表一个游戏队伍的数据模型类。
 */
public class Team {

    private final String id;
    private final String displayName;
    private final TextColor color;
    private final Set<UUID> members;

    public Team(String id, String displayName, TextColor color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.members = new HashSet<>();
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public TextColor getColor() { return color; }
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public int getSize() { return members.size(); }

    // --- 成员管理方法 ---
    public void addMember(Player player) { members.add(player.getUniqueId()); }
    public void removeMember(Player player) { members.remove(player.getUniqueId()); }
    public boolean isMember(Player player) { return members.contains(player.getUniqueId()); }

    /**
     * [改造] 向队伍中的所有在线成员广播一条消息。
     * 现在直接接收一个 Component 对象，以支持更丰富的文本格式。
     * @param message 要发送的 Component 消息
     */
    public void broadcast(Component message) {
        for (UUID memberUuid : members) {
            Player player = Bukkit.getPlayer(memberUuid);
            // 确保玩家在线，才发送消息
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
}

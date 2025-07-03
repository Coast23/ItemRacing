package top.coast23.ItemRacing.managers;

import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 负责所有与队伍相关的逻辑。
 * 它持有一个所有活动队伍的集合，并提供方法来管理队伍及其成员。
 */
public class TeamManager {

    // 使用 Map 来存储所有队伍，以队伍的唯一ID (例如 "red") 作为键。
    // 这提供了非常快速的查找效率。
    private final Map<String, Team> teams = new HashMap<>();

    /**
     * 创建一个新队伍并将其添加到管理器中。
     *
     * @param id          队伍的唯一ID (例如 "red", "blue")。
     * @param displayName 游戏内显示的名称 (例如 "红队")。
     * @param color       队伍的颜色。
     */
    public void createTeam(String id, String displayName, TextColor color) {
        if (teams.containsKey(id)) {
            // 避免创建重复ID的队伍。
            return;
        }
        Team newTeam = new Team(id, displayName, color);
        teams.put(id, newTeam);
    }

    /**
     * 通过唯一ID获取一个队伍。
     *
     * @param id 要查找的队伍ID。
     * @return 一个包含Team的Optional（如果找到），否则为空。
     */
    public Optional<Team> getTeamById(String id) {
        return Optional.ofNullable(teams.get(id));
    }

    /**
     * 获取所有已创建队伍的集合。
     *
     * @return 所有队伍的集合。
     */
    public Collection<Team> getAllTeams() {
        return teams.values();
    }

    /**
     * 查找特定玩家所属的队伍。
     *
     * @param player 要检查的玩家。
     * @return 一个包含玩家所在队伍的Optional，如果玩家不在任何队伍中则为空。
     */
    public Optional<Team> getPlayerTeam(Player player) {
        for (Team team : teams.values()) {
            if (team.isMember(player)) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }

    /**
     * 将一个玩家添加到一个指定的队伍中。
     * 这个方法会自动将玩家从他们可能所在的任何其他队伍中移除。
     *
     * @param player 要添加的玩家。
     * @param teamId 要加入的队伍ID。
     * @return 如果玩家成功加入则返回true，如果队伍不存在则返回false。
     */
    public boolean addPlayerToTeam(Player player, String teamId) {
        Optional<Team> teamToJoin = getTeamById(teamId);

        if (teamToJoin.isEmpty()) {
            // 指定的队伍不存在。
            return false;
        }

        // 首先，将玩家从他们当前的队伍中移除（如果有的话），以防止玩家同时属于多个队伍。
        removePlayerFromCurrentTeam(player);

        // 将玩家添加到新队伍中。
        teamToJoin.get().addMember(player);
        return true;
    }

    /**
     * 将一个玩家从他们当前所在的任何队伍中移除。
     *
     * @param player 要移除的玩家。
     */
    public void removePlayerFromCurrentTeam(Player player) {
        getPlayerTeam(player).ifPresent(currentTeam -> currentTeam.removeMember(player));
    }
}


package top.coast23.ItemRacing.managers;

import top.coast23.ItemRacing.ItemRacing;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * [改造后] 负责管理游戏中的所有物品。
 */
public class ItemManager {

    private final ItemRacing plugin;
    private final List<Material> survivalItemPool = new ArrayList<>();

    // [改造] 使用EnumSet以获得更好的性能，并添加新的黑名单项目
    private final Set<Material> itemBlacklist = EnumSet.of(
            // --- 基础/虚拟方块 ---
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR, Material.WATER, Material.LAVA,
            // --- 技术性/管理员方块 ---
            Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
            Material.BARRIER, Material.STRUCTURE_VOID, Material.STRUCTURE_BLOCK, Material.JIGSAW,
            Material.LIGHT, Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK,
            // --- 生存无法获取方块 ---
            Material.BEDROCK, Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.END_GATEWAY,
            Material.NETHER_PORTAL, Material.REINFORCED_DEEPSLATE,
            Material.SPAWNER, Material.TRIAL_SPAWNER, Material.VAULT,
            // --- 状态依赖/不可拾取 ---
            Material.FARMLAND, Material.DIRT_PATH, Material.FROSTED_ICE,
            Material.PISTON_HEAD, Material.MOVING_PISTON,
            Material.BUBBLE_COLUMN, Material.TRIPWIRE,
            // --- 依赖NBT数据 (为简化逻辑，初期排除) ---
            Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION,
            Material.ENCHANTED_BOOK, Material.TIPPED_ARROW, Material.OMINOUS_BOTTLE,
            Material.SUSPICIOUS_STEW, Material.WRITTEN_BOOK, Material.FILLED_MAP,
            Material.FIREWORK_ROCKET, Material.FIREWORK_STAR,
            // [新增] 纹样陶片
            Material.ANGLER_POTTERY_SHERD, Material.ARCHER_POTTERY_SHERD, Material.ARMS_UP_POTTERY_SHERD,
            Material.BLADE_POTTERY_SHERD, Material.BREWER_POTTERY_SHERD, Material.BURN_POTTERY_SHERD,
            Material.DANGER_POTTERY_SHERD, Material.EXPLORER_POTTERY_SHERD, Material.FRIEND_POTTERY_SHERD,
            Material.HEART_POTTERY_SHERD, Material.HEARTBREAK_POTTERY_SHERD, Material.HOWL_POTTERY_SHERD,
            Material.MINER_POTTERY_SHERD, Material.MOURNER_POTTERY_SHERD, Material.PLENTY_POTTERY_SHERD,
            Material.PRIZE_POTTERY_SHERD, Material.SHEAF_POTTERY_SHERD, Material.SHELTER_POTTERY_SHERD,
            Material.SKULL_POTTERY_SHERD, Material.SNORT_POTTERY_SHERD,
            // 头颅, 但保留了凋零骷髅头
            Material.PLAYER_HEAD, Material.ZOMBIE_HEAD, Material.CREEPER_HEAD, Material.SKELETON_SKULL, /*Material.WITHER_SKELETON_SKULL,*/ Material.DRAGON_HEAD, Material.PIGLIN_HEAD,
            // 深层煤炭矿石和深层绿宝石矿石
            Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_EMERALD_ORE,
            // 潮涌核心
            Material.CONDUIT,
            // 龙蛋
            Material.DRAGON_EGG,
            // 下界合金块
            Material.NETHERITE_BLOCK,
            // 沉重核心
            Material.HEAVY_CORE,
            // 重锤
            Material.MACE,
            // 下界之星
            Material.NETHER_STAR,
            // 信标
            Material.BEACON,
            // 石化橡木台阶
            Material.PETRIFIED_OAK_SLAB,
            // 青蛙卵
            Material.FROGSPAWN,
            // 紫水晶母岩
            Material.BUDDING_AMETHYST,
            // 高草丛、高海草、大型蕨
            Material.TALL_GRASS/*, Material.TALL_DRY_GRASS*/, Material.TALL_SEAGRASS, Material.LARGE_FERN,
            // 鹦鹉壳
            Material.NAUTILUS_SHELL,
            // 三叉戟
            Material.TRIDENT,
            // 不死图腾
            Material.TOTEM_OF_UNDYING,
            // 紫颂植株
            Material.CHORUS_PLANT
    );

    public ItemManager(ItemRacing plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载生存模式可获取的物品池。
     */
    public void loadSurvivalItemPool() {
        survivalItemPool.clear();

        for (Material material : Material.values()) {
            String name = material.name();
            // [改造] 使用更严谨的规则进行过滤
            if (material.isLegacy() ||
                    itemBlacklist.contains(material) ||
                    name.endsWith("_SPAWN_EGG") ||
            //        name.endsWith("_BANNER_PATTERN") || // 排除所有旗帜图案
                    name.startsWith("MUSIC_DISC_") || // 排除所有音乐唱片
                    name.contains("CHAINMAIL_") || // 排除所有锁链装备
                    name.contains("TORCHFLOWER") || // 火把花
                    name.contains("PITCHER") || // 瓶子草与荚果
                    name.contains("SNIFFER") || // 嗅探兽蛋
                    name.contains("_ARMOR_TRIM") || // 锻造模板
                    name.contains("_FROGLIGHT") || // 蛙明灯
                    name.contains("INFESTED_") || // 虫蚀方块
                    name.contains("COMMAND_BLOCK")) // 命令方块
            {
                continue;
            }

            if (material.isItem()) {
                survivalItemPool.add(material);
            }
        }
        plugin.getLogger().info("动态物品库加载成功！共找到 " + survivalItemPool.size() + " 个生存模式可获取物品。");
    }

    /**
     * 从生存物品池中随机生成一份包含指定数量物品的任务列表。
     */
    public List<Material> generateRequiredItems(int amount) {
        if (survivalItemPool.isEmpty()) {
            plugin.getLogger().warning("生存物品池为空，无法生成任务列表！");
            return new ArrayList<>();
        }
        Collections.shuffle(survivalItemPool);
        int finalAmount = Math.min(amount, survivalItemPool.size());
        return new ArrayList<>(survivalItemPool.subList(0, finalAmount));
    }
    public int getTotalItemAmount(){
        return survivalItemPool.size();
    }
}

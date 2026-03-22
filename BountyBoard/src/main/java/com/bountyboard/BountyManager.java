package com.bountyboard;

import com.bountyboard.inventory.impl.BountyBoardGUI;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XItemFlag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BountyManager {

    private final BountyBoard plugin;
    private final Map<UUID, Bounty> bounties = new LinkedHashMap<>();
    private final Map<UUID, BountyBoardGUI> awaitingTargetPlayers = new HashMap<>();
    private final Set<String> boardLocationStrings = new HashSet<>();
    private File dataFile;
    private Team wantedTeam;

    public BountyManager(BountyBoard plugin) {
        this.plugin = plugin;
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }

        setupScoreboard();

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection bountySection = data.getConfigurationSection("bounties");
        if (bountySection != null) {
            for (String key : bountySection.getKeys(false)) {
                ConfigurationSection bs = bountySection.getConfigurationSection(key);
                if (bs == null) continue;
                String target = bs.getString("target", "Unknown");
                String poster = bs.getString("poster", "Unknown");
                long postTime = bs.getLong("postTime", System.currentTimeMillis());
                List<ItemStack> rewards = new ArrayList<>();
                ConfigurationSection rewardSection = bs.getConfigurationSection("rewards");
                if (rewardSection != null) {
                    for (String ri : rewardSection.getKeys(false)) {
                        Object obj = rewardSection.get(ri);
                        if (obj instanceof ItemStack) {
                            rewards.add((ItemStack) obj);
                        }
                    }
                }
                try {
                    UUID id = UUID.fromString(key);
                    bounties.put(id, new Bounty(id, target, poster, rewards, postTime));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        boardLocationStrings.addAll(data.getStringList("boards"));

        for (Bounty bounty : bounties.values()) {
            applyWantedEffect(bounty.getTargetName());
        }
    }

    public void saveData() {
        if (dataFile == null) return;
        FileConfiguration data = new YamlConfiguration();

        for (Bounty bounty : bounties.values()) {
            String path = "bounties." + bounty.getId().toString();
            data.set(path + ".target", bounty.getTargetName());
            data.set(path + ".poster", bounty.getPosterName());
            data.set(path + ".postTime", bounty.getPostTime());
            List<ItemStack> rewards = bounty.getRewards();
            for (int i = 0; i < rewards.size(); i++) {
                data.set(path + ".rewards." + i, rewards.get(i));
            }
        }

        data.set("boards", new ArrayList<>(boardLocationStrings));

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    private void setupScoreboard() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        wantedTeam = board.getTeam("bb_wanted");
        if (wantedTeam == null) {
            wantedTeam = board.registerNewTeam("bb_wanted");
        }
        wantedTeam.setColor(ChatColor.RED);
        wantedTeam.setPrefix("§c⚑ ");
        wantedTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
    }

    public void registerCraftingRecipe() {
        ItemStack boardItem = createBoardItem();

        ShapedRecipe recipe = new ShapedRecipe(plugin.getBoardKey(), boardItem);
        recipe.shape("PPP", "SCS", "WWW");

        Material paper = XMaterial.matchXMaterial("PAPER").map(XMaterial::parseMaterial).orElse(null);
        Material stick = XMaterial.matchXMaterial("STICK").map(XMaterial::parseMaterial).orElse(null);
        Material chest = XMaterial.matchXMaterial("CHEST").map(XMaterial::parseMaterial).orElse(null);
        Material planks = XMaterial.matchXMaterial("OAK_PLANKS").map(XMaterial::parseMaterial).orElse(null);

        if (paper == null || stick == null || chest == null || planks == null) {
            plugin.getLogger().severe("Failed to register crafting recipe: material lookup failed.");
            return;
        }

        recipe.setIngredient('P', paper);
        recipe.setIngredient('S', stick);
        recipe.setIngredient('C', chest);
        recipe.setIngredient('W', planks);

        Bukkit.addRecipe(recipe);
    }

    public ItemStack createBoardItem() {
        ItemStack item = XMaterial.matchXMaterial("CHEST").map(XMaterial::parseItem).orElse(null);
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lBounty Board");
        meta.setLore(Arrays.asList(
                "§7Place this board in the world",
                "§7to post and track bounties.",
                "",
                "§eRight-click to open"
        ));
        XItemFlag.decorationOnly(meta);
        meta.getPersistentDataContainer().set(plugin.getBoardKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isBountyBoardItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(plugin.getBoardKey(), PersistentDataType.BYTE);
    }

    public boolean isBountyBoard(Block block) {
        if (block == null) return false;
        if (boardLocationStrings.contains(locationToString(block.getLocation()))) return true;
        if (!(block.getState() instanceof TileState)) return false;
        TileState ts = (TileState) block.getState();
        return ts.getPersistentDataContainer().has(plugin.getBoardKey(), PersistentDataType.BYTE);
    }

    public void registerBoard(Location location) {
        boardLocationStrings.add(locationToString(location));
    }

    public void unregisterBoard(Location location) {
        boardLocationStrings.remove(locationToString(location));
    }

    public void addBounty(Bounty bounty) {
        bounties.put(bounty.getId(), bounty);
        applyWantedEffect(bounty.getTargetName());
        saveData();
    }

    public void removeBounty(UUID bountyId) {
        Bounty bounty = bounties.remove(bountyId);
        if (bounty == null) return;
        boolean hasMore = bounties.values().stream()
                .anyMatch(b -> b.getTargetName().equalsIgnoreCase(bounty.getTargetName()));
        if (!hasMore) {
            removeWantedEffect(bounty.getTargetName());
        }
        saveData();
    }

    public List<Bounty> getBountiesForTarget(String targetName) {
        return bounties.values().stream()
                .filter(b -> b.getTargetName().equalsIgnoreCase(targetName))
                .collect(Collectors.toList());
    }

    public List<Bounty> getAllBounties() {
        List<Bounty> list = new ArrayList<>(bounties.values());
        list.sort((a, b) -> Long.compare(b.getPostTime(), a.getPostTime()));
        return list;
    }

    public boolean hasActiveBounty(String targetName) {
        return bounties.values().stream()
                .anyMatch(b -> b.getTargetName().equalsIgnoreCase(targetName));
    }

    public void applyWantedEffect(String playerName) {
        if (wantedTeam != null) {
            wantedTeam.addEntry(playerName);
        }
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            player.setGlowing(true);
        }
    }

    public void removeWantedEffect(String playerName) {
        if (wantedTeam != null) {
            wantedTeam.removeEntry(playerName);
        }
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            player.setGlowing(false);
        }
    }

    public void setAwaitingTarget(UUID playerId, BountyBoardGUI gui) {
        awaitingTargetPlayers.put(playerId, gui);
    }

    public BountyBoardGUI getAwaitingTargetGUI(UUID playerId) {
        return awaitingTargetPlayers.get(playerId);
    }

    public void clearAwaitingTarget(UUID playerId) {
        awaitingTargetPlayers.remove(playerId);
    }

    public boolean isAwaitingTarget(UUID playerId) {
        return awaitingTargetPlayers.containsKey(playerId);
    }

    private String locationToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public Location stringToLocation(String s) {
        String[] parts = s.split(",");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
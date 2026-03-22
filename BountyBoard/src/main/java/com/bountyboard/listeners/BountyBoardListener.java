package com.bountyboard.listeners;

import com.bountyboard.Bounty;
import com.bountyboard.BountyBoard;
import com.bountyboard.inventory.impl.BountyBoardGUI;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class BountyBoardListener implements Listener {

    private final BountyBoard plugin;

    public BountyBoardListener(BountyBoard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!plugin.getBountyManager().isBountyBoardItem(item)) return;

        Block placed = event.getBlockPlaced();
        if (placed.getState() instanceof TileState) {
            TileState state = (TileState) placed.getState();
            state.getPersistentDataContainer().set(plugin.getBoardKey(), PersistentDataType.BYTE, (byte) 1);
            state.update();
        }
        plugin.getBountyManager().registerBoard(placed.getLocation());

        Player player = event.getPlayer();
        player.sendMessage("§a§lBounty Board placed! §7Right-click it to manage bounties.");
        XSound.matchXSound("BLOCK_CHEST_OPEN").ifPresent(s -> s.play(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!plugin.getBountyManager().isBountyBoard(block)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        BountyBoardGUI gui = new BountyBoardGUI(plugin, block.getLocation());
        plugin.getGuiManager().openGUI(gui, player);
        XSound.matchXSound("BLOCK_CHEST_OPEN").ifPresent(s -> s.play(player));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.CHEST) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof org.bukkit.block.Chest)) return;
        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) holder;
        if (plugin.getBountyManager().isBountyBoard(chest.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.getBountyManager().isBountyBoard(block)) return;

        event.setDropItems(false);
        plugin.getBountyManager().unregisterBoard(block.getLocation());

        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ItemStack boardItem = plugin.getBountyManager().createBoardItem();
            if (boardItem != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), boardItem);
            }
        }
        event.getPlayer().sendMessage("§7Bounty Board removed.");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        List<Bounty> targetBounties = plugin.getBountyManager().getBountiesForTarget(dead.getName());
        if (targetBounties.isEmpty()) return;

        int totalItems = 0;
        for (Bounty bounty : targetBounties) {
            for (ItemStack reward : bounty.getRewards()) {
                if (reward == null || reward.getType().isAir()) continue;
                killer.getInventory().addItem(reward.clone()).forEach((k, leftover) ->
                        killer.getWorld().dropItemNaturally(killer.getLocation(), leftover));
                totalItems++;
            }
            plugin.getBountyManager().removeBounty(bounty.getId());
        }

        final int finalTotalItems = totalItems;
        String msg = plugin.getConfig().getString("kill-message",
                        "&6[Bounty] &c{killer} &7claimed the bounty on &c{target} &7and received {count} reward item(s)!")
                .replace("&", "§")
                .replace("{killer}", killer.getName())
                .replace("{target}", dead.getName())
                .replace("{count}", String.valueOf(finalTotalItems));

        Bukkit.broadcastMessage(msg);
        XSound.matchXSound("ENTITY_PLAYER_LEVELUP").ifPresent(s -> s.play(killer));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getBountyManager().hasActiveBounty(player.getName())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getBountyManager().applyWantedEffect(player.getName());
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getBountyManager().isAwaitingTarget(player.getUniqueId())) return;

        event.setCancelled(true);
        final String input = event.getMessage().trim();
        final BountyBoardGUI gui = plugin.getBountyManager().getAwaitingTargetGUI(player.getUniqueId());
        plugin.getBountyManager().clearAwaitingTarget(player.getUniqueId());

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cCancelled target selection.");
            if (gui != null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getGuiManager().openGUI(gui, player));
            }
            return;
        }

        if (input.length() < 3 || input.length() > 16 || !input.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage("§cInvalid player name. Names must be 3-16 alphanumeric characters.");
            if (gui != null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getGuiManager().openGUI(gui, player));
            }
            return;
        }

        if (gui != null) {
            gui.setTargetName(input);
            player.sendMessage("§aTarget set to §c" + input + "§a!");
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getGuiManager().openGUI(gui, player));
        }
    }
}
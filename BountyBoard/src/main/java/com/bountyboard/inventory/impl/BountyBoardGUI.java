package com.bountyboard.inventory.impl;

import com.bountyboard.Bounty;
import com.bountyboard.BountyBoard;
import com.bountyboard.inventory.InventoryButton;
import com.bountyboard.inventory.InventoryGUI;
import com.cryptomorin.xseries.XItemFlag;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BountyBoardGUI extends InventoryGUI {

    private static final int SET_TARGET_SLOT = 1;
    private static final int TARGET_DISPLAY_SLOT = 3;
    private static final List<Integer> REWARD_SLOTS = Arrays.asList(18, 19, 20, 27, 28, 29, 36, 37, 38);
    private static final int POST_BUTTON_SLOT = 46;
    private static final int CLEAR_BUTTON_SLOT = 48;
    private static final List<Integer> SEPARATOR_SLOTS = Arrays.asList(4, 13, 22, 31, 40, 49);
    private static final int BOUNTIES_HEADER_SLOT = 6;
    private static final List<Integer> BOUNTY_DISPLAY_SLOTS = Arrays.asList(15, 24, 33, 42);
    private static final int SCROLL_UP_SLOT = 51;
    private static final int SCROLL_DOWN_SLOT = 52;
    private static final int REWARD_LABEL_SLOT = 9;
    private static final int REWARD_LABEL_SLOT2 = 10;

    private final BountyBoard plugin;
    private final Location boardLocation;
    private String targetName = null;
    private int scrollOffset = 0;

    public BountyBoardGUI(BountyBoard plugin, Location boardLocation) {
        super();
        this.plugin = plugin;
        this.boardLocation = boardLocation;
    }

    public void setTargetName(String name) {
        this.targetName = name;
    }

    public String getTargetName() {
        return targetName;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§6⚑ Bounty Board");
    }

    @Override
    public void decorate(Player player) {
        setupButtons(player);
        super.decorate(player);
    }

    private void setupButtons(Player player) {
        for (int i = 0; i < 54; i++) {
            if (!isSpecialSlot(i)) {
                final int slot = i;
                addButton(slot, new InventoryButton()
                        .creator(p -> createGlassPane())
                        .consumer(e -> {
                        }));
            }
        }

        for (int sep : SEPARATOR_SLOTS) {
            addButton(sep, new InventoryButton()
                    .creator(p -> createSeparator())
                    .consumer(e -> {
                    }));
        }

        addButton(SET_TARGET_SLOT, new InventoryButton()
                .creator(p -> createSetTargetItem())
                .consumer(e -> handleSetTarget((Player) e.getWhoClicked())));

        addButton(TARGET_DISPLAY_SLOT, new InventoryButton()
                .creator(p -> createTargetDisplayItem())
                .consumer(e -> {
                }));

        addButton(REWARD_LABEL_SLOT, new InventoryButton()
                .creator(p -> createLabel("§e§lREWARD ITEMS", Arrays.asList("§7Place items you wish", "§7to offer as the reward")))
                .consumer(e -> {
                }));

        addButton(POST_BUTTON_SLOT, new InventoryButton()
                .creator(p -> createPostButton())
                .consumer(e -> handlePost((Player) e.getWhoClicked())));

        addButton(CLEAR_BUTTON_SLOT, new InventoryButton()
                .creator(p -> createClearButton())
                .consumer(e -> handleClear((Player) e.getWhoClicked())));

        addButton(BOUNTIES_HEADER_SLOT, new InventoryButton()
                .creator(p -> createLabel("§6§lACTIVE BOUNTIES", Arrays.asList("§7Currently tracked targets")))
                .consumer(e -> {
                }));

        addButton(SCROLL_UP_SLOT, new InventoryButton()
                .creator(p -> createScrollItem("§a▲ Scroll Up"))
                .consumer(e -> handleScrollUp((Player) e.getWhoClicked())));

        addButton(SCROLL_DOWN_SLOT, new InventoryButton()
                .creator(p -> createScrollItem("§c▼ Scroll Down"))
                .consumer(e -> handleScrollDown((Player) e.getWhoClicked())));

        List<Bounty> allBounties = plugin.getBountyManager().getAllBounties();
        for (int i = 0; i < BOUNTY_DISPLAY_SLOTS.size(); i++) {
            final int bountyIndex = scrollOffset + i;
            final int displaySlot = BOUNTY_DISPLAY_SLOTS.get(i);
            if (bountyIndex < allBounties.size()) {
                final Bounty bounty = allBounties.get(bountyIndex);
                addButton(displaySlot, new InventoryButton()
                        .creator(p -> createBountyItem(bounty))
                        .consumer(e -> {
                        }));
            } else {
                addButton(displaySlot, new InventoryButton()
                        .creator(p -> createEmptyBountySlot())
                        .consumer(e -> {
                        }));
            }
        }
    }

    private boolean isSpecialSlot(int slot) {
        if (slot == SET_TARGET_SLOT) return true;
        if (slot == TARGET_DISPLAY_SLOT) return true;
        if (slot == POST_BUTTON_SLOT) return true;
        if (slot == CLEAR_BUTTON_SLOT) return true;
        if (slot == BOUNTIES_HEADER_SLOT) return true;
        if (slot == SCROLL_UP_SLOT) return true;
        if (slot == SCROLL_DOWN_SLOT) return true;
        if (slot == REWARD_LABEL_SLOT) return true;
        if (REWARD_SLOTS.contains(slot)) return true;
        if (SEPARATOR_SLOTS.contains(slot)) return true;
        if (BOUNTY_DISPLAY_SLOTS.contains(slot)) return true;
        return false;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory().equals(getInventory())) {
            int slot = event.getSlot();
            if (REWARD_SLOTS.contains(slot)) {
                return;
            }
            super.onClick(event);
        } else {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    private void handleSetTarget(Player player) {
        plugin.getBountyManager().setAwaitingTarget(player.getUniqueId(), this);
        player.closeInventory();
        player.sendMessage("§eType the target player's name in chat.");
        player.sendMessage("§7Type §ccancel §7to abort.");
    }

    private void handlePost(Player player) {
        if (targetName == null || targetName.isEmpty()) {
            XSound.matchXSound("ENTITY_VILLAGER_NO").ifPresent(s -> s.play(player));
            player.sendMessage("§cYou must set a target first! Click 'Set Target'.");
            return;
        }

        if (!plugin.getConfig().getBoolean("allow-self-bounty", false) && player.getName().equalsIgnoreCase(targetName)) {
            XSound.matchXSound("ENTITY_VILLAGER_NO").ifPresent(s -> s.play(player));
            player.sendMessage("§cYou cannot place a bounty on yourself!");
            return;
        }

        List<ItemStack> rewards = new ArrayList<>();
        for (int slot : REWARD_SLOTS) {
            ItemStack item = getInventory().getItem(slot);
            if (item != null && !item.getType().isAir()) {
                rewards.add(item.clone());
                getInventory().setItem(slot, null);
            }
        }

        if (rewards.isEmpty()) {
            XSound.matchXSound("ENTITY_VILLAGER_NO").ifPresent(s -> s.play(player));
            player.sendMessage("§cYou must add at least one reward item!");
            return;
        }

        Bounty bounty = new Bounty(UUID.randomUUID(), targetName, player.getName(), rewards, System.currentTimeMillis());
        plugin.getBountyManager().addBounty(bounty);

        XSound.matchXSound("ENTITY_EXPERIENCE_ORB_PICKUP").ifPresent(s -> s.play(player));
        player.sendMessage("§a✓ Bounty successfully posted on §c" + targetName + "§a!");

        String prevTarget = targetName;
        targetName = null;
        player.closeInventory();

        String msg = plugin.getConfig().getString("bounty-posted-message", "&6[Bounty] &7A bounty has been placed on &c{target}&7!")
                .replace("&", "§")
                .replace("{target}", prevTarget);
        if (plugin.getConfig().getBoolean("broadcast-on-bounty", true)) {
            Bukkit.broadcastMessage(msg);
        }
    }

    private void handleClear(Player player) {
        for (int slot : REWARD_SLOTS) {
            ItemStack item = getInventory().getItem(slot);
            if (item != null && !item.getType().isAir()) {
                player.getInventory().addItem(item).forEach((k, leftover) ->
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                getInventory().setItem(slot, null);
            }
        }
        targetName = null;
        XSound.matchXSound("UI_BUTTON_CLICK").ifPresent(s -> s.play(player));
        decorate(player);
    }

    private void handleScrollUp(Player player) {
        if (scrollOffset > 0) {
            scrollOffset--;
            decorate(player);
            XSound.matchXSound("UI_BUTTON_CLICK").ifPresent(s -> s.play(player));
        }
    }

    private void handleScrollDown(Player player) {
        int totalBounties = plugin.getBountyManager().getAllBounties().size();
        if (scrollOffset + BOUNTY_DISPLAY_SLOTS.size() < totalBounties) {
            scrollOffset++;
            decorate(player);
            XSound.matchXSound("UI_BUTTON_CLICK").ifPresent(s -> s.play(player));
        }
    }

    private ItemStack createGlassPane() {
        ItemStack item = XMaterial.matchXMaterial("GRAY_STAINED_GLASS_PANE").map(XMaterial::parseItem).orElse(null);
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§r");
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSeparator() {
        ItemStack item = XMaterial.matchXMaterial("BLACK_STAINED_GLASS_PANE").map(XMaterial::parseItem).orElse(null);
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§r");
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSetTargetItem() {
        ItemStack item = XMaterial.matchXMaterial("NAME_TAG").map(XMaterial::parseItem).orElse(null);
        if (item == null) return createGlassPane();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lSet Target");
        meta.setLore(Arrays.asList(
                "§7Click to type a player name",
                "§7in chat as the bounty target.",
                "",
                targetName != null ? "§aCurrent: §c" + targetName : "§cNo target set"
        ));
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTargetDisplayItem() {
        ItemStack item = XMaterial.matchXMaterial("PAPER").map(XMaterial::parseItem).orElse(null);
        if (item == null) return createGlassPane();
        ItemMeta meta = item.getItemMeta();
        if (targetName != null && !targetName.isEmpty()) {
            meta.setDisplayName("§c§l⚑ " + targetName);
            meta.setLore(Arrays.asList("§7This player is the current target."));
        } else {
            meta.setDisplayName("§7No Target Set");
            meta.setLore(Arrays.asList("§7Click 'Set Target' to pick", "§7a player to place a bounty on."));
        }
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPostButton() {
        boolean ready = targetName != null && !targetName.isEmpty();
        String matName = ready ? "LIME_WOOL" : "RED_WOOL";
        ItemStack item = XMaterial.matchXMaterial(matName).map(XMaterial::parseItem).orElse(null);
        if (item == null) return createGlassPane();
        ItemMeta meta = item.getItemMeta();
        if (ready) {
            meta.setDisplayName("§a§lPost Bounty");
            meta.setLore(Arrays.asList(
                    "§7Post the bounty on §c" + targetName + "§7.",
                    "§7Reward items will be taken",
                    "§7from the grid on the left.",
                    "",
                    "§eClick to confirm!"
            ));
        } else {
            meta.setDisplayName("§c§lCannot Post");
            meta.setLore(Arrays.asList(
                    "§7You need to:",
                    targetName == null ? "§c✗ §7Set a target player" : "§a✓ §7Target set",
                    "§c✗ §7Add at least one reward item"
            ));
        }
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClearButton() {
        ItemStack item = XMaterial.matchXMaterial("BARRIER").map(XMaterial::parseItem).orElse(null);
        if (item == null) return createGlassPane();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lClear All");
        meta.setLore(Arrays.asList("§7Clears the target and returns", "§7all reward items to you."));
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLabel(String displayName, List<String> lore) {
        ItemStack item = XMaterial.matchXMaterial("WRITABLE_BOOK").map(XMaterial::parseItem).orElse(null);
        if (item == null) return createGlassPane();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createScrollItem(String name) {
        ItemStack item = XMaterial.matchXMaterial("ARROW").map(XMaterial::parseItem).orElse(null);
        if (item == null) return createGlassPane();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBountyItem(Bounty bounty) {
        ItemStack item = XMaterial.matchXMaterial("PAPER").map(XMaterial::parseItem).orElse(null);
        if (item == null) return createEmptyBountySlot();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l⚑ " + bounty.getTargetName());
        List<String> lore = new ArrayList<>();
        lore.add("§7Posted by: §f" + bounty.getPosterName());
        lore.add("§7Reward:");
        int shown = 0;
        for (ItemStack reward : bounty.getRewards()) {
            if (reward == null || reward.getType().isAir()) continue;
            if (shown >= 5) {
                lore.add("§7... and " + (bounty.getRewards().size() - shown) + " more");
                break;
            }
            String name = reward.getItemMeta() != null && reward.getItemMeta().hasDisplayName()
                    ? reward.getItemMeta().getDisplayName()
                    : formatMaterialName(XMaterial.matchXMaterial(reward.getType()).name());
            lore.add("  §f" + reward.getAmount() + "x §e" + name);
            shown++;
        }
        meta.setLore(lore);
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptyBountySlot() {
        ItemStack item = XMaterial.matchXMaterial("LIGHT_GRAY_STAINED_GLASS_PANE").map(XMaterial::parseItem).orElse(null);
        if (item == null) return createGlassPane();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7No bounty");
        XItemFlag.decorationOnly(meta);
        item.setItemMeta(meta);
        return item;
    }

    private String formatMaterialName(String rawName) {
        String[] parts = rawName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
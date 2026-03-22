package com.bountyboard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BountyBoardCommand implements CommandExecutor, TabCompleter {

    private final BountyBoard plugin;

    public BountyBoardCommand(BountyBoard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bountyboard.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                handleGive(sender);
                break;
            case "list":
                handleList(sender);
                break;
            case "clear":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /bountyboard clear <target>");
                    return true;
                }
                handleClear(sender, args[1]);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleGive(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this subcommand.");
            return;
        }
        Player player = (Player) sender;
        ItemStack board = plugin.getBountyManager().createBoardItem();
        if (board == null) {
            player.sendMessage("§cFailed to create Bounty Board item.");
            return;
        }
        player.getInventory().addItem(board).forEach((k, leftover) ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.sendMessage("§aYou have received a §6Bounty Board§a!");
    }

    private void handleList(CommandSender sender) {
        List<Bounty> all = plugin.getBountyManager().getAllBounties();
        if (all.isEmpty()) {
            sender.sendMessage("§7There are no active bounties.");
            return;
        }
        sender.sendMessage("§6§l--- Active Bounties ---");
        for (Bounty bounty : all) {
            sender.sendMessage("§c⚑ " + bounty.getTargetName()
                    + " §7| Posted by §f" + bounty.getPosterName()
                    + " §7| " + bounty.getRewards().size() + " reward item(s)");
        }
    }

    private void handleClear(CommandSender sender, String targetName) {
        List<Bounty> bounties = plugin.getBountyManager().getBountiesForTarget(targetName);
        if (bounties.isEmpty()) {
            sender.sendMessage("§7No bounties found for player §c" + targetName + "§7.");
            return;
        }
        for (Bounty bounty : bounties) {
            plugin.getBountyManager().removeBounty(bounty.getId());
        }
        sender.sendMessage("§aCleared " + bounties.size() + " bounty/bounties for §c" + targetName + "§a.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l--- Bounty Board Commands ---");
        sender.sendMessage("§e/bountyboard give §7- Receive a Bounty Board item");
        sender.sendMessage("§e/bountyboard list §7- List all active bounties");
        sender.sendMessage("§e/bountyboard clear <target> §7- Remove all bounties for a player");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "list", "clear").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}
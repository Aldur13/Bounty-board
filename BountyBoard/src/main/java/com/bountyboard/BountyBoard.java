package com.bountyboard;

import com.bountyboard.inventory.gui.GUIListener;
import com.bountyboard.inventory.gui.GUIManager;
import com.bountyboard.listeners.BountyBoardListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class BountyBoard extends JavaPlugin {

    private GUIManager guiManager;
    private BountyManager bountyManager;
    private NamespacedKey boardKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.boardKey = new NamespacedKey(this, "bounty_board");
        this.guiManager = new GUIManager();
        this.bountyManager = new BountyManager(this);

        bountyManager.loadData();
        bountyManager.registerCraftingRecipe();

        Bukkit.getPluginManager().registerEvents(new GUIListener(guiManager), this);
        Bukkit.getPluginManager().registerEvents(new BountyBoardListener(this), this);

        getCommand("bountyboard").setExecutor(new BountyBoardCommand(this));
        getCommand("bountyboard").setTabCompleter(new BountyBoardCommand(this));

        getLogger().info("BountyBoard enabled successfully.");
    }

    @Override
    public void onDisable() {
        bountyManager.saveData();
        getLogger().info("BountyBoard disabled. Data saved.");
    }
}
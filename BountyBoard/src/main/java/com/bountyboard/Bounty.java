package com.bountyboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Bounty {
    private UUID id;
    private String targetName;
    private String posterName;
    private List<ItemStack> rewards;
    private long postTime;
}
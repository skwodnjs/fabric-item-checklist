package jwn.item_checklist.util;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemChecklistHelper {
    // 1. 어디에 있는지 (list)
    // 2. 몇 개 부족한지 or 넘치는지 (int)

    public enum ItemLocation {
        INVENTORY, ENDER_CHEST, SHULKER_BOX, NONE
    }

    public static boolean hasEnoughItem(ClientPlayerEntity player, Item checklistItem) {
        int checklistCount = countChecklist(player, checklistItem);
        int inventoryCount = countInventory(player, checklistItem);
        int enderBoxCount = countEnderChest(player, checklistItem);
        List<Integer> shulkerBoxCount = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            shulkerBoxCount.add(i, countSingleShulkerBox(player, i, checklistItem));
        }

        int shulkerTotal = 0;
        for (int i : shulkerBoxCount) {
            shulkerTotal += i;
        }
        int total = inventoryCount + enderBoxCount + shulkerTotal;

        return total >= checklistCount;
    }

    public static ItemLocation getItemLocation(ClientPlayerEntity player, Item item) {
        if (player == null) return ItemLocation.NONE;

        // 인벤토리 확인
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() == item && !stack.isEmpty()) return ItemLocation.INVENTORY;
        }

        // 엔더상자 확인
        for (ItemStack stack : player.getEnderChestInventory().getHeldStacks()) {
            if (stack.getItem() == item && !stack.isEmpty()) return ItemLocation.ENDER_CHEST;
        }

        // 셜커박스 확인
        for (int i = 0; i < 36; i++) {
            if (ItemChecklistHelper.countSingleShulkerBox(player, i, item) > 0)
                return ItemLocation.SHULKER_BOX;
        }

        return ItemLocation.NONE;
    }


    private static int countChecklist(ClientPlayerEntity player, Item checklistItem) {
        int checklistCount = 0;
        if (player instanceof ItemChecklistProvider provider) {
            for (ItemStack stack : provider.getItemChecklist()) {
                if (stack.getItem() == checklistItem) {
                    checklistCount += stack.getCount();
                }
            }
        }

        return checklistCount;
    }

    private static int countInventory(ClientPlayerEntity player, Item checklistItem) {
        int inventoryCount = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() == checklistItem) {
                inventoryCount += stack.getCount();
            }
        }

        return inventoryCount;
    }

    private static int countEnderChest(ClientPlayerEntity player, Item checklistItem) {
        int enderChestCount = 0;
        for (ItemStack stack : player.getEnderChestInventory().getHeldStacks()) {
            if (stack.getItem() == checklistItem) {
                enderChestCount += stack.getCount();
            }
        }

        return enderChestCount;
    }

    private static int countSingleShulkerBox(ClientPlayerEntity player, int slot, Item checklistItem) {
        // 0 ~ 8    : Hotbar
        // 9 ~ 35   : Inventory

        ItemStack stack = player.getInventory().getStack(slot);

        if (!(stack.getItem() instanceof BlockItem blockItem)) return 0;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return 0;

        int count = 0;

        ContainerComponent container = stack.getComponents().get(DataComponentTypes.CONTAINER);
        if (container != null) {
            List<ItemStack> items = container.stream().toList();
            for (ItemStack innerStack : items) {
                if (innerStack.getItem() == checklistItem) {
                    count += innerStack.getCount();
                }
            }
        }

        return count;
    }

}

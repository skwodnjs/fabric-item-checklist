package jwn.item_checklist.util;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemChecklistHelper {
    public enum ItemLocation {
        INVENTORY(0),
        SHULKER_BOX(1);

        final int index;

        ItemLocation(int i) {
            index = i;
        }

        public int getIndex() {
            return index;
        }
    }

    public static boolean hasEnoughItem(ClientPlayerEntity player, Item checklistItem) {
        int checklistCount = countChecklist(player, checklistItem);
        int inventoryCount = countInventory(player, checklistItem);
        int shulkerTotal = 0;
        for (int i = 0; i < 36; i++) {
            shulkerTotal += countSingleShulkerBox(player, i, checklistItem);
        }
        int total = inventoryCount + shulkerTotal;

        return total >= checklistCount;
    }

    public static List<Integer> countAllItemObtained(ClientPlayerEntity player, Item checklistItem) {
        // 전체 길이: 1 (INVENTORY) + 36 (SHULKER_BOX)
        int totalSize = 2 + 36;
        List<Integer> result = new ArrayList<>(Collections.nCopies(totalSize, 0));

        // 인벤토리 개수 기록
        result.set(ItemLocation.INVENTORY.index, countInventory(player, checklistItem));

        // 셜커박스 인벤토리 개수 기록
        for (int i = 0; i < 36; i++) {
            result.set(ItemLocation.SHULKER_BOX.index + i, countSingleShulkerBox(player, i, checklistItem));
        }

        return result;
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

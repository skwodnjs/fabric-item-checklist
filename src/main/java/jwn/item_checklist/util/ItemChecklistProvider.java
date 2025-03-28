package jwn.item_checklist.util;

import net.minecraft.item.ItemStack;

import java.util.List;

public interface ItemChecklistProvider {
    List<ItemStack> getItemChecklist();

    void addItemToChecklist(ItemStack stack);

    void removeItemFromChecklist(ItemStack stack);
}

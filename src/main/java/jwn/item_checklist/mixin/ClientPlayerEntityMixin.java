package jwn.item_checklist.mixin;

import jwn.item_checklist.util.ItemChecklistProvider;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin implements ItemChecklistProvider {

    @Unique
    private final List<ItemStack> itemChecklist = new ArrayList<>();

    @Override
    public List<ItemStack> getItemChecklist() {
        return itemChecklist;
    }

    @Override
    public void addItemToChecklist(ItemStack stack) {
        if (stack.isEmpty()) return;

        int remaining = stack.getCount();
        int maxCount = stack.getMaxCount();

        // 먼저 기존 스택에 추가 가능한 만큼 채움
        for (ItemStack existing : itemChecklist) {
            if (existing.getItem() == stack.getItem() && existing.getCount() < existing.getMaxCount()) {
                int space = existing.getMaxCount() - existing.getCount();
                int toAdd = Math.min(space, remaining);
                existing.increment(toAdd);
                remaining -= toAdd;
                if (remaining <= 0) return;
            }
        }

        // 남은 수량을 새 스택으로 분할해서 추가
        while (remaining > 0) {
            int toAdd = Math.min(maxCount, remaining);
            ItemStack newStack = stack.copy();
            newStack.setCount(toAdd);
            itemChecklist.add(newStack);
            remaining -= toAdd;
        }

        sortByItemThenCount();
    }

    @Override
    public void removeItemFromChecklist(ItemStack stack) {
        if (stack.isEmpty()) return;

        int toRemove = stack.getCount();

        for (int i = itemChecklist.size() - 1; i >= 0 && toRemove > 0; i--) {
            ItemStack existing = itemChecklist.get(i);

            if (existing.getItem() == stack.getItem()) {
                int existingCount = existing.getCount();

                if (existingCount <= toRemove) {
                    toRemove -= existingCount;
                    itemChecklist.remove(i);
                } else {
                    existing.decrement(toRemove);
                    toRemove = 0;
                }
            }
        }

        sortByItemThenCount();
    }

    @Unique
    public void clearChecklist() {
        itemChecklist.clear();
    }

    @Unique
    private  void sortByItemThenCount() {
        Map<Item, Integer> firstAppearanceOrder = new LinkedHashMap<>();

        // 등장 순서 기록
        for (ItemStack stack : itemChecklist) {
            Item item = stack.getItem();
            firstAppearanceOrder.putIfAbsent(item, firstAppearanceOrder.size());
        }

        itemChecklist.sort((a, b) -> {
            int orderA = firstAppearanceOrder.get(a.getItem());
            int orderB = firstAppearanceOrder.get(b.getItem());

            if (orderA != orderB) {
                return Integer.compare(orderA, orderB);
            }

            // 같은 그룹이면 count 기준 내림차순
            return Integer.compare(b.getCount(), a.getCount());
        });
    }
}
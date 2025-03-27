package jwn.item_checklist.screen;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChecklistScreen extends Screen {
    private List<ItemGroup> tabs;
    private final List<ItemStack> visibleItems = new ArrayList<>();
    private static final ItemGroup SEARCH_RESULT_TAB = FabricItemGroup.builder()
            .displayName(Text.translatable("gui.item_checklist.search_result"))
            .icon(() -> new ItemStack(Items.COMPASS))
            .entries((context, entries) -> {

            })
            .build();

    private int selectedTabIndex = 0;

    private int scrollOffset = 0; // 스크롤 위치
    private final int itemsPerRow = 9;
    private final int itemSize = 18;

    private int leftCenter;
    private int rightCenter;

    public ChecklistScreen() {
        super(Text.of("My Screen"));
    }

    @Override
    protected void init() {
        tabs = List.of(
                Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.BUILDING_BLOCKS)),
                Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.COLORED_BLOCKS)),
                Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.NATURAL)),
                Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.FUNCTIONAL)),
                Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.REDSTONE)),
                Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.TOOLS)),
                Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.COMBAT)),
                Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.FOOD_AND_DRINK)),
                Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.INGREDIENTS)),
                SEARCH_RESULT_TAB
        );
        selectTab(selectedTabIndex);

        int buttonY = 10;
        int buttonWidth = 20;
        int buttonHeight = 20;

        leftCenter = width / 4 - 5;
        rightCenter = width * 3 / 4 + 5;

        // 왼쪽 버튼 (◀)
        ButtonWidget leftButton = ButtonWidget.builder(Text.of("◀"), b -> {
            if (selectedTabIndex > 0) {
                selectedTabIndex--;
                selectTab(selectedTabIndex);
            }
        }).dimensions(leftCenter - 65, buttonY, buttonWidth, buttonHeight).build();

        // 오른쪽 버튼 (▶)
        ButtonWidget rightButton = ButtonWidget.builder(Text.of("▶"), b -> {
            if (selectedTabIndex < tabs.size() - 1) {
                selectedTabIndex++;
                selectTab(selectedTabIndex);
            }
        }).dimensions(leftCenter + 65 - buttonWidth, buttonY, buttonWidth, buttonHeight).build();

        this.addDrawableChild(leftButton);
        this.addDrawableChild(rightButton);
    }

    private void selectTab(int index) {
        if (MinecraftClient.getInstance().world == null) return;
        if (MinecraftClient.getInstance().player == null) return;

        FeatureSet features = MinecraftClient.getInstance().world.getEnabledFeatures();
        boolean showOpTab = MinecraftClient.getInstance().player.isCreativeLevelTwoOp();
        RegistryWrapper.WrapperLookup registries = MinecraftClient.getInstance().world.getRegistryManager();

        ItemGroups.updateDisplayContext(features, showOpTab, registries);

        visibleItems.clear();
        visibleItems.addAll(tabs.get(index).getDisplayStacks());

        scrollOffset = 0; // 탭 전환 시 스크롤 초기화
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalItems = visibleItems.size();
        int rowsVisible = (this.height - 40 - 5) / itemSize;    // 한 화면에 보여줄 row 수
        int maxScroll = Math.max(0, (totalItems / itemsPerRow) - rowsVisible);  // 최대 스크롤 가능 범위

        // 마우스 휠로 스크롤 이동
        scrollOffset = MathHelper.clamp(scrollOffset - (int) verticalAmount, 0, maxScroll);
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
        super.render(context, mouseX, mouseY, delta);
        
        // 왼쪽 화면

        // 상단 텍스트
        String titleLeft = tabs.get(selectedTabIndex).getDisplayName().getString();
        int titleLeftX = leftCenter - (textRenderer.getWidth(titleLeft) / 2);
        context.drawText(textRenderer, titleLeft, titleLeftX, 15, 0xFFFFFF, true);

        // 아이템 그리기
        int totalRowWidth = itemsPerRow * itemSize;                     // 가로 길이
        int itemXStart = leftCenter - totalRowWidth / 2;
        int itemYStart = 40;

        int rowsVisible = (this.height - itemYStart - 5) / itemSize;    // row 개수
        int startIdx = scrollOffset * itemsPerRow;

        ItemStack hoveredStack = ItemStack.EMPTY;                       // 마우스가 올려진 아이템

        for (int i = 0; i < rowsVisible * itemsPerRow && startIdx + i < visibleItems.size(); i++) {
            ItemStack stack = visibleItems.get(startIdx + i);
            int drawX = itemXStart + (i % itemsPerRow) * itemSize;
            int drawY = itemYStart + (i / itemsPerRow) * itemSize;

            context.drawItem(stack, drawX, drawY);
            context.drawStackOverlay(textRenderer, stack, drawX, drawY, null);

            // 마우스가 이 아이템 위에 있을 때
            if (mouseX >= drawX && mouseX < drawX + 16 &&
                    mouseY >= drawY && mouseY < drawY + 16) {
                hoveredStack = stack;
            }
        }

        // 툴팁 표시
        if (!hoveredStack.isEmpty()) {
            context.drawItemTooltip(textRenderer, hoveredStack, mouseX, mouseY);
        }

        // 오른쪽 화면
        context.fill(rightCenter - totalRowWidth / 2, itemYStart, rightCenter + totalRowWidth / 2 + 10, this.height - 5, 0x88000000);

        // 상단 텍스트
        String titleRight = Text.translatable("gui.item_checklist.checklist").getString();
        int titleRightX = rightCenter - (textRenderer.getWidth(titleRight) / 2);
        context.drawText(textRenderer, titleRight, titleRightX, 15, 0xFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // 왼쪽 클릭
            int leftWidth = this.width / 2;
            int centerX = leftWidth / 2;

            int totalRowWidth = itemsPerRow * itemSize;
            int itemXStart = centerX - (totalRowWidth / 2);
            int itemYStart = 40;

            int rowsVisible = (this.height - 40) / itemSize;
            int startIdx = scrollOffset * itemsPerRow;

            for (int i = 0; i < rowsVisible * itemsPerRow && startIdx + i < visibleItems.size(); i++) {
                int drawX = itemXStart + (i % itemsPerRow) * itemSize;
                int drawY = itemYStart + (i / itemsPerRow) * itemSize;

                // 마우스 좌표가 아이템 범위 안에 들어온 경우
                if (mouseX >= drawX && mouseX < drawX + 16 &&
                        mouseY >= drawY && mouseY < drawY + 16) {
                    ItemStack clickedStack = visibleItems.get(startIdx + i);
                    System.out.println(clickedStack.getName().getString()); // 이름 출력
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean shouldPause() {
        return false;
    }
}



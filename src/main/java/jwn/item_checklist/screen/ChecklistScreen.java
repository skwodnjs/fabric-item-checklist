package jwn.item_checklist.screen;

import jwn.item_checklist.keybindings.KeyInputHandler;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

    // UI
    final int PADDING = 10;
    final int LEFT_BUTTON_GAP = 65;
    final int LEFT_ITEM_Y = 40;
    final int TITLE_Y = 15;

    final int LEFT_RATIO = 6;
    final int RIGHT_RATIO = 7;

    int LEFT_CENTER;
    int RIGHT_CENTER;

    public static final List<ItemStack> SEARCH_RESULTS = new ArrayList<>();
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

    private ItemStack hoveredStack;

    private TextFieldWidget searchBox;

    public ChecklistScreen() {
        super(Text.of("My Screen"));
    }

    @Override
    protected void init() {
        if (MinecraftClient.getInstance().world != null && MinecraftClient.getInstance().player != null) {
            FeatureSet features = MinecraftClient.getInstance().world.getEnabledFeatures();
            boolean showOpTab = MinecraftClient.getInstance().player.isCreativeLevelTwoOp();
            RegistryWrapper.WrapperLookup registries = MinecraftClient.getInstance().world.getRegistryManager();
            ItemGroups.updateDisplayContext(features, showOpTab, registries);

            List<ItemGroup> sourceGroups = List.of(
                    Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.BUILDING_BLOCKS)),
                    Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.COLORED_BLOCKS)),
                    Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.NATURAL)),
                    Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.FUNCTIONAL)),
                    Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.REDSTONE)),
                    Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.TOOLS)),
                    Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.COMBAT)),
                    Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.FOOD_AND_DRINK)),
                    Objects.requireNonNull(Registries.ITEM_GROUP.get(ItemGroups.INGREDIENTS))
            );

            for (ItemGroup group : sourceGroups) {
                SEARCH_RESULTS.addAll(group.getDisplayStacks());
            }
        }

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

        LEFT_CENTER = this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2;
        RIGHT_CENTER = this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) + this.width * RIGHT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2;

        int buttonY = PADDING;
        int buttonWidth = 20;
        int buttonHeight = 20;

        // 왼쪽 버튼 (◀)
        ButtonWidget leftButton = ButtonWidget.builder(Text.of("◀"), b -> {
            if (selectedTabIndex == 0) {
                selectedTabIndex = tabs.size() - 1;
            } else {
                selectedTabIndex--;
            }
            selectTab(selectedTabIndex);
        }).dimensions(LEFT_CENTER - LEFT_BUTTON_GAP, buttonY, buttonWidth, buttonHeight).build();

        // 오른쪽 버튼 (▶)
        ButtonWidget rightButton = ButtonWidget.builder(Text.of("▶"), b -> {
            if (selectedTabIndex < tabs.size() - 1) {
                selectedTabIndex++;
            } else {
                selectedTabIndex = 0;
            }
            selectTab(selectedTabIndex);
        }).dimensions(LEFT_CENTER + LEFT_BUTTON_GAP - buttonWidth, buttonY, buttonWidth, buttonHeight).build();

        this.addDrawableChild(leftButton);
        this.addDrawableChild(rightButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
        super.render(context, mouseX, mouseY, delta);

        // 왼쪽 화면

        // 검색 창
        boolean isSearchTab = tabs.get(selectedTabIndex) == SEARCH_RESULT_TAB;

        if (searchBox != null && searchBox.isVisible()) {
            searchBox.render(context, mouseX, mouseY, delta);
        }

        int itemYStart = isSearchTab ? LEFT_ITEM_Y + 20 : LEFT_ITEM_Y;

        // 상단 텍스트
        String titleLeft = tabs.get(selectedTabIndex).getDisplayName().getString();
        int titleLeftX = LEFT_CENTER - (textRenderer.getWidth(titleLeft) / 2);
        context.drawText(textRenderer, titleLeft, titleLeftX, TITLE_Y, 0xFFFFFF, true);

        // 아이템 그리기
        int totalRowWidth = itemsPerRow * itemSize;                     // 가로 길이
        int itemXStart = LEFT_CENTER - totalRowWidth / 2;

        int rowsVisible = (this.height - itemYStart - 5) / itemSize;    // row 개수
        int startIdx = scrollOffset * itemsPerRow;

        hoveredStack = ItemStack.EMPTY;                       // 마우스가 올려진 아이템

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
        context.fill(RIGHT_CENTER - this.width * RIGHT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2 + PADDING, PADDING,
                RIGHT_CENTER + this.width * RIGHT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2 - PADDING, this.height - PADDING, 0x88000000);

        // 상단 텍스트
        String titleRight = Text.translatable("gui.item_checklist.checklist").getString();
        int titleRightX = RIGHT_CENTER - (textRenderer.getWidth(titleRight) / 2);
        context.drawText(textRenderer, titleRight, titleRightX, TITLE_Y, 0xFFFFFF, true);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean result;
        if (searchBox != null && searchBox.isFocused()) {
            result = searchBox.charTyped(chr, modifiers);
            updateSearchResults(searchBox.getText());
            return result;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox == null || !searchBox.isFocused()) {
            if (KeyInputHandler.OpenChecklistKey.matchesKey(keyCode, scanCode)) {
                this.close();
                return true;
            }
        }

        boolean result;
        if (searchBox != null && searchBox.isFocused()) {
            result = searchBox.keyPressed(keyCode, scanCode, modifiers);
            updateSearchResults(searchBox.getText());
        } else {
            result = super.keyPressed(keyCode, scanCode, modifiers);
        }
        return result;
    }

    private void updateSearchResults(String query) {
        SEARCH_RESULTS.clear();

        if (query.isBlank()) {
            for (ItemGroup group : tabs) {
                if (group != SEARCH_RESULT_TAB) {
                    SEARCH_RESULTS.addAll(group.getDisplayStacks());
                }
            }
        } else {
            String lower = query.toLowerCase();

            for (ItemGroup group : tabs) {
                if (group == SEARCH_RESULT_TAB) continue;

                for (ItemStack stack : group.getDisplayStacks()) {
                    String displayName = stack.getName().getString().toLowerCase();
                    String itemId = Registries.ITEM.getId(stack.getItem()).toString().toLowerCase();

                    if (displayName.contains(lower) || itemId.contains(lower)) {
                        SEARCH_RESULTS.add(stack);
                    }
                }
            }
        }

        if (tabs.get(selectedTabIndex) == SEARCH_RESULT_TAB) {
            visibleItems.clear();
            visibleItems.addAll(SEARCH_RESULTS);
            scrollOffset = 0;
        }

        if (MinecraftClient.getInstance().world != null && MinecraftClient.getInstance().player != null) {
            FeatureSet features = MinecraftClient.getInstance().world.getEnabledFeatures();
            boolean showOpTab = MinecraftClient.getInstance().player.isCreativeLevelTwoOp();
            RegistryWrapper.WrapperLookup registries = MinecraftClient.getInstance().world.getRegistryManager();
            ItemGroups.updateDisplayContext(features, showOpTab, registries);
        }
    }

    private void selectTab(int index) {
        visibleItems.clear();
        if (searchBox != null) {
            searchBox.setText("");
            searchBox.setFocused(false);
        }

        boolean isSearchTab = tabs.get(index) == SEARCH_RESULT_TAB;

        if (isSearchTab) {
            visibleItems.addAll(SEARCH_RESULTS);

            if (searchBox == null) {
                searchBox = new TextFieldWidget(
                        textRenderer,
                        LEFT_CENTER - this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) * 7 / 10 / 2 - 1,
                        35,
                        this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) * 7 / 10,
                        20,
                        Text.of("Search")
                );
                searchBox.setMaxLength(50);
                searchBox.setEditableColor(0xFFFFFF);
                this.addDrawableChild(searchBox);
            } else {
                searchBox.setVisible(true);
            }

        } else {
            visibleItems.addAll(tabs.get(index).getDisplayStacks());

            // 검색 탭이 아니면 숨김
            if (searchBox != null) {
                searchBox.setVisible(false);
            }
        }

        scrollOffset = 0; // 탭 전환 시 스크롤 초기화
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        boolean isSearchTab = tabs.get(selectedTabIndex) == SEARCH_RESULT_TAB;
        int itemYStart = isSearchTab ? 60 : 40;

        int leftXStart = LEFT_CENTER - (itemsPerRow * itemSize) / 2;
        int leftXEnd = leftXStart + itemsPerRow * itemSize;
        int leftYEnd = this.height;

        // 마우스가 왼쪽 아이템 영역 안에 있을 때만 스크롤 적용
        if (mouseX >= leftXStart && mouseX < leftXEnd &&
                mouseY >= itemYStart && mouseY < leftYEnd) {

            int totalItems = visibleItems.size();
            int rowsVisible = (this.height - itemYStart - 5) / itemSize;
            int maxScroll = Math.max(0, (totalItems / itemsPerRow) - rowsVisible);

            scrollOffset = MathHelper.clamp(scrollOffset - (int) verticalAmount, 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (searchBox != null && searchBox.mouseClicked(mouseX, mouseY, button)) {
                searchBox.setFocused(true);
                return true;
            }

            if (!hoveredStack.isEmpty()) {
                boolean shiftDown = hasShiftDown();
                if (shiftDown) {
                    System.out.println(hoveredStack.getName().getString() + "!!");
                } else {
                    System.out.println(hoveredStack.getName().getString());
                }

                if (searchBox != null) {
                    searchBox.setFocused(false);
                }
                return true;
            }

            if (searchBox != null) {
                searchBox.setFocused(false);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);

        LEFT_CENTER = this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2;
        RIGHT_CENTER = this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) + this.width * RIGHT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2;

        if (searchBox != null) {
            searchBox.setX(LEFT_CENTER - searchBox.getWidth() / 2);
            searchBox.setY(35); // 고정된 y 값 유지
            searchBox.setWidth(this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) * 7 / 10);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

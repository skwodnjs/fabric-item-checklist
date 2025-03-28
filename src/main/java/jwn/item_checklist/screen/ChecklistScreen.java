package jwn.item_checklist.screen;

import jwn.item_checklist.keybindings.KeyInputHandler;
import jwn.item_checklist.util.ItemChecklistProvider;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
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
    private final List<ItemStack> VISIBLE_ITEMS_LEFT = new ArrayList<>();
    private final List<ItemStack> VISIBLE_ITEMS_RIGHT = new ArrayList<>();

    // UI
    final int PADDING = 10;
    final int LEFT_BUTTON_GAP = 65;
    final int SEARCH_BOX_Y = 35;

    final int LEFT_RATIO = 6;
    final int RIGHT_RATIO = 7;

    int LEFT_CENTER;
    int RIGHT_CENTER;

    final int LEFT_ITEM_Y = 40;
    final int RIGHT_ITEM_Y = 32;

    final int TITLE_Y = 15;

    public static final List<ItemStack> SEARCH_RESULTS = new ArrayList<>();
    private static final ItemGroup SEARCH_RESULT_TAB = FabricItemGroup.builder()
            .displayName(Text.translatable("gui.item_checklist.search_result"))
            .icon(() -> new ItemStack(Items.COMPASS))
            .entries((context, entries) -> {

            })
            .build();

    private int SELECTED_TAB_INDEX = 0;

    private int LEFT_SCROLL_OFFSET = 0; // 스크롤 위치
    private int RIGHT_SCROLL_OFFSET = 0; // 스크롤 위치
    private final int ITEMS_PER_ROW = 9;
    private final int ITEM_SIZE = 16;
    private final int ITEM_PADDING_LEFT = 2;
    private final int ITEM_PADDING_RIGHT = 4;
    private final int ITEM_SPACE_LEFT = ITEM_SIZE + ITEM_PADDING_LEFT;
    private final int ITEM_SPACE_RIGHT = ITEM_SIZE + ITEM_PADDING_RIGHT;

    private ItemStack LEFT_HOVERED_STACK;
    private ItemStack RIGHT_HOVERED_STACK;

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
        selectTab(SELECTED_TAB_INDEX);

        LEFT_CENTER = this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2;
        RIGHT_CENTER = this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) + this.width * RIGHT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2;

        int buttonY = PADDING;
        int buttonWidth = 20;
        int buttonHeight = 20;

        // 왼쪽 버튼 (◀)
        ButtonWidget leftButton = ButtonWidget.builder(Text.of("◀"), b -> {
            if (SELECTED_TAB_INDEX == 0) {
                SELECTED_TAB_INDEX = tabs.size() - 1;
            } else {
                SELECTED_TAB_INDEX--;
            }
            selectTab(SELECTED_TAB_INDEX);
        }).dimensions(LEFT_CENTER - LEFT_BUTTON_GAP, buttonY, buttonWidth, buttonHeight).build();

        // 오른쪽 버튼 (▶)
        ButtonWidget rightButton = ButtonWidget.builder(Text.of("▶"), b -> {
            if (SELECTED_TAB_INDEX < tabs.size() - 1) {
                SELECTED_TAB_INDEX++;
            } else {
                SELECTED_TAB_INDEX = 0;
            }
            selectTab(SELECTED_TAB_INDEX);
        }).dimensions(LEFT_CENTER + LEFT_BUTTON_GAP - buttonWidth, buttonY, buttonWidth, buttonHeight).build();

        this.addDrawableChild(leftButton);
        this.addDrawableChild(rightButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player instanceof ItemChecklistProvider provider) {
            VISIBLE_ITEMS_RIGHT.clear();
            VISIBLE_ITEMS_RIGHT.addAll(provider.getItemChecklist());
        }

        super.render(context, mouseX, mouseY, delta);

        // 왼쪽 화면

        // 검색 창
        boolean isSearchTab = tabs.get(SELECTED_TAB_INDEX) == SEARCH_RESULT_TAB;

        if (searchBox != null && searchBox.isVisible()) {
            searchBox.render(context, mouseX, mouseY, delta);
        }

        int itemYStart = isSearchTab ? LEFT_ITEM_Y + 20 : LEFT_ITEM_Y;

        // 상단 텍스트
        String titleLeft = tabs.get(SELECTED_TAB_INDEX).getDisplayName().getString();
        int titleLeftX = LEFT_CENTER - (textRenderer.getWidth(titleLeft) / 2);
        context.drawText(textRenderer, titleLeft, titleLeftX, TITLE_Y, 0xFFFFFF, true);

        // 아이템 그리기
        int totalRowWidthLeft = ITEMS_PER_ROW * ITEM_SPACE_LEFT;                     // 가로 길이
        int itemXStartLeft = LEFT_CENTER - totalRowWidthLeft / 2;

        int leftRowsVisible = (this.height - itemYStart - 5) / ITEM_SPACE_LEFT;    // row 개수
        int leftStartIdx = LEFT_SCROLL_OFFSET * ITEMS_PER_ROW;

        LEFT_HOVERED_STACK = ItemStack.EMPTY;

        for (int i = 0; i < leftRowsVisible * ITEMS_PER_ROW && leftStartIdx + i < VISIBLE_ITEMS_LEFT.size(); i++) {
            ItemStack stack = VISIBLE_ITEMS_LEFT.get(leftStartIdx + i);
            int drawX = itemXStartLeft + (i % ITEMS_PER_ROW) * ITEM_SPACE_LEFT;
            int drawY = itemYStart + (i / ITEMS_PER_ROW) * ITEM_SPACE_LEFT;

            context.drawItem(stack, drawX, drawY);
            context.drawStackOverlay(textRenderer, stack, drawX, drawY, null);

            // 마우스가 이 아이템 위에 있을 때
            if (mouseX >= drawX && mouseX < drawX + 16 &&
                    mouseY >= drawY && mouseY < drawY + 16) {
                LEFT_HOVERED_STACK = stack;
            }
        }

        // 툴팁 표시
        if (!LEFT_HOVERED_STACK.isEmpty()) {
            context.drawItemTooltip(textRenderer, LEFT_HOVERED_STACK, mouseX, mouseY);
        }


        // 오른쪽 화면
        context.fill(RIGHT_CENTER - this.width * RIGHT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2 + PADDING,
                PADDING,
                RIGHT_CENTER + this.width * RIGHT_RATIO / (LEFT_RATIO + RIGHT_RATIO) / 2 - PADDING,
                this.height - PADDING, 0x88000000);

        // 상단 텍스트
        String titleRight = Text.translatable("gui.item_checklist.checklist").getString();
        int titleRightX = RIGHT_CENTER - (textRenderer.getWidth(titleRight) / 2);
        context.drawText(textRenderer, titleRight, titleRightX, TITLE_Y, 0xFFFFFF, true);

        // 아이템 그리기
        int totalRowWidthRight = ITEMS_PER_ROW * ITEM_SPACE_RIGHT;                  // 가로 길이
        int itemXStartRight = RIGHT_CENTER - totalRowWidthRight / 2;

        int rightRowsVisible = (this.height - RIGHT_ITEM_Y - 5) / ITEM_SPACE_RIGHT;   // row 개수
        int rightStartIdx = RIGHT_SCROLL_OFFSET * ITEMS_PER_ROW;

        RIGHT_HOVERED_STACK = ItemStack.EMPTY;                       // 마우스가 올려진 아이템

        for (int i = 0; i < rightRowsVisible * ITEMS_PER_ROW && rightStartIdx + i < VISIBLE_ITEMS_RIGHT.size(); i++) {
            ItemStack stack = VISIBLE_ITEMS_RIGHT.get(rightStartIdx + i);
            int drawX = itemXStartRight + (i % ITEMS_PER_ROW) * ITEM_SPACE_RIGHT;
            int drawY = RIGHT_ITEM_Y + (i / ITEMS_PER_ROW) * ITEM_SPACE_RIGHT;

            context.drawItem(stack, drawX, drawY);
            context.drawStackOverlay(textRenderer, stack, drawX, drawY, null);

            // 마우스가 이 아이템 위에 있을 때
            if (mouseX >= drawX && mouseX < drawX + 16 &&
                    mouseY >= drawY && mouseY < drawY + 16) {
                RIGHT_HOVERED_STACK = stack;
            }
        }

        // 툴팁 표시
        if (!RIGHT_HOVERED_STACK.isEmpty()) {
            context.drawItemTooltip(textRenderer, RIGHT_HOVERED_STACK, mouseX, mouseY);
        }
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

        if (tabs.get(SELECTED_TAB_INDEX) == SEARCH_RESULT_TAB) {
            VISIBLE_ITEMS_LEFT.clear();
            VISIBLE_ITEMS_LEFT.addAll(SEARCH_RESULTS);
            LEFT_SCROLL_OFFSET = 0;
        }

        if (MinecraftClient.getInstance().world != null && MinecraftClient.getInstance().player != null) {
            FeatureSet features = MinecraftClient.getInstance().world.getEnabledFeatures();
            boolean showOpTab = MinecraftClient.getInstance().player.isCreativeLevelTwoOp();
            RegistryWrapper.WrapperLookup registries = MinecraftClient.getInstance().world.getRegistryManager();
            ItemGroups.updateDisplayContext(features, showOpTab, registries);
        }
    }

    private void selectTab(int index) {
        VISIBLE_ITEMS_LEFT.clear();
        if (searchBox != null) {
            searchBox.setText("");
            searchBox.setFocused(false);
        }

        boolean isSearchTab = tabs.get(index) == SEARCH_RESULT_TAB;

        if (isSearchTab) {
            VISIBLE_ITEMS_LEFT.addAll(SEARCH_RESULTS);

            if (searchBox == null) {
                searchBox = new TextFieldWidget(
                        textRenderer,
                        LEFT_CENTER - this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) * 7 / 10 / 2 - 1,
                        SEARCH_BOX_Y,
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
            VISIBLE_ITEMS_LEFT.addAll(tabs.get(index).getDisplayStacks());

            // 검색 탭이 아니면 숨김
            if (searchBox != null) {
                searchBox.setVisible(false);
            }
        }

        LEFT_SCROLL_OFFSET = 0; // 탭 전환 시 스크롤 초기화
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        boolean isSearchTab = tabs.get(SELECTED_TAB_INDEX) == SEARCH_RESULT_TAB;

        int leftYStart = isSearchTab ? LEFT_ITEM_Y + 20 : LEFT_ITEM_Y;
        int leftYEnd = this.height;

        int leftXStart = LEFT_CENTER - (ITEMS_PER_ROW * ITEM_SPACE_LEFT) / 2;
        int leftXEnd = leftXStart + ITEMS_PER_ROW * ITEM_SPACE_LEFT;

        // 마우스가 왼쪽 아이템 영역 안에 있을 때 스크롤 적용
        if (mouseX >= leftXStart && mouseX < leftXEnd &&
                mouseY >= leftYStart && mouseY < leftYEnd) {

            int totalItems = VISIBLE_ITEMS_LEFT.size();
            int rowsVisible = (this.height - leftYStart - 5) / ITEM_SPACE_LEFT;
            int maxScroll = Math.max(0, (totalItems / ITEMS_PER_ROW) - rowsVisible);

            LEFT_SCROLL_OFFSET = MathHelper.clamp(LEFT_SCROLL_OFFSET - (int) verticalAmount, 0, maxScroll);
            return true;
        }

        int rightYStart = RIGHT_ITEM_Y;
        int rightYEnd = this.height;

        int rightXStart = RIGHT_CENTER - (ITEMS_PER_ROW * ITEM_SPACE_RIGHT) / 2;
        int rightXEnd = rightXStart + ITEMS_PER_ROW * ITEM_SPACE_RIGHT;

        // 마우스가 오른쪽 아이템 영역 안에 있을 때 스크롤 적용
        if (mouseX >= rightXStart && mouseX < rightXEnd &&
                mouseY >= rightYStart && mouseY < rightYEnd) {

            int totalItems = VISIBLE_ITEMS_LEFT.size();
            int rowsVisible = (this.height - rightYStart - 5) / ITEM_SPACE_RIGHT;
            int maxScroll = Math.max(0, (totalItems / ITEMS_PER_ROW) - rowsVisible);

            RIGHT_SCROLL_OFFSET = MathHelper.clamp(RIGHT_SCROLL_OFFSET - (int) verticalAmount, 0, maxScroll);
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

            if (!LEFT_HOVERED_STACK.isEmpty()) {
                boolean shiftDown = hasShiftDown();
                if (client != null && client.player != null && client.player instanceof ItemChecklistProvider provider) {
                    ItemStack toAdd = LEFT_HOVERED_STACK.copy();
                    toAdd.setCount(shiftDown ? toAdd.getMaxCount() : 1);
                    provider.addItemToChecklist(toAdd);
                    System.out.println(toAdd.getName().getString() + " / " + toAdd.getCount());
                }

                if (searchBox != null) {
                    searchBox.setFocused(false);
                }
                return true;
            }

            if (!RIGHT_HOVERED_STACK.isEmpty()) {
                boolean shiftDown = hasShiftDown();
                if (client != null && client.player != null && client.player instanceof ItemChecklistProvider provider) {
                    ItemStack toRemove = RIGHT_HOVERED_STACK.copy();
                    toRemove.setCount(shiftDown ? toRemove.getMaxCount() : 1);
                    provider.removeItemFromChecklist(toRemove);
                    System.out.println(toRemove.getName().getString() + " / " + toRemove.getCount());
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
            searchBox.setY(SEARCH_BOX_Y); // 고정된 y 값 유지
            searchBox.setWidth(this.width * LEFT_RATIO / (LEFT_RATIO + RIGHT_RATIO) * 7 / 10);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

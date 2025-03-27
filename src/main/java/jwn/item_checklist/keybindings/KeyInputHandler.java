package jwn.item_checklist.keybindings;

import jwn.item_checklist.ItemChecklistClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeyInputHandler {
    public static final String KEY_CATEGORY_KEY_BINDINGS = "key.category." + ItemChecklistClient.MOD_ID + ".key_bindings";
    public static final String KEY_OPEN_CHECKLIST = "key." + ItemChecklistClient.MOD_ID + ".open_checklist";

    public static KeyBinding OpenChecklistKey;

    public static void registerKeyInputs() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (OpenChecklistKey.wasPressed()) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("checklist open!"), false);
                }
            }
        });
    }

    public static void register() {
        OpenChecklistKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_OPEN_CHECKLIST,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KEY_CATEGORY_KEY_BINDINGS
        ));

        registerKeyInputs();
    }
}

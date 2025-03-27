package jwn.item_checklist;

import jwn.item_checklist.keybindings.KeyInputHandler;
import net.fabricmc.api.ClientModInitializer;

public class ItemChecklistClient implements ClientModInitializer {
	public static final String MOD_ID = "item_checklist";

	@Override
	public void onInitializeClient() {
		KeyInputHandler.register();
	}
}
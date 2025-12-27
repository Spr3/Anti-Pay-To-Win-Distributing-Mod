// Sapire: https://www.youtube.com/@Sapire
// Last Updated: 12/27/2025
// ANTI PAY TO WIN PAY ALL BUY ALL

package sapire.payeveryone;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class PayEveryone implements ModInitializer {
	public static final String MOD_ID = "payeveryone";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	MinecraftClient client = MinecraftClient.getInstance();
	List<String> players = new ArrayList<>();

	private int monievalue = 0,index = 0,tickcounting = 0;
	private int tickwait = 20;
	private boolean STOPPaying = false, STOPAHG = false;

	@Override
	public void onInitialize() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("PayAll").then(argument("value", IntegerArgumentType.integer()).executes(context -> {
			monievalue = IntegerArgumentType.getInteger(context, "value");
			if (STOPPaying) {
				STOPPaying = false;
			}
			else {
				grabplayers();
			}
			return 1;
		})));});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("BuyOutAuctionHouse").executes(context -> {
			BuyOutAH();
			return 1;
		}));});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("STOPWHATDOING").executes(context -> {
			STOPPaying = true;
			STOPAHG = true;
			return 1;
		}));});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("ChangeTicks").then(argument("value", IntegerArgumentType.integer()).executes(context -> {
			tickwait = IntegerArgumentType.getInteger(context, "value");
			tickcounting = 0;
			return 1;
		})));});
	}

	private void paypeople(String element) {
		client.player.networkHandler.sendChatCommand("pay " + element + " " + (int) Math.floor(monievalue/client.getNetworkHandler().getPlayerList().size()));
	}

	private void grabplayers() {
		client.getNetworkHandler().getPlayerList().forEach(entry -> {
			players.add(entry.getProfile().name());
		});
		loop();
	}
	//loop for pay all
	private void loop() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (STOPPaying) {
				return;
			}
			tickcounting++;
			if (tickcounting >= tickwait) {
				tickcounting = 0;
				paypeople(players.get(index));
				index++;
			}
		});
	}


	// \/ BUYING OUT THE AH!!
	private void BuyOutAH() {
		client.player.networkHandler.sendChatCommand("ah");
		AHloop();
	}

	private void AHloop() {
		ClientPlayerEntity player = client.player;
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (STOPAHG) {
				return;
			}
			tickcounting++;
			if (tickcounting >= tickwait) {
				tickcounting = 0;
				try {
					client.interactionManager.clickSlot(player.currentScreenHandler.syncId, 0, 0,SlotActionType.PICKUP,player);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
// Sapire: https://www.youtube.com/@Sapire
// Last Updated: 12/27/2025
// ANTI PAY TO WIN PAY ALL BUY ALL

package sapire.payeveryone;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class PayEveryone implements ModInitializer {
	public static final String MOD_ID = "payeveryone";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	MinecraftClient client = MinecraftClient.getInstance();
	List<String> players = new ArrayList<>();
	List<String> ignoreplayers = new ArrayList<>();

	private int monievalue = 0,index = 0,tickcounting = 0;
	private int tickwait = 20;
	private boolean STOPPaying = false, STOPAHG = false, STOPTPA = false;

	private boolean pickeditem = false;
	private int positionAH = 0, positionBuyfromah = 0;

	Item theitem = Items.AIR;
	private int sameitemcoundter = 1;

	@Override
	public void onInitialize() {
		//Pay All
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("PayAll").then(argument("value", IntegerArgumentType.integer()).executes(context -> {
			monievalue = IntegerArgumentType.getInteger(context, "value");
			if (STOPPaying) {
				STOPPaying = false;
			}
			else {
				grabplayers();
				loop();
			}
			return 1;
		})));});
		//BuyOutAuctionHouse
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("BuyOutAuctionHouse").executes(context -> {
			STOPAHG = false;
			sameitemcoundter = 1;
			BuyOutAH();
			return 1;
		}));});
		//STOP WHAT ITS DOING
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("STOPWHATDOING").executes(context -> {
			STOPPaying = true;
			STOPAHG = true;
			STOPTPA = true;
			return 1;
		}));});
		//ChangeTicks
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("ChangeTicks").then(argument("value", IntegerArgumentType.integer()).executes(context -> {
			tickwait = IntegerArgumentType.getInteger(context, "value");
			tickcounting = 0;
			return 1;
		})));});
		//SetUpAuctionHouseBuying
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("SetUpAuctionHouseBuying").then(argument("first position", IntegerArgumentType.integer()).then(argument("second position", IntegerArgumentType.integer()).executes(context -> {
			positionAH = IntegerArgumentType.getInteger(context, "first position");
			positionBuyfromah = IntegerArgumentType.getInteger(context, "second position");
			return 1;
		}))));});
		//Add players to Ignored Players
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("Ignore").then(argument("Player", StringArgumentType.string()).executes(context -> {
			ignoreplayers.add(StringArgumentType.getString(context, "Player"));
			return 1;
		})));});
		//Shows ignored players
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("IgnoredPlayers").executes(context -> {
			printingnoredplayers();
			return 1;
		}));});
		//TELEPORT TO PLAYERS
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {dispatcher.register(literal("TELEPORTTOPLAYERS").executes(context -> {
			STOPTPA = false;
			teletoplayers();
			return 1;
		}));});
	}

	private void teletoplayers() {
		grabplayers();
		//every tick
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (STOPTPA) {
				return;
			}
			//every 20 ticks
			tickcounting++;
			if (tickcounting >= tickwait) {
				//reset the tick counter to 0, sendtelerequest with the player, get read for the next player
				tickcounting = 0;
				sendtelerequest(players.get(index));
				index++;
			}
		});
	}

	private void sendtelerequest(String element) {
		//if it has something other than a number or character skip
		//if players is in IGNORED PLAYERS skip
		if (checkisrealplayer(element) && !shouldskipplayer(element)) {
			//sends tp request
            assert client.player != null;
            client.player.networkHandler.sendChatCommand("tpa " + element);
		}
	}

	//shows ignored players
	private void printingnoredplayers() {
		StringBuilder print = new StringBuilder("Ignored Players: ");
        for (String ignoreplayer : ignoreplayers) {
            print.append(ignoreplayer).append(", ");
        }
        assert client.player != null;
        client.player.sendMessage(Text.of(print.toString()), false);
	}

	//reset the tick counter to 0, sendtelerequest with the player, get read for the next player
	private void paypeople(String element) {
		if (checkisrealplayer(element) && !shouldskipplayer(element)) {
			assert client.player != null;
			client.player.networkHandler.sendChatCommand("pay " + element + " " + (int) (double) (monievalue / Objects.requireNonNull(client.getNetworkHandler()).getPlayerList().size()));
		}
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
        assert client.player != null;
        client.player.networkHandler.sendChatCommand("ah");
		AHloop();
	}

	private void AHloop() {
		ClientPlayerEntity player = client.player;
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (STOPAHG || isInventoryFull()) {
				return;
			}
			tickcounting++;
			if (tickcounting >= tickwait) {
				tickcounting = 0;
				try {
					if (!pickeditem){
                        assert client.player != null;
                        ItemStack slotposition = client.player.getInventory().getStack(positionAH);
						if (theitem == slotposition.getItem()) {
							sameitemcoundter++;
							if (sameitemcoundter >= 5) {
								STOPAHG = true;
							}
						} else {
							theitem = slotposition.getItem();
							sameitemcoundter = 1;
						}
						pickeditem = true;
                        assert client.interactionManager != null;
                        assert player != null;
                        client.interactionManager.clickSlot(player.currentScreenHandler.syncId, positionAH, 0,SlotActionType.PICKUP,player);
					}
					else {
						pickeditem = false;
                        assert client.interactionManager != null;
                        assert player != null;
                        client.interactionManager.clickSlot(player.currentScreenHandler.syncId, positionBuyfromah, 0,SlotActionType.PICKUP,player);
                        assert client.player != null;
                        client.player.networkHandler.sendChatCommand("ah");
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public boolean isInventoryFull() {
		if (client.player != null) {
			for (int slot = 9; slot < 34; slot++) {
				ItemStack slotposition = client.player.getInventory().getStack(slot);
				if (slotposition.getItem() == Items.AIR){
					return false;
				}
			}
		}
		STOPAHG = true;
		return true;
	}

	private boolean shouldskipplayer(String player) {
        for (String ignoreplayer : ignoreplayers) {
            if (Objects.equals(player, ignoreplayer)) {
                return true;
            }
        }
		return false;
	}

	private boolean checkisrealplayer(String player) {
		Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");
		Matcher matcher = pattern.matcher(player);
        return !matcher.find();
	}

	private void grabplayers() {
		players.clear();
		Objects.requireNonNull(client.getNetworkHandler()).getPlayerList().forEach(entry -> {
			players.add(entry.getProfile().name());
		});
	}
}

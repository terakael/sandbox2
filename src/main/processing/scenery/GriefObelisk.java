package main.processing.scenery;

import main.processing.attackable.Player;
import main.requests.UseRequest;
import main.responses.ResponseMaps;
import main.types.Items;

public class GriefObelisk extends Obelisk {
	public GriefObelisk() {
		enchantChance = 3;
	}

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		final int slot = request.getSlot();
		
		Items srcItem = Items.withValue(srcItemId); 
		if (srcItem == null)
			return false;
		
		switch (srcItem) {
		case COPPER_SWORD_V:
			attemptToEnchant(Items.COPPER_SWORD_V, Items.COPPER_SWORD_VI, slot, player, responseMaps);
			return true;
		case COPPER_DAGGERS_V:
			attemptToEnchant(Items.COPPER_DAGGERS_V, Items.COPPER_DAGGERS_VI, slot, player, responseMaps);
			return true;
		case COPPER_HAMMER_V:
			attemptToEnchant(Items.COPPER_HAMMER_V, Items.COPPER_HAMMER_VI, slot, player, responseMaps);
			return true;
		case IRON_SWORD_V:
			attemptToEnchant(Items.IRON_SWORD_V, Items.IRON_SWORD_VI, slot, player, responseMaps);
			return true;
		case IRON_DAGGERS_V:
			attemptToEnchant(Items.IRON_DAGGERS_V, Items.IRON_DAGGERS_VI, slot, player, responseMaps);
			return true;
		case IRON_HAMMER_V:
			attemptToEnchant(Items.IRON_HAMMER_V, Items.IRON_HAMMER_VI, slot, player, responseMaps);
			return true;
		case STEEL_SWORD_V:
			attemptToEnchant(Items.STEEL_SWORD_V, Items.STEEL_SWORD_VI, slot, player, responseMaps);
			return true;
		case STEEL_DAGGERS_V:
			attemptToEnchant(Items.STEEL_DAGGERS_V, Items.STEEL_DAGGERS_VI, slot, player, responseMaps);
			return true;
		case STEEL_HAMMER_V:
			attemptToEnchant(Items.STEEL_HAMMER_V, Items.STEEL_HAMMER_VI, slot, player, responseMaps);
			return true;
		case MITHRIL_SWORD_V:
			attemptToEnchant(Items.MITHRIL_SWORD_V, Items.MITHRIL_SWORD_VI, slot, player, responseMaps);
			return true;
		case MITHRIL_DAGGERS_V:
			attemptToEnchant(Items.MITHRIL_DAGGERS_V, Items.MITHRIL_DAGGERS_VI, slot, player, responseMaps);
			return true;
		case MITHRIL_HAMMER_V:
			attemptToEnchant(Items.MITHRIL_HAMMER_V, Items.MITHRIL_HAMMER_VI, slot, player, responseMaps);
			return true;
		case ADDY_SWORD_V:
			attemptToEnchant(Items.ADDY_SWORD_V, Items.ADDY_SWORD_VI, slot, player, responseMaps);
			return true;
		case ADDY_DAGGERS_V:
			attemptToEnchant(Items.ADDY_DAGGERS_V, Items.ADDY_DAGGERS_VI, slot, player, responseMaps);
			return true;
		case ADDY_HAMMER_V:
			attemptToEnchant(Items.ADDY_HAMMER_V, Items.ADDY_HAMMER_VI, slot, player, responseMaps);
			return true;
		case RUNE_SWORD_V:
			attemptToEnchant(Items.RUNE_SWORD_V, Items.RUNE_SWORD_VI, slot, player, responseMaps);
			return true;
		case RUNE_DAGGERS_V:
			attemptToEnchant(Items.RUNE_DAGGERS_V, Items.RUNE_DAGGERS_VI, slot, player, responseMaps);
			return true;
		case RUNE_HAMMER_V:
			attemptToEnchant(Items.RUNE_HAMMER_V, Items.RUNE_HAMMER_VI, slot, player, responseMaps);
			return true;
		case DRAGON_SWORD_V:
			attemptToEnchant(Items.DRAGON_SWORD_V, Items.DRAGON_SWORD_VI, slot, player, responseMaps);
			return true;
		default:
			break;
		}
		return false;
	}

}

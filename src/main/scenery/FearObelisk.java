package main.scenery;

import main.processing.Player;
import main.responses.ResponseMaps;
import main.types.Items;

public class FearObelisk extends Obelisk {
	public FearObelisk() {
		enchantChance = 6;
	}

	@Override
	public boolean use(int srcItemId, int slot, Player player, ResponseMaps responseMaps) {
		Items srcItem = Items.withValue(srcItemId); 
		if (srcItem == null)
			return false;
		
		switch (srcItem) {
		case COPPER_SWORD_IV:
			attemptToEnchant(Items.COPPER_SWORD_IV, Items.COPPER_SWORD_V, slot, player, responseMaps);
			return true;
		case COPPER_DAGGERS_IV:
			attemptToEnchant(Items.COPPER_DAGGERS_IV, Items.COPPER_DAGGERS_V, slot, player, responseMaps);
			return true;
		case COPPER_HAMMER_IV:
			attemptToEnchant(Items.COPPER_HAMMER_IV, Items.COPPER_HAMMER_V, slot, player, responseMaps);
			return true;
		case IRON_SWORD_IV:
			attemptToEnchant(Items.IRON_SWORD_IV, Items.IRON_SWORD_V, slot, player, responseMaps);
			return true;
		case IRON_DAGGERS_IV:
			attemptToEnchant(Items.IRON_DAGGERS_IV, Items.IRON_DAGGERS_V, slot, player, responseMaps);
			return true;
		case IRON_HAMMER_IV:
			attemptToEnchant(Items.IRON_HAMMER_IV, Items.IRON_HAMMER_V, slot, player, responseMaps);
			return true;
		case STEEL_SWORD_IV:
			attemptToEnchant(Items.STEEL_SWORD_IV, Items.STEEL_SWORD_V, slot, player, responseMaps);
			return true;
		case STEEL_DAGGERS_IV:
			attemptToEnchant(Items.STEEL_DAGGERS_IV, Items.STEEL_DAGGERS_V, slot, player, responseMaps);
			return true;
		case STEEL_HAMMER_IV:
			attemptToEnchant(Items.STEEL_HAMMER_IV, Items.STEEL_HAMMER_V, slot, player, responseMaps);
			return true;
		case MITHRIL_SWORD_IV:
			attemptToEnchant(Items.MITHRIL_SWORD_IV, Items.MITHRIL_SWORD_V, slot, player, responseMaps);
			return true;
		case MITHRIL_DAGGERS_IV:
			attemptToEnchant(Items.MITHRIL_DAGGERS_IV, Items.MITHRIL_DAGGERS_V, slot, player, responseMaps);
			return true;
		case MITHRIL_HAMMER_IV:
			attemptToEnchant(Items.MITHRIL_HAMMER_IV, Items.MITHRIL_HAMMER_V, slot, player, responseMaps);
			return true;
		case ADDY_SWORD_IV:
			attemptToEnchant(Items.ADDY_SWORD_IV, Items.ADDY_SWORD_V, slot, player, responseMaps);
			return true;
		case ADDY_DAGGERS_IV:
			attemptToEnchant(Items.ADDY_DAGGERS_IV, Items.ADDY_DAGGERS_V, slot, player, responseMaps);
			return true;
		case ADDY_HAMMER_IV:
			attemptToEnchant(Items.ADDY_HAMMER_IV, Items.ADDY_HAMMER_V, slot, player, responseMaps);
			return true;
		case RUNE_SWORD_IV:
			attemptToEnchant(Items.RUNE_SWORD_IV, Items.RUNE_SWORD_V, slot, player, responseMaps);
			return true;
		case RUNE_DAGGERS_IV:
			attemptToEnchant(Items.RUNE_DAGGERS_IV, Items.RUNE_DAGGERS_V, slot, player, responseMaps);
			return true;
		case RUNE_HAMMER_IV:
			attemptToEnchant(Items.RUNE_HAMMER_IV, Items.RUNE_HAMMER_V, slot, player, responseMaps);
			return true;
		default:
			break;
		}
		return false;
	}
}

package main.scenery;

import main.processing.Player;
import main.requests.UseRequest;
import main.responses.ResponseMaps;
import main.types.Items;

public class ShameObelisk extends Obelisk {
	public ShameObelisk() {
		enchantChance = 12;
	}

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		final int slot = request.getSlot();
		
		Items srcItem = Items.withValue(srcItemId); 
		if (srcItem == null)
			return false;
		
		switch (srcItem) {
		case COPPER_SWORD_III:
			attemptToEnchant(Items.COPPER_SWORD_III, Items.COPPER_SWORD_IV, slot, player, responseMaps);
			return true;
		case COPPER_DAGGERS_III:
			attemptToEnchant(Items.COPPER_DAGGERS_III, Items.COPPER_DAGGERS_IV, slot, player, responseMaps);
			return true;
		case COPPER_HAMMER_III:
			attemptToEnchant(Items.COPPER_HAMMER_III, Items.COPPER_HAMMER_IV, slot, player, responseMaps);
			return true;
		case IRON_SWORD_III:
			attemptToEnchant(Items.IRON_SWORD_III, Items.IRON_SWORD_IV, slot, player, responseMaps);
			return true;
		case IRON_DAGGERS_III:
			attemptToEnchant(Items.IRON_DAGGERS_III, Items.IRON_DAGGERS_IV, slot, player, responseMaps);
			return true;
		case IRON_HAMMER_III:
			attemptToEnchant(Items.IRON_HAMMER_III, Items.IRON_HAMMER_IV, slot, player, responseMaps);
			return true;
		case STEEL_SWORD_III:
			attemptToEnchant(Items.STEEL_SWORD_III, Items.STEEL_SWORD_IV, slot, player, responseMaps);
			return true;
		case STEEL_DAGGERS_III:
			attemptToEnchant(Items.STEEL_DAGGERS_III, Items.STEEL_DAGGERS_IV, slot, player, responseMaps);
			return true;
		case STEEL_HAMMER_III:
			attemptToEnchant(Items.STEEL_HAMMER_III, Items.STEEL_HAMMER_IV, slot, player, responseMaps);
			return true;
		case MITHRIL_SWORD_III:
			attemptToEnchant(Items.MITHRIL_SWORD_III, Items.MITHRIL_SWORD_IV, slot, player, responseMaps);
			return true;
		case MITHRIL_DAGGERS_III:
			attemptToEnchant(Items.MITHRIL_DAGGERS_III, Items.MITHRIL_DAGGERS_IV, slot, player, responseMaps);
			return true;
		case MITHRIL_HAMMER_III:
			attemptToEnchant(Items.MITHRIL_HAMMER_III, Items.MITHRIL_HAMMER_IV, slot, player, responseMaps);
			return true;
		case ADDY_SWORD_III:
			attemptToEnchant(Items.ADDY_SWORD_III, Items.ADDY_SWORD_IV, slot, player, responseMaps);
			return true;
		case ADDY_DAGGERS_III:
			attemptToEnchant(Items.ADDY_DAGGERS_III, Items.ADDY_DAGGERS_IV, slot, player, responseMaps);
			return true;
		case ADDY_HAMMER_III:
			attemptToEnchant(Items.ADDY_HAMMER_III, Items.ADDY_HAMMER_IV, slot, player, responseMaps);
			return true;
		case RUNE_SWORD_III:
			attemptToEnchant(Items.RUNE_SWORD_III, Items.RUNE_SWORD_IV, slot, player, responseMaps);
			return true;
		case RUNE_DAGGERS_III:
			attemptToEnchant(Items.RUNE_DAGGERS_III, Items.RUNE_DAGGERS_IV, slot, player, responseMaps);
			return true;
		case RUNE_HAMMER_III:
			attemptToEnchant(Items.RUNE_HAMMER_III, Items.RUNE_HAMMER_IV, slot, player, responseMaps);
			return true;
		case DRAGON_SWORD_III:
			attemptToEnchant(Items.DRAGON_SWORD_III, Items.DRAGON_SWORD_IV, slot, player, responseMaps);
			return true;
		default:
			break;
		}
		return false;
	}
}

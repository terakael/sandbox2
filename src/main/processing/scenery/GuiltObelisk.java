package main.processing.scenery;

import main.processing.Player;
import main.requests.UseRequest;
import main.responses.ResponseMaps;
import main.types.Items;

public class GuiltObelisk extends Obelisk {
	public GuiltObelisk() {
		enchantChance = 25;
	}

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		final int slot = request.getSlot();
		
		Items srcItem = Items.withValue(srcItemId); 
		if (srcItem == null)
			return false;
		
		switch (srcItem) {
		case COPPER_SWORD_II:
			attemptToEnchant(Items.COPPER_SWORD_II, Items.COPPER_SWORD_III, slot, player, responseMaps);
			return true;
		case COPPER_DAGGERS_II:
			attemptToEnchant(Items.COPPER_DAGGERS_II, Items.COPPER_DAGGERS_III, slot, player, responseMaps);
			return true;
		case COPPER_HAMMER_II:
			attemptToEnchant(Items.COPPER_HAMMER_II, Items.COPPER_HAMMER_III, slot, player, responseMaps);
			return true;
		case IRON_SWORD_II:
			attemptToEnchant(Items.IRON_SWORD_II, Items.IRON_SWORD_III, slot, player, responseMaps);
			return true;
		case IRON_DAGGERS_II:
			attemptToEnchant(Items.IRON_DAGGERS_II, Items.IRON_DAGGERS_III, slot, player, responseMaps);
			return true;
		case IRON_HAMMER_II:
			attemptToEnchant(Items.IRON_HAMMER_II, Items.IRON_HAMMER_III, slot, player, responseMaps);
			return true;
		case STEEL_SWORD_II:
			attemptToEnchant(Items.STEEL_SWORD_II, Items.STEEL_SWORD_III, slot, player, responseMaps);
			return true;
		case STEEL_DAGGERS_II:
			attemptToEnchant(Items.STEEL_DAGGERS_II, Items.STEEL_DAGGERS_III, slot, player, responseMaps);
			return true;
		case STEEL_HAMMER_II:
			attemptToEnchant(Items.STEEL_HAMMER_II, Items.STEEL_HAMMER_III, slot, player, responseMaps);
			return true;
		case MITHRIL_SWORD_II:
			attemptToEnchant(Items.MITHRIL_SWORD_II, Items.MITHRIL_SWORD_III, slot, player, responseMaps);
			return true;
		case MITHRIL_DAGGERS_II:
			attemptToEnchant(Items.MITHRIL_DAGGERS_II, Items.MITHRIL_DAGGERS_III, slot, player, responseMaps);
			return true;
		case MITHRIL_HAMMER_II:
			attemptToEnchant(Items.MITHRIL_HAMMER_II, Items.MITHRIL_HAMMER_III, slot, player, responseMaps);
			return true;
		case ADDY_SWORD_II:
			attemptToEnchant(Items.ADDY_SWORD_II, Items.ADDY_SWORD_III, slot, player, responseMaps);
			return true;
		case ADDY_DAGGERS_II:
			attemptToEnchant(Items.ADDY_DAGGERS_II, Items.ADDY_DAGGERS_III, slot, player, responseMaps);
			return true;
		case ADDY_HAMMER_II:
			attemptToEnchant(Items.ADDY_HAMMER_II, Items.ADDY_HAMMER_III, slot, player, responseMaps);
			return true;
		case RUNE_SWORD_II:
			attemptToEnchant(Items.RUNE_SWORD_II, Items.RUNE_SWORD_III, slot, player, responseMaps);
			return true;
		case RUNE_DAGGERS_II:
			attemptToEnchant(Items.RUNE_DAGGERS_II, Items.RUNE_DAGGERS_III, slot, player, responseMaps);
			return true;
		case RUNE_HAMMER_II:
			attemptToEnchant(Items.RUNE_HAMMER_II, Items.RUNE_HAMMER_III, slot, player, responseMaps);
			return true;
		case DRAGON_SWORD_II:
			attemptToEnchant(Items.DRAGON_SWORD_II, Items.DRAGON_SWORD_III, slot, player, responseMaps);
			return true;
		default:
			break;
		}
		return false;
	}

}

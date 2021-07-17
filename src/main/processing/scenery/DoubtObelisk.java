package main.processing.scenery;

import main.processing.Player;
import main.requests.UseRequest;
import main.responses.ResponseMaps;
import main.types.Items;

public class DoubtObelisk extends Obelisk {
	public DoubtObelisk() {
		enchantChance = 50;
	}

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		final int slot = request.getSlot();
		
		Items srcItem = Items.withValue(srcItemId); 
		if (srcItem == null)
			return false;
		
		switch (srcItem) {
		case COPPER_SWORD:
			attemptToEnchant(Items.COPPER_SWORD, Items.COPPER_SWORD_II, slot, player, responseMaps);
			return true;
		case COPPER_DAGGERS:
			attemptToEnchant(Items.COPPER_DAGGERS, Items.COPPER_DAGGERS_II, slot, player, responseMaps);
			return true;
		case COPPER_HAMMER:
			attemptToEnchant(Items.COPPER_HAMMER, Items.COPPER_HAMMER_II, slot, player, responseMaps);
			return true;
		case IRON_SWORD:
			attemptToEnchant(Items.IRON_SWORD, Items.IRON_SWORD_II, slot, player, responseMaps);
			return true;
		case IRON_DAGGERS:
			attemptToEnchant(Items.IRON_DAGGERS, Items.IRON_DAGGERS_II, slot, player, responseMaps);
			return true;
		case IRON_HAMMER:
			attemptToEnchant(Items.IRON_HAMMER, Items.IRON_HAMMER_II, slot, player, responseMaps);
			return true;
		case STEEL_SWORD:
			attemptToEnchant(Items.STEEL_SWORD, Items.STEEL_SWORD_II, slot, player, responseMaps);
			return true;
		case STEEL_DAGGERS:
			attemptToEnchant(Items.STEEL_DAGGERS, Items.STEEL_DAGGERS_II, slot, player, responseMaps);
			return true;
		case STEEL_HAMMER:
			attemptToEnchant(Items.STEEL_HAMMER, Items.STEEL_HAMMER_II, slot, player, responseMaps);
			return true;
		case MITHRIL_SWORD:
			attemptToEnchant(Items.MITHRIL_SWORD, Items.MITHRIL_SWORD_II, slot, player, responseMaps);
			return true;
		case MITHRIL_DAGGERS:
			attemptToEnchant(Items.MITHRIL_DAGGERS, Items.MITHRIL_DAGGERS_II, slot, player, responseMaps);
			return true;
		case MITHRIL_HAMMER:
			attemptToEnchant(Items.MITHRIL_HAMMER, Items.MITHRIL_HAMMER_II, slot, player, responseMaps);
			return true;
		case ADDY_SWORD:
			attemptToEnchant(Items.ADDY_SWORD, Items.ADDY_SWORD_II, slot, player, responseMaps);
			return true;
		case ADDY_DAGGERS:
			attemptToEnchant(Items.ADDY_DAGGERS, Items.ADDY_DAGGERS_II, slot, player, responseMaps);
			return true;
		case ADDY_HAMMER:
			attemptToEnchant(Items.ADDY_HAMMER, Items.ADDY_HAMMER_II, slot, player, responseMaps);
			return true;
		case RUNE_SWORD:
			attemptToEnchant(Items.RUNE_SWORD, Items.RUNE_SWORD_II, slot, player, responseMaps);
			return true;
		case RUNE_DAGGERS:
			attemptToEnchant(Items.RUNE_DAGGERS, Items.RUNE_DAGGERS_II, slot, player, responseMaps);
			return true;
		case RUNE_HAMMER:
			attemptToEnchant(Items.RUNE_HAMMER, Items.RUNE_HAMMER_II, slot, player, responseMaps);
			return true;
		case DRAGON_SWORD:
			attemptToEnchant(Items.DRAGON_SWORD, Items.DRAGON_SWORD_II, slot, player, responseMaps);
			return true;
		default:
			break;
		}
		return false;
	}
}

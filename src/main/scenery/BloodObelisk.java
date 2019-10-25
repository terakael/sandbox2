package main.scenery;

import main.processing.Player;
import main.responses.ResponseMaps;
import main.types.Items;

public class BloodObelisk extends Obelisk {
	public BloodObelisk() {
		enchantChance = 1;
	}

	@Override
	public boolean use(int srcItemId, int slot, Player player, ResponseMaps responseMaps) {
		Items srcItem = Items.withValue(srcItemId); 
		if (srcItem == null)
			return false;
		
		switch (srcItem) {
		case COPPER_SWORD_VI:
			attemptToEnchant(Items.COPPER_SWORD_VI, Items.COPPER_SWORD_VII, slot, player, responseMaps);
			return true;
		case COPPER_DAGGERS_VI:
			attemptToEnchant(Items.COPPER_DAGGERS_VI, Items.COPPER_DAGGERS_VII, slot, player, responseMaps);
			return true;
		case COPPER_HAMMER_VI:
			attemptToEnchant(Items.COPPER_HAMMER_VI, Items.COPPER_HAMMER_VII, slot, player, responseMaps);
			return true;
		case IRON_SWORD_VI:
			attemptToEnchant(Items.IRON_SWORD_VI, Items.IRON_SWORD_VII, slot, player, responseMaps);
			return true;
		case IRON_DAGGERS_VI:
			attemptToEnchant(Items.IRON_DAGGERS_VI, Items.IRON_DAGGERS_VII, slot, player, responseMaps);
			return true;
		case IRON_HAMMER_VI:
			attemptToEnchant(Items.IRON_HAMMER_VI, Items.IRON_HAMMER_VII, slot, player, responseMaps);
			return true;
		case STEEL_SWORD_VI:
			attemptToEnchant(Items.STEEL_SWORD_VI, Items.STEEL_SWORD_VII, slot, player, responseMaps);
			return true;
		case STEEL_DAGGERS_VI:
			attemptToEnchant(Items.STEEL_DAGGERS_VI, Items.STEEL_DAGGERS_VII, slot, player, responseMaps);
			return true;
		case STEEL_HAMMER_VI:
			attemptToEnchant(Items.STEEL_HAMMER_VI, Items.STEEL_HAMMER_VII, slot, player, responseMaps);
			return true;
		case MITHRIL_SWORD_VI:
			attemptToEnchant(Items.MITHRIL_SWORD_VI, Items.MITHRIL_SWORD_VII, slot, player, responseMaps);
			return true;
		case MITHRIL_DAGGERS_VI:
			attemptToEnchant(Items.MITHRIL_DAGGERS_VI, Items.MITHRIL_DAGGERS_VII, slot, player, responseMaps);
			return true;
		case MITHRIL_HAMMER_VI:
			attemptToEnchant(Items.MITHRIL_HAMMER_VI, Items.MITHRIL_HAMMER_VII, slot, player, responseMaps);
			return true;
		case ADDY_SWORD_VI:
			attemptToEnchant(Items.ADDY_SWORD_VI, Items.ADDY_SWORD_VII, slot, player, responseMaps);
			return true;
		case ADDY_DAGGERS_VI:
			attemptToEnchant(Items.ADDY_DAGGERS_VI, Items.ADDY_DAGGERS_VII, slot, player, responseMaps);
			return true;
		case ADDY_HAMMER_VI:
			attemptToEnchant(Items.ADDY_HAMMER_VI, Items.ADDY_HAMMER_VII, slot, player, responseMaps);
			return true;
		case RUNE_SWORD_VI:
			attemptToEnchant(Items.RUNE_SWORD_VI, Items.RUNE_SWORD_VII, slot, player, responseMaps);
			return true;
		case RUNE_DAGGERS_VI:
			attemptToEnchant(Items.RUNE_DAGGERS_VI, Items.RUNE_DAGGERS_VII, slot, player, responseMaps);
			return true;
		case RUNE_HAMMER_VI:
			attemptToEnchant(Items.RUNE_HAMMER_VI, Items.RUNE_HAMMER_VII, slot, player, responseMaps);
			return true;
		case DRAGON_SWORD_VI:
			attemptToEnchant(Items.DRAGON_SWORD_VI, Items.DRAGON_SWORD_VII, slot, player, responseMaps);
			return true;
		default:
			break;
		}
		return false;
	}
}

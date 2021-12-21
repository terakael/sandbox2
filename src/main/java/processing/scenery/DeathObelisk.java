package processing.scenery;

import processing.attackable.Player;
import requests.UseRequest;
import responses.ResponseMaps;
import types.Items;

public class DeathObelisk extends Obelisk{
	public DeathObelisk() {
		enchantChance = 1;
	}

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		final int slot = request.getSlot();
		
		Items srcItem = Items.withValue(srcItemId); 
		if (srcItem == null)
			return false;
		
		switch (srcItem) {
		case COPPER_SWORD_VII:
			attemptToEnchant(Items.COPPER_SWORD_VII, Items.COPPER_SWORD_VIII, slot, player, responseMaps);
			return true;
		case COPPER_DAGGERS_VII:
			attemptToEnchant(Items.COPPER_DAGGERS_VII, Items.COPPER_DAGGERS_VIII, slot, player, responseMaps);
			return true;
		case COPPER_HAMMER_VII:
			attemptToEnchant(Items.COPPER_HAMMER_VII, Items.COPPER_HAMMER_VIII, slot, player, responseMaps);
			return true;
		case IRON_SWORD_VII:
			attemptToEnchant(Items.IRON_SWORD_VII, Items.IRON_SWORD_VIII, slot, player, responseMaps);
			return true;
		case IRON_DAGGERS_VII:
			attemptToEnchant(Items.IRON_DAGGERS_VII, Items.IRON_DAGGERS_VIII, slot, player, responseMaps);
			return true;
		case IRON_HAMMER_VII:
			attemptToEnchant(Items.IRON_HAMMER_VII, Items.IRON_HAMMER_VIII, slot, player, responseMaps);
			return true;
		case STEEL_SWORD_VII:
			attemptToEnchant(Items.STEEL_SWORD_VII, Items.STEEL_SWORD_VIII, slot, player, responseMaps);
			return true;
		case STEEL_DAGGERS_VII:
			attemptToEnchant(Items.STEEL_DAGGERS_VII, Items.STEEL_DAGGERS_VIII, slot, player, responseMaps);
			return true;
		case STEEL_HAMMER_VII:
			attemptToEnchant(Items.STEEL_HAMMER_VII, Items.STEEL_HAMMER_VIII, slot, player, responseMaps);
			return true;
		case MITHRIL_SWORD_VII:
			attemptToEnchant(Items.MITHRIL_SWORD_VII, Items.MITHRIL_SWORD_VIII, slot, player, responseMaps);
			return true;
		case MITHRIL_DAGGERS_VII:
			attemptToEnchant(Items.MITHRIL_DAGGERS_VII, Items.MITHRIL_DAGGERS_VIII, slot, player, responseMaps);
			return true;
		case MITHRIL_HAMMER_VII:
			attemptToEnchant(Items.MITHRIL_HAMMER_VII, Items.MITHRIL_HAMMER_VIII, slot, player, responseMaps);
			return true;
		case ADDY_SWORD_VII:
			attemptToEnchant(Items.ADDY_SWORD_VII, Items.ADDY_SWORD_VIII, slot, player, responseMaps);
			return true;
		case ADDY_DAGGERS_VII:
			attemptToEnchant(Items.ADDY_DAGGERS_VII, Items.ADDY_DAGGERS_VIII, slot, player, responseMaps);
			return true;
		case ADDY_HAMMER_VII:
			attemptToEnchant(Items.ADDY_HAMMER_VII, Items.ADDY_HAMMER_VIII, slot, player, responseMaps);
			return true;
		case RUNE_SWORD_VII:
			attemptToEnchant(Items.RUNE_SWORD_VII, Items.RUNE_SWORD_VIII, slot, player, responseMaps);
			return true;
		case RUNE_DAGGERS_VII:
			attemptToEnchant(Items.RUNE_DAGGERS_VII, Items.RUNE_DAGGERS_VIII, slot, player, responseMaps);
			return true;
		case RUNE_HAMMER_VII:
			attemptToEnchant(Items.RUNE_HAMMER_VII, Items.RUNE_HAMMER_VIII, slot, player, responseMaps);
			return true;
		case DRAGON_SWORD_VII:
			attemptToEnchant(Items.DRAGON_SWORD_VII, Items.DRAGON_SWORD_VIII, slot, player, responseMaps);
			return true;
		default:
			break;
		}
		return false;
	}
	
}

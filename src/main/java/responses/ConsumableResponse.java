package responses;

import java.util.HashMap;

import database.dao.ConsumableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.StatsDao;
import database.dto.ConsumableEffectsDto;
import processing.attackable.Player;
import processing.managers.FightManager;
import requests.ConsumableRequest;
import requests.Request;
import requests.RequestFactory;
import types.Buffs;
import types.Items;
import types.Stats;
import types.StorageTypes;

public abstract class ConsumableResponse extends Response {
	public ConsumableResponse() {
		setCombatInterrupt(false); // cannot eat/drink during combat, and it doesn't interrupt
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ConsumableRequest))
			return;
		
//		if (FightManager.fightWithFighterExists(player)) {
//			setRecoAndResponseText(0, "you can't do that during combat.");
//			responseMaps.addClientOnlyResponse(player, this);
//			return;
//		}
		
		int slot = ((ConsumableRequest)req).getSlot();
		int itemId = ((ConsumableRequest)req).getObjectId();

		if (!ConsumableDao.isConsumable(itemId)) {
			setRecoAndResponseText(0, String.format("you can't %s that.", getAction()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		Integer slotItemId = PlayerStorageDao.getItemIdInSlot(player.getId(), StorageTypes.INVENTORY, slot);
		if (slotItemId != itemId) {
			setRecoAndResponseText(0, String.format("you can't %s that.", getAction()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, ConsumableDao.getBecomesItemId(itemId), 1, ItemDao.getMaxCharges(itemId));
		
		InventoryUpdateResponse invUpdate = new InventoryUpdateResponse(); 
		invUpdate.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
		invUpdate.setResponseText(String.format("you %s the %s.", getAction(), ItemDao.getNameFromId(itemId, false)));
		
		consume(player, itemId, responseMaps);
	}
	
	public void consume(Player player, int itemId, ResponseMaps responseMaps) {
		boolean hpModified = false;
		HashMap<Stats, Integer> relativeBoosts = StatsDao.getRelativeBoostsByPlayerId(player.getId());
		
		for (ConsumableEffectsDto dto : ConsumableDao.getConsumableEffects(itemId)) {
			Stats stat = Stats.withValue(dto.getStatId());
			if (!relativeBoosts.containsKey(stat))
				continue;
			
			int newRelativeBoost;
			switch (Stats.withValue(dto.getStatId())) {
			case HITPOINTS: {
				newRelativeBoost = relativeBoosts.get(stat) + dto.getAmount();
				
				if (newRelativeBoost > player.getBonuses().get(Stats.HITPOINTS))// the hp relative boost is negative for how many hp lost
					newRelativeBoost = player.getBonuses().get(Stats.HITPOINTS);
				hpModified = true;
				player.setCurrentHp(player.getDto().getMaxHp() + newRelativeBoost);
				break;
			}
			default: {
				// for example lets say you're 60 strength with a strength pot.
				//the strength pot gives +6 strength, therefore your newRelativeBoost should max at 6.
				// if you're currently somehow boosted to 70 strength, then the strength pot should take no effect.
				// conversely, if you're drinking some negative strength pot, it should keep draining your
				// stat until you get to -60 boost; therefore 60 strength + -60 boost = 0 strength, and no less than that.
				// 99 * (10 / 100)
				// = 99 * 0.1
				// = 
				newRelativeBoost = relativeBoosts.get(stat);
				int proposedBoost = (int)(dto.getAmount() + player.getStats().get(stat) * ((float)dto.getPct() / 100));
				if (newRelativeBoost < proposedBoost) {
					newRelativeBoost += proposedBoost;
					if (newRelativeBoost > proposedBoost)
						newRelativeBoost = proposedBoost;
				}
				
				// min cap at -statLevel i.e. bringing your stat down to 0.
				int statLevel = StatsDao.getStatLevelByStatIdPlayerId(stat, player.getId());
				if (statLevel + newRelativeBoost < 0)
					newRelativeBoost = -statLevel;
				
				// special handling for prayer
				if (Stats.withValue(dto.getStatId()) == Stats.PRAYER) {
					newRelativeBoost = Math.min(0, newRelativeBoost);// prayer cannot boost over max so must always be negative
					player.setPrayerPoints(StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, player.getId()) + newRelativeBoost, responseMaps);
				}
				break;
			}
			}
			StatsDao.setRelativeBoostByPlayerIdStatId(player.getId(), stat, newRelativeBoost);
		}
		
		if (hpModified) {
			PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
			playerUpdateResponse.setId(player.getId());
			playerUpdateResponse.setCurrentHp(player.getCurrentHp());
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), playerUpdateResponse);
		}
		
		player.refreshBoosts();
		new StatBoostResponse().process(null, player, responseMaps);
		
		handleCustomActions(itemId, player, responseMaps);
	}
	
	private void handleCustomActions(int itemId, Player player, ResponseMaps responseMaps) {
		Items item = Items.withValue(itemId);
		if (item == null)
			return;// isn't in the enum, doesn't necessarily mean it's invalid though.
		
		switch (item) {
		case RESTORATION_FLASK_6:
		case RESTORATION_FLASK_5:
		case RESTORATION_FLASK_4:
		case RESTORATION_FLASK_3:
		case RESTORATION_FLASK_2:
		case RESTORATION_FLASK_1:
		case RESTORATION_POTION_4:
		case RESTORATION_POTION_3:
		case RESTORATION_POTION_2:
		case RESTORATION_POTION_1: {
			player.addBuff(Buffs.RESTORATION);
			
			MessageResponse resp = new MessageResponse();
			resp.setColour("white");
			resp.setResponseText("your stats will restore much faster for the next 30 seconds.");
			responseMaps.addClientOnlyResponse(player, resp);
			break;
		}
		
		case GOBLIN_STANK_FLASK_6:
		case GOBLIN_STANK_FLASK_5:
		case GOBLIN_STANK_FLASK_4:
		case GOBLIN_STANK_FLASK_3:
		case GOBLIN_STANK_FLASK_2:
		case GOBLIN_STANK_FLASK_1:
		case GOBLIN_STANK_4:
		case GOBLIN_STANK_3:
		case GOBLIN_STANK_2:
		case GOBLIN_STANK_1: {
			player.addBuff(Buffs.GOBLIN_STANK);
			
			MessageResponse resp = new MessageResponse();
			resp.setColour("white");
			resp.setResponseText("goblin scent starts coming out of your pores...");
			responseMaps.addClientOnlyResponse(player, resp);
			break;
		}
		
		case ANTIPOISON_FLASK_6:
		case ANTIPOISON_FLASK_5:
		case ANTIPOISON_FLASK_4:
		case ANTIPOISON_FLASK_3:
		case ANTIPOISON_FLASK_2:
		case ANTIPOISON_FLASK_1:
		case ANTIPOISON_4:
		case ANTIPOISON_3:
		case ANTIPOISON_2:
		case ANTIPOISON_1: {
			if (player.isPoisoned()) {
				player.clearPoison();
				responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("you immediately feel better.", "white"));
			}
			player.setPoisonImmunity(30);
			break;
		}
		
		case ZOMBIE_EEL: {
			player.inflictPoison(4);
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("the decay starts eating away at your insides!", "#00aa00"));
			break;
		}
		default:
			break;
		}
	}
}

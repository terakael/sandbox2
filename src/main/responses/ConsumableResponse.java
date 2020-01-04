package main.responses;

import java.util.HashMap;

import main.database.ConsumableDao;
import main.database.ConsumableEffectsDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.processing.FightManager;
import main.processing.Player;
import main.requests.ConsumableRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.Buffs;
import main.types.Items;
import main.types.Stats;
import main.types.StorageTypes;

public abstract class ConsumableResponse extends Response {
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ConsumableRequest))
			return;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		int slot = ((ConsumableRequest)req).getSlot();
		int itemId = ((ConsumableRequest)req).getObjectId();

		if (!ConsumableDao.isConsumable(itemId)) {
			setRecoAndResponseText(0, String.format("you can't %s that.", getAction()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		Integer slotItemId = PlayerStorageDao.getItemIdInSlot(player.getId(), 1, slot);
		if (slotItemId != itemId) {
			setRecoAndResponseText(0, String.format("you can't %s that.", getAction()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), slot, ConsumableDao.getBecomesItemId(itemId), 1);
		
		boolean hpModified = false;
		HashMap<Stats, Integer> relativeBoosts = StatsDao.getRelativeBoostsByPlayerId(player.getId());
		
		for (ConsumableEffectsDto dto : ConsumableDao.getConsumableEffects(itemId)) {
			Stats stat = Stats.withValue(dto.getStatId());
			if (!relativeBoosts.containsKey(stat))
				continue;
			
			int newRelativeBoost;
			if (dto.getStatId() == Stats.HITPOINTS.getValue()) {
				newRelativeBoost = relativeBoosts.get(stat) + dto.getAmount();
				if (newRelativeBoost > 0)// the hp relative boost is negative for how many hp lost
					newRelativeBoost = 0;
				hpModified = true;
				player.setCurrentHp(player.getDto().getMaxHp() + newRelativeBoost);
			} else {
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
			}
			
			StatsDao.setRelativeBoostByPlayerIdStatId(player.getId(), stat, newRelativeBoost);
		}
		
		if (hpModified) {
			PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
			playerUpdateResponse.setId(player.getId());
			playerUpdateResponse.setHp(player.getCurrentHp());
			responseMaps.addBroadcastResponse(playerUpdateResponse);
		}
		
		player.refreshBoosts();
		new StatBoostResponse().process(null, player, responseMaps);
		
		InventoryUpdateResponse invUpdate = new InventoryUpdateResponse(); 
		invUpdate.process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
		invUpdate.setResponseText(String.format("you %s the %s.", getAction(), ItemDao.getNameFromId(itemId)));
		
		handleCustomActions(itemId, player, responseMaps);
	}
	
	private void handleCustomActions(int itemId, Player player, ResponseMaps responseMaps) {
		switch (Items.withValue(itemId)) {
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
		default:
			break;
		}
	}
}

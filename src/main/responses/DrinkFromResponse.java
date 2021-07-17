package main.responses;

import main.database.dao.ConstructableDao;
import main.database.dao.ConsumableDao;
import main.database.dto.ConstructableDto;
import main.processing.PathFinder;
import main.processing.attackable.Player;
import main.processing.attackable.Player.PlayerState;
import main.processing.managers.ConstructableManager;
import main.processing.managers.FightManager;
import main.requests.DrinkFromRequest;
import main.requests.Request;

public class DrinkFromResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		DrinkFromRequest request = (DrinkFromRequest)req;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {	
			player.faceDirection(request.getTileId(), responseMaps);
			
			final int constructableId = ConstructableManager.getConstructableIdByTileId(player.getFloor(), request.getTileId());
			ConstructableDto constructable = ConstructableDao.getConstructableBySceneryId(constructableId);
			
			if (!ConsumableDao.isConsumable(constructable.getTertiaryId())) {
				// chalices are always made with the potion that will be consumed, i.e. the tertiary ingredient.
				// therefore if it's not consumable then there's somethign wrong (trying to drink from something that isn't a chalice).
				// this is actually a redundant check because the chalice ids are hardcoded below anyway.
				return;
			}
			
			switch (constructableId) {
				case 130: // gob stank chalice
				case 131: // def chalice
				case 132: // acc chalice
				case 133: // antipoison chalice
				case 134: // str chalice
				case 135: // regen chalice
					new DrinkResponse().consume(player, constructable.getTertiaryId(), responseMaps);
					
				default:
					break;
			}
		}
	}

}

package responses;

import database.dao.ArtisanMasterDao;
import processing.PathFinder;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import processing.managers.NPCManager;
import processing.managers.ShopManager;
import requests.Request;
import requests.ShopRequest;
import types.ArtisanShopTabs;

public class ShopResponse extends Response {
//	@Setter private HashMap<Integer, ShopItemDto> shopStock = new HashMap<>();
//	@Setter private String shopName;
	
	public ShopResponse() {
		setAction("shop");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ShopRequest))
				return;
		
		ShopRequest request = (ShopRequest)req;
		NPC npc = NPCManager.get().getNpcByInstanceId(player.getFloor(), request.getObjectId());// request tileid is the instnace id
		if (npc == null) {
			return;
		}
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), npc.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), npc.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(npc.getTileId(), responseMaps);
			
			if (ArtisanMasterDao.npcIsArtisanMaster(npc.getId())) {
				// artisan shops are different from regular shops (stuff is bought with points etc)
				new ShowArtisanShopResponse(ArtisanShopTabs.task).process(null, player, responseMaps);
			} else {
				new ShowShopResponse(ShopManager.getShopByOwnerId(npc.getId())).process(null, player, responseMaps);
			}
		}
	}
}
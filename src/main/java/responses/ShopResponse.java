package responses;

import database.dao.ArtisanMasterDao;
import processing.PathFinder;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.LocationManager;
import processing.managers.ShopManager;
import processing.stores.Store;
import requests.Request;
import requests.ShopRequest;
import types.ArtisanShopTabs;

public class ShopResponse extends WalkAndDoResponse {
//	public ShopResponse() {
//		setAction("shop");
//	}

//	@Override
//	public void process(Request req, Player player, ResponseMaps responseMaps) {
//		if (!(req instanceof ShopRequest))
//				return;
//		
//		ShopRequest request = (ShopRequest)req;
//		final NPC npc = LocationManager.getNpcNearPlayerByInstanceId(player, request.getObjectId());
//		if (npc == null) {
//			return;
//		}
//		
////		if (FightManager.fightWithFighterIsBattleLocked(player)) {
////			setRecoAndResponseText(0, "you can't do that during combat.");
////			responseMaps.addClientOnlyResponse(player, this);
////			return;
////		}
////		FightManager.cancelFight(player, responseMaps);
//		
//		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), npc.getTileId())) {
//			player.setTarget(npc);
//			player.setState(PlayerState.chasing);
////			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), npc.getTileId(), false));
////			player.setState(PlayerState.walking);
//			player.setSavedRequest(req);
//			return;
//		} else {
//			player.faceDirection(npc.getTileId(), responseMaps);
//			
//			if (ArtisanMasterDao.npcIsArtisanMaster(npc.getId())) {
//				// artisan shops are different from regular shops (stuff is bought with points etc)
//				new ShowArtisanShopResponse(ArtisanShopTabs.task).process(null, player, responseMaps);
//			} else {
//				new ShowShopResponse(ShopManager.getShopByOwnerId(npc.getId())).process(null, player, responseMaps);
//			}
//		}
//	}
	
	@Override
	protected boolean setTarget(Request req, Player player, ResponseMaps responseMaps) {
		ShopRequest request = (ShopRequest)req;
		target = LocationManager.getNpcNearPlayerByInstanceId(player, request.getObjectId());
		if (target == null)
			return false;
		
		return true;
	}

	@Override
	protected void doAction(Request req, Player player, ResponseMaps responseMaps) {
		if (ArtisanMasterDao.npcIsArtisanMaster(((NPC)target).getId())) {
			new ShowArtisanShopResponse(ArtisanShopTabs.task).process(null, player, responseMaps);
		} else {
			final Store shop = ShopManager.getShopByOwnerId(((NPC)target).getId());
			if (shop != null)
				new ShowShopResponse(shop).process(null, player, responseMaps);
		}
		
		player.setTarget(null);
	}
}

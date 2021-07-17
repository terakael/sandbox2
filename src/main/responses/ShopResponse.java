package main.responses;

import java.util.HashMap;
import java.util.stream.Collectors;

import lombok.Setter;
import main.database.dao.ShopDao;
import main.database.dto.ShopItemDto;
import main.processing.PathFinder;
import main.processing.attackable.NPC;
import main.processing.attackable.Player;
import main.processing.attackable.Player.PlayerState;
import main.processing.managers.ClientResourceManager;
import main.processing.managers.FightManager;
import main.processing.managers.NPCManager;
import main.processing.managers.ShopManager;
import main.processing.stores.Store;
import main.requests.Request;
import main.requests.ShopRequest;

public class ShopResponse extends Response {
	@Setter private HashMap<Integer, ShopItemDto> shopStock = new HashMap<>();
	@Setter private String shopName;
	
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
			Store shop = ShopManager.getShopByOwnerId(npc.getId());
			if (shop != null) {
				ClientResourceManager.addItems(player, shop.getStock().values().stream().map(ShopItemDto::getItemId).collect(Collectors.toSet()));
				
				player.setShopId(shop.getShopId());
				shopStock = shop.getStock();
				shopName = ShopDao.getShopNameById(shop.getShopId());
				responseMaps.addClientOnlyResponse(player, this);
			}
		}
	}
}

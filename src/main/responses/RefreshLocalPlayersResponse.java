package main.responses;

import java.util.ArrayList;

import main.database.AnimationDao;
import main.database.StatsDao;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.requests.Request;
import main.types.Stats;

public class RefreshLocalPlayersResponse extends Response {
	private ArrayList<PlayerUpdateResponse> players = null;
	
	public RefreshLocalPlayersResponse() {
		setAction("refresh_players");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		players = new ArrayList<>();
		for (Player localPlayer : WorldProcessor.getPlayersNearTile(player.getRoomId(), player.getTileId(), 15)) {
			if (localPlayer.getId() == player.getId())
				continue;// don't add self
			
			PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
			playerUpdate.setId(localPlayer.getId());
			playerUpdate.setName(localPlayer.getDto().getName());
			playerUpdate.setTileId(localPlayer.getTileId());
			playerUpdate.setRoomId(localPlayer.getRoomId());
			playerUpdate.setCurrentHp(StatsDao.getCurrentHpByPlayerId(localPlayer.getId()));
			playerUpdate.setMaxHp(localPlayer.getStats().get(Stats.HITPOINTS));
			playerUpdate.setCombatLevel(StatsDao.getCombatLevelByPlayerId(localPlayer.getId()));
			playerUpdate.setEquipAnimations(AnimationDao.getEquipmentAnimationsByPlayerId(localPlayer.getId()));
			playerUpdate.setBaseAnimations(AnimationDao.loadAnimationsByPlayerId(localPlayer.getId()));
			players.add(playerUpdate);
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}

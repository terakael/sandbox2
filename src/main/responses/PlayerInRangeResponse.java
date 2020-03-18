package main.responses;

import java.util.ArrayList;
import java.util.Set;

import main.database.AnimationDao;
import main.database.StatsDao;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.requests.Request;
import main.types.Stats;

public class PlayerInRangeResponse extends Response {
	private ArrayList<PlayerUpdateResponse> players = new ArrayList<>();
	
	public PlayerInRangeResponse() {
		setAction("player_in_range");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
	public void addPlayers(Set<Integer> playerIds) {
		for (Integer playerId : playerIds) {
			Player localPlayer = WorldProcessor.getPlayerById(playerId);
			
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
	}

}

package responses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import database.dao.EquipmentDao;
import database.dao.PlayerBaseAnimationsDao;
import database.dao.StatsDao;
import processing.WorldProcessor;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;
import types.Stats;

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
			playerUpdate.setCurrentHp(StatsDao.getCurrentHpByPlayerId(localPlayer.getId()));
			playerUpdate.setMaxHp(localPlayer.getStats().get(Stats.HITPOINTS));
			playerUpdate.setCombatLevel(StatsDao.getCombatLevelByPlayerId(localPlayer.getId()));
			playerUpdate.setEquipAnimations(EquipmentDao.getEquipmentAnimationsByPlayerId(localPlayer.getId()));
			playerUpdate.setBaseAnimations(PlayerBaseAnimationsDao.getBaseAnimationsBasedOnEquipmentTypes(localPlayer.getId()));
			
			// literally only needed so we can check if the player is equipping daggers, so we can draw them correctly.
			playerUpdate.setWeaponType(EquipmentDao.getEquipmentTypeByEquipmentId(EquipmentDao.getWeaponIdByPlayerId(localPlayer.getId())));
			
			players.add(playerUpdate);
		}
	}

}

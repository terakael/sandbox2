package main.responses;

import main.database.AnimationDao;
import main.database.LadderConnectionDao;
import main.database.LadderConnectionDto;
import main.database.StatsDao;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.ClimbRequest;
import main.requests.Request;
import main.types.Stats;

public class FinishClimbResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ClimbRequest))
			return;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		ClimbRequest request = (ClimbRequest)req;
		
		if (!PathFinder.isNextTo(player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getRoomId(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			for (LadderConnectionDto dto : LadderConnectionDao.getLadderConnections()) {
				if (dto.getFromRoomId() == player.getRoomId() && dto.getFromTileId() == request.getTileId()) {
					player.setTileId(dto.getToTileId());
					player.setRoomId(dto.getToRoomId());
					
					// this message gets broadcast to those around the ladder the player is leaving, so they can stop processing the player
					PlayerUpdateResponse leavingPlayer = (PlayerUpdateResponse)ResponseFactory.create("player_update");
					leavingPlayer.setId(player.getDto().getId());
					leavingPlayer.setTileId(dto.getToTileId());
					leavingPlayer.setRoomId(dto.getToRoomId());
					responseMaps.addLocalResponse(dto.getFromRoomId(), dto.getFromTileId(), leavingPlayer);
					
					// this message gets broadcast to those around the destination ladder, so they can load the player properly
					PlayerUpdateResponse arrivingPlayer = (PlayerUpdateResponse)ResponseFactory.create("player_update");
					arrivingPlayer.setId(player.getId());
					arrivingPlayer.setName(player.getDto().getName());
					arrivingPlayer.setTileId(dto.getToTileId());
					arrivingPlayer.setRoomId(dto.getToRoomId());
					arrivingPlayer.setCurrentHp(StatsDao.getCurrentHpByPlayerId(player.getId()));
					arrivingPlayer.setMaxHp(player.getStats().get(Stats.HITPOINTS));
					arrivingPlayer.setCombatLevel(StatsDao.getCombatLevelByPlayerId(player.getId()));
					arrivingPlayer.setEquipAnimations(AnimationDao.getEquipmentAnimationsByPlayerId(player.getId()));
					arrivingPlayer.setBaseAnimations(AnimationDao.loadAnimationsByPlayerId(player.getId()));
					responseMaps.addLocalResponse(dto.getToRoomId(), dto.getToTileId(), arrivingPlayer);
					
					// for the player doing the climbing, we want to completely refresh all the players as we're ending up in a different place
					new RefreshLocalPlayersResponse().process(null, player, responseMaps);
					
					if (dto.getFromRoomId() != dto.getToRoomId()) // if the room changes we need to reload the room
						new LoadRoomResponse().process(null, player, responseMaps);
					
					break;
				}
			}
			player.setState(PlayerState.idle);
		}
	}

}

package responses;

import database.dao.ArtisanMasterDao;
import database.dao.PlayerArtisanTaskDao;
import database.dao.StatsDao;
import database.dto.ArtisanMasterDto;
import processing.PathFinder;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import processing.managers.FightManager;
import processing.managers.NPCManager;
import requests.Request;
import requests.TaskRequest;
import types.Stats;

public class TaskResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof TaskRequest))
			return;
		
		TaskRequest request = (TaskRequest)req;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you're too busy fighting!");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		NPC npc = NPCManager.get().getNpcByInstanceId(player.getFloor(), request.getObjectId());
		if (npc == null)
			return;
		
		ArtisanMasterDto master = ArtisanMasterDao.getArtisanMasterByNpcId(npc.getId());
		if (master == null)
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), npc.getTileId(), player.getTileId())) {
			player.setTarget(npc);	
			player.setSavedRequest(req);
		} else {
			player.faceDirection(npc.getTileId(), responseMaps);
			handleAssignTask(master, player, responseMaps);
		}
	}
	
	private void handleAssignTask(ArtisanMasterDto master, Player player, ResponseMaps responseMaps) {
		if (StatsDao.getStatLevelByStatIdPlayerId(Stats.ARTISAN, player.getId()) < master.getArtisanRequirement()) {
			setRecoAndResponseText(0, String.format("you need %d artisan to learn from this master.", master.getArtisanRequirement()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (PlayerArtisanTaskDao.taskInProgress(player.getId())) {
			setRecoAndResponseText(0, "you are already assigned a task; click the artisan skill icon for details.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		ArtisanManager.newTask(player, master.getAssignmentLevelRangeMin(), master.getAssignmentLevelRangeMax(), responseMaps);
	}

}

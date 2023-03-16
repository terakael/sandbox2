package responses;

import java.util.Set;
import java.util.stream.Collectors;

import database.dao.MineableDao;
import database.dto.MineableDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;

public class ShowMiningSkillWindowResponse extends Response {
	private Set<MineableDto> mineables;
	
	public ShowMiningSkillWindowResponse() {
		setAction("show_mining_skill_window");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		mineables = MineableDao.getAllMineables();
		
		ClientResourceManager.addScenery(player, mineables.stream().map(MineableDto::getSceneryId).collect(Collectors.toSet()));
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}

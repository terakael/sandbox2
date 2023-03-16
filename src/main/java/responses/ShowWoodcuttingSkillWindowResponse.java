package responses;

import java.util.Set;
import java.util.stream.Collectors;

import database.dao.ChoppableDao;
import database.dto.ChoppableDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;

public class ShowWoodcuttingSkillWindowResponse extends Response {
	private Set<ChoppableDto> choppables;
	
	public ShowWoodcuttingSkillWindowResponse() {
		setAction("show_woodcutting_skill_window");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		choppables = ChoppableDao.getAllChoppables();
		
		ClientResourceManager.addScenery(player, choppables.stream().map(ChoppableDto::getSceneryId).collect(Collectors.toSet()));
		
		responseMaps.addClientOnlyResponse(player, this);
	}
}

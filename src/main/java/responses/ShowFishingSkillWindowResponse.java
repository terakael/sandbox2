package responses;

import java.util.Set;
import java.util.stream.Collectors;

import database.dao.FishableDao;
import database.dto.FishableDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;

public class ShowFishingSkillWindowResponse extends Response {
	private Set<FishableDto> fishables;
	
	public ShowFishingSkillWindowResponse() {
		setAction("show_fishing_skill_window");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		fishables = FishableDao.getAllFishables();
		
		ClientResourceManager.addItems(player, fishables.stream().map(FishableDto::getItemId).collect(Collectors.toSet()));
		ClientResourceManager.addItems(player, fishables.stream().map(FishableDto::getToolId).collect(Collectors.toSet()));
		ClientResourceManager.addItems(player, fishables.stream().map(FishableDto::getBaitId).collect(Collectors.toSet()));
		
		responseMaps.addClientOnlyResponse(player, this);
	}
}

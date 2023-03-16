package responses;

import java.util.Set;
import java.util.stream.Collectors;

import database.dao.CookableDao;
import database.dto.CookableDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;

public class ShowCookingSkillWindowResponse extends Response {
	private Set<CookableDto> cookables;

	public ShowCookingSkillWindowResponse() {
		setAction("show_cooking_skill_window");
		setCombatInterrupt(false);
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		cookables = CookableDao.getAllCookables();
		
		ClientResourceManager.addItems(player, cookables.stream().map(CookableDto::getCookedItemId).collect(Collectors.toSet()));
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}

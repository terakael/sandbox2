package responses;

import java.util.Set;
import java.util.stream.Collectors;

import database.dao.CastableDao;
import database.dto.CastableDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;

public class ShowMagicSkillWindowResponse extends Response {
	private Set<CastableDto> castables;
	
	public ShowMagicSkillWindowResponse() {
		setAction("show_magic_skill_window");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		castables = CastableDao.getAllCastables();
		
		ClientResourceManager.addItems(player, castables.stream().map(CastableDto::getItemId).collect(Collectors.toSet()));
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}

package responses;

import java.util.List;
import java.util.stream.Collectors;

import database.dao.SmeltableDao;
import database.dao.SmithableDao;
import database.dto.SmeltableDto;
import database.dto.SmithableDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;
import requests.ShowSmithingSkillWindowRequest;
import types.SmithingSkillWindowTabs;

public class ShowSmithingSkillWindowResponse extends Response {
	private List<SmithableDto> smithables = null;
	private SmithingSkillWindowTabs selectedTab = null;
	private SmithingSkillWindowTabs[] tabs = null;
	
	public ShowSmithingSkillWindowResponse() {
		setAction("show_smithing_skill_window");
	}
	
	public ShowSmithingSkillWindowResponse(SmithingSkillWindowTabs tab) {
		setAction("show_smithing_skill_window");
		selectedTab = tab;
		smithables = SmithableDao.getAllItemsByBarId(selectedTab.getBarId());
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// this response can be activated in two ways:
		// 1. manually via code: call the constructor with the specified tab (in this case the request can/will be null)
		// 2. automatically via client request: pull the requested tab from the request (in this case the request must be valid)
		if (selectedTab == null || smithables == null) {
			if (!(req instanceof ShowSmithingSkillWindowRequest))
				return; // if the tab constructor isn't called and the request is wrong, then the response has been used incorrectly.
			
			try {
				// if the user fucks with it, this will throw an exception, so just bail silently
				selectedTab = SmithingSkillWindowTabs.valueOf(((ShowSmithingSkillWindowRequest)req).getTab());
			} catch (Exception e) {
				return;
			}
			
			smithables = SmithableDao.getAllItemsByBarId(selectedTab.getBarId());
		}
		
		tabs = SmithingSkillWindowTabs.values();
		
		// add the bar to the smithing tab as well
		SmeltableDto smeltable = SmeltableDao.getSmeltableByBarId(selectedTab.getBarId());
		smithables.add(new SmithableDto(smeltable.getBarId(), smeltable.getLevel(), smeltable.getBarId(), 1));
		
		ClientResourceManager.addItems(player, smithables.stream()
				.map(SmithableDto::getItemId)
				.collect(Collectors.toSet()));
//		ClientResourceManager.addItems(player, Collections.singleton(selectedTab.getBarId()));
		responseMaps.addClientOnlyResponse(player, this);
	}
}

package responses;

import java.util.Set;
import java.util.stream.Collectors;

import database.dao.ConstructableDao;
import database.dto.ConstructableDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;
import requests.ShowConstructionSkillWindowRequest;
import types.ConstructionSkillWindowTabs;
import types.Items;

public class ShowConstructionSkillWindowResponse extends Response {
	private Set<ConstructableDto> constructables = null;
	private ConstructionSkillWindowTabs selectedTab = null;
	private ConstructionSkillWindowTabs[] tabs;
	
	public ShowConstructionSkillWindowResponse() {
		setAction("show_construction_skill_window");
	}
	
	public ShowConstructionSkillWindowResponse(ConstructionSkillWindowTabs tab) {
		selectedTab = tab;
		setAction("show_construction_skill_window");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (selectedTab == null) {
			if (!(req instanceof ShowConstructionSkillWindowRequest))
				return;
			
			try {
				selectedTab = ConstructionSkillWindowTabs.valueOf(((ShowConstructionSkillWindowRequest)req).getTab());
			} catch (Exception e) {
				return; // user fuckery could result in an invalid tab; just bail silently
			}
		}
		
		tabs = ConstructionSkillWindowTabs.values();
		
		if (selectedTab == ConstructionSkillWindowTabs.fires) {
			// fires don't have a plank; their common denominator is that they all use a tinderbox
			constructables = ConstructableDao.getAllConstructables().stream()
					.filter(e -> e.getToolId() == Items.TINDERBOX.getValue())
					.collect(Collectors.toSet());
		} else {
			constructables = ConstructableDao.getAllConstructablesWithMaterials(selectedTab.getPlankId());
		}
		ClientResourceManager.addScenery(player, constructables.stream().map(ConstructableDto::getResultingSceneryId).collect(Collectors.toSet()));
		ClientResourceManager.addItems(player, constructables.stream().map(ConstructableDto::getPlankId).collect(Collectors.toSet()));
		ClientResourceManager.addItems(player, constructables.stream().map(ConstructableDto::getBarId).collect(Collectors.toSet()));
		ClientResourceManager.addItems(player, constructables.stream().map(ConstructableDto::getTertiaryId).collect(Collectors.toSet()));
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}

package main.responses;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import main.database.dto.ConstructableDto;
import main.processing.ClientResourceManager;
import main.processing.Player;
import main.requests.Request;

public class ShowConstructionTableResponse extends Response {
	private Set<ConstructableDto> constructableOptions;
	
	public ShowConstructionTableResponse(Set<ConstructableDto> options) {
		setAction("show_construction_table");
		constructableOptions = options;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (constructableOptions == null || constructableOptions.size() <= 1)
			return; // if the size is 1 then we should be making it without showing the menu.
		
		// all items to load for the player
		Set<Integer> allItemIds = new HashSet<>();
		allItemIds.addAll(constructableOptions.stream().map(ConstructableDto::getPlankId).collect(Collectors.toSet()));
		allItemIds.addAll(constructableOptions.stream().map(ConstructableDto::getBarId).collect(Collectors.toSet()));
		allItemIds.addAll(constructableOptions.stream().map(ConstructableDto::getTertiaryId).collect(Collectors.toSet()));
		ClientResourceManager.addItems(player, allItemIds);
		
		// also the resulting scenery that should show
		ClientResourceManager.addScenery(player, constructableOptions.stream().map(ConstructableDto::getResultingSceneryId).collect(Collectors.toSet()));
		responseMaps.addClientOnlyResponse(player, this);
	}

}

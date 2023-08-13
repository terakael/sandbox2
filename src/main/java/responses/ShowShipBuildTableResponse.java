package responses;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import database.dto.ConstructableDto;
import database.dto.ItemDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;

@SuppressWarnings("unused")
public class ShowShipBuildTableResponse extends Response {
	private Set<ItemDto> buildOptions;
	private boolean flatpack;
	private int tileId;
	
	public ShowShipBuildTableResponse(Set<ItemDto> options, int tileId) {
		setAction("show_ship_build_table");
		setCombatInterrupt(false);
		buildOptions = options;
		this.tileId = tileId;
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// all items to load for the player
		Set<Integer> allItemIds = new HashSet<>();
//		allItemIds.addAll(constructableOptions.stream().map(ConstructableDto::getPlankId).collect(Collectors.toSet()));
//		allItemIds.addAll(constructableOptions.stream().map(ConstructableDto::getBarId).collect(Collectors.toSet()));
//		allItemIds.addAll(constructableOptions.stream().map(ConstructableDto::getTertiaryId).collect(Collectors.toSet()));
		ClientResourceManager.addItems(player, allItemIds);
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}

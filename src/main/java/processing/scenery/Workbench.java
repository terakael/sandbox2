package processing.scenery;

import java.util.Set;
import java.util.stream.Collectors;

import database.dao.ConstructableDao;
import database.dto.ConstructableDto;
import processing.attackable.Player;
import requests.ConstructionRequest;
import requests.UseRequest;
import responses.ConstructionResponse;
import responses.ResponseMaps;
import responses.ShowConstructionTableResponse;

public class Workbench implements Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int itemId = request.getSrc();
		
		Set<ConstructableDto> constructables = ConstructableDao.getAllConstructablesWithMaterials(itemId).stream()
				.filter(e -> e.getFlatpackItemId() > 0)
				.collect(Collectors.toSet());
		
		if (constructables.isEmpty()) {
			return false; // no matches; nothing interesting happens (e.g. use tinderbox on helmet or whatevs)
		}
		else if (constructables.size() == 1) {
			// one result means we don't show the menu, just build the thing
			// it's a set so just do a foreach to get the only entry
			constructables.forEach(e -> {
				ConstructionRequest constructionRequest = new ConstructionRequest();
				constructionRequest.setSceneryId(e.getResultingSceneryId());
				constructionRequest.setFlatpack(true);
				constructionRequest.setTileId(request.getDest());
				new ConstructionResponse().process(constructionRequest, player, responseMaps);
			});
		} else {
			// show a menu with all the constructables
			new ShowConstructionTableResponse(constructables, true, request.getDest()).process(null, player, responseMaps);
		}
		
		return true;
	}

}

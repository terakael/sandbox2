package main.scenery;

import java.util.Set;
import java.util.stream.Collectors;

import main.database.dao.ConstructableDao;
import main.database.dto.ConstructableDto;
import main.processing.Player;
import main.requests.ConstructionRequest;
import main.requests.UseRequest;
import main.responses.ConstructionResponse;
import main.responses.ResponseMaps;
import main.responses.ShowConstructionTableResponse;

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
				new ConstructionResponse().process(constructionRequest, player, responseMaps);
			});
		} else {
			// show a menu with all the constructables
			new ShowConstructionTableResponse(constructables, true).process(null, player, responseMaps);
		}
		
		return true;
	}

}

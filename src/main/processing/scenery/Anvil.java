package main.processing.scenery;

import java.util.List;
import java.util.stream.Collectors;

import main.database.dao.SmeltableDao;
import main.database.dao.SmithableDao;
import main.database.dto.SmithableDto;
import main.processing.attackable.Player;
import main.processing.managers.ClientResourceManager;
import main.requests.UseRequest;
import main.responses.ResponseMaps;
import main.responses.ShowSmithingTableResponse;

public class Anvil implements Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		if (!SmeltableDao.itemIsBar(srcItemId))
			return false;
		
		// note that coal isn't in this list as you cannot use coal with the furnace.
		// you need to use a primary ore; coal is used as a secondary ore for the other ores.
		List<SmithableDto> smithingOptions = SmithableDao.getAllItemsByBarId(srcItemId);
		
		ShowSmithingTableResponse response = new ShowSmithingTableResponse();
		response.setSmithingOptions(smithingOptions);
		responseMaps.addClientOnlyResponse(player, response);
		
		// if the client has never been sent the material information, send it now.
		ClientResourceManager.addItems(player, smithingOptions.stream().map(SmithableDto::getItemId).collect(Collectors.toSet()));
		return true;
	}

}

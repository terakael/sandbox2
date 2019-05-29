package main.scenery;

import main.database.SmithableDao;
import main.processing.Player;
import main.responses.ResponseMaps;
import main.responses.ShowSmithingTableResponse;

public class Furnace extends Scenery {

	@Override
	public boolean use(int srcItemId, Player player, ResponseMaps responseMaps) {
		switch (srcItemId) {
		case 3: // copper
		case 4: // iron
		case 6: // mithril
			// note that coal isn't in this list as you cannot use coal with the furnace.
			// you need to use a primary ore; coal is used as a secondary ore for the other ores.
			ShowSmithingTableResponse response = new ShowSmithingTableResponse();
			response.setOreId(srcItemId);
			response.setSmithingOptions(SmithableDao.getAllItemsThatUseMaterial(srcItemId));
			responseMaps.addClientOnlyResponse(player, response);
			return true;
		default:
			break;
		}
		
		return false;
	}
	
}

package responses;

import processing.attackable.Player;
import requests.Request;
import requests.SwitchArtisanShopTabRequest;
import types.ArtisanShopTabs;

public class SwitchArtisanShopTabResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof SwitchArtisanShopTabRequest))
			return;
		
		try {
			final String newTabAsc = ((SwitchArtisanShopTabRequest)req).getTabName();
			ArtisanShopTabs newTab = ArtisanShopTabs.valueOf(newTabAsc);
			
			new ShowArtisanShopResponse(newTab).process(null, player, responseMaps);
		} catch (Exception e) {
			// user fuckery caused the tab name to be incorrect probably, return nothing
		}
	}

}

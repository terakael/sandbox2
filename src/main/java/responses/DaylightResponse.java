package responses;

import processing.attackable.Player;
import requests.Request;

@SuppressWarnings("unused")
public class DaylightResponse extends Response {
	private float brightness;
	private boolean transitionInstantly;
	public DaylightResponse(boolean isDaylight, boolean transitionInstantly) {
		setAction("daylight");
		brightness = isDaylight ? 1.0f : 0.4f;
		this.transitionInstantly = transitionInstantly;
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
	}
}

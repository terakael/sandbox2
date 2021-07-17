package main.responses;

import main.processing.attackable.Player;
import main.requests.Request;

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
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
	}
}

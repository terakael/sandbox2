package system;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class PlayerRequestManager {
	public enum PlayerRequestType { trade, duel }
	
	@Getter @Setter @AllArgsConstructor
	private static class PlayerRequest {
		int playerId;
		int otherPlayerId;
		PlayerRequestType requestType;
	}
	
	static Map<Integer, PlayerRequest> requestMap = new HashMap<>();
	
	private PlayerRequestManager() {}
	
	public static void addRequest(int playerId, int otherPlayerId, PlayerRequestType requestType) {
		// intentionally overwrite existing request from playerId.
		// e.g. if player sent a duel request to one player, then a trade request to another player,
		// then we overwrite the existing duel request.  Therefore a player only has one active request at a time.
		PlayerRequestManager.requestMap.put(playerId, new PlayerRequest(playerId, otherPlayerId, requestType));
	}
	
	public static boolean requestExists(int playerId, int otherPlayerId, PlayerRequestType requestType) {
		if (requestMap.containsKey(playerId)) {
			PlayerRequest req = requestMap.get(playerId);
			return req.getOtherPlayerId() == otherPlayerId && req.getRequestType() == requestType;
		}
		return false;
	}

	public static void removeRequest(int objectId) {
		PlayerRequestManager.requestMap.remove(objectId);
	}
}

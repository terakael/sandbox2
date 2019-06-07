package main.responses;

import java.util.HashMap;

import lombok.Setter;
import main.database.StatsDao;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.Request;

@Setter
public class AddExpResponse extends Response {
	
	private int id;
	private boolean relative = true;// are we adding or setting
	
	// statShortName,exp
	HashMap<Integer, Integer> stats = new HashMap<>();

	public AddExpResponse() {
		setAction("addexp");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof AddExpRequest))
			return;
		
		AddExpRequest request = (AddExpRequest)req;
		id = request.getId();// unneeded; its obvious that it's the current player
		if (request.getStatId() != -1)
			StatsDao.addExpToPlayer(request.getId(), request.getStatId(), request.getExp());
		
		addExp(request.getStatId(), request.getExp());
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	public void addExp(int statId, int exp) {
		stats.put(statId, exp);
	}

}

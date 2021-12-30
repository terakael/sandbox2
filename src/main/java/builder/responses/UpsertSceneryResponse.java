package builder.responses;

import java.util.List;

import builder.requests.Request;
import builder.requests.UpsertSceneryRequest;
import database.dao.SceneryDao;

public class UpsertSceneryResponse extends Response {

	@Override
	public void process(Request req, List<Response> responses) {
		if (!(req instanceof UpsertSceneryRequest))
			return;
		
		final UpsertSceneryRequest request = (UpsertSceneryRequest)req;
		
		if (request.getId() == 0)
			SceneryDao.deleteRoomScenery(request.getFloor(), request.getTileId());
		else
			SceneryDao.upsertRoomScenery(request.getFloor(), request.getTileId(), request.getId());
		
		new LoadInstancesResponse().process(null, responses);
	}

}

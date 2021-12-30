package builder.responses;

import java.util.List;

import builder.requests.Request;
import builder.requests.UpsertNpcRequest;
import database.dao.NPCDao;

public class UpsertNpcResponse extends Response {

	@Override
	public void process(Request req, List<Response> responses) {
		if (!(req instanceof UpsertNpcRequest))
			return;
		
		final UpsertNpcRequest request = (UpsertNpcRequest)req;
		
		if (request.getId() == 0)
			NPCDao.deleteRoomNpc(request.getFloor(), request.getTileId());
		else
			NPCDao.upsertRoomNpc(request.getFloor(), request.getTileId(), request.getId());
		
		new LoadInstancesResponse().process(null, responses);
	}

}

package builder.responses;

import java.util.List;

import builder.requests.Request;
import builder.requests.UpsertGroundTextureRequest;
import database.dao.GroundTextureDao;

public class UpsertGroundTextureResponse extends Response{

	@Override
	public void process(Request req, List<Response> responses) {
		if (!(req instanceof UpsertGroundTextureRequest))
			return;
		
		final UpsertGroundTextureRequest request = (UpsertGroundTextureRequest)req;
		
		// TODO PathFinder update
		GroundTextureDao.upsertGroundTexture(request.getFloor(), request.getTileId(), request.getId());
		
		new LoadInstancesResponse().process(null, responses);
	}

}

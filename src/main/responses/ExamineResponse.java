package main.responses;

import java.util.HashMap;
import java.util.Map;

import javax.websocket.Session;

import main.database.ItemDao;
import main.database.SceneryDao;
import main.processing.WorldProcessor;
import main.requests.ExamineRequest;
import main.requests.Request;

public class ExamineResponse extends Response {
	private String examineText;
	
	private static Map<Integer, String> sceneryExamineMap = new HashMap<>();
	private static Map<Integer, String> itemExamineMap = new HashMap<>();
	public static void initializeExamineMap() {
		sceneryExamineMap = SceneryDao.getExamineMap();
		itemExamineMap = ItemDao.getExamineMap();
	}

	public ExamineResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof ExamineRequest))
			return;
		
		ExamineRequest request = (ExamineRequest)req;
		
		switch (request.getType()) {
		case "scenery":
			examineText = sceneryExamineMap.get(request.getObjectId());
			break;
		case "item":
			examineText = itemExamineMap.get(request.getObjectId());
			break;
		default:
			break;
		}
		
		responseMaps.addClientOnlyResponse(WorldProcessor.playerSessions.get(client), this);
	}

}

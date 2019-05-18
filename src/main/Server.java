package main;

import java.util.HashSet;
import java.util.Scanner;

import javax.websocket.DeploymentException;

import main.database.MineableDao;
import main.database.SceneryDao;
import main.processing.PathFinder;
import main.processing.WorldProcessor;
import main.responses.CachedResourcesResponse;
import main.responses.ExamineResponse;

public class Server {

	public static void main(String[] args) {
		org.glassfish.tyrus.server.Server server = new org.glassfish.tyrus.server.Server("localhost", 45555, "/ws", null, Endpoint.class);
		org.glassfish.tyrus.server.Server resourceServer = new org.glassfish.tyrus.server.Server("localhost", 45556, "/ws", null, ResourceEndpoint.class);
		
		try {
			resourceServer.start();
			server.start();
			
//			HashSet<Integer> treeLocations = SceneryDao.getImpassableTileIdsByRoomId(1);
//			for (int i = 0; i < 250*250; ++i) {
//				if (treeLocations.contains(i))
//					continue;
//				
//				int sceneryId = (int) (Math.random() * 5) + 10;
//				if ((int)(Math.random() * 100) < 10)
//					SceneryDao.addRoomScenery(1, sceneryId, i);
//			}
			
			CachedResourcesResponse.get();// loads/caches all the sprite maps, scenery etc and gets sent on client load
			PathFinder.get();// init the path nodes and relationships
			ExamineResponse.initializeExamineMap();// all the scenery examine
			MineableDao.setupCaches();// mineable tiles, mineable objects
			
			WorldProcessor processor = new WorldProcessor();
			processor.start();
			
			System.out.println("press any key to quit");
			new Scanner(System.in).nextLine();
		} catch (DeploymentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			server.stop();
		}
	}
}

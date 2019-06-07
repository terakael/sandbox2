package main;

import java.util.HashSet;
import java.util.Scanner;

import javax.websocket.DeploymentException;

import main.database.ItemDao;
import main.database.MineableDao;
import main.database.NPCDao;
import main.database.SceneryDao;
import main.processing.PathFinder;
import main.processing.WorldProcessor;
import main.responses.CachedResourcesResponse;
import main.responses.ExamineResponse;
import main.utils.RandomUtil;

public class Server {

	public static void main(String[] args) {
		org.glassfish.tyrus.server.Server server = new org.glassfish.tyrus.server.Server("localhost", 45555, "/ws", null, Endpoint.class);
		org.glassfish.tyrus.server.Server resourceServer = new org.glassfish.tyrus.server.Server("localhost", 45556, "/ws", null, ResourceEndpoint.class);
		
		try {
			resourceServer.start();
			server.start();
			
//			
//			
//			HashSet<Integer> treeLocations = SceneryDao.getImpassableTileIdsByRoomId(1);
//			HashSet<Integer> npcInstances = NPCDao.getNpcInstanceIds();
//			
//			for (int i = 0; i < 100; ++i) {
//				
//				int potentialTileId = -1;
//				while (true) {
//					potentialTileId = RandomUtil.getRandom(0, PathFinder.LENGTH * PathFinder.LENGTH - 1);
//					
//					if (treeLocations.contains(potentialTileId))
//						continue;
//					
//					if (npcInstances.contains(potentialTileId))
//						continue;
//					
//					break;
//				}
//				
//				if (potentialTileId != -1)
//					NPCDao.addNpcInstance(1, 10, potentialTileId);
//			}
			
			CachedResourcesResponse.get();// loads/caches all the sprite maps, scenery etc and gets sent on client load
			PathFinder.get();// init the path nodes and relationships
			ExamineResponse.initializeExamineMap();// all the scenery examine
			MineableDao.setupCaches();// mineable tiles, mineable objects
			ItemDao.setupCaches();
			
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

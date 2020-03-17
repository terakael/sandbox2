package main;

import java.io.IOException;
import java.util.Scanner;

import javax.websocket.DeploymentException;

import main.database.BrewableDao;
import main.database.CastableDao;
import main.database.CatchableDao;
import main.database.ConsumableDao;
import main.database.CookableDao;
import main.database.EquipmentDao;
import main.database.FishableDao;
import main.database.GroundTextureDao;
import main.database.ItemDao;
import main.database.LadderConnectionDao;
import main.database.MineableDao;
import main.database.NpcMessageDao;
import main.database.PickableDao;
import main.database.RespawnableDao;
import main.database.SceneryDao;
import main.database.ShopDao;
import main.database.UseItemOnItemDao;
import main.processing.MinimapGenerator;
import main.processing.NPCManager;
import main.processing.PathFinder;
import main.processing.ShopManager;
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
			
			PathFinder.get();// init the path nodes and relationships
			SceneryDao.setupCaches();
			ExamineResponse.initializeExamineMap();// all the scenery examine
			MineableDao.setupCaches();// mineable tiles, mineable objects
			ItemDao.setupCaches();
			GroundTextureDao.cacheTextures();
			NPCManager.get().loadNpcs();
			NpcMessageDao.setupCaches();
			ConsumableDao.cacheConsumables();
			ConsumableDao.cacheConsumableEffects();
			CookableDao.cacheCookables();
			CatchableDao.cacheCatchables();
			UseItemOnItemDao.cacheData();
			EquipmentDao.setupCaches();
			BrewableDao.cacheBrewables();
			ShopDao.setupCaches();
			ShopManager.setupShops();
			RespawnableDao.setupCaches();// ground items that respawn once picked up
			GroundItemManager.initialize();
			LadderConnectionDao.setupCaches();
			PickableDao.setupCaches();
			CastableDao.setupCaches();
			FishableDao.setupCaches();
			try {
				MinimapGenerator.createImage(1);
				MinimapGenerator.createImage(10001);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// should be last after all the other caches are set up
			CachedResourcesResponse.get();// loads/caches all the sprite maps, scenery etc and gets sent on client load
			
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

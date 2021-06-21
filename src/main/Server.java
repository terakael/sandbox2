package main;

import java.io.IOException;
import java.util.Scanner;

import javax.websocket.DeploymentException;

import main.database.dao.AnimationDao;
import main.database.dao.BrewableDao;
import main.database.dao.BuryableDao;
import main.database.dao.CastableDao;
import main.database.dao.CatchableDao;
import main.database.dao.ChoppableDao;
import main.database.dao.ClimbableDao;
import main.database.dao.ConstructableDao;
import main.database.dao.ConsumableDao;
import main.database.dao.ContextOptionsDao;
import main.database.dao.CookableDao;
import main.database.dao.DoorDao;
import main.database.dao.EquipmentDao;
import main.database.dao.FishableDao;
import main.database.dao.GroundTextureDao;
import main.database.dao.ItemDao;
import main.database.dao.MineableDao;
import main.database.dao.MinimapSegmentDao;
import main.database.dao.NpcMessageDao;
import main.database.dao.PickableDao;
import main.database.dao.PlayerAnimationDao;
import main.database.dao.PlayerDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.PrayerDao;
import main.database.dao.ReinforcementBonusesDao;
import main.database.dao.RespawnableDao;
import main.database.dao.SawmillableDao;
import main.database.dao.SceneryDao;
import main.database.dao.ShopDao;
import main.database.dao.SmeltableDao;
import main.database.dao.SmithableDao;
import main.database.dao.SpriteFrameDao;
import main.database.dao.SpriteMapDao;
import main.database.dao.TeleportableDao;
import main.database.dao.UseItemOnItemDao;
import main.processing.DatabaseUpdater;
import main.processing.NPCManager;
import main.processing.PathFinder;
import main.processing.ShopManager;
import main.processing.WorldProcessor;
import main.responses.CachedResourcesResponse;
import main.responses.ExamineResponse;

public class Server {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		org.glassfish.tyrus.server.Server server = new org.glassfish.tyrus.server.Server("localhost", 45555, "/ws", null, Endpoint.class);
		org.glassfish.tyrus.server.Server resourceServer = new org.glassfish.tyrus.server.Server("localhost", 45556, "/ws", null, ResourceEndpoint.class);
		
		try {
			resourceServer.start();
			server.start();
			
			setupCaches();
			
			WorldProcessor processor = new WorldProcessor();
			processor.start();
			
			DatabaseUpdater dbUpdater = new DatabaseUpdater();
			dbUpdater.start();
			
			System.out.println("press any key to quit");
			new Scanner(System.in).nextLine();
		} catch (DeploymentException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			server.stop();
		}
	}
	
	private static void setupCaches() throws IOException {
		System.out.println("caching sprite frames");
		SpriteFrameDao.setupCaches();
		
		System.out.println("caching animations");
		AnimationDao.setupCaches();
		
		System.out.println("caching sprite maps");
		SpriteMapDao.setupCaches();

		System.out.println("caching context options");
		ContextOptionsDao.setupCaches();
		
		System.out.println("caching base player animations");
		PlayerAnimationDao.setupCaches();
		
		System.out.println("caching distinct roomIds");
		GroundTextureDao.cacheDistinctFloors(); // what constitutes a room is having at least one ground texture: no ground textures, the room doesnt exist.
		
		System.out.println("caching ground texture instances");
		GroundTextureDao.cacheTileIdsByGroundTextureId();
		
		System.out.println("caching scenery");
		SceneryDao.setupCaches();
		
		System.out.println("caching minimap segments");
		MinimapSegmentDao.setupCaches();
		
		System.out.println("caching doors");
		DoorDao.setupCaches();
		
		System.out.println("initializing pathfinder");
		PathFinder.get();// init the path nodes and relationships
		
		System.out.println("initializing examine map");
		ExamineResponse.initializeExamineMap();// all the scenery examine
		
		System.out.println("caching mineables");
		MineableDao.setupCaches();// mineable tiles, mineable objects
		
		System.out.println("caching items");
		ItemDao.setupCaches();
		
		System.out.println("caching player storage");
		PlayerStorageDao.cachePlayerStorage();
		
		System.out.println("caching ground textures");
		GroundTextureDao.cacheTextures();
		
		System.out.println("caching player stuff");
		PlayerDao.setupCaches();
		
		System.out.println("caching npcs");
		NPCManager.get().loadNpcs();
		
		System.out.println("caching npc messages");
		NpcMessageDao.setupCaches();
		
		System.out.println("caching consumables");
		ConsumableDao.cacheConsumables();
		
		System.out.println("caching consumable effects");
		ConsumableDao.cacheConsumableEffects();
		
		System.out.println("caching cookables");
		CookableDao.cacheCookables();
		
		System.out.println("caching catchables");
		CatchableDao.cacheCatchables();
		
		System.out.println("caching item -> item usage");
		UseItemOnItemDao.cacheData();
		
		System.out.println("caching equipment");
		EquipmentDao.setupCaches();
		
		System.out.println("caching brewables");
		BrewableDao.cacheBrewables();
		
		System.out.println("caching shops");
		ShopDao.setupCaches();
		
		System.out.println("setting up shops");
		ShopManager.setupShops();
		
		System.out.println("caching respawnables");
		RespawnableDao.setupCaches();// ground items that respawn once picked up
		
		System.out.println("initializing ground item manager");
		GroundItemManager.initialize();
		
		System.out.println("caching pickables");
		PickableDao.setupCaches();
		
		System.out.println("caching castables");
		CastableDao.setupCaches();
		
		System.out.println("caching teleportables");
		TeleportableDao.setupCaches();
		
		System.out.println("caching fishables");
		FishableDao.setupCaches();
		
		System.out.println("caching reinforcement bonuses");
		ReinforcementBonusesDao.setupCaches();
		
		System.out.println("caching prayers");
		PrayerDao.setupCaches();
		
		System.out.println("caching buryables");
		BuryableDao.setupCaches();
		
		System.out.println("caching climbables");
		ClimbableDao.setupCaches();
		
		System.out.println("caching smithables");
		SmithableDao.setupCaches();
		
		System.out.println("caching smeltables");
		SmeltableDao.setupCaches();
		
		System.out.println("caching choppables");
		ChoppableDao.setupCaches();
		
		System.out.println("caching sawmill");
		SawmillableDao.setupCaches();
		
		System.out.println("caching constructables");
		ConstructableDao.setupCaches();
		
		System.out.println("caching client resources");
		// should be last after all the other caches are set up
		CachedResourcesResponse.get();// loads/caches all the sprite maps, scenery etc and gets sent on client load
	}
}

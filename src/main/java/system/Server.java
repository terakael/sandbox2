package system;
import java.io.IOException;
import java.util.Scanner;

import javax.websocket.DeploymentException;

import database.dao.AnimationDao;
import database.dao.ArtisanMasterDao;
import database.dao.BrewableDao;
import database.dao.BuryableDao;
import database.dao.CastableDao;
import database.dao.CatchableDao;
import database.dao.ChoppableDao;
import database.dao.ClimbableDao;
import database.dao.ConstructableDao;
import database.dao.ConsumableDao;
import database.dao.ContextOptionsDao;
import database.dao.CookableDao;
import database.dao.DoorDao;
import database.dao.EquipmentDao;
import database.dao.FishableDao;
import database.dao.GroundTextureDao;
import database.dao.ItemDao;
import database.dao.MineableDao;
import database.dao.MinimapSegmentDao;
import database.dao.NpcMessageDao;
import database.dao.PickableDao;
import database.dao.PlayerAnimationDao;
import database.dao.PlayerArtisanTaskBreakdownDao;
import database.dao.PlayerArtisanTaskDao;
import database.dao.PlayerDao;
import database.dao.PlayerTybaltsTaskDao;
import database.dao.PrayerDao;
import database.dao.ReinforcementBonusesDao;
import database.dao.RespawnableDao;
import database.dao.SawmillableDao;
import database.dao.SceneryDao;
import database.dao.ShopDao;
import database.dao.SmeltableDao;
import database.dao.SmithableDao;
import database.dao.SpriteFrameDao;
import database.dao.SpriteMapDao;
import database.dao.TeleportableDao;
import database.dao.UndeadArmyWavesDao;
import database.dao.UseItemOnItemDao;
import processing.PathFinder;
import processing.WorldProcessor;
import processing.managers.ArtisanManager;
import processing.managers.DatabaseUpdater;
import processing.managers.NPCManager;
import processing.managers.ShopManager;
import responses.CachedResourcesResponse;
import responses.ExamineResponse;

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
		
		System.out.println("caching sawmillables");
		SawmillableDao.setupCaches();
		
		System.out.println("caching constructables");
		ConstructableDao.setupCaches();
		
		System.out.println("caching tybalt's tasks");
		PlayerTybaltsTaskDao.setupCaches();
		
		System.out.println("caching undead army waves");
		UndeadArmyWavesDao.setupCaches();
		
		System.out.println("caching artisan items");
		ArtisanManager.setupCaches();
		
		System.out.println("caching artisan task breakdown");
		PlayerArtisanTaskBreakdownDao.setupCaches();
		
		System.out.println("caching artisan tasks");
		PlayerArtisanTaskDao.setupCaches();
		
		System.out.println("caching artisan masters");
		ArtisanMasterDao.setupCaches();
		
		System.out.println("caching client resources");
		// should be last after all the other caches are set up
		CachedResourcesResponse.get();// loads/caches all the sprite maps, scenery etc and gets sent on client load
	}
}

package system;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javax.websocket.DeploymentException;

import database.dao.AnimationDao;
import database.dao.ArtisanEnhanceableItemsDao;
import database.dao.ArtisanMasterDao;
import database.dao.ArtisanShopStockDao;
import database.dao.ArtisanTaskItemReplacementDao;
import database.dao.ArtisanToolEquivalentDao;
import database.dao.BaseAnimationsDao;
import database.dao.BrewableDao;
import database.dao.BuryableDao;
import database.dao.CastableDao;
import database.dao.CatchableDao;
import database.dao.ChoppableDao;
import database.dao.ClimbableDao;
import database.dao.ClockDao;
import database.dao.ColourPaletteDao;
import database.dao.ConstructableDao;
import database.dao.ConsumableDao;
import database.dao.ContextOptionsDao;
import database.dao.CookableDao;
import database.dao.DialogueDao;
import database.dao.DoorDao;
import database.dao.EmptyableDao;
import database.dao.EquipmentDao;
import database.dao.FishableDao;
import database.dao.FishingDepthDao;
import database.dao.GroundTextureDao;
import database.dao.ItemDao;
import database.dao.MineableDao;
import database.dao.MinimapSegmentDao;
import database.dao.NPCDao;
import database.dao.NpcMessageDao;
import database.dao.PetDao;
import database.dao.PickableDao;
import database.dao.PlayerArtisanBlockedTaskDao;
import database.dao.PlayerArtisanTaskBreakdownDao;
import database.dao.PlayerArtisanTaskDao;
import database.dao.PlayerBaseAnimationsDao;
import database.dao.PlayerDao;
import database.dao.PlayerTybaltsTaskDao;
import database.dao.PrayerDao;
import database.dao.ReinforcementBonusesDao;
import database.dao.RespawnableDao;
import database.dao.SawmillableDao;
import database.dao.SceneryDao;
import database.dao.ShipAccessoryDao;
import database.dao.ShipDao;
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
import processing.managers.BankManager;
import processing.managers.ConstructableManager;
import processing.managers.DatabaseUpdater;
import processing.managers.HousePetsManager;
import processing.managers.HousingManager;
import processing.managers.ShopManager;
import processing.managers.UndeadArmyManager;
import processing.managers.WallManager;
import processing.managers.WanderingPetManager;
import responses.CachedResourcesResponse;
import responses.ExamineResponse;

public class Server {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		org.glassfish.tyrus.server.Server server = new org.glassfish.tyrus.server.Server("0.0.0.0", 45555, "/",
				null, Endpoint.class);
		org.glassfish.tyrus.server.Server resourceServer = new org.glassfish.tyrus.server.Server("0.0.0.0", 45556,
				"/", null, ResourceEndpoint.class);
		org.glassfish.tyrus.server.Server builderServer = new org.glassfish.tyrus.server.Server("localhost", 45557,
				"/ws", null, builder.system.Endpoint.class);

		try {
			resourceServer.start();
			server.start();
			builderServer.start();

			setupCaches();

			WorldProcessor processor = new WorldProcessor();
			processor.start();

			DatabaseUpdater dbUpdater = new DatabaseUpdater();
			dbUpdater.start();

			System.out.println("press any key to quit");
			// new Scanner(System.in).nextLine();
			new CountDownLatch(1).await();
		} catch (DeploymentException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			server.stop();
		}
	}

	private static void setupCaches() throws IOException, URISyntaxException {
		System.out.println("caching player names");
		PlayerDao.setupCaches();

		System.out.println("caching sprite frames");
		SpriteFrameDao.setupCaches();

		System.out.println("caching animations");
		AnimationDao.setupCaches();

		System.out.println("caching sprite maps");
		SpriteMapDao.setupCaches();

		System.out.println("caching context options");
		ContextOptionsDao.setupCaches();

		System.out.println("caching base player animations");
		BaseAnimationsDao.setupCaches();
		PlayerBaseAnimationsDao.setupCaches();

		System.out.println("caching ground textures");
		GroundTextureDao.setupCaches();

		System.out.println("caching scenery");
		SceneryDao.setupCaches();

		System.out.println("caching minimap segments");
		MinimapSegmentDao.setupCaches();

		System.out.println("caching doors");
		DoorDao.setupCaches();

		System.out.println("caching walls");
		WallManager.setupCaches();

		System.out.println("initializing pathfinder");
		PathFinder.get();// init the path nodes and relationships

		System.out.println("initializing examine map");
		ExamineResponse.initializeExamineMap();// all the scenery examine

		System.out.println("caching mineables");
		MineableDao.setupCaches();// mineable tiles, mineable objects

		System.out.println("caching items");
		ItemDao.setupCaches();

		System.out.println("caching npcs");
		NPCDao.setupCaches();

		System.out.println("caching npc messages");
		NpcMessageDao.setupCaches();

		System.out.println("caching dialogue");
		DialogueDao.setupCaches();

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
		UndeadArmyManager.init();

		System.out.println("caching artisan items");
		ArtisanManager.setupCaches();

		System.out.println("caching artisan task breakdown");
		PlayerArtisanTaskBreakdownDao.setupCaches();

		System.out.println("caching artisan tasks");
		PlayerArtisanTaskDao.setupCaches();

		System.out.println("caching artisan masters");
		ArtisanMasterDao.setupCaches();

		System.out.println("caching artisan shop stock");
		ArtisanShopStockDao.setupCaches();

		System.out.println("caching artisan enhanceable items");
		ArtisanEnhanceableItemsDao.setupCaches();

		System.out.println("caching artisan blocked tasks");
		PlayerArtisanBlockedTaskDao.setupCaches();

		System.out.println("caching artisan task replacements");
		ArtisanTaskItemReplacementDao.setupCaches();

		System.out.println("caching artisan tool equivalents");
		ArtisanToolEquivalentDao.setupCaches();

		System.out.println("caching pets");
		PetDao.setupCaches();

		System.out.println("caching wandering pets");
		WanderingPetManager.get().loadWanderingPets();

		System.out.println("caching emptyables");
		EmptyableDao.setupCaches();

		System.out.println("caching clocks");
		ClockDao.setupCaches();

		System.out.println("caching housing");
		HousingManager.setupCaches();

		System.out.println("caching housing constructables");
		ConstructableManager.setupCaches();

		System.out.println("caching house pets");
		HousePetsManager.setupCaches();

		System.out.println("caching ships");
		ShipDao.setupCaches();

		System.out.println("caching ship accessories");
		ShipAccessoryDao.setupCaches();

		System.out.println("caching fishing depths");
		FishingDepthDao.setupCaches();

		System.out.println("caching banks");
		BankManager.setupCaches();

		System.out.println("caching colour palette");
		ColourPaletteDao.setupCaches();

		System.out.println("caching client resources");
		// should be last after all the other caches are set up
		CachedResourcesResponse.get();// loads/caches all the sprite maps, scenery etc and gets sent on client load
	}
}

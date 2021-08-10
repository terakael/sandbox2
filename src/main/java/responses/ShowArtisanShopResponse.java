package responses;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.dao.ArtisanEnhanceableItemsDao;
import database.dao.ArtisanMasterDao;
import database.dao.ArtisanShopStockDao;
import database.dao.ArtisanTaskOptionsDao;
import database.dao.PlayerArtisanTaskDao;
import database.dao.PlayerStorageDao;
import database.dto.ArtisanEnhanceableItemsDto;
import database.dto.ArtisanTaskOptionsDto;
import database.dto.PlayerArtisanTaskDto;
import processing.WorldProcessor;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import processing.managers.LocationManager;
import requests.Request;
import system.GroundItemManager;
import types.ArtisanShopTabs;
import types.StorageTypes;

@SuppressWarnings("unused")
public class ShowArtisanShopResponse extends Response {
	private int points = 0; 
	private ArtisanShopTabs selectedTab;
	private ArtisanShopTabs[] tabs;
	
	// for selectedTab == task
	private Set<ArtisanTaskOptionsDto> taskOptions = null;
	
	// for selectedTab == enhance
	private Set<ArtisanEnhanceableItemsDto> enhanceableItems = null;
	
	// for selectedTab == shop
	private Map<Integer, Integer> shopStock = null; // itemId, numPoints

	public ShowArtisanShopResponse(ArtisanShopTabs selectedTab) {
		setAction("show_artisan_shop");
		this.selectedTab = selectedTab;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		final boolean playerIsNearArtisanMaster = LocationManager.getLocalNpcs(player.getFloor(), player.getTileId(), 5, WorldProcessor.isDaytime()).stream()
				.map(npc -> npc.getDto().getId())
				.collect(Collectors.toSet())
				.containsAll(ArtisanMasterDao.getAllArtisanMasterNpcIds());
		if (!playerIsNearArtisanMaster)
			return;
		
		final PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(player.getId());
		points = task == null ? 0 : task.getTotalPoints();
		tabs = ArtisanShopTabs.values();
		
		switch (selectedTab) {
		case task: {
			taskOptions = ArtisanTaskOptionsDao.getTaskOptions();
			ClientResourceManager.addSpriteFramesAndSpriteMaps(player, taskOptions.stream().map(ArtisanTaskOptionsDto::getIconId).collect(Collectors.toSet()));
			break;
		}
		case enhance: {
			final List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			
			enhanceableItems = ArtisanEnhanceableItemsDao.getEnhanceableItems().entrySet().stream()
				.filter(entry -> invItemIds.contains(entry.getKey()))
				.map(Map.Entry::getValue)
				.collect(Collectors.toSet());
			
			// we already have the unenhanced versions as they're in our inventory, so we just need to add the enhanced ones
			ClientResourceManager.addItems(player, new HashSet<>(enhanceableItems.stream()
					.map(ArtisanEnhanceableItemsDto::getEnhancedItemId)
					.collect(Collectors.toSet())));
			break;
		}
		case shop: {
			shopStock = ArtisanShopStockDao.getShopStock().entrySet().stream()
				.filter(e -> !PlayerStorageDao.itemExistsInPlayerStorage(player.getId(), e.getKey()) && 
								!GroundItemManager.itemIsOnGround(player.getId(), e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			ClientResourceManager.addItems(player, shopStock.keySet());
			break;
		}
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}

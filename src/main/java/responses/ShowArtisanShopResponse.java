package responses;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.dao.ArtisanEnhanceableItemsDao;
import database.dao.ArtisanShopStockDao;
import database.dao.PlayerArtisanBlockedTaskDao;
import database.dao.PlayerArtisanTaskBreakdownDao;
import database.dao.PlayerArtisanTaskDao;
import database.dao.PlayerStorageDao;
import database.dto.ArtisanEnhanceableItemsDto;
import database.dto.PlayerArtisanBlockedTaskDto;
import database.dto.PlayerArtisanTaskDto;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import processing.managers.ClientResourceManager;
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
//	private Set<ArtisanTaskOptionsDto> taskOptions = null;
	private Integer currentTaskItemId = null;
	private Integer numAssigned = null;
	private Integer numCompleted = null;
	private Map<Integer, Integer> blockedTaskItemIds = null;
	
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
		if (!ArtisanManager.playerIsNearMaster(player))
			return;
		
		final PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(player.getId());
		points = task == null ? 0 : task.getTotalPoints();
		tabs = ArtisanShopTabs.values();
		
		switch (selectedTab) {
		case task: {
			currentTaskItemId = task.getItemId();
			numAssigned = task.getAssignedAmount();
			numCompleted = numAssigned - PlayerArtisanTaskBreakdownDao.getRemainingTaskCount(player.getId(), currentTaskItemId);
			ClientResourceManager.addItems(player, Collections.singleton(currentTaskItemId));
			
			PlayerArtisanBlockedTaskDto blockedTaskDto = PlayerArtisanBlockedTaskDao.getBlockedTasksByPlayerId(player.getId());
			blockedTaskItemIds = new LinkedHashMap<>();
			blockedTaskItemIds.put(1, blockedTaskDto.getItem1());
			blockedTaskItemIds.put(2, blockedTaskDto.getItem2());
			blockedTaskItemIds.put(3, blockedTaskDto.getItem3());
			blockedTaskItemIds.put(4, blockedTaskDto.getItem4());
			blockedTaskItemIds.put(5, blockedTaskDto.getItem5());
			ClientResourceManager.addItems(player, new HashSet<>(blockedTaskItemIds.values()));
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

package responses;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.dao.ArtisanShopStockDao;
import database.dao.ArtisanTaskOptionsDao;
import database.dao.ItemDao;
import database.dao.PlayerArtisanTaskDao;
import database.dao.PlayerStorageDao;
import database.dto.ArtisanTaskOptionsDto;
import database.dto.InventoryItemDto;
import database.dto.PlayerArtisanTaskDto;
import processing.attackable.Player;
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
	private Set<ArtisanTaskOptionsDto> taskOptions = null;
	
	// for selectedTab == enhance
	private List<InventoryItemDto> enhanceableItems = null;
	
	// for selectedTab == shop
	private Map<Integer, Integer> shopStock = null; // itemId, numPoints

	public ShowArtisanShopResponse(ArtisanShopTabs selectedTab) {
		setAction("show_artisan_shop");
		this.selectedTab = selectedTab;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO need to be in range of an artisan master
		
		final PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(player.getId());
		points = task == null ? 0 : task.getTotalPoints();
		tabs = ArtisanShopTabs.values();
		
		switch (selectedTab) {
		case task:
			taskOptions = ArtisanTaskOptionsDao.getTaskOptions();
			ClientResourceManager.addSpriteFramesAndSpriteMaps(player, taskOptions.stream().map(ArtisanTaskOptionsDto::getIconId).collect(Collectors.toSet()));
			break;
		case enhance:
			enhanceableItems = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.INVENTORY).values().stream()
				.filter(e -> ItemDao.getMaxCharges(e.getItemId()) > 0 && e.getCharges() == ItemDao.getMaxCharges(e.getItemId()))
				.collect(Collectors.toList());
			
			// TODO client resource manager for the enhanced versions (vial -> flask etc)
			break;
		case shop:
			shopStock = ArtisanShopStockDao.getShopStock().entrySet().stream()
				.filter(e -> !PlayerStorageDao.itemExistsInPlayerStorage(player.getId(), e.getKey()) && 
								!GroundItemManager.itemIsOnGround(player.getId(), e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			ClientResourceManager.addItems(player, shopStock.keySet());
			break;
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}

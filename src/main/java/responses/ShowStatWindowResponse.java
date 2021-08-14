package responses;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import database.dao.StatsDao;
import database.dto.ArtisanMaterialChainDto;
import database.dto.StatWindowRowDto;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import processing.managers.ClientResourceManager;
import requests.Request;
import requests.ShowStatWindowRequest;
import types.ConstructionSkillWindowTabs;
import types.SmithingSkillWindowTabs;
import types.Stats;

public class ShowStatWindowResponse extends Response {
	public ShowStatWindowResponse() {
		setAction("show_stat_window");
	}
	
	private int statId = 0;
	private List<StatWindowRowDto> rows = null;
	private List<ArtisanMaterialChainDto> artisanData = null;

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ShowStatWindowRequest))
			return;
		
		ShowStatWindowRequest request = (ShowStatWindowRequest)req;
		
		statId = StatsDao.getStatIdByName(request.getStat());
		if (statId == -1)
			return;
		
		Stats stat = Stats.withValue(statId);
		if (stat == null)
			return;
		
		switch (stat) {
		case ARTISAN:
			artisanData = ArtisanManager.getTaskList(player.getId());
			ClientResourceManager.addItems(player, artisanData.stream()
					.flatMap(ArtisanMaterialChainDto::flattened)
					.map(ArtisanMaterialChainDto::getItemId)
					.collect(Collectors.toSet()));
			break;
			
		case SMITHING:
			new ShowSmithingSkillWindowResponse(SmithingSkillWindowTabs.copper).process(null, player, responseMaps);
			return;
			
		case MINING:
			new ShowMiningSkillWindowResponse().process(null, player, responseMaps);
			return;
			
		case WOODCUTTING:
			new ShowWoodcuttingSkillWindowResponse().process(null, player, responseMaps);
			return;
			
		case FISHING:
			new ShowFishingSkillWindowResponse().process(null, player, responseMaps);
			return;
			
		case COOKING:
			new ShowCookingSkillWindowResponse().process(null, player, responseMaps);
			return;
			
		case MAGIC:
			new ShowMagicSkillWindowResponse().process(null, player, responseMaps);
			return;
			
		case CONSTRUCTION:
			new ShowConstructionSkillWindowResponse(ConstructionSkillWindowTabs.fires).process(null, player, responseMaps);
			return;
			
		default:
			rows = StatsDao.getStatWindowRows().get(stat);
			break;
		}
		
//		if (stat == Stats.ARTISAN) {
//			// artisan has a dynamic task check so it's handled differently
//			
//			artisanData = ArtisanManager.getTaskList(player.getId());
//			ClientResourceManager.addItems(player, artisanData.stream()
//					.flatMap(ArtisanMaterialChainDto::flattened)
//					.map(ArtisanMaterialChainDto::getItemId)
//					.collect(Collectors.toSet()));
//			
//		} else if (stat == Stats.SMITHING) {
//			new ShowSmithingSkillWindowResponse(SmithingSkillWindowTabs.copper).process(null, player, responseMaps);
//			return;
//		} else {
//			// potentially null; expected behaviour (handled on the client side)
//			rows = StatsDao.getStatWindowRows().get(stat);
//		}
		
		if (rows != null) {
			// if the client hasn't been sent the appropriate resources to show this window, send them now.
			Set<Integer> statWindowItemIds = new HashSet<>();
			for (StatWindowRowDto dto : rows) {
				statWindowItemIds.add(dto.getItemId());
				
				if (dto.getItemId2() != null)
					statWindowItemIds.add(dto.getItemId2());
				
				if (dto.getItemId3() != null)
					statWindowItemIds.add(dto.getItemId3());
				
				if (dto.getItemId4() != null)
					statWindowItemIds.add(dto.getItemId4());
			}
			ClientResourceManager.addItems(player, statWindowItemIds);
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}

package processing.scenery.constructable;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import database.dto.ConstructableDto;
import processing.attackable.Player;
import responses.ResponseMaps;
import system.GroundItemManager;
import types.Items;
import utils.RandomUtil;

public class NaturesShrine extends RadialConstructable {
	private List<Integer> flowerIds; // list due to random access
	private Set<Integer> recentTileIds = new LinkedHashSet<>();
	
	private final static int GROW_TIMER = 5;
	private int growOffset;
	
	public NaturesShrine(int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile) {
		super(floor, tileId, lifetimeTicks, dto, onHousingTile, 2);
		
		growOffset = RandomUtil.getRandom(0, GROW_TIMER); // used so flowers grow on different ticks when there's multiple Natures Shrines around.  Looks weird otherwise.
		
		flowerIds = List.<Integer>of(
					Items.ORANGE_HARNIA.getValue(),
					Items.RED_RUSSINE.getValue(),
					Items.SKY_FLOWER.getValue(),
					Items.DARK_BLUEBELL.getValue(),
					Items.STARFLOWER.getValue()
				);
	}

	@Override
	public void processConstructable(int tickId, ResponseMaps responseMaps) {		
		recentTileIds.addAll(getPlayersOnAffectingTileIds().stream()
				.map(Player::getTileId)
				.filter(t -> t != tileId) // don't spawn flowers on top of the shrine
				.collect(Collectors.toCollection(LinkedHashSet::new)));
		
		if (tickId % GROW_TIMER == growOffset) {
			Integer maxFlowersPerTimer = 2;
	
			final Iterator<Integer> iter = recentTileIds.iterator();
			while (iter.hasNext()) {
				int nextTileId = iter.next();
				iter.remove();
				if (GroundItemManager.getGlobalItemCountAtTileId(floor, nextTileId) == 0) {
					GroundItemManager.addGlobally(floor, nextTileId, flowerIds.get(RandomUtil.getRandom(0, flowerIds.size())));
					if (--maxFlowersPerTimer == 0)
						return;
				}
			}
		}
	}
}

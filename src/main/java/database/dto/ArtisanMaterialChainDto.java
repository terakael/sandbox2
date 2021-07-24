package database.dto;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import lombok.Getter;

public class ArtisanMaterialChainDto {	
	@Getter private int itemId;
	private int completedAmount;
	private int assignedAmount;
	private List<ArtisanMaterialChainDto> childMaterial;
	
	public ArtisanMaterialChainDto(int itemId, int completedAmount, int assignedAmount) {
		this.itemId = itemId;
		this.completedAmount = completedAmount;
		this.assignedAmount = assignedAmount;
		this.childMaterial = new LinkedList<>();
	}
	
	public Stream<ArtisanMaterialChainDto> flattened() {
        return Stream.concat(
                Stream.of(this),
                childMaterial.stream().flatMap(ArtisanMaterialChainDto::flattened));
    }
	
	public void addChild(ArtisanMaterialChainDto child) {
		childMaterial.add(child);
	}
}

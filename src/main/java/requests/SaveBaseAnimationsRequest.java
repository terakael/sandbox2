package requests;

import java.util.List;

import database.dto.SaveBaseAnimationsDto;
import lombok.Getter;

@Getter
public class SaveBaseAnimationsRequest extends Request {
	private List<SaveBaseAnimationsDto> animations;
}

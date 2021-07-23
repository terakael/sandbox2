package requests;

import lombok.Getter;

@Getter
public class StorageMoveRequest extends Request {
	private int src;
	private int dest;
}

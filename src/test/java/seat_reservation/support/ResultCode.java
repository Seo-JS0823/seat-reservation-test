package seat_reservation.support;

public enum ResultCode {

	SUCCESS("success"),
	
	;
	private final String code;
	
	ResultCode(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}
}

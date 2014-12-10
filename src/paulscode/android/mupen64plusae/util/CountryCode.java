package paulscode.android.mupen64plusae.util;

public enum CountryCode {
	GERMANY(0x44),
	USA(0x45),
	FRANCE(0x46),
	ITALY(0x49),
	JAPAN(0x4a),
	KOREA(0x4b),
	EUROPE(0x50),
	JAPAN_KOREA(0x51),
	SPAIN(0x53),
	UNKNOWN(0);
	
	private short value;
	
	private CountryCode(int value) {
		this.value = (short)value;
	}
	
	public short getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		switch(this) {
		case GERMANY:
			return "(G)";
		case USA:
			return "(U)";
		case FRANCE:
			return "(F)";
		case ITALY:
			return "(I)";
		case JAPAN:
			return "(J)";
		case KOREA:
			return "(K)";
		case EUROPE:
			return "(E)";
		case JAPAN_KOREA:
			return "(1)";
		case SPAIN:
			return "(S)";
		default:
			return "";
		}
	}
}

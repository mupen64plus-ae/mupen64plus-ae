package paulscode.android.mupen64plusae.util;

public enum CountryCode {
	UNKNOWN(0xFF, "(Unknown)"),
	DEMO(0x00, "(Demo)"),
	BETA(0x07, "(Beta)"),
	JAPAN_USA(0x41, "(JU)"),
	GERMANY(0x44, "(G)"),
	USA(0x45, "(U)"),
	FRANCE(0x46, "(F)"),
	ITALY(0x49, "(I)"),
	JAPAN(0x4a, "(J)"),
	KOREA(0x4b, "(K)"),
	JAPAN_KOREA(0x51, "(JK)"),
	SPAIN(0x53, "(S)"),
	AUSTRALIA(0x55, "(A)"),
	AUSTRALIA_ALT(0x59, "(A)"),
	EUROPE_1(0x50, "(E)"),
	EUROPE_2(0x58, "(E)"),
	EUROPE_3(0x20, "(E)"),
	EUROPE_4(0x21, "(E)"),
	EUROPE_5(0x38, "(E)"),
	EUROPE_6(0x70, "(E)");
	
	private byte value;
	private String text;
	
	private CountryCode(int value, String text) {
		this.value = (byte)value;
		this.text = text;
	}
	
	public byte getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return this.text;
	}

	public static CountryCode getCountryCode(int value)
	{
		CountryCode[] codes = CountryCode.values();
		for (CountryCode code : codes)
			if (code.getValue() == value)
				return code;
		return CountryCode.UNKNOWN;
	}
}

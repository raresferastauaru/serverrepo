public class SqlParam {
	private String type;
	private String value;
	
	public SqlParam(String type, String value) {
		this.type = type;
		this.value = value;
	}

	public String getType() {
		return this.type;
	}

	public String getValue() {
		return this.value;
	}
}

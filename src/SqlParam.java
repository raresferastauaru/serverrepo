public class SqlParam {
	private String _type;
	private String _value;
	
	public SqlParam(String type, String value) {
		this._type = type;
		this._value = value;
	}

	public String getType() {
		return this._type;
	}

	public String getValue() {
		return this._value;
	}
}

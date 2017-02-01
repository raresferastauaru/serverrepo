
public class FileHashDetails {
	
	private String hashCode;
	private String creationTime;
	private String lastWriteTime;
	private String isReadOnly;
	
	public FileHashDetails(String hashCode, String creationTime, String lastWriteTime, String isReadOnly) {
		this.hashCode = hashCode;
		this.creationTime = creationTime;
		this.lastWriteTime = lastWriteTime;
		this.isReadOnly = isReadOnly;
	}

	public String getHashCode() {
		return hashCode;
	}

	public String getCreationTime() {
		return creationTime;
	}

	public String getLastWriteTime() {
		return lastWriteTime;
	}

	public String getIsReadOnly() {
		return isReadOnly;
	}	
}
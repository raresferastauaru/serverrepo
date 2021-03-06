public class FileHashDetails {
	
	private String _fileName;
	private String _hashCode;
	private String _creationTime;
	private String _lastWriteTime;
	private String _isReadOnly;
	
	public FileHashDetails(String fileName, String hashCode, String creationTime, String lastWriteTime, String isReadOnly) {
		_fileName = fileName;
		_hashCode = hashCode;
		_creationTime = creationTime;
		_lastWriteTime = lastWriteTime;
		_isReadOnly = isReadOnly;
	}

	public String getFileName() {
		return _fileName;
	}

	public String getHashCode() {
		return _hashCode;
	}

	public String getCreationTime() {
		return _creationTime;
	}

	public String getLastWriteTime() {
		return _lastWriteTime;
	}

	public String getIsReadOnly() {
		return _isReadOnly;
	}	
	
	public String toString()
    {
        String str = "FileHash:"
                  + _hashCode + ", "
                  + _creationTime + ", "
                  + _lastWriteTime + ", "
                  + _isReadOnly;
        return str;
    }
}
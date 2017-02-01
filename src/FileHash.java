public class FileHash
{
	private Integer _hashId;
    private String _fullPath;
    private String _relativePath;
    private Integer _hashCode;

    public FileHash(Integer hashId, Integer hashCode, String fullPath)
    {
    	_hashId = hashId;
        _fullPath = fullPath;
        _relativePath = Helper.getRelativePath(_fullPath);
        _hashCode = hashCode;
    }

    public Integer getHashId()
    {
        return _hashId;
    }

    public String getFullPath()
    {
        return _fullPath;
    }

    public String getRelativePath(){
        return _relativePath;
    }

    public Integer getHashCode()
    {
        return _hashCode;
    }
}

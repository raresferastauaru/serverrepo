public class FileHash
{
	private Integer _hashId;
    private Integer _hashCode;
    private String _fullPath;
    private String _relativePath;

    public FileHash(Integer hashId, Integer hashCode, String fullPath)
    {
    	_hashId = hashId;
        _hashCode = hashCode;
        _fullPath = fullPath;
        _relativePath = Helper.getRelativePath(_fullPath);
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

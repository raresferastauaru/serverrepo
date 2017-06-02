public class OdroidNode
{
	private String _name;
    private String _ip;
    private String _port;

    public OdroidNode(String name, String ip, String port)
    {
        _name = name;
        _ip = ip;
        _port = port;
    }

    public String getName()
    {
        return _name;
    }

    public String getIP()
    {
        return _ip;
    }

    public String getPort()
    {
        return _port;
    }

    public byte[] getOdroidNodeInfos() 
    {
        String infos = _ip + ":" + _port + ":";
        byte[] infosBytes = infos.getBytes();
        return infosBytes;
    }
}

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

    public String getOdroidNodeFullInfos()
    {
        String infos = _name + ", " + _port + ", " + _port;
        return infos;
    }

    @Override public boolean equals(Object obj) 
    { 
        if (obj == this)  
            return true; 

        if (obj == null || obj.getClass() != this.getClass())
            return false; 
        
        OdroidNode odroidNode = (OdroidNode) obj; 

        return _name.equals(odroidNode.getName()) 
                && _ip.equals(odroidNode.getIP())
                && _port.equals(odroidNode.getPort());
    }
}

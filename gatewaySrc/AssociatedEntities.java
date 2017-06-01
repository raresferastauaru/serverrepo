public class AssociatedEntities
{
	private OdroidNode _odroidNode;
	private String _userName;

	public AssociatedEntities(OdroidNode odroidNode, String userName)
	{
		_odroidNode = odroidNode;
		_userName = userName;
	}

	public OdroidNode getOdroidNode()
	{
		return _odroidNode;
	}

	public String getUserName()
	{
		return _userName;
	}
}
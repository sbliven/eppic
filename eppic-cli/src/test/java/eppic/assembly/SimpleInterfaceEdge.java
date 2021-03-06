package eppic.assembly;

import javax.vecmath.Point3i;

public class SimpleInterfaceEdge implements InterfaceEdgeInterface {
	
	private int interfaceId;
	private int clusterId;
	
	public SimpleInterfaceEdge(int interfaceId, int clustertId) {
		this.interfaceId = interfaceId;
		this.clusterId = clustertId;
	}

	@Override
	public int getInterfaceId() {
		return interfaceId;
	}

	@Override
	public int getClusterId() {
		return clusterId;
	}

	@Override
	public Point3i getXtalTrans() {
		return new Point3i(0,0,0);
	}

	public String toString() {
		return interfaceId+"-"+clusterId;
	}

	@Override
	public void setInterfaceId(int interfaceId) {
		this.interfaceId = interfaceId;
		
	}

	@Override
	public void setClusterId(int clusterId) {
		this.clusterId = clusterId;
		
	}

	@Override
	public void setXtalTrans(Point3i xtalTrans) {
		// not implemented
	}

	@Override
	public boolean isIsologous() {
		// not implemented
		return false;
	}

	@Override
	public void setIsIsologous(boolean isIsologous) {
		// not implemented
		
	}

	@Override
	public boolean isInfinite() {
		// not implemented
		return false;
	}

	@Override
	public void setIsInfinite(boolean isInfinite) {
		// not implemented
		
	}
	
	@Override
	public String getXtalTransString() {
		return ""; // not implemented
	}

	
}

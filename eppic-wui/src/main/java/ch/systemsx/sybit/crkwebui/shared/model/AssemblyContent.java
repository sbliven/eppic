package ch.systemsx.sybit.crkwebui.shared.model;

import eppic.model.AssemblyContentDB;

public class AssemblyContent {

	private int uid;
	
	
	private int mmSize;
	private String symmetry;
	private String stoichiometry;
	private String composition;
	
	private Assembly assembly;
	
	public int getUid() {
		return uid;
	}
	
	public void setUid(int uid) {
		this.uid = uid;
	}
	
	public int getMmSize() {
		return mmSize;
	}
	
	public void setMmSize(int mmSize) {
		this.mmSize = mmSize;
	}
	
	public String getSymmetry() {
		return symmetry;
	}
	
	public void setSymmetry(String symmetry) {
		this.symmetry = symmetry;
	}
	
	public String getStoichiometry() {
		return stoichiometry;
	}
	
	public void setStoichiometry(String stoichiometry) {
		this.stoichiometry = stoichiometry;
	}
	
	public String getComposition() {
		return composition;
	}
	
	public void setComposition(String composition) {
		this.composition = composition;
	}
	
	public Assembly getAssembly() {
		return assembly;
	}

	public void setAssembly(Assembly assembly) {
		this.assembly = assembly;
	}

	public static AssemblyContent create(AssemblyContentDB assemblyContentDB) {
		AssemblyContent ac = new AssemblyContent();
		ac.setMmSize(assemblyContentDB.getMmSize());
		ac.setSymmetry(assemblyContentDB.getSymmetry());
		ac.setStoichiometry(assemblyContentDB.getStoichiometry());
		ac.setComposition(assemblyContentDB.getComposition());
		return ac;
	}
	
}

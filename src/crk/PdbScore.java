package crk;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class PdbScore implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String pdbName;
	private String pdbTitle;
	private ScoringType scoType;
	private boolean isScoreWeighted;
	private double bioCutoff;
	private double xtalCutoff;
	private int homologsCutoff; // min number homologs required
	private int minCoreSize; // total core size xtal-call cutoff
	private int minMemberCoreSize; // per-member core size xtal-call cutoff
	private double idCutoff; // sequence identity cutoff
	private double queryCovCutoff;
	private int maxNumSeqsCutoff; // max num sequences used
	private double[] bsaToAsaCutoffs;
	private double bsaToAsaSoftCutoff;
	private double bsaToAsaRelaxStep;
	private boolean zoomUsed;

	private Map<Integer,InterfaceScore> interfScores; 
	
	
	public PdbScore() {
		interfScores = new TreeMap<Integer,InterfaceScore>();
	}
	
	public void addInterfScore(InterfaceScore interfScore) {
		interfScores.put(interfScore.getId(), interfScore);
	}

	public String getPdbName() {
		return pdbName;
	}

	public void setPdbName(String pdbName) {
		this.pdbName = pdbName;
	}
	
	public String getPdbTitle() {
		return pdbTitle;
	}
	
	public void setPdbTitle(String pdbTitle) {
		this.pdbTitle = pdbTitle;
	}

	public ScoringType getScoType() {
		return scoType;
	}

	public void setScoType(ScoringType scoType) {
		this.scoType = scoType;
	}

	public boolean isScoreWeighted() {
		return isScoreWeighted;
	}

	public void setScoreWeighted(boolean isScoreWeighted) {
		this.isScoreWeighted = isScoreWeighted;
	}

	public double getBioCutoff() {
		return bioCutoff;
	}

	public void setBioCutoff(double bioCutoff) {
		this.bioCutoff = bioCutoff;
	}

	public double getXtalCutoff() {
		return xtalCutoff;
	}

	public void setXtalCutoff(double xtalCutoff) {
		this.xtalCutoff = xtalCutoff;
	}

	public int getHomologsCutoff() {
		return homologsCutoff;
	}

	public void setHomologsCutoff(int homologsCutoff) {
		this.homologsCutoff = homologsCutoff;
	}

	public int getMinCoreSize() {
		return minCoreSize;
	}

	public void setMinCoreSize(int minCoreSize) {
		this.minCoreSize = minCoreSize;
	}

	public int getMinMemberCoreSize() {
		return minMemberCoreSize;
	}

	public void setMinMemberCoreSize(int minMemberCoreSize) {
		this.minMemberCoreSize = minMemberCoreSize;
	}

	public double getIdCutoff() {
		return idCutoff;
	}

	public void setIdCutoff(double idCutoff) {
		this.idCutoff = idCutoff;
	}

	public double getQueryCovCutoff() {
		return queryCovCutoff;
	}

	public void setQueryCovCutoff(double queryCovCutoff) {
		this.queryCovCutoff = queryCovCutoff;
	}

	public int getMaxNumSeqsCutoff() {
		return maxNumSeqsCutoff;
	}

	public void setMaxNumSeqsCutoff(int maxNumSeqsCutoff) {
		this.maxNumSeqsCutoff = maxNumSeqsCutoff;
	}

	public double[] getBsaToAsaCutoffs() {
		return bsaToAsaCutoffs;
	}
	
	public void setBsaToAsaCutoffs(double[] bsaToAsaCutoffs) {
		this.bsaToAsaCutoffs = bsaToAsaCutoffs;
	}

	public double getBsaToAsaSoftCutoff() {
		return bsaToAsaSoftCutoff;
	}

	public void setBsaToAsaSoftCutoff(double bsaToAsaSoftCutoff) {
		this.bsaToAsaSoftCutoff = bsaToAsaSoftCutoff;
	}

	public double getBsaToAsaRelaxStep() {
		return bsaToAsaRelaxStep;
	}

	public void setBsaToAsaRelaxStep(double bsaToAsaRelaxStep) {
		this.bsaToAsaRelaxStep = bsaToAsaRelaxStep;
	}

	public boolean isZoomUsed() {
		return zoomUsed;
	}

	public void setZoomUsed(boolean zoomUsed) {
		this.zoomUsed = zoomUsed;
	}

	public InterfaceScore getInterfScore(int id) {
		return this.interfScores.get(id);
	}
	
	public Map<Integer,InterfaceScore> getInterfaceScoreMap()
	{
		return interfScores;
	}
	
}

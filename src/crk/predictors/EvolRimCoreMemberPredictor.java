package crk.predictors;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import owl.core.structure.InterfaceRimCore;
import owl.core.structure.Residue;

import crk.CRKParams;
import crk.CallType;
import crk.InterfaceEvolContext;
import crk.ScoringType;

public class EvolRimCoreMemberPredictor implements InterfaceTypePredictor {

	private static final Log LOGGER = LogFactory.getLog(EvolRimCoreMemberPredictor.class);
		
	private String callReason;
	private List<String> warnings;
	
	private InterfaceEvolContext iec;
	private int molecId;
	
//	private ScoringType scoringType;
	
	private double callCutoff;
	
	private double bsaToAsaCutoff;
	
	private double coreScore;
	private double rimScore;
	private double scoreRatio;
	
	private CallType call;
	
	public EvolRimCoreMemberPredictor(InterfaceEvolContext iec, int molecId) {
		this.iec = iec;
		this.molecId = molecId;
		this.warnings = new ArrayList<String>();
	}
	
	private boolean canDoEntropyScoring() {
		return iec.getChainEvolContext(molecId).hasQueryMatch();
	}
	
	@Override
	public CallType getCall() {
		
		int memberSerial = molecId+1;
		
		iec.getInterface().calcRimAndCore(bsaToAsaCutoff);
		InterfaceRimCore rimCore = iec.getInterface().getRimCore(molecId);
		
		int countsUnrelCoreRes = -1;
		int countsUnrelRimRes = -1;
		if (canDoEntropyScoring()) {
			List<Residue> unreliableCoreRes = iec.getUnreliableCoreRes(molecId);
			List<Residue> unreliableRimRes = iec.getUnreliableRimRes(molecId);
			countsUnrelCoreRes = unreliableCoreRes.size();
			countsUnrelRimRes = unreliableRimRes.size();
			String msg = iec.getReferenceMismatchWarningMsg(unreliableCoreRes,"core");
			if (msg!=null) {
				LOGGER.warn(msg);
				warnings.add(msg);
			}
			msg = iec.getReferenceMismatchWarningMsg(unreliableRimRes,"rim");
			if (msg!=null) {
				LOGGER.warn(msg);
				warnings.add(msg);
			}
		}
		
		call = null;

		if (!iec.isProtein(molecId)) {
			call = CallType.NO_PREDICTION;
			LOGGER.info("Interface "+iec.getInterface().getId()+", member "+memberSerial+" calls NOPRED because it is not a protein");
			callReason = memberSerial+": is not a protein";
		}
		else if (!canDoEntropyScoring()) {
			call = CallType.NO_PREDICTION;
			callReason = memberSerial+": no evol scores calculation could be performed (no uniprot query match)";
		}
		else if (!iec.hasEnoughHomologs(molecId)) {
			call = CallType.NO_PREDICTION;
			LOGGER.info("Interface "+iec.getInterface().getId()+", member "+memberSerial+" calls NOPRED because there are not enough homologs to calculate evolutionary scores");
			callReason = memberSerial+": there are only "+iec.getChainEvolContext(molecId).getNumHomologs()+
					" homologs to calculate evolutionary scores (at least "+iec.getMinNumSeqs()+" required)";
		} 
		else if (scoreRatio==-1) {
			// this happens whenever the value wasn't initialized, in practice it will 
			// happen when doing Ka/Ks scoring and for some reason (e.g. no CDS match for query) it can't be done 
			call = CallType.NO_PREDICTION;
			callReason = memberSerial+": no evol scores calculation could be performed";
		}
		else if (rimCore.getCoreSize()<CRKParams.MIN_NUMBER_CORE_RESIDUES_EVOL_SCORE) {
			call = CallType.NO_PREDICTION;
			callReason = memberSerial+": not enough core residues to calculate evolutionary score (at least "+CRKParams.MIN_NUMBER_CORE_RESIDUES_EVOL_SCORE+" needed)";
		}
		else if (((double)countsUnrelCoreRes/(double)rimCore.getCoreSize())>CRKParams.MAX_ALLOWED_UNREL_RES) {
			call = CallType.NO_PREDICTION;
			LOGGER.info("Interface "+iec.getInterface().getId()+", member "+memberSerial+" calls NOPRED because there are not enough reliable core residues ("+
					countsUnrelCoreRes+" unreliable residues out of "+rimCore.getCoreSize()+" residues in core)");
			callReason = memberSerial+": there are not enough reliable core residues: "+
					countsUnrelCoreRes+" unreliable out of "+rimCore.getCoreSize()+" in core";
		}
		else if (((double)countsUnrelRimRes/(double)rimCore.getRimSize())>CRKParams.MAX_ALLOWED_UNREL_RES) {
			call = CallType.NO_PREDICTION;
			LOGGER.info("Interface "+iec.getInterface().getId()+", member "+memberSerial+" calls NOPRED because there are not enough reliable rim residues ("+
					countsUnrelRimRes+" unreliable residues out of "+rimCore.getRimSize()+" residues in rim)");
			callReason = memberSerial+": there are not enough reliable rim residues: "+
					countsUnrelRimRes+" unreliable out of "+rimCore.getRimSize()+" in rim";
		}
		else {
			if (scoreRatio<callCutoff) {
				call = CallType.BIO;
				callReason = memberSerial+": score "+
						String.format("%4.2f",scoreRatio)+" is below cutoff ("+String.format("%4.2f", callCutoff)+")";
			} else if (scoreRatio>callCutoff) {
				call = CallType.CRYSTAL;
				callReason = memberSerial+": score "+
						String.format("%4.2f",scoreRatio)+" is above cutoff ("+String.format("%4.2f", callCutoff)+")";
			} else if (Double.isNaN(scoreRatio)) {
				call = CallType.NO_PREDICTION;
				callReason = memberSerial+": score is NaN";
			} 
		}

		
		return call;
	}

	@Override
	public String getCallReason() {
		return callReason;
	}

	@Override
	public List<String> getWarnings() {
		return warnings;
	}
	
	/**
	 * Calculates the entropy scores for this interface member.
	 * Subsequently use {@link #getCall()} and {@link #getScore()} to get the call and score
	 * @param weighted
	 */
	public void scoreEntropy(boolean weighted) {
		scoreInterfaceMember(weighted, ScoringType.ENTROPY);
		//scoringType = ScoringType.ENTROPY;
	}
	
//	/**
//	 * Calculates the ka/ks scores for this interface member.
//	 * Subsequently use {@link #getCall()} and {@link #getScore()} to get the call and score 
//	 * @param weighted
//	 */
//	public void scoreKaKs(boolean weighted) {
//		scoreInterfaceMember(weighted, ScoringType.KAKS);
//		scoringType = ScoringType.KAKS;
//	}
	
	private void scoreInterfaceMember(boolean weighted, ScoringType scoType) {	
		if (!canDoEntropyScoring()) {
			scoreRatio = Double.NaN;
			return;
		}
		iec.getInterface().calcRimAndCore(bsaToAsaCutoff);
		InterfaceRimCore rimCore = iec.getInterface().getRimCore(molecId);
		rimScore  = iec.calcScore(rimCore.getRimResidues(), molecId, scoType, weighted);
		coreScore = iec.calcScore(rimCore.getCoreResidues(),molecId, scoType, weighted);
		if (rimScore==0) {
			scoreRatio = CRKParams.SCORERATIO_INFINITY_VALUE;
		} else {
			scoreRatio = coreScore/rimScore;
		}
	}

	/**
	 * Gets the ratio score core over rim
	 * @return
	 */
	@Override
	public double getScore() {
		return scoreRatio;
	}
	
	public double getCoreScore() {
		return coreScore;
	}
	
	public double getRimScore() {
		return rimScore;
	}
	
	public void setCallCutoff(double callCutoff) {
		this.callCutoff = callCutoff;
	}
	
	public void setBsaToAsaCutoff(double bsaToAsaCutoff) { 
		this.bsaToAsaCutoff = bsaToAsaCutoff;
	}
	
	public void resetCall() {
		this.call = null;
		//this.scoringType = null;
		this.warnings = new ArrayList<String>();
		this.callReason = null;
		this.coreScore = -1;
		this.rimScore = -1;
		this.scoreRatio = -1;
	}
}

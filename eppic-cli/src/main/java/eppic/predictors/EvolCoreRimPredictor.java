package eppic.predictors;

import java.util.ArrayList;
import java.util.List;

import org.biojava.nbio.structure.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eppic.CallType;
import eppic.EppicParams;
import eppic.InterfaceEvolContext;


public class EvolCoreRimPredictor implements InterfaceTypePredictor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvolCoreRimPredictor.class);
	
	private InterfaceEvolContext iec;
	
	private String callReason;
	private List<String> warnings;
	
	private String[] callReasonSides; // temp array (length 2) to hold call reasons per side

	private CallType call; 
	
	private double score; 
	private double score1;
	private double score2;
	
	private boolean check1;
	private boolean check2;
	
	private CallType veto;
	
	private double callCutoff;
	
	private double bsaToAsaCutoff;
	private double minAsaForSurface;

	public EvolCoreRimPredictor(InterfaceEvolContext iec) {
		this.iec = iec;
		this.warnings = new ArrayList<String>();
		this.callReasonSides = new String[2];
		
		this.veto = null;
	}
	
	private boolean canDoEntropyScoring(int molecId) {
		return iec.getChainEvolContext(molecId).hasQueryMatch();
	}

	
	@Override
	public CallType getCall() {
		return call;
	}

	@Override
	public String getCallReason() {
		return callReason;
	}
	
	@Override
	public List<String> getWarnings() {
		return this.warnings;
	}

	@Override
	public void computeScores() {
		
		// pre-check and calculating scores
				
		check1 = checkInterfaceSide(InterfaceEvolContext.FIRST);
		check2 = checkInterfaceSide(InterfaceEvolContext.SECOND);
		
		score1 = scoreInterfaceSide(InterfaceEvolContext.FIRST);
		score2 = scoreInterfaceSide(InterfaceEvolContext.SECOND);
		
		// if a veto is present score is set to NaN (i.e. score is not used for decision)
		if (veto!=null) {
			score = Double.NaN;
		}		
		// the final score is the average of both sides if both can be scored or just one side if only one side can be scored		
		else if (check1 && check2) {
			score = (score1+score2)/2.0;
		} else if (check1) {
			score = score1;
		} else if (check2) {
			score = score2;
		} else {
			// if both check1 and check2 are false then we assign NaN
			score = Double.NaN;
		}

		
		// assigning call

		if (veto!=null) {
			call = veto;
			// callReason has to be assigned when assigning veto			
		}
		else if (!check1 && !check2) {
			call = CallType.NO_PREDICTION;
			callReason = callReasonSides[0]+"\n"+callReasonSides[1];
		}
		else {


			if (score<=callCutoff) {
				call = CallType.BIO;
			} else if (score>callCutoff) {
				call = CallType.CRYSTAL;
			} else if (Double.isNaN(score)) {
				call = CallType.NO_PREDICTION;
			} 		

			String belowAboveStr;
			if (call==CallType.BIO) belowAboveStr = "below";
			else belowAboveStr = "above";
			String reason = "Score "+String.format("%4.2f", score)+" is "+belowAboveStr+" cutoff ("+String.format("%4.2f", callCutoff)+")";
			if (check1 && !check2) reason += ". Based on side 1 only";
			else if (!check1 && check2) reason += ". Based on side 2 only";

			callReason = reason;

		}
	}

	private boolean checkInterfaceSide(int molecId) {
		
		String scoreType = "core-rim";
		
		List<Group> cores = null;
		if (molecId == InterfaceEvolContext.FIRST) {
			cores = iec.getInterface().getCoreResidues(bsaToAsaCutoff, minAsaForSurface).getFirst();
		} else if (molecId == InterfaceEvolContext.SECOND) {
			cores = iec.getInterface().getCoreResidues(bsaToAsaCutoff, minAsaForSurface).getSecond();
		}
		List<Group> rims = null;
		if (molecId == InterfaceEvolContext.FIRST) {
			rims = iec.getInterface().getRimResidues(bsaToAsaCutoff, minAsaForSurface).getFirst();
		} else if (molecId == InterfaceEvolContext.SECOND) {
			rims = iec.getInterface().getRimResidues(bsaToAsaCutoff, minAsaForSurface).getSecond();
		}
		
		// we don't do nopreds anymore for cases with unreliable residues, instead 
		// we only warn (done in generateInterfaceWarnings)
		// See issue https://github.com/eppic-team/eppic/issues/34
		generateInterfaceWarnings(cores,rims,molecId);		
		
		int interfaceId = iec.getInterface().getId();
		int memberSerial = molecId + 1;
		
		if (!InterfaceEvolContext.isProtein(iec.getInterface(), molecId)) {
			LOGGER.info("Interface "+interfaceId+", member "+memberSerial+": can't calculate "+scoreType+" score because it is not a protein");
			callReasonSides[molecId] = "Side "+ memberSerial+" is not a protein";
			return false;
		}
		if (!canDoEntropyScoring(molecId)) {
			LOGGER.info("Interface "+interfaceId+", member "+memberSerial+": can't calculate "+scoreType+" score because it has no UniProt reference");
			callReasonSides[molecId] = "Side "+memberSerial+" has no UniProt reference";
			return false;
		}
		if (!iec.hasEnoughHomologs(molecId)) {
			LOGGER.info("Interface "+interfaceId+", member "+memberSerial+": can't calculate "+scoreType+" score because there are not enough homologs");
			callReasonSides[molecId] = "Side "+memberSerial+" has only "+iec.getChainEvolContext(molecId).getNumHomologs()+
					" homologs (at least "+iec.getMinNumSeqs()+" required)";
			return false;
		} 
		if (cores.size()<EppicParams.MIN_NUMBER_CORE_RESIDUES_EVOL_SCORE) {

			// a special condition for core size, we don't want that if one side has too few cores, 
			// then the prediction is based only on the other side. We veto the whole interface scoring in this case
			callReason = "Not enough core residues (in at least 1 side) to calculate "+scoreType+
					" score. At least "+EppicParams.MIN_NUMBER_CORE_RESIDUES_EVOL_SCORE+" needed";
			veto = CallType.NO_PREDICTION;
			return false;
		}
		

		
		return true;
		
	}
	
	/**
	 * Generates warnings (to LOGGER and member variable) for given side of interface.
	 * @param cores
	 * @param rims
	 * @param molecId
	 * @return an array of size 2 with counts of unreliable core (index 0) and rim (index 1) residues
	 */
	private int[] generateInterfaceWarnings(List<Group>cores, List<Group> rims, int molecId) {
		
		int countUnrelCoreRes = -1;
		int countUnrelRimRes = -1;
		if (canDoEntropyScoring(molecId)) {
			List<Group> unreliableCoreRes = iec.getReferenceMismatchResidues(cores, molecId);
			List<Group> unreliableRimRes = iec.getReferenceMismatchResidues(rims, molecId);
			countUnrelCoreRes = unreliableCoreRes.size();
			countUnrelRimRes = unreliableRimRes.size();
			String msg = iec.getReferenceMismatchWarningMsg(unreliableCoreRes,"core");
			if (msg!=null) {
				LOGGER.info(msg);
				warnings.add(msg);
			}
			msg = iec.getReferenceMismatchWarningMsg(unreliableRimRes,"rim");
			if (msg!=null) {
				LOGGER.info(msg);
				warnings.add(msg);
			}
		}
		int[] unrelRes = {countUnrelCoreRes, countUnrelRimRes};
		return unrelRes;
	}
	
	private double scoreInterfaceSide(int molecId) {	
		if (!canDoEntropyScoring(molecId)) {			
			return Double.NaN;
		}
		double scoreRatio = Double.NaN;
		
		List<Group> cores = null;
		if (molecId == InterfaceEvolContext.FIRST) {
			cores = iec.getInterface().getCoreResidues(bsaToAsaCutoff, minAsaForSurface).getFirst();
		} else if (molecId == InterfaceEvolContext.SECOND) {
			cores = iec.getInterface().getCoreResidues(bsaToAsaCutoff, minAsaForSurface).getSecond();
		}
		List<Group> rims = null;
		if (molecId == InterfaceEvolContext.FIRST) {
			rims = iec.getInterface().getRimResidues(bsaToAsaCutoff, minAsaForSurface).getFirst();
		} else if (molecId == InterfaceEvolContext.SECOND) {
			rims = iec.getInterface().getRimResidues(bsaToAsaCutoff, minAsaForSurface).getSecond();
		}
		
		double rimScore  = iec.calcScore(rims, molecId);
		double coreScore = iec.calcScore(cores,molecId);
		
		int interfaceId = iec.getInterface().getId();
		LOGGER.info("Interface "+interfaceId+", member "+(molecId+1)+": average entropy of core "+String.format("%4.2f", coreScore));
		LOGGER.info("Interface "+interfaceId+", member "+(molecId+1)+": average entropy of rim "+String.format("%4.2f",rimScore));
		
		if (rimScore==0) {
			scoreRatio = EppicParams.SCORERATIO_INFINITY_VALUE;
		} else {
			scoreRatio = coreScore/rimScore;
		}
		
		return scoreRatio;
	}
	
	/**
	 * Gets the final score computed from both members of the interface.
	 * @return
	 */
	@Override
	public double getScore() {
		return score;
	}
	
	@Override
	public double getScore1() {
		return score1;
	}
	
	@Override
	public double getConfidence() {
		return CONFIDENCE_UNASSIGNED;
	}
	
	@Override
	public double getScore2() {
		return score2;
	}
	
	public void setCallCutoff(double callCutoff) {
		this.callCutoff = callCutoff;
	}
	
	public void setBsaToAsaCutoff(double bsaToAsaCutoff, double minAsaForSurface) {
		this.bsaToAsaCutoff = bsaToAsaCutoff;
		this.minAsaForSurface = minAsaForSurface;
	}
	
	
}

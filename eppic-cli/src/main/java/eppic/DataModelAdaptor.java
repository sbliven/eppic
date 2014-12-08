package eppic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Compound;
import org.biojava.bio.structure.PDBCrystallographicInfo;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.contact.GroupContact;
import org.biojava.bio.structure.contact.GroupContactSet;
import org.biojava.bio.structure.contact.StructureInterface;
import org.biojava.bio.structure.contact.StructureInterfaceCluster;
import org.biojava.bio.structure.contact.StructureInterfaceList;
import org.biojava.bio.structure.xtal.CrystalCell;
import org.biojava.bio.structure.xtal.SpaceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eppic.analysis.compare.BioUnitView;
import eppic.analysis.compare.InterfaceMatcher;
import eppic.analysis.compare.SimpleInterface;
import eppic.commons.sequence.Homolog;
import eppic.commons.util.Goodies;
import eppic.model.ContactDB;
import eppic.model.HomologDB;
import eppic.model.ChainClusterDB;
import eppic.model.InterfaceClusterDB;
import eppic.model.InterfaceClusterScoreDB;
import eppic.model.InterfaceDB;
import eppic.model.ResidueDB;
import eppic.model.InterfaceScoreDB;
import eppic.model.PdbInfoDB;
import eppic.model.AssemblyDB;
import eppic.model.ScoringMethod;
import eppic.model.UniProtRefWarningDB;
import eppic.model.RunParametersDB;
import eppic.model.InterfaceWarningDB;
import eppic.predictors.CombinedClusterPredictor;
import eppic.predictors.CombinedPredictor;
import eppic.predictors.EvolCoreRimClusterPredictor;
import eppic.predictors.EvolCoreSurfaceClusterPredictor;
import eppic.predictors.EvolCoreSurfacePredictor;
import eppic.predictors.EvolCoreRimPredictor;
import eppic.predictors.GeometryClusterPredictor;
import eppic.predictors.GeometryPredictor;


public class DataModelAdaptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataModelAdaptor.class);
	
	private static final int FIRST = 0;
	private static final int SECOND = 1;
	
	private static final double CONFIDENCE_NOT_AVAILABLE = -1.0;
	private static final double SCORE_NOT_AVAILABLE = -1.0;
	
	private PdbInfoDB pdbInfo;
	
	private EppicParams params;
	
	private RunParametersDB runParameters;
	
	// a temp map to hold the warnings per interface, used in order to eliminate duplicate warnings
	private HashMap<Integer,HashSet<String>> interfId2Warnings;
	
	public DataModelAdaptor() {
		pdbInfo = new PdbInfoDB();
		interfId2Warnings = new HashMap<Integer, HashSet<String>>();
	}
	
	public PdbInfoDB getPdbInfo() {
		return pdbInfo;
	}
	
	public void setParams(EppicParams params) {
		this.params = params;
		pdbInfo.setPdbCode(params.getPdbCode());
		runParameters = new RunParametersDB();
		runParameters.setMinNumSeqsCutoff(params.getMinNumSeqs());
		runParameters.setHomSoftIdCutoff(params.getHomSoftIdCutoff());
		runParameters.setHomHardIdCutoff(params.getHomHardIdCutoff());
		runParameters.setQueryCovCutoff(params.getQueryCoverageCutoff());
		runParameters.setMaxNumSeqsCutoff(params.getMaxNumSeqs());
		runParameters.setReducedAlphabet(params.getReducedAlphabet());
		runParameters.setCaCutoffForGeom(params.getCAcutoffForGeom());
		runParameters.setCaCutoffForCoreRim(params.getCAcutoffForRimCore());
		runParameters.setCaCutoffForCoreSurface(params.getCAcutoffForZscore());
		runParameters.setCrCallCutoff(params.getCoreRimScoreCutoff());
		runParameters.setCsCallCutoff(params.getCoreSurfScoreCutoff());
		runParameters.setGeomCallCutoff(params.getMinCoreSizeForBio());
		runParameters.setPdbInfo(pdbInfo);
		runParameters.setEppicVersion(EppicParams.PROGRAM_VERSION);
		runParameters.setSearchMode(params.getHomologsSearchMode().getName());
		pdbInfo.setRunParameters(runParameters);
	}
	
	public void setPdbMetadata(Structure pdb) {
		pdbInfo.setTitle(pdb.getPDBHeader().getTitle());
		// TODO here we used to have the release date but it doesn't seem to be in biojava, do we want it?
		pdbInfo.setReleaseDate(pdb.getPDBHeader().getDepDate());
		PDBCrystallographicInfo pdbXtallographicInfo = pdb.getCrystallographicInfo();
		SpaceGroup sg = (pdbXtallographicInfo==null?null:pdbXtallographicInfo.getSpaceGroup());
		pdbInfo.setSpaceGroup(sg==null?null:sg.getShortSymbol());
		pdbInfo.setResolution(pdb.getPDBHeader().getResolution());
		pdbInfo.setRfreeValue(pdb.getPDBHeader().getRfree());
		pdbInfo.setExpMethod(pdb.getPDBHeader().getExperimentalTechniques().iterator().next().getName());
		
		CrystalCell cc = (pdbXtallographicInfo==null?null:pdbXtallographicInfo.getCrystalCell());
		if (cc!=null) {
			pdbInfo.setCellA(cc.getA());
			pdbInfo.setCellB(cc.getB());
			pdbInfo.setCellC(cc.getC());
			pdbInfo.setCellAlpha(cc.getAlpha());
			pdbInfo.setCellBeta(cc.getBeta());
			pdbInfo.setCellGamma(cc.getGamma());			
		}
	}
	
	public void setInterfaces(StructureInterfaceList interfaces) {

		
		List<StructureInterfaceCluster> interfaceClusters = interfaces.getClusters();
		List<InterfaceClusterDB> icDBs = new ArrayList<InterfaceClusterDB>();
		for (StructureInterfaceCluster ic:interfaceClusters) {
			InterfaceClusterDB icDB = new InterfaceClusterDB();
			icDB.setClusterId(ic.getId());			
			icDB.setPdbCode(pdbInfo.getPdbCode());
			icDB.setAvgArea(ic.getTotalArea());
			icDB.setNumMembers(ic.getMembers().size());
			icDB.setPdbInfo(pdbInfo);
			
			List<InterfaceDB> iDBs = new ArrayList<InterfaceDB>();
			
			// setting relations parent/child
			icDBs.add(icDB);
			icDB.setInterfaces(iDBs);
			
			for (StructureInterface interf:ic.getMembers()) {
				//System.out.println("Interface " + interf.getId());
				
				InterfaceDB interfaceDB = new InterfaceDB();
				interfaceDB.setInterfaceId(interf.getId());
				interfaceDB.setClusterId(interf.getCluster().getId());
				interfaceDB.setArea(interf.getTotalArea());
				
				interfaceDB.setChain1(interf.getMoleculeIds().getFirst());
				interfaceDB.setChain2(interf.getMoleculeIds().getSecond());
				
				interfaceDB.setOperator(SpaceGroup.getAlgebraicFromMatrix(interf.getTransforms().getSecond().getMatTransform()));
				interfaceDB.setOperatorType(interf.getTransforms().getSecond().getTransformType().getShortName());
				interfaceDB.setInfinite(interf.isInfinite());
				interfaceDB.setOperatorId(interf.getTransforms().getSecond().getTransformId());
				interfaceDB.setXtalTrans_x(interf.getTransforms().getSecond().getCrystalTranslation().x);
				interfaceDB.setXtalTrans_y(interf.getTransforms().getSecond().getCrystalTranslation().y);
				interfaceDB.setXtalTrans_z(interf.getTransforms().getSecond().getCrystalTranslation().z);
				
				interfaceDB.setPdbCode(pdbInfo.getPdbCode());
				
				// setting relations parent/child
				iDBs.add(interfaceDB);				
				interfaceDB.setInterfaceCluster(icDB);
				
				interfId2Warnings.put(interf.getId(),new HashSet<String>());
				
				
				// the contacts table
				List<ContactDB> contacts = new ArrayList<ContactDB>();
				
				interfaceDB.setContacts(contacts);
							
				GroupContactSet groupContacts = new GroupContactSet(interf.getContacts());
				
				for (GroupContact groupContact:groupContacts) {
										
					ContactDB contact = new ContactDB();
					// TODO here we are storing the SEQRES residue serials, how to get them with biojava?
					contact.setFirstResNumber(pair.getFirst().getResidueSerial());
					contact.setSecondResNumber(pair.getSecond().getResidueSerial());
					contact.setFirstResType(groupContact.getPair().getFirst().getPDBName());
					contact.setSecondResType(groupContact.getPair().getSecond().getPDBName());
					contact.setFirstBurial(iRes.getBsaToAsaRatio());
					contact.setSecondBurial(jRes.getBsaToAsaRatio());
					
					contact.setMinDistance(groupContact.getMinDistance());
					contact.setClash(edge.isClash());
					contact.setDisulfide(edge.isDisulfide());
					contact.setNumAtoms(edge.getnAtoms());
					contact.setNumHBonds(edge.getnHBonds()); 
					
					contact.setInterfaceId(interf.getId()); 
					contact.setPdbCode(pdbInfo.getPdbCode());
					
					contacts.add(contact);
					
					// parent/child
					contact.setInterfaceItem(interfaceDB);
					
				}
				
				// sorting so that at least in text files we'll get a nice sorting
				Collections.sort(contacts, new Comparator<ContactDB>() {

					@Override
					public int compare(ContactDB first, ContactDB second) {
						int iFirst = first.getFirstResNumber();
						int jFirst = first.getSecondResNumber();
						int iSecond = second.getFirstResNumber();
						int jSecond = second.getSecondResNumber();
						
						if (iFirst>iSecond) return 1;
						if (iFirst<iSecond) return -1;
						
						if (jFirst>jSecond) return 1;
						if (jFirst<jSecond) return -1;
						
						return 0;
					}
					
				}); 
				
				
			}
		}
		pdbInfo.setInterfaceClusters(icDBs);
		
	}
	
	// TODO we need to port biounit stuff from Biojava properly, do it later
//	public void setPdbBioUnits(PdbBioUnitList bioUnitList) {
//		// assemblies (biounits) parsed from PDB
//
//		List<BioUnitView> reducedBioUnits = matchToInterfaceClusters(bioUnitList);		
//		
//		for(BioUnitView reducedBioUnit: reducedBioUnits){
//			//PdbBioUnit unit = bioUnitList.get(bioUnitId);
//
//			AssemblyDB assembly = new AssemblyDB();			
//			assembly.setMethod(reducedBioUnit.getType().getType());
//			assembly.setMmSize(reducedBioUnit.getMmSize());
//			assembly.setPdbCode(pdbInfo.getPdbCode());			
//			assembly.setConfidence(CONFIDENCE_NOT_AVAILABLE);
//
//			// setting relations parent/child
//			assembly.setPdbInfo(pdbInfo);
//			pdbInfo.addAssembly(assembly);
//
//			Set<Integer> memberClusterIds = reducedBioUnit.getClusterIds();
//
//			List<InterfaceClusterScoreDB> memberClusterScoresDB = new ArrayList<InterfaceClusterScoreDB>();
//			assembly.setInterfaceClusterScores(memberClusterScoresDB);
//			for (InterfaceClusterDB icDB:pdbInfo.getInterfaceClusters()) {
//				
//				if (memberClusterIds.contains(icDB.getClusterId())) {
//					// all member interface clusters are assigned bio
//					
//					InterfaceClusterScoreDB icsDB = new InterfaceClusterScoreDB();
//					memberClusterScoresDB.add(icsDB);				
//					
//					icsDB.setScore(SCORE_NOT_AVAILABLE);
//					icsDB.setScore1(SCORE_NOT_AVAILABLE);
//					icsDB.setScore2(SCORE_NOT_AVAILABLE);
//					icsDB.setCallName(CallType.BIO.getName());
//					icsDB.setConfidence(CONFIDENCE_NOT_AVAILABLE);
//					icsDB.setMethod(reducedBioUnit.getType().getType());				
//					icsDB.setClusterId(icDB.getClusterId());
//					icsDB.setPdbCode(pdbInfo.getPdbCode());
//
//					// setting relations parent/child
//					icsDB.setInterfaceCluster(icDB);
//					icDB.addInterfaceClusterScore(icsDB);
//
//					// only the bio interfaces are part of the assembly
//					icsDB.setAssembly(assembly);
//					
//				} else {
//					// The rest (not members) are assigned xtal
//					// We need to do this otherwise there's no distinction between 
//					// missing annotations and real xtal annotations
//					InterfaceClusterScoreDB icsDB = new InterfaceClusterScoreDB();
//					icsDB.setScore(SCORE_NOT_AVAILABLE);
//					icsDB.setScore1(SCORE_NOT_AVAILABLE);
//					icsDB.setScore2(SCORE_NOT_AVAILABLE);
//					icsDB.setCallName(CallType.CRYSTAL.getName());
//					icsDB.setConfidence(CONFIDENCE_NOT_AVAILABLE);
//					icsDB.setMethod(reducedBioUnit.getType().getType());				
//					icsDB.setClusterId(icDB.getClusterId());
//					icsDB.setPdbCode(pdbInfo.getPdbCode());
//
//					// setting relations parent/child
//					icsDB.setInterfaceCluster(icDB);
//					icDB.addInterfaceClusterScore(icsDB);
//
//				}
//			}
//
//		}
//
//	}
	
//	/**
//	 * For each PDB bio unit in the given list, map the PDB-annotated interfaces to our interface cluster ids
//	 * and cull the list by:
//	 * 1) removing PDB bio units having same type, mmSize and set of interface cluster ids
//	 * 2) from those left, choosing the one with maximal mmSize within each type
//	 * @param bioUnitList
//	 * @return
//	 */
//	private List<BioUnitView> matchToInterfaceClusters(PdbBioUnitList bioUnitList) {
//
//		List<BioUnitView> culledBioUnits = new ArrayList<BioUnitView>();
//		int serial = 0;
//		for (PdbBioUnit bioUnit:bioUnitList) {
//			serial++;
//			BioUnitView bu = new BioUnitView();
//			bu.setMmSize(bioUnit.getSize());
//			bu.setType(bioUnit.getType());
//
//			List<SimpleInterface> bioUnitInterfaces = SimpleInterface.createSimpleInterfaceListFromPdbBioUnit(bioUnit);
//			InterfaceMatcher im = new InterfaceMatcher(pdbInfo.getInterfaceClusters(),bioUnitInterfaces);
//			for (InterfaceClusterDB ic:pdbInfo.getInterfaceClusters()) {
//				for (InterfaceDB i:ic.getInterfaces()) {
//					if (im.oursMatch(i.getInterfaceId())) {
//						bu.addInterfClusterId(ic.getClusterId()); 							 
//					} 
//				}
//			}
//			// 1st culling
//			// this will depend on the BioUnitView.equals implementation:
//			// essentially through this we remove redundant biounits (same size, type and set of cluster ids)
//			if (!culledBioUnits.contains(bu)) {
//				culledBioUnits.add(bu);
//			}
//
//			if (!im.checkTheirsMatch()) {
//				String msg = "";
//				for (SimpleInterface theirI:im.getTheirsNotMatching()) {
//					msg += theirI.toString()+"\t";
//				}
//
//				// This actually happens even if the mapping is fine. That's because we enumerate the biounit 
//				// interfaces exhaustively, and thus sometimes an interface might not happen in reality because 
//				// 2 molecules don't make a contact. 
//				LOGGER.info("Some interfaces of PDB bio unit "+serial+" of "+bioUnitList.size()+
//						" (type="+bioUnit.getType()+", size="+bioUnit.getSize()+") do not match any of the EPPIC interfaces."
//						+ " Non-matching interfaces are: "+msg);
//
//			}
//			
//			if (!im.checkOneToOneMapping()) {
//				// This is not really a mapping problem, that's why it is only logged INFO
//				// It will happen in many bona-fide proper mappings:
//				// e.g. 2a7n or 1ae9 (the bio interfaces are in 4-fold or 3-fold xtal axes and thus 
//				//      the operators given in bio-unit are repeated, for instance for 3-fold the operator 
//				//      appears twice to construct the 2 symmetry partners, while in eppic it appears only once)
//				LOGGER.info("Multiple match for an interface of PDB bio unit "+serial+" of "+bioUnitList.size()+
//					" (type="+bioUnit.getType()+", size="+bioUnit.getSize()+").");
//			}
//
//
//		}
//		
//		// 2nd culling: now we need to make sure that there's only 1 biounit per method (pisa, pqs, authors)
//		// multiplicity of assemblies per method is not fitting into our eppic data model
//		List<BioUnitView> finalList = new ArrayList<BioUnitView>(BioUnitAssignmentType.values().length);
//		
//		for (BioUnitAssignmentType type:BioUnitAssignmentType.values()) {
//			List<BioUnitView> sorted = getSortedBiounitsForType(culledBioUnits, type);
//			if (sorted.isEmpty()) continue;
//			// we get the largest (if more than 1 of same size, then largest will be randomly chosen from there)
//			BioUnitView bu = sorted.get(sorted.size()-1);
//			finalList.add(bu);
//			if (sorted.size()>1) {
//				int maxSize = bu.getMmSize();
//				LOGGER.info("More than 1 PDB bio unit annotation of type "+bu.getType().getType()+". Will only use the first one of size "+bu.getMmSize());
//				for (int i=0;i<sorted.size()-1;i++) {
//					if (sorted.get(i).getMmSize()<maxSize) {
//						LOGGER.info("PDB bio unit of size "+sorted.get(i).getMmSize()+", type "+sorted.get(i).getType().getType()+" will be discarded");
//					} else {
//						LOGGER.warn("PDB bio unit of same size as maximal one ("+sorted.get(i).getMmSize()+", type "+sorted.get(i).getType().getType()+") will be discarded");
//					}
//				}
//			}
//
//		}
//		
//		return finalList;
//	}
	
//	private static List<BioUnitView> getSortedBiounitsForType(List<BioUnitView> bioUnits, BioUnitAssignmentType type) {
//		List<BioUnitView> list = new ArrayList<BioUnitView>();
//		for (BioUnitView bu:bioUnits) {
//			if (bu.getType()==type) list.add(bu);
//		}
//		Collections.sort(list);
//		return list;
//	}
	
	public void setGeometryScores(List<GeometryPredictor> gps, List<GeometryClusterPredictor> gcps) {
		
		// geometry scores per interface
		for (int i=0;i<gps.size();i++) {
			InterfaceDB ii = pdbInfo.getInterface(i+1);
			InterfaceScoreDB is = new InterfaceScoreDB();
			ii.addInterfaceScore(is);
			is.setInterfaceItem(ii);
			is.setInterfaceId(ii.getInterfaceId());
			CallType call = gps.get(i).getCall();
			is.setCallName(call.getName());
			is.setCallReason(gps.get(i).getCallReason());
			is.setMethod(ScoringMethod.EPPIC_GEOMETRY);
			is.setPdbCode(ii.getPdbCode());
			is.setConfidence(gps.get(i).getConfidence());
			is.setScore(gps.get(i).getScore());
			is.setScore1(gps.get(i).getScore1());
			is.setScore2(gps.get(i).getScore2());
			
			if(gps.get(i).getWarnings() != null) {
				
				List<String> warnings = gps.get(i).getWarnings();
				for(String warning: warnings) {
					
					// we first add warning to the temp HashSets in order to eliminate duplicates, 
					// in the end we fill the InterfaceItemDBs by calling addInterfaceWarnings
					interfId2Warnings.get(ii.getInterfaceId()).add(warning);
				}
			}

		}

		// geometry scores per interface cluster
		for (int i=0;i<gcps.size();i++) {

			InterfaceClusterDB ic = pdbInfo.getInterfaceClusters().get(i);
			GeometryClusterPredictor gcp = gcps.get(i);
			
			// method eppic-gm
			InterfaceClusterScoreDB ics = new InterfaceClusterScoreDB();
			ics.setMethod(ScoringMethod.EPPIC_GEOMETRY);
			ics.setCallName(gcp.getCall().getName());
			ics.setCallReason(gcp.getCallReason());
			ics.setScore(gcp.getScore());
			ics.setScore1(gcp.getScore1());
			ics.setScore2(gcp.getScore2());
			ics.setConfidence(gcp.getConfidence());
			ics.setPdbCode(pdbInfo.getPdbCode());
			ics.setClusterId(ic.getClusterId());

			// setting relations child/parent
			ics.setInterfaceCluster(ic); 
			ic.addInterfaceClusterScore(ics);
		}

	}

	public void setEvolScores(InterfaceEvolContextList iecl) {
		
		List<ChainClusterDB> chainClusterDBs = new ArrayList<ChainClusterDB>();
		
		ChainEvolContextList cecl = iecl.getChainEvolContextList();
		pdbInfo.setNumChainClusters(cecl.size());
		
		for (ChainEvolContext cec:cecl.getAllChainEvolContext()) {
			ChainClusterDB chainClusterDB = new ChainClusterDB();
			Compound cc = null;
			for (Compound compound:cecl.getPdb().getCompounds()) {
				if (compound.getRepresentative().getChainID().equals(cec.getRepresentativeChainCode())) {
					cc = compound;
				}
			}
			chainClusterDB.setRepChain(cc.getRepresentative().getChainID());
			StringBuilder sb = new StringBuilder();
			for (int i=0;i<cc.getChains().size();i++) {
				sb.append(cc.getChains().get(i).getChainID());
				if (i!=cc.getChains().size()-1) sb.append(",");
			}
			chainClusterDB.setMemberChains(sb.toString());
			chainClusterDB.setHasUniProtRef(cec.hasQueryMatch());
			
			List<UniProtRefWarningDB> queryWarningItemDBs = new ArrayList<UniProtRefWarningDB>();
			for(String queryWarning : cec.getQueryWarnings()) {
				
				UniProtRefWarningDB queryWarningItemDB = new UniProtRefWarningDB();
				queryWarningItemDB.setChainCluster(chainClusterDB);
				queryWarningItemDB.setText(queryWarning);
				queryWarningItemDBs.add(queryWarningItemDB);
			}
			
			chainClusterDB.setUniProtRefWarnings(queryWarningItemDBs);
			chainClusterDB.setPdbCode(pdbInfo.getPdbCode());
			
			if (cec.hasQueryMatch()) { //all other fields remain null otherwise
				
				chainClusterDB.setNumHomologs(cec.getNumHomologs());
				chainClusterDB.setRefUniProtId(cec.getQuery().getUniId()); 
				chainClusterDB.setFirstTaxon(cec.getQuery().getFirstTaxon());
				chainClusterDB.setLastTaxon(cec.getQuery().getLastTaxon());
				
				chainClusterDB.setMsaAlignedSeq(cec.getAlignment().getAlignedSequence(cec.getQuery().getUniId()));
				 
				chainClusterDB.setRefUniProtStart(cec.getQueryInterval().beg);
				chainClusterDB.setRefUniProtEnd(cec.getQueryInterval().end);
				
				chainClusterDB.setPdbStart(cec.getPDBPosForQueryUniprotPos(cec.getQueryInterval().beg));
				chainClusterDB.setPdbEnd(cec.getPDBPosForQueryUniprotPos(cec.getQueryInterval().end));
				
				chainClusterDB.setPdbAlignedSeq(cec.getPdb2uniprotAln().getAlignedSequence(1).getSequenceAsString());
				chainClusterDB.setAliMarkupLine(String.valueOf(cec.getPdb2uniprotAln().getMarkupLine()));
				chainClusterDB.setRefAlignedSeq(cec.getPdb2uniprotAln().getAlignedSequence(2).getSequenceAsString());
				chainClusterDB.setSeqIdCutoff(cec.getIdCutoff());
				chainClusterDB.setClusteringSeqId(cec.getUsedClusteringPercentId()/100.0);
				
				List<HomologDB> homologDBs = new ArrayList<HomologDB>();
				for (Homolog hom:cec.getHomologs().getFilteredSubset()) {
					HomologDB homologDB = new HomologDB();
					homologDB.setUniProtId(hom.getUniId());
					homologDB.setQueryStart(hom.getBlastHsp().getQueryStart());
					homologDB.setQueryEnd(hom.getBlastHsp().getQueryEnd());
					homologDB.setSubjectStart(hom.getBlastHsp().getSubjectStart());
					homologDB.setSubjectEnd(hom.getBlastHsp().getSubjectEnd());
					homologDB.setAlignedSeq(cec.getAlignment().getAlignedSequence(hom.getIdentifier()));
					if (hom.getUnirefEntry().hasTaxons()) {
						homologDB.setFirstTaxon(hom.getUnirefEntry().getFirstTaxon());
						homologDB.setLastTaxon(hom.getUnirefEntry().getLastTaxon());
					}
					homologDB.setSeqId(hom.getPercentIdentity()/100.0);
					homologDB.setQueryCoverage(hom.getQueryCoverage());					
					
					homologDBs.add(homologDB);
					
					homologDB.setChainCluster(chainClusterDB);
					
				}
				
				chainClusterDB.setHomologs(homologDBs);
			} 

			chainClusterDB.setPdbInfo(pdbInfo);
			chainClusterDBs.add(chainClusterDB);	
		}
		
		pdbInfo.setChainClusters(chainClusterDBs);
		

		for (int i=0;i<iecl.size();i++) {
			
			InterfaceEvolContext iec = iecl.get(i);
			InterfaceDB ii = pdbInfo.getInterface(i+1);
			
			// 1) we add entropy values to the residue details
			addEntropyToResidueDetails(ii.getResidues(), iec);
			
			
			// 2) core-surface scores
			EvolCoreSurfacePredictor ecsp = iec.getEvolCoreSurfacePredictor();
			InterfaceScoreDB isCS = new InterfaceScoreDB();
			ii.addInterfaceScore(isCS);
			isCS.setInterfaceItem(ii);
			isCS.setInterfaceId(iec.getInterface().getId());
			isCS.setMethod(ScoringMethod.EPPIC_CORESURFACE);

			CallType call = ecsp.getCall();	
			isCS.setCallName(call.getName());
			isCS.setCallReason(ecsp.getCallReason());
			
			if(ecsp.getWarnings() != null) {
				List<String> warnings = ecsp.getWarnings();
				for(String warning: warnings) {
					// we first add warning to the temp HashSets in order to eliminate duplicates, 
					// in the end we fill the InterfaceItemDBs by calling addInterfaceWarnings
					interfId2Warnings.get(ii.getInterfaceId()).add(warning);
				}
			}

			isCS.setScore1(ecsp.getScore1());
			isCS.setScore2(ecsp.getScore2());
			isCS.setScore(ecsp.getScore());	
			
			isCS.setConfidence(ecsp.getConfidence());
			isCS.setPdbCode(ii.getPdbCode());
			
			// 3) core-rim scores
			EvolCoreRimPredictor ecrp = iec.getEvolCoreRimPredictor();

			InterfaceScoreDB isCR = new InterfaceScoreDB();
			isCR.setInterfaceItem(ii);
			ii.addInterfaceScore(isCR);
			isCR.setInterfaceId(iec.getInterface().getId());
			isCR.setMethod(ScoringMethod.EPPIC_CORERIM);

			call = ecrp.getCall();	
			isCR.setCallName(call.getName());
			isCR.setCallReason(ecrp.getCallReason());
			
			if(ecrp.getWarnings() != null) {
				List<String> warnings = ecrp.getWarnings();
				for(String warning: warnings) {
					// we first add warning to the temp HashSets in order to eliminate duplicates, 
					// in the end we fill the InterfaceItemDBs by calling addInterfaceWarnings
					interfId2Warnings.get(ii.getInterfaceId()).add(warning);
				}
			}

			isCR.setScore1(ecrp.getScore1());
			isCR.setScore2(ecrp.getScore2());
			isCR.setScore(ecrp.getScore());				

			isCR.setConfidence(ecrp.getConfidence());
			isCR.setPdbCode(ii.getPdbCode());

			
			
		}

		// 4) interface cluster scores
		for (InterfaceClusterDB ic:pdbInfo.getInterfaceClusters()) {

			// method eppic-cr
			EvolCoreRimClusterPredictor ecrcp = iecl.getEvolCoreRimClusterPredictor(ic.getClusterId());
			InterfaceClusterScoreDB ics = new InterfaceClusterScoreDB();
			ics.setMethod(ScoringMethod.EPPIC_CORERIM);
			ics.setCallName(ecrcp.getCall().getName());
			ics.setCallReason(ecrcp.getCallReason());
			ics.setScore(ecrcp.getScore());
			ics.setScore1(ecrcp.getScore1());
			ics.setScore2(ecrcp.getScore2());
			ics.setConfidence(ecrcp.getConfidence());
			ics.setPdbCode(pdbInfo.getPdbCode());
			ics.setClusterId(ic.getClusterId());

			// setting relations child/parent
			ics.setInterfaceCluster(ic); 
			ic.addInterfaceClusterScore(ics);
			
			// method eppic-cs
			EvolCoreSurfaceClusterPredictor ecscp = iecl.getEvolCoreSurfaceClusterPredictor(ic.getClusterId());
			ics = new InterfaceClusterScoreDB();
			ics.setMethod(ScoringMethod.EPPIC_CORESURFACE);			
			ics.setCallName(ecscp.getCall().getName());
			ics.setCallReason(ecscp.getCallReason());
			ics.setScore(ecscp.getScore());
			ics.setScore1(ecscp.getScore1());
			ics.setScore2(ecscp.getScore2());
			ics.setConfidence(ecscp.getConfidence());
			ics.setPdbCode(pdbInfo.getPdbCode());
			ics.setClusterId(ic.getClusterId());

			// setting relations child/parent
			ics.setInterfaceCluster(ic); 
			ic.addInterfaceClusterScore(ics);
		}
		

	}
	
	public void setCombinedPredictors(List<CombinedPredictor> cps, List<CombinedClusterPredictor> ccps) {

		// per interface combined scores
		for (int i=0;i<cps.size();i++) {
			InterfaceDB ii = pdbInfo.getInterface(i+1);
			InterfaceScoreDB is = new InterfaceScoreDB();
			ii.addInterfaceScore(is);
			is.setMethod(ScoringMethod.EPPIC_FINAL);
			is.setCallName(cps.get(i).getCall().getName());
			is.setCallReason(cps.get(i).getCallReason());
			is.setConfidence(cps.get(i).getConfidence());
			is.setInterfaceItem(ii);
			is.setInterfaceId(ii.getInterfaceId());
			is.setPdbCode(ii.getPdbCode());
			is.setScore(cps.get(i).getScore());
			is.setScore1(cps.get(i).getScore1());
			is.setScore2(cps.get(i).getScore2());
			
			if(cps.get(i).getWarnings() != null) {
				
				List<String> warnings = cps.get(i).getWarnings();
				for(String warning: warnings) {
					
					// we first add warning to the temp HashSets in order to eliminate duplicates, 
					// in the end we fill the InterfaceItemDBs by calling addInterfaceWarnings
					interfId2Warnings.get(ii.getInterfaceId()).add(warning);
				}
			}
		}
		
		// per cluster combined scores
		for (int i=0;i<ccps.size();i++) {
			
			InterfaceClusterDB ic = pdbInfo.getInterfaceClusters().get(i);
			
			InterfaceClusterScoreDB ics = new InterfaceClusterScoreDB();
			
			ics.setMethod(ScoringMethod.EPPIC_FINAL);
			ics.setCallName(ccps.get(i).getCall().getName());
			ics.setCallReason(ccps.get(i).getCallReason());
			ics.setScore(ccps.get(i).getScore());
			ics.setScore1(ccps.get(i).getScore1());
			ics.setScore2(ccps.get(i).getScore2());
			ics.setConfidence(ccps.get(i).getConfidence());
			ics.setPdbCode(pdbInfo.getPdbCode());
			ics.setClusterId(ic.getClusterId());

			// setting relations child/parent
			ics.setInterfaceCluster(ic); 
			ic.addInterfaceClusterScore(ics);
		}
	}
	
	public void writeSerializedModelFile(File file) throws EppicException {
		try {
			Goodies.serialize(file,pdbInfo);
		} catch (IOException e) {
			throw new EppicException(e, e.getMessage(), true);
		}
	}
	
	public void setResidueDetails(StructureInterfaceList interfaces) {
		for (StructureInterface interf:interfaces) {
			
			InterfaceDB ii = pdbInfo.getInterface(interf.getId());

			// we add the residue details
			addResidueDetails(ii, interf, params.isDoEvolScoring());
		}
	}
	
	private void addResidueDetails(InterfaceDB ii, StructureInterface interf, boolean includeEntropy) {
		
		List<ResidueDB> iril = new ArrayList<ResidueDB>();
		ii.setResidues(iril);

		addResidueDetailsOfPartner(iril, interf, 0);
		addResidueDetailsOfPartner(iril, interf, 1);

		for(ResidueDB iri : iril) {
			iri.setInterfaceItem(ii);
		}
	}
	
	private void addResidueDetailsOfPartner(List<ResidueDB> iril, StructureInterface interf, int molecId) {
		if (interf.isProtein()) {
			PdbChain mol = null;
			if (molecId==FIRST) {
				mol = interf.getFirstMolecule();
			}
			else if (molecId==SECOND) {
				mol = interf.getSecondMolecule();
			}
			
			for (Residue residue:mol) {
				String resType = residue.getLongCode();
				int assignment = ResidueDB.OTHER;
				
				double asa = residue.getAsa();
				double bsa = residue.getBsa();
				
				// NOTE the regions are mutually exclusive (one and only one assignment per region)

				// For the case of CORE_EVOL/CORE_GEOM we are assuming that CORE_EVOL is a superset of CORE_GEOM 
				// (i.e. that caCutoffForRimCore<caCutoffForGeom)
				// thus, as groups are exclusive, to get the actual full subset of CORE_EVOL one needs to get
				// the union of CORE_EVOL and CORE_GEOM
				
				// this should match the condition in owl.core.structure.PdbChain.getRimAndCore()
				if (residue.getAsa()>params.getMinAsaForSurface() && residue.getBsa()>0) {
					// NOTE: we use here caCutoffForRimCore as the one and only for both evol methods
					// NOTE2: we are assuming that caCutoffForRimCore<caCutoffForGeom, if that doesn't hold this won't work!
					if (residue.getBsaToAsaRatio()<params.getCAcutoffForRimCore()) {
						assignment = ResidueDB.RIM_EVOLUTIONARY;
					} else if (residue.getBsaToAsaRatio()<params.getCAcutoffForGeom()){
						assignment = ResidueDB.CORE_EVOLUTIONARY; 
					} else {
						assignment = ResidueDB.CORE_GEOMETRY;
					} 
					
				// residues not in interface but still with more ASA than minimum required are called surface
				} else if (residue.getAsa()>params.getMinAsaForSurface()) {
					assignment = ResidueDB.SURFACE;
				}
				
				
				ResidueDB iri = new ResidueDB();
				iri.setResidueNumber(residue.getSerial());
				iri.setPdbResidueNumber(residue.getPdbSerial());
				iri.setResidueType(resType);
				iri.setAsa(asa);
				iri.setBsa(bsa);
				iri.setRegion(assignment);
				iri.setEntropyScore(-1.0);
				iri.setSide(molecId+1); // structure ids are 1 and 2 while molecId are 0 and 1

				iri.setInterfaceItem(pdbInfo.getInterface(interf.getId()));
				iril.add(iri);
			}
		}
	}

	private void addEntropyToResidueDetails(List<ResidueDB> iril, InterfaceEvolContext iec) {
		StructureInterface interf = iec.getInterface();
		
		
		int[] molecIds = new int[2];
		molecIds[0] = 0;
		molecIds[1] = 1;

		// beware the counter is global for both molecule 1 and 2 (as the List<ResidueDB> contains both, identified by a structure id 1 or 2)
		int i = 0;  

		for (int molecId:molecIds) { 
			ChainEvolContext cec = iec.getChainEvolContext(molecId);
			PdbChain mol = null;
			if (molecId==FIRST) {
				mol = interf.getFirstMolecule();
			}
			else if (molecId==SECOND) {
				mol = interf.getSecondMolecule();
			}

			if (interf.isProtein()) {
				 
				List<Double> entropies = null;
				if (cec.hasQueryMatch()) 
					entropies = cec.getConservationScores();
				for (Residue residue:mol) {

	 				ResidueDB iri = iril.get(i);
					
					int queryUniprotPos = -1;
					if (!mol.isNonPolyChain() && mol.getSequence().isProtein() && cec.hasQueryMatch()) 
						queryUniprotPos = cec.getQueryUniprotPosForPDBPos(residue.getSerial());

					float entropy = -1;
					// we used to have here: "&& residue instanceof AaResidue" but that was preventing entropy values of mismatch-to-ref-uniprot-residues to be passed
					// for het residues we do have entropy values too as the entropy values are calculated on the reference uniprot sequence (where no het residues are present)
					if (entropies!=null) {	
						if (queryUniprotPos!=-1) entropy = (float) entropies.get(queryUniprotPos-1).doubleValue();
					}

					iri.setEntropyScore(entropy); 
					i++;
				}
			}
		}
		
		
	}
	
	public void setUniProtVersion(String uniProtVersion) {
		this.runParameters.setUniProtVersion(uniProtVersion);
	}
	
	/**
	 * Add to the pdbInfo member the cached warnings interfId2Warnings, compiled in
	 * {@link #setGeometryScores(List)}, {@link #setCombinedPredictors(List)} and {@link #setEvolScores(InterfaceEvolContextList)} 
	 */
	public void setInterfaceWarnings() {
		
		for (InterfaceClusterDB ic:pdbInfo.getInterfaceClusters()) {
			for (InterfaceDB ii:ic.getInterfaces()) {
				for (String warning : interfId2Warnings.get(ii.getInterfaceId())) {
					InterfaceWarningDB warningItem = new InterfaceWarningDB();
					warningItem.setText(warning);
					warningItem.setInterfaceItem(ii);
					ii.getInterfaceWarnings().add(warningItem);
				}
			}
		}
	}
	
	public static String getChainClusterString(Compound compound) {

		StringBuilder sb = new StringBuilder();

		sb.append(compound.getRepresentative().getChainID());

		if (compound.getChains().size()>1) {

			sb.append(" (");
			for (int i=0;i<compound.getChains().size();i++) {

				if (compound.getChains().get(i)==compound.getRepresentative()) {
					continue;
				}

				sb.append(compound.getChains().get(i).getChainID()+",");

			}

			sb.deleteCharAt(sb.length()-1);
			sb.append(")");
		}

		return sb.toString();
	}
	
}

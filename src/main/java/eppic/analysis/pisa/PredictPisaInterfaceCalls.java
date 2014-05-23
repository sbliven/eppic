/**
 * 
 */
package eppic.analysis.pisa;

import eppic.DataModelAdaptor;
import eppic.EppicParams;
import eppic.model.PdbInfoDB;
import gnu.getopt.Getopt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.xml.sax.SAXException;

import owl.core.connections.pisa.PisaAsmSetList;
import owl.core.connections.pisa.PisaAssembliesXMLParser;
import owl.core.connections.pisa.PisaInterfaceList;
import owl.core.connections.pisa.PisaInterfaceXMLParser;
import owl.core.structure.ChainInterfaceList;
import owl.core.structure.PdbAsymUnit;
import owl.core.structure.PdbLoadException;
import owl.core.util.FileFormatException;
import owl.core.util.Goodies;


/**
 * Class to read in the data from .xml.gz PISA files for interfaces and assemblies and parse them.
 * Finally predict "xtal" or "bio" for each interface corresponding to eppic.
 * @author biyani_n
 *
 */
public class PredictPisaInterfaceCalls {
	
	private static final String PROGRAM_NAME = "PredictPisaInterfaceCalls";
	private static final String DEFAULT_CIF_DIR = "/nfs/data/dbs/pdb/data/structures/all/mmCIF";
	
	private String cifDir;
	
	private File serializedFilesDir;
	
	private File interfaceFile;
	private File assemblyFile;
	
	private List<PisaPdbData> pisaDatas;
	
	public PredictPisaInterfaceCalls(File interfaceFile, File assemblyFile, String cifDirPath, File serializedFilesDir, int pisaVersion) throws SAXException, IOException, FileFormatException, PdbLoadException{
		this.interfaceFile = interfaceFile;
		this.assemblyFile = assemblyFile;
		this.cifDir = cifDirPath;
		this.serializedFilesDir = serializedFilesDir;
		this.pisaDatas = createPisaDatafromFiles(assemblyFile, interfaceFile, pisaVersion);
	}
	
	public String getCifDir() {
		return cifDir;
	}

	public void setCifDir(String cifDir) {
		this.cifDir = cifDir;
	}

	public File getInterfaceFile() {
		return interfaceFile;
	}

	public void setInterfaceFile(File interfaceFile) {
		this.interfaceFile = interfaceFile;
	}

	public File getAssemblyFile() {
		return assemblyFile;
	}

	public void setAssemblyFile(File assemblyFile) {
		this.assemblyFile = assemblyFile;
	}

	public List<PisaPdbData> getPisaDatas() {
		return pisaDatas;
	}

	public void setPisaDatas(List<PisaPdbData> pisaDatas) {
		this.pisaDatas = pisaDatas;
	}

	public List<PisaPdbData> createPisaDatafromFiles(File assemblyFile, File interfaceFile, int pisaVersion) throws SAXException, IOException, PdbLoadException, FileFormatException{
		List<PisaPdbData> pisaDataList = new ArrayList<PisaPdbData>();
		
		//Parse Assemblies
		PisaAssembliesXMLParser assemblyParser = new PisaAssembliesXMLParser(new GZIPInputStream(new FileInputStream(assemblyFile)), pisaVersion);
		Map<String,PisaAsmSetList> assemblySetListMap = assemblyParser.getAllAssemblies();		
		
		//Parse Interfaces
		PisaInterfaceXMLParser interfaceParser = new PisaInterfaceXMLParser(new GZIPInputStream(new FileInputStream(interfaceFile)));
		Map<String, PisaInterfaceList> interfaceListMap = interfaceParser.getAllInterfaces();
		
		for(String pdbCode:assemblySetListMap.keySet()){
			if(!interfaceListMap.keySet().contains(pdbCode))
				System.err.println("Warning: Assembly file different from interface file; Interface file does not contain data for pdb:"+pdbCode);
			else{
				try{
					PdbInfoDB pdbInfo = null;
					if (serializedFilesDir!=null) {
						pdbInfo = getPdbInfoFromFile(pdbCode);
					} else {
						pdbInfo = getPdbInfo(getPdb(pdbCode));						
					}
					PisaPdbData local = new PisaPdbData(pdbInfo, assemblySetListMap.get(pdbCode), interfaceListMap.get(pdbCode));					
					pisaDataList.add(local);
				} catch(PdbLoadException e){
					System.err.println("ERROR: Unable to load pdb file: "+pdbCode+", error: "+e.getMessage());
				} catch(IOException e) {
					System.err.println("ERROR: Unable to deserialize from file for pdb "+pdbCode+", error: "+e.getMessage());
				} catch (ClassNotFoundException e) {
					System.err.println("ERROR: Unable to deserialize from file for pdb "+pdbCode+", error: "+e.getMessage());
				}
			}
		}
		
		return pisaDataList;
		
	}
	
	private PdbAsymUnit getPdb(String pdbCode) throws IOException, FileFormatException, PdbLoadException {
		File cifFile = File.createTempFile(pdbCode, "cif");
		PdbAsymUnit.grabCifFile(this.cifDir, null, pdbCode, cifFile, false);
		
		cifFile.deleteOnExit();
		return new PdbAsymUnit(cifFile);
	}
	
	private PdbInfoDB getPdbInfo(PdbAsymUnit pdb) {
		DataModelAdaptor dma = new DataModelAdaptor();
		
		pdb.removeHatoms();
		ChainInterfaceList eppicInterfaces = pdb.getAllInterfaces(EppicParams.INTERFACE_DIST_CUTOFF, 
				EppicParams.DEF_NSPHEREPOINTS_ASA_CALC, 1, true, false, 
				EppicParams.DEF_MIN_SIZE_COFACTOR_FOR_ASA,
				EppicParams.MIN_INTERFACE_AREA_TO_KEEP);
		eppicInterfaces.initialiseClusters(pdb, EppicParams.CLUSTERING_RMSD_CUTOFF, EppicParams.CLUSTERING_MINATOMS, EppicParams.CLUSTERING_ATOM_TYPE);
		dma.setInterfaces(eppicInterfaces, null);
		PdbInfoDB pdbInfo = dma.getPdbInfo();
		pdbInfo.setPdbCode(pdb.getPdbCode());
		return pdbInfo;
	}
	
	private PdbInfoDB getPdbInfoFromFile(String pdbCode) throws ClassNotFoundException, IOException {
		String midIndex = pdbCode.substring(1,3);
		File subdir = new File(serializedFilesDir,"divided"+File.separator+midIndex+File.separator+pdbCode);
		File webuidatFile = new File(subdir,pdbCode+".webui.dat");
		return (PdbInfoDB)Goodies.readFromFile(webuidatFile);
	}

	public static void printHeaders(PrintStream out) {
		//Print Header
		out.printf("#%4s %8s %8s %8s\n","PDB","EPPIC_ID","PISA_ID","PisaCall");

	}
	
	public void printData(PrintStream out){
		for(PisaPdbData data:this.pisaDatas){
			for(int eppicI:data.getEppicToPisaInterfaceMap().keySet()){
				out.printf("%5s %8s %8s %8s\n",data.getPdbCode(),eppicI,data.getPisaIdForEppicInterface(eppicI),data.getPisaCallFromEppicInterface(eppicI).getName() );
			}
		}
	}

	/**
	 * Test method to take input assembly and interfaces gzipped xml files and produce the output
	 * @param args
	 * @throws PdbLoadException 
	 * @throws FileFormatException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) {
		
		File assemFile = null;
		File interfFile = null;
		String cifPath = DEFAULT_CIF_DIR;
		int pisaVersion = PisaAssembliesXMLParser.VERSION2;
		
		File listFile = null;
		File dir = null;
		File serializedFilesDir = null;
		
		String help = "Usage: \n" +
				PROGRAM_NAME+"\n" +
				"   -a         :  PISA assemblies gzipped xml file\n"+
				"   -i         :  PISA interfaces gzipped xml file\n" +
				"   -l         :  file with list of PDB codes, use in combination with -d (-a and -i"+
				"                 will be ignored)\n"+
				"   -d         :  dir containing PISA <pdb>.assemblies.xml.gz and <pdb>.interfaces.xml.gz\n"+
				"                 files. Use in combination with -l (-a and -i will be ignored)\n"+
				"  [-w]        :  dir containing webui.dat files for each PDB (in the usual divided layout).\n"+
				"                 If -w not specified, then interfaces will be calculated on the fly from mmCIF \n"+
				"                 files read from dir given in -c\n"+
				"  [-c]        :  Path to cif files directory\n"+
				"  [-v]        :  PISA version, either "+PisaAssembliesXMLParser.VERSION1+
				" for web PISA or "+PisaAssembliesXMLParser.VERSION2+
				" for ccp4 package's command line PISA (default "+PisaAssembliesXMLParser.VERSION2+")\n";

		Getopt g = new Getopt(PROGRAM_NAME, args, "a:i:l:d:w:c:v:h?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'a':
				assemFile = new File(g.getOptarg());
				break;
			case 'i':
				interfFile = new File(g.getOptarg());
				break;
			case 'l':
				listFile = new File(g.getOptarg());
				break;
			case 'd':
				dir = new File(g.getOptarg());
				break;
			case 'w':
				serializedFilesDir = new File(g.getOptarg());
				break;
			case 'c':
				cifPath = g.getOptarg();
				break;
			case 'v':
				pisaVersion = Integer.parseInt(g.getOptarg());
				break;
			case 'h':
			case '?':
				System.out.println(help);
				System.exit(0);
				break; // getopt() already printed an error
			}
		}
		
		if ((assemFile == null || interfFile == null) && (listFile == null || dir == null)){
			System.err.println("Must specify either: -a/-i (single files) or -l/-d (list file and dir)");
			System.err.println(help);
			System.exit(1);
		}
		

		if (assemFile!=null) {
			// single files
			PredictPisaInterfaceCalls predictor;
			try {
				predictor = new PredictPisaInterfaceCalls(interfFile,assemFile,cifPath,serializedFilesDir,pisaVersion);
				printHeaders(System.out); 
				predictor.printData(System.out);

			} catch (SAXException e) {
				System.err.println("Problem reading pisa files, error: "+e.getMessage());				
			} catch (IOException e) {
				System.err.println("Problem reading pisa files, error: "+e.getMessage());
			} catch (FileFormatException e) {
				System.err.println("Problem reading pisa files, error: "+e.getMessage());
			} catch (PdbLoadException e) {
				System.err.println("Problem reading pisa files, error: "+e.getMessage());
			}
			
		} else {
			printHeaders(System.out); 
			// list file and dir
			List<String> list = readListFile(listFile);
			for (String pdbCode:list) {
				
				interfFile = new File(dir,pdbCode+".interfaces.xml.gz");
				assemFile = new File(dir,pdbCode+".assemblies.xml.gz"); 

				try {
					PredictPisaInterfaceCalls predictor = 
						new PredictPisaInterfaceCalls(interfFile,assemFile,cifPath,serializedFilesDir,pisaVersion);
					predictor.printData(System.out);
				} catch (IOException e) {
					System.err.println("Problem reading file for pdb "+pdbCode+", error: "+e.getMessage());
					continue;
				} catch (SAXException e) {
					System.err.println("Problem reading xml file for pdb "+pdbCode+", error: "+e.getMessage());
					continue;
				} catch (PdbLoadException e) {
					System.err.println("Problem reading file for pdb "+pdbCode+", error: "+e.getMessage());
					continue;
				} catch (FileFormatException e) {
					System.err.println("Problem reading file for pdb "+pdbCode+", error: "+e.getMessage());
					continue;
				} catch (Exception e) {
					System.err.println("Unexpected problem ["+e.getClass().getCanonicalName()+"] for pdb "+pdbCode+", error: "+
										e.getMessage());
					continue;
				}
			}
			
		}

	}
	
	private static List<String> readListFile(File listFile) {
		List<String> list = new ArrayList<String>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(listFile));

			String line;
			while((line = br.readLine())!=null) {
				if (line.startsWith("#")) continue;
				if (line.isEmpty()) continue;
				list.add(line);
			}
			br.close();
		} catch (IOException e) {
			System.err.println("Problem while reading list file "+listFile+", error: "+e.getMessage());
			System.exit(1);
		}
		return list;
	}

}

package eppic.assembly;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class AssemblyGroup implements Iterable<Assembly> {

	//private static final Logger logger = LoggerFactory.getLogger(AssemblyGroup.class);
			
			
	private List<Assembly> list;
		
	
	public AssemblyGroup() {
		list = new ArrayList<Assembly>();
	}
	
	public void add(Assembly assembly) {		
		list.add(assembly);
	}
	
	public Assembly get(int i) {
		return list.get(i);
	}
	
	/**
	 * Gets the Assemblies from this group that have maximal number of 
	 * engaged interface clusters, 
	 * e.g. for group {1,2,3} {1,2,4} {1,2} {2,3}, it returns {1,2,3} {1,2,4}
	 * @return
	 */
	private List<Assembly> getLargestAssemblies() {
		TreeMap<Integer, List<Assembly>> map = new TreeMap<Integer, List<Assembly>>();
		for (Assembly a:list) {
			int n = a.getNumEngagedInterfaceClusters();
			List<Assembly> group = new ArrayList<Assembly>();
			if (map.containsKey(n)) {
				group = map.get(n);
			} else {
				map.put(n, group);
			}
			group.add(a);
		}
		
		return map.lastEntry().getValue();
	}
	
	public List<AssemblyGroup> sortIntoClusters() {
		
		List<Assembly> maximals = getLargestAssemblies();
		
		
		List<AssemblyGroup> sortedGroups = new ArrayList<AssemblyGroup>();
		
		for (Assembly max: maximals) {
			AssemblyGroup g = new AssemblyGroup();
			g.add(max); // the first member will always be the maximal group, so we can choose it as the representative of the cluster
			sortedGroups.add(g);
			for (Assembly a:list) {
				if (a==max) continue;
				if (max.isChild(a)) {
					g.add(a);
				}
			}
		}
		return sortedGroups;
	}
	
	public int size() {
		return list.size();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Assembly a:list) {
			sb.append(a.toString()+" ");
		}
		return sb.toString();
	}

	@Override
	public Iterator<Assembly> iterator() {
		return list.iterator();
	}
}

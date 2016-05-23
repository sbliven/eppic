package ch.systemsx.sybit.crkwebui.client.results.gui.panels;

import java.util.List;

import ch.systemsx.sybit.crkwebui.client.commons.appdata.AppPropertiesManager;
import ch.systemsx.sybit.crkwebui.client.commons.appdata.ApplicationContext;
import ch.systemsx.sybit.crkwebui.client.commons.util.StyleGenerator;
import ch.systemsx.sybit.crkwebui.shared.model.InterfaceCluster;
import ch.systemsx.sybit.crkwebui.shared.model.PdbInfo;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.sencha.gxt.core.client.dom.ScrollSupport.ScrollMode;
import com.sencha.gxt.core.client.util.Margins;
import com.sencha.gxt.widget.core.client.container.CssFloatLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.HorizontalLayoutContainer.HorizontalLayoutData;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer.VerticalLayoutData;
import com.sencha.gxt.widget.core.client.form.FieldSet;

public class AssemblyInfoPanel extends FieldSet {
	
	private static final int PANEL_WIDTH = 370;
	
	public static HTML assembly_info;
	
	public AssemblyInfoPanel(PdbInfo pdbInfo){
		
		//this.setHeading(StyleGenerator.defaultFontStyleString(
		//		AppPropertiesManager.CONSTANTS.info_panel_assembly_info()));
		this.setHeading("General Information");
		 
		this.setBorders(true);
		this.setWidth(PANEL_WIDTH);
		
		this.addStyleName("eppic-rounded-border");
		this.addStyleName("eppic-info-panel");
		
		CssFloatLayoutContainer mainContainer = new CssFloatLayoutContainer();
    	mainContainer.setScrollMode(ScrollMode.AUTO);	
    	
		int num_interfaces = 0;
		List<InterfaceCluster> clusters = ApplicationContext.getPdbInfo().getInterfaceClusters();
		for(InterfaceCluster ic : clusters){
			num_interfaces += ic.getInterfaces().size();
		}
    	assembly_info = new HTML("Number of assemblies: " + ApplicationContext.getPdbInfo().getAssemblies().size() + "<br>Number of interfaces: " + num_interfaces + "<br>Number of interface clusters: " + ApplicationContext.getPdbInfo().getInterfaceClusters().size());

    	mainContainer.add(assembly_info);
    	
    	this.setWidget(mainContainer);
		
	}

}

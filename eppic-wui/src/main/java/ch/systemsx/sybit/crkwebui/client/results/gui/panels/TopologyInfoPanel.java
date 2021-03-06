package ch.systemsx.sybit.crkwebui.client.results.gui.panels;


import ch.systemsx.sybit.crkwebui.client.commons.appdata.ApplicationContext;
import ch.systemsx.sybit.crkwebui.client.commons.gui.images.ImageWithTooltip;
import ch.systemsx.sybit.crkwebui.client.commons.managers.PopupRunner;
import ch.systemsx.sybit.crkwebui.client.commons.managers.ViewerRunner;
import ch.systemsx.sybit.crkwebui.shared.model.PdbInfo;
import eppic.EppicParams;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.sencha.gxt.widget.core.client.container.HorizontalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.form.FieldSet;

public class TopologyInfoPanel extends FieldSet {
	
	private static final int PANEL_WIDTH = 370;
	
	public static HTML assembly_info;
	
	public TopologyInfoPanel(PdbInfo pdbInfo){
		
		this.setHeadingHtml("Topology");
		 
		this.setBorders(true);
		this.setWidth(PANEL_WIDTH);
		
		this.addStyleName("eppic-rounded-border");
		this.addStyleName("eppic-info-panel");
		
		VerticalLayoutContainer mainContainer = new VerticalLayoutContainer();
		
		HorizontalLayoutContainer imagesContainer = new HorizontalLayoutContainer();  
		imagesContainer.setHeight(75);
    
		String jobId = ApplicationContext.getPdbInfo().getJobId();
		// PdbCodes are case insensitive
		if(ApplicationContext.getPdbInfo().getJobId().length() == 4)
			jobId = jobId.toLowerCase();
		
		String thumbnailUrl = 
				ApplicationContext.getSettings().getResultsLocationForJob(jobId) +
				"/" + ApplicationContext.getPdbInfo().getTruncatedInputName() +
				EppicParams.ASSEMBLIES_COORD_FILES_SUFFIX +
				"." + ApplicationContext.getSelectedAssemblyId() + ".75x75.png";
		
		ImageWithTooltip leftimage = new ImageWithTooltip(thumbnailUrl, null, "Click to open in 3D viewer");
		leftimage.setWidth("75px");
    	HTML spacer2 = new HTML("<div style='width:10px'></div>");
    	
		String thumbnailUrl2 = 
				ApplicationContext.getSettings().getResultsLocationForJob(jobId) +
				"/" + ApplicationContext.getPdbInfo().getTruncatedInputName() +
				EppicParams.ASSEMBLIES_DIAGRAM_FILES_SUFFIX +
				"." + ApplicationContext.getSelectedAssemblyId() + ".75x75.png";

		ImageWithTooltip rightimage = new ImageWithTooltip(thumbnailUrl2, null, "Click for detailed view");
		
		leftimage.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				ViewerRunner.runViewerAssembly(ApplicationContext.getSelectedAssemblyId()+"");
			}
			
		}); 
		
		rightimage.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				PopupRunner.popupAssemblyDiagram(ApplicationContext.getSelectedAssemblyId()+"");
			}
		
		});		
		
		imagesContainer.add(leftimage);
		imagesContainer.add(spacer2);
		imagesContainer.add(rightimage);
		mainContainer.add(imagesContainer);
		
    	HorizontalLayoutContainer linkContainer = new HorizontalLayoutContainer();

    	Anchor anchor = new Anchor("View Assembly in Unit Cell");
    	anchor.addClickHandler(new ClickHandler() {		
			@Override
			public void onClick(ClickEvent event) {
				PopupRunner.popupLatticeGraph(ApplicationContext.getSelectedAssemblyId()+"");
			}
		});
    	linkContainer.add(anchor);
    	
		mainContainer.add(linkContainer);
		
    	this.setWidget(mainContainer);

    	
		
	}

}

package ch.systemsx.sybit.crkwebui.server.jmol.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.biojava.nbio.structure.StructureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.systemsx.sybit.crkwebui.server.commons.servlets.BaseServlet;
import ch.systemsx.sybit.crkwebui.server.commons.util.io.DirLocatorUtil;
import ch.systemsx.sybit.crkwebui.server.files.downloader.servlets.FileDownloadServlet;
import ch.systemsx.sybit.crkwebui.server.jmol.generators.AssemblyDiagramPageGenerator;
import ch.systemsx.sybit.crkwebui.server.jmol.validators.AssemblyDiagramServletInputValidator;
import ch.systemsx.sybit.crkwebui.shared.exceptions.DaoException;
import ch.systemsx.sybit.crkwebui.shared.exceptions.ValidationException;
import ch.systemsx.sybit.crkwebui.shared.model.Interface;
import ch.systemsx.sybit.crkwebui.shared.model.PdbInfo;
import eppic.commons.util.IntervalSet;

/**
 * Servlet used to display an AssemblyDiagram page.
 * 
 * The following are the valid values for the parameters:
 * <pre>
 * 
 * Parameter name 					Parameter value
 * --------------					---------------
 * id								String (the jobId hash)
 * interfaces						String (comma-separated list of interface ids)
 * clusters							String (comma-separated list of interface cluster ids). Superseded by interfaces.
 *
 * @author Spencer Bliven
 */
public class AssemblyDiagramServlet extends BaseServlet
{

	private static final long serialVersionUID = 1L;

	/**
	 * The servlet name, note that the name is defined in the web.xml file.
	 */
	public static final String SERVLET_NAME = "assemblyDiagram";

	private static final Logger logger = LoggerFactory.getLogger(AssemblyDiagramServlet.class);

	//private String resultsLocation;
	private String destination_path;
	
	private String atomCachePath;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		//resultsLocation = properties.getProperty("results_location");
		destination_path = properties.getProperty("destination_path");
		atomCachePath = propertiesCli.getProperty("ATOM_CACHE_PATH");
		
		if (atomCachePath == null) 
			logger.warn("ATOM_CACHE_PATH is not set in config file, will not be able to reuse cache for PDB cif.gz files!");
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{

		//TODO add type=interface/assembly as parameter, so that assemblies can also be supported


		String jobId = request.getParameter(FileDownloadServlet.PARAM_ID);
		String requestedIfacesStr = request.getParameter(LatticeGraphServlet.PARAM_INTERFACES);
		String requestedClusterStr = request.getParameter(LatticeGraphServlet.PARAM_CLUSTERS);
		String size = request.getParameter(JmolViewerServlet.PARAM_SIZE);
		String format = request.getParameter(LatticeGraphServlet.PARAM_FORMAT);

		logger.info("Requested assemblyDiagram page for jobId={},interfaces={},clusters={},format={}",jobId,requestedIfacesStr,requestedClusterStr,format);

		PrintWriter outputStream = null;

		try
		{
			AssemblyDiagramServletInputValidator.validateLatticeGraphInput(jobId,requestedIfacesStr,requestedClusterStr,format);

			PdbInfo pdbInfo = LatticeGraphServlet.getPdbInfo(jobId);
			String input = pdbInfo.getInputName();
			String inputPrefix = pdbInfo.getTruncatedInputName();

			// job directory on local filesystem
			File dir = DirLocatorUtil.getJobDir(new File(destination_path), jobId);

			List<Interface> ifaceList = LatticeGraphServlet.getInterfaceList(pdbInfo);

			//TODO better to filter interfaces here before construction, or afterwards?
			IntervalSet requestedIntervals = LatticeGraphServlet.parseInterfaceListWithClusters(requestedIfacesStr,requestedClusterStr,ifaceList);
			Collection<Integer> requestedIfaces = requestedIntervals.getIntegerSet();

			String title = jobId + " - Assembly Diagram";
			if(requestedIfaces != null && !requestedIfaces.isEmpty()) {
				title += " for interfaces "+requestedIfacesStr;
			}


			outputStream = new PrintWriter(response.getOutputStream());

			if(format != null && format.equalsIgnoreCase("json")) {
				File auFile = LatticeGraphServlet.getAuFileName(dir, input, atomCachePath);
				// important: input (second param) here must be the truncated input name or otherwise user jobs don't work - JD 2017-02-04
				AssemblyDiagramPageGenerator.generateJSONPage(dir,inputPrefix, auFile, ifaceList, requestedIfaces,outputStream);
			} else {
				// Request URL, with format=json
				StringBuffer jsonURL = request.getRequestURL();
				Map<String, String[]> query = new LinkedHashMap<>(request.getParameterMap());
				query.put("format", new String[] {"json"});
				jsonURL.append('?')
				.append(
						query.entrySet().stream()
						.<String>flatMap( entry -> Arrays.stream(entry.getValue()).map(s -> entry.getKey()+"="+s) )
						.collect(Collectors.joining("&"))
						);
				String webappRoot = request.getContextPath();
				String servletPath = request.getServletPath();
				logger.debug("Context path: {}, servlet path: {}", webappRoot, servletPath);
				AssemblyDiagramPageGenerator.generateHTMLPage(title, size, jsonURL.toString(), ifaceList, requestedIfaces,outputStream, webappRoot);
				// TODO start generating JSON now, since we know that request is coming
			}


		}
		catch(ValidationException e)
		{
			response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Input values are incorrect: " + e.getMessage());
		}
		catch(IOException e)
		{
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error during preparation of Assembly Diagram page.");
			logger.error("Error during preparation of Assembly Diagram page.",e);
		} catch(DaoException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error during preparation of Assembly Diagram page.");
			logger.error("Error during preparation of Assembly Diagram page.",e);
		} catch (StructureException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error during preparation of Assembly Diagram page.");
			logger.error("Error during preparation of Assembly Diagram page.",e);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error during preparation of Assembly Diagram page.");
			logger.error("Error during preparation of Assembly Diagram page.",e);
		}
		finally
		{
			if(outputStream != null)
			{
				try
				{
					outputStream.close();
				}
				catch(Throwable t) {}
			}
		}
	}
}

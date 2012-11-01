package ch.systemsx.sybit.crkwebui.client.callbacks;

import ch.systemsx.sybit.crkwebui.client.CrkWebServiceAsync;
import ch.systemsx.sybit.crkwebui.client.controllers.AppPropertiesManager;
import ch.systemsx.sybit.crkwebui.client.controllers.EventBusManager;
import ch.systemsx.sybit.crkwebui.client.data.StatusMessageType;
import ch.systemsx.sybit.crkwebui.client.events.UpdateStatusLabelEvent;
import ch.systemsx.sybit.crkwebui.shared.model.RunJobData;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.HasRpcToken;
import com.google.gwt.user.client.rpc.XsrfToken;

/**
 * Callback used to handle xsrf validation when trying to run new job.
 * @author srebniak_a
 *
 */
public class RunJobCallbackXsrf implements AsyncCallback<XsrfToken>
{
	private CrkWebServiceAsync crkWebService;
	
	private RunJobData runJobData;
	
	public RunJobCallbackXsrf(CrkWebServiceAsync crkWebService,
							  RunJobData runJobData)
	{
		this.crkWebService = crkWebService;
		this.runJobData = runJobData;
	}
	
	@Override
	public void onFailure(Throwable caught) 
	{
		EventBusManager.EVENT_BUS.fireEvent(new UpdateStatusLabelEvent(AppPropertiesManager.CONSTANTS.callback_get_xsrf_token_error(), caught));
	}

	@Override
	public void onSuccess(XsrfToken token)
	{
		if (token != null)
		{
			((HasRpcToken)crkWebService).setRpcToken(token);
			crkWebService.runJob(runJobData, new RunJobCallback());
		}
		else 
		{
			EventBusManager.EVENT_BUS.fireEvent(new UpdateStatusLabelEvent(AppPropertiesManager.CONSTANTS.callback_get_xsrf_token_error() + " - incorrect type", 
																		   StatusMessageType.SYSTEM_ERROR));
		}
	}
}

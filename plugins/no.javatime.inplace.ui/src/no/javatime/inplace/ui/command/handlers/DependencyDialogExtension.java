package no.javatime.inplace.ui.command.handlers;

import no.javatime.inplace.extender.intface.Extender;
import no.javatime.inplace.extender.intface.ExtenderException;
import no.javatime.inplace.extender.intface.Extenders;
import no.javatime.inplace.extender.intface.Extension;
import no.javatime.inplace.extender.intface.Introspector;
import no.javatime.inplace.pl.dependencies.intface.DependencyDialog;

import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Not in use. Experimental.
 */
public class DependencyDialogExtension implements Extension<DependencyDialog> {

	
	private Extender<DependencyDialog> extender;
	private DependencyDialog serviceObject = null;

	public DependencyDialogExtension() {
		extender = getExtender();
		serviceObject = extender.getService();
	}
	
	public int openAsService() {
		return getService().open();
	}
	
	public boolean closeAsService() {
		return getService().close();
	}

	@SuppressWarnings("unchecked")
	public Integer open() throws ExtenderException {

		return (Integer) Introspector.invoke(DependencyDialog.OPEN_METHOD, extender.getServiceClass(), (Class[]) null, serviceObject, (Object[]) null);
	}

	@SuppressWarnings("unchecked")
	public Boolean close() throws ExtenderException {

		return (Boolean) Introspector.invoke(DependencyDialog.CLOSE_METHOD, extender.getServiceClass(), (Class[]) null, serviceObject, (Object[]) null);
	}

	@Override
	public DependencyDialog getService() {
		return extender.getService();
	}

	@Override
	public DependencyDialog getService(Bundle bundle) {
		return extender.getService(bundle);
	}

	@Override
	public Extender<DependencyDialog> getExtender() throws ExtenderException {
			return Extenders.getExtender(DependencyDialog.class.getName());
	}

	@Override
	public DependencyDialog getTrackedService() throws ExtenderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void closeTrackedService() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Boolean ungetService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean ungetService(Bundle bundle) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void openServiceTracker(ServiceTrackerCustomizer<DependencyDialog, DependencyDialog> customizer)
			throws ExtenderException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Bundle getUserBundle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTrackingCount() {
		// TODO Auto-generated method stub
		return 0;
	}
}

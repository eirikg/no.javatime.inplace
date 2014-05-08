package no.javatime.inplace.ui.extender;

import no.javatime.inplace.bundlemanager.ExtenderException;
import no.javatime.inplace.ui.Activator;
import no.javatime.inplace.ui.service.DependencyDialog;

import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;


public class DependencyDialogProxy extends Extender {

	public final static String ID = DependencyDialogProxy.class.getName();
	public final static String OPEN_CONTRACT = "open";
	public final static String CLOSE_CONTRACT = "close";
	
	private ServiceTracker<DependencyDialog, DependencyDialog> dlgTracker;
	
	public DependencyDialogProxy(Long bundleId, String interfaceName, String className) {
		super(bundleId, interfaceName, className);
		openTracker();
	}
	
	public static DependencyDialogProxy  getInstance() {
		// Consult the tracker for each invocation in case bundle has been removed/uninstalled
		Extender em = Extender.getInstance(DependencyDialog.class.getName());
		if (null != em && em instanceof DependencyDialogProxy) {
			return (DependencyDialogProxy) em;
		}
		return null;
	}

	private void openTracker() {		
		if (null == dlgTracker) {
			dlgTracker = new ServiceTracker<DependencyDialog, DependencyDialog>
					(Activator.getContext(), DependencyDialog.class.getName(), null);			
			dlgTracker.open();
		}
	}

	public void closeTracker() {
		if (null != dlgTracker) {
			dlgTracker.close();
			dlgTracker = null;
		}
	}
	
	public DependencyDialog getDlgService() {
		if (null == dlgTracker) {
			openTracker();
		}
		return dlgTracker.getService();
	}	
	

	public int openDlg() {
		int status = getDlgService().open();
		closeTracker();
		return status;
	}
	
	public int open() throws ExtenderException {
		try {
			Class<?> cls = getCls();
			Object obj = getObject();			
			return (int) Introspector.invoke(OPEN_CONTRACT, cls, (Class[]) null, obj, (Object[]) null);
		} catch (ExtenderException e) {
			throw e;
		}
	}

	public boolean close() throws ExtenderException {
		Class<?> cls = getCls();
		Object obj = getObject();			
		return (boolean) Introspector.invoke(CLOSE_CONTRACT, cls, (Class[]) null, obj, (Object[]) null);
	}
}

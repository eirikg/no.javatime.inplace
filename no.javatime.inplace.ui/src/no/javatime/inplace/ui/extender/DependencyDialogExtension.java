package no.javatime.inplace.ui.extender;

import no.javatime.inplace.extender.provider.Extender;
import no.javatime.inplace.extender.provider.Extension;
import no.javatime.inplace.extender.provider.Introspector;
import no.javatime.inplace.pl.dependencies.service.DependencyDialog;
import no.javatime.inplace.region.manager.InPlaceException;

public class DependencyDialogExtension extends Extension<DependencyDialog> {

	public final static String OPEN_CONTRACT = "open";
	public final static String CLOSE_CONTRACT = "close";
	private Extender<DependencyDialog> e;
	
	public DependencyDialogExtension() {
		super(DependencyDialog.class);
	}
	
	public int openAsService() {
		return getService().open();
	}
	
	public boolean closeAsService() {
		return getService().close();
	}

	public Integer open() throws InPlaceException {
		try {
			e = getInstance();
		} catch (InPlaceException e) {
			throw e;
		}
		return (Integer) Introspector.invoke(OPEN_CONTRACT, e.getExtensionClass(), (Class[]) null, e.createExtensionObjec(), (Object[]) null);
	}

	public Boolean close() throws InPlaceException {

		try {
			e = getInstance();
		} catch (InPlaceException e) {
			throw e;
		}
		return (Boolean) Introspector.invoke(CLOSE_CONTRACT, e.getExtensionClass(), (Class[]) null, e.createExtensionObjec(), (Object[]) null);
	}
}

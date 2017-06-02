package no.javatime.util.view;

import no.javatime.util.Activator;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 * Methods for hide/show, get and check visibility of views. 
 *
 */
public class ViewUtil {

	private static boolean isVisible;

	/**
	 * Check if the view specified by the part id is visible in the workbench
	 * 
	 * @param partId part id of the view
	 * @return true if visible
	 */
	public static Boolean isVisible(final String partId) {

		isVisible = false;
		Display display = Activator.getDisplay();
		if (null == display) {
			return false;
		}
		display.syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = Activator.getDefault().getActivePage();
				if (null != page) {
					IViewReference viewReference = page.findViewReference(partId);
					if (null != viewReference) {
						final IViewPart view = viewReference.getView(false);
						if (null != view) {
							isVisible = page.isPartVisible(view);
						}
					}
				}
			}
		});		
		return isVisible;
	}

	/**
	 * Verify that a view is visible on the active page
	 * 
	 * @param part the class instance of the view to check for visibility
	 * @return true if the view is visible and false if not
	 */
	public static Boolean isVisible(final Class<? extends ViewPart> part) {
	
		isVisible = false;
		Display display = Activator.getDisplay();
		if (null == display) {
			return false;
		}
		display.syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = Activator.getDefault().getActivePage();
				if (null != page) {
					IViewReference[] vRefs = page.getViewReferences();
					for (IViewReference vr : vRefs) {
						IViewPart vp = vr.getView(false);
						if (null != vp && vp.getClass().equals(part)) {
							isVisible = true;
							return;
						}
					}
				}
			}
		});		
		return isVisible;
	}
	
	/**
	 * Get the view part based on the specified part id
	 * 
	 * @param partId the part id of the view
	 * @return the view part instance or null
	 */
	public static IViewPart get(String partId) {
		IWorkbenchPage page = Activator.getDefault().getActivePage();
		if (null != page) {
			return page.findView(partId);
		}
		return null;
	}

	/**
	 * Hide view if visible or set focus on an already open view
	 * 
	 * @param viewId part id of view
	 */
	public static void hide(final String viewId) {
		Display display = Activator.getDisplay();
		if (null == display) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = Activator.getDefault().getActivePage();
				if (null != page) {
					// Hide even if not visible, may be on another tab
					IViewPart mv = page.findView(viewId);
					if (null != mv) {
						page.hideView(mv);
					}
				}
			}
		});
	}

	/**
	 * Open view if closed or set focus on an already open view
	 * 
	 * @param parId part id of the view to show
	 */
	public static void show(final String parId) {
		Display display = Activator.getDisplay();
		if (null == display) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = Activator.getDefault().getActivePage();
				if (null != page) {
					if (!isVisible(parId)) {
						try {
							page.showView(parId);
						} catch (PartInitException e) {
						}
					} else {
						IViewPart mv = page.findView(parId);
						if (null != mv) {
							mv.setFocus();
						}
					}
				}
			}
		});
	}
}

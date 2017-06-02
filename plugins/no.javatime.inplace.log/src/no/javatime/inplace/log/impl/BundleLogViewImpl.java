package no.javatime.inplace.log.impl;

import no.javatime.inplace.log.Activator;
import no.javatime.inplace.log.intface.BundleLogView;
import no.javatime.inplace.log.view.LogView;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

public class BundleLogViewImpl implements BundleLogView {

	public static String MESSAGE_VIEW_ID = "no.javatime.inplace.log.view";
	
	private boolean isVisible;

	public ImageDescriptor getLogViewImage() {
		return LogView.messageViewImage;
	}

	public String getViewId() {
		return MESSAGE_VIEW_ID;
	}

	public void show() {
		show(getViewId());
	}

	/**
	 * Open view if closed or set focus on an already open view
	 * @param viewId part id of view
	 */
	public void show(final String viewId) {
		Display display = Activator.getDisplay();
		if (null == display) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = Activator.getDefault().getActivePage();
				if (null != page) {
					if (!isVisible(viewId)) {
						try {
							page.showView(viewId);
						} catch (PartInitException e) {
						}
					} else  {
						IViewPart mv = page.findView(viewId);
						if (null != mv) {
							mv.setFocus();
						}
					}
				}
			}
		});
	}
	
	public void hide() {
		hide(getViewId());
	}

	/**
	 * Hide view if visible or set focus on an already open view
	 */
	public void hide(final String viewId) {
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

	public IViewPart getView(String partId) {
		IWorkbenchPage page = Activator.getDefault().getActivePage();
		if (null != page) {
			return page.findView(partId);
		}
		return null;
	}

	/**
	 * Verify that a view is visible on the active page
	 * 
	 * @param part the view to check
	 * @return true if the view is visible and false if not
	 */
	public Boolean isViewVisible(final Class<? extends ViewPart> part) {

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

	public boolean isVisible() {
		return isVisible(getViewId());
	}

	public boolean isVisible(final String viewId) {

		isVisible = false;
		Display display = Activator.getDisplay();
		if (null == display) {
			return false;
		}
		display.syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = Activator.getDefault().getActivePage();
				if (null != page) {
					IViewReference viewReference = page.findViewReference(viewId);
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
}

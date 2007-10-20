package com.simulcraft.test.gui.access;

import junit.framework.Assert;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.simulcraft.test.gui.core.InUIThread;
import com.simulcraft.test.gui.core.NotInUIThread;

public class ShellAccess extends CompositeAccess
{
	public ShellAccess(Shell shell) {
		super(shell);
	}

    @Override
	public Shell getControl() {
		return (Shell)widget;
	}
    
    @Override
    protected Point getAbsolutePointToClick() {
        return getCenter(getControl().getBounds());
    }

	@InUIThread
	public MenuAccess getMenuBar() {
		return new MenuAccess(getControl().getMenuBar());
	}

	@NotInUIThread
	public void chooseFromMainMenu(String labelPath) {
	    MenuAccess menuAccess = getMenuBar();
	    for (String label : labelPath.split("\\|"))
	        //menuAccess = menuAccess.findMenuItemByLabel(label).activateWithMouseClick();
	        menuAccess = menuAccess.activateMenuItemWithMouse(label);
	}

	/**
	 * Asserts that this shell is the active shell, i.e. it waits (with timeout)
	 * for it to become active.
	 */
	@InUIThread
	public void assertIsActive() {
		Assert.assertTrue("not the active shell", Display.getCurrent().getActiveShell() == getControl());
	}
}

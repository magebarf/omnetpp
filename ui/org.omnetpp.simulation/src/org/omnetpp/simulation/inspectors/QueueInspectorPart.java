package org.omnetpp.simulation.inspectors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.simulation.figures.QueueInspectorFigure;
import org.omnetpp.simulation.model.cObject;
import org.omnetpp.simulation.model.cQueue;

/**
 * Experimental graphical inspector for queues
 * @author Andras
 */
//XXX add tooltip for message items
//XXX add similar context menu as GraphicalModulePart has
//XXX lots of stuff common with GraphicalModulePart -- factor them out? 
public class QueueInspectorPart extends InspectorPart {
	private Map<cObject,IFigure> objectToFigureMap = new HashMap<cObject, IFigure>();
	private Map<IFigure,cObject> figureToObjectMap = new HashMap<IFigure, cObject>();
	private cObject[] prevObjects = null; // just to detect changes...
	
	public QueueInspectorPart(cObject object) {
		super(object);

		if (object.isFilledIn())
            object.safeLoad();
		
		figure = new QueueInspectorFigure();
		figure.setInspectorPart(this);

        // mouse handling
		//XXX near copy-paste from GraphicalModulePart, factor out!
        figure.addMouseListener(new MouseListener() {
            //@Override
            public void mouseDoubleClicked(MouseEvent me) {
                handleMouseDoubleClick(me);
            }

			//@Override
			public void mousePressed(MouseEvent me) {
                handleMousePressed(me);
			}

			//@Override
			public void mouseReleased(MouseEvent me) {
                handleMouseReleased(me);
			}
        });
	}

	@Override
	public void refresh() {
		super.refresh();
		if (!isDisposed()) {
			cQueue queue = (cQueue)object;
			QueueInspectorFigure queueFigure = (QueueInspectorFigure)figure;

			// only rebuild everything if queue contents has changed
			cObject[] childObjects = new cObject[0]; //FIXME queue.getChildObjects();
			if (!Arrays.equals(childObjects, prevObjects)) {
				// clear and re-add message figures
				prevObjects = childObjects;
				queueFigure.removeAll();
				objectToFigureMap.clear();
				figureToObjectMap.clear();
				for (cObject msg : childObjects) {
					IFigure msgFigure = queueFigure.createQueueItemFigure();
					queueFigure.add(msgFigure);
					objectToFigureMap.put(msg,msgFigure);
					figureToObjectMap.put(msgFigure,msg);
				}
			}
		}
	}
	
    public IFigure findQueueItemFigureAt(int x, int y) {
        for (IFigure f = figure.findFigureAt(x, y); f!=null && f!=figure; f=f.getParent())
        	if (figureToObjectMap.containsKey(f))
        		return f;
        return null;
    }

	
	//@Override
	public boolean isMaximizable() {
		return false;
	}

	//@Override
	public void populateContextMenu(MenuManager contextMenuManager, Point p) {
        contextMenuManager.add(new Action("Close") {
            @Override
            public void run() {
                getContainer().close(QueueInspectorPart.this);
            }
        });
	}

    //XXX near copy/paste from GraphicalModulePart - factor out
    @Override
    public void selectionChanged(IStructuredSelection selection) {
    	super.selectionChanged(selection);

    	// update selection border around submodules
        List list = selection.toList();
        for (cObject obj : objectToFigureMap.keySet()) {
        	IFigure itemFigure = objectToFigureMap.get(obj);
        	//FIXME TODO itemFigure.setSelectionBorderShown(list.contains(obj));
        	itemFigure.setForegroundColor(list.contains(obj) ? ColorFactory.BLACK : ColorFactory.RED);
        }
    }

    //XXX near copy/paste from GraphicalModulePart - factor out
    protected void handleMouseDoubleClick(MouseEvent me) {
    	IFigure queueItemFigure = findQueueItemFigureAt(me.x,me.y);
		System.out.println("clicked item: " + queueItemFigure);
		if (queueItemFigure != null) {
			cObject obj = figureToObjectMap.get(queueItemFigure);
            //TODO Activator.openInspector2(obj, true);
		}
    }

    //XXX near copy/paste from GraphicalModulePart - factor out
    protected void handleMousePressed(MouseEvent me) {
    	IFigure queueItemFigure = findQueueItemFigureAt(me.x,me.y);
		System.out.println("clicked item: " + queueItemFigure);
		if (queueItemFigure == null) {
            if ((me.getState()& SWT.CONTROL) != 0)
            	inspectorContainer.toggleSelection(getObject());
            else
            	inspectorContainer.select(getObject(), true);
		}
		else {
			cObject obj = figureToObjectMap.get(queueItemFigure);
            if ((me.getState()& SWT.CONTROL) != 0)
            	inspectorContainer.toggleSelection(obj);
            else
            	inspectorContainer.select(obj, true);
		}
		//note: no me.consume()! it would kill the move/resize listener
    }

    protected void handleMouseReleased(MouseEvent me) {
    }


}
package org.omnetpp.ned.editor.graph.edit.policies;

import java.util.List;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.FlowLayoutEditPolicy;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.omnetpp.ned.editor.graph.commands.CloneCommand;
import org.omnetpp.ned.editor.graph.commands.CreateToplevelComponentCommand;
import org.omnetpp.ned.editor.graph.commands.ReorderCommand;
import org.omnetpp.ned.editor.graph.commands.SetCompoundModuleConstraintCommand;
import org.omnetpp.ned2.model.INamedGraphNode;
import org.omnetpp.ned2.model.NEDElement;

/**
 * Layout policy used in the top levele NedFile element allowing a vertical, toolbar like
 * layout, rearange of components.
 * @author rhornig
 *
 */
public class NedFileLayoutEditPolicy extends FlowLayoutEditPolicy {

	/** (non-Javadoc)
	 * @see org.eclipse.gef.editpolicies.OrderedLayoutEditPolicy#createChildEditPolicy(org.eclipse.gef.EditPart)
	 * we create a resize police where only SOUTH and EAST sides are modifiable
	 */
	@Override
	protected EditPolicy createChildEditPolicy(EditPart child) {
		ResizableEditPolicy policy = new ResizeFeedbackEditPolicy();
		// we need only resize support for the south east and corner
		policy.setResizeDirections(PositionConstants.SOUTH_EAST);
		return policy;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.gef.editpolicies.FlowLayoutEditPolicy#isHorizontal()
	 * WARNING we override this function so it is possible to use the ToolbarLayout
	 * with the FlowLayoutPolicy, because implementation of FlowLayoutEditPolicy#isHorizonta()
	 * depends on FlowLayout.
	 */
	@Override
	protected boolean isHorizontal() {
		return false;
	}
	
	// TODO implement generic clone command
	@SuppressWarnings("unchecked")
	@Override
	protected Command getCloneCommand(ChangeBoundsRequest request) {
		
		EditPart iPoint = getInsertionReference(request);
		NEDElement insertBeforeNode = (iPoint != null) ? (NEDElement)iPoint.getModel() : null;
		NEDElement parent = (NEDElement)getHost().getModel();
		CloneCommand cloneCmd = new CloneCommand(parent, insertBeforeNode);

		// iterate through all involved editparts and add their model to the coning list
		for (GraphicalEditPart currPart : (List<GraphicalEditPart>)request.getEditParts())
			cloneCmd.addPart((NEDElement)currPart.getModel());
		
		return cloneCmd;
	}
		
	// adding an already existing node is not supported
	protected Command createAddCommand(EditPart child, EditPart after) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gef.editpolicies.OrderedLayoutEditPolicy#createMoveChildCommand(org.eclipse.gef.EditPart, org.eclipse.gef.EditPart)
	 * if wherePart is null we must insert the child at the end  
	 */
	protected Command createMoveChildCommand(EditPart movedPart, EditPart wherePart) {
		NEDElement where = (wherePart != null) ? (NEDElement)wherePart.getModel() : null;
		NEDElement node = (NEDElement)movedPart.getModel();
		return new ReorderCommand(where, node);
	}

	protected Command getCreateCommand(CreateRequest request) {
		NEDElement newElement = (NEDElement)request.getNewObject();
		EditPart insertionPoint = getInsertionReference(request);
		NEDElement where = (insertionPoint != null) ? (NEDElement)insertionPoint.getModel() : null;
		NEDElement parent = (NEDElement)getHost().getModel();
		return new CreateToplevelComponentCommand(parent, where, newElement);
	}
	/**
	 * Returns the <code>Command</code> to resize a group of children.
	 * @param request the ChangeBoundsRequest
	 * @return the Command
	 */
	@SuppressWarnings("unchecked")
	protected Command getResizeChildrenCommand(ChangeBoundsRequest request) {
		CompoundCommand resize = new CompoundCommand();
		Command c;

		for (GraphicalEditPart child : (List<GraphicalEditPart>)request.getEditParts()) {
			c = createChangeConstraintCommand(request, child, getConstraintFor(request, child));
			resize.add(c);
		}
		return resize.unwrap();
	}
	
    protected Command createChangeConstraintCommand(ChangeBoundsRequest request,
    						EditPart child, Object constraint) {
        // HACK for fixing issue when the model returns unspecified size (-1,-1)
        // we have to calculate the center point in that direction manually using the size info
        // from the figure directly (which knows it's size) This is the inverse transformation of
        // CenteredXYLayout's traf.
        Rectangle figureBounds = ((GraphicalEditPart)child).getFigure().getBounds();
        Rectangle modelConstraint = (Rectangle)constraint;
        if (modelConstraint.width < 0) modelConstraint.x += figureBounds.width / 2;
        if (modelConstraint.height < 0) modelConstraint.y += figureBounds.height / 2;

        // create the constraint change command 
        INamedGraphNode module = (INamedGraphNode) child.getModel();
        SetCompoundModuleConstraintCommand cmd = new SetCompoundModuleConstraintCommand(module);
        cmd.setSize(modelConstraint.getSize());

        // if size constrant is not specified, then remove it from the model too
        // TODO is this needed?
        if ((modelConstraint.width < 0 || modelConstraint.height < 0) && module.getDisplayString().getCompoundSize() == null)
            cmd.setSize(null);
        
        return cmd;
    }

    /**
	 * Generates a draw2d constraint object derived from the specified child EditPart using
	 * the provided Request. The returned constraint will be translated to the application's
	 * model later using {@link #translateToModelConstraint(Object)}.
	 * @param request the ChangeBoundsRequest
	 * @param child the child EditPart for which the constraint should be generated
	 * @return the draw2d constraint
	 */
	protected Object getConstraintFor (ChangeBoundsRequest request, GraphicalEditPart child) {
		Rectangle rect = new PrecisionRectangle(child.getFigure().getBounds());
		child.getFigure().translateToAbsolute(rect);
		rect = request.getTransformedRectangle(rect);
		child.getFigure().translateToRelative(rect);
		rect.translate(getLayoutContainer().getClientArea().getLocation().getNegated());
		return rect;
	}

    /**
	 * Factors out RESIZE  requests, otherwise calls <code>super</code>.
	 * @see org.eclipse.gef.EditPolicy#getCommand(Request)
	 */
	public Command getCommand(Request request) {
		if (REQ_RESIZE_CHILDREN.equals(request.getType()))
			return getResizeChildrenCommand((ChangeBoundsRequest)request);

		return super.getCommand(request);
	}
}

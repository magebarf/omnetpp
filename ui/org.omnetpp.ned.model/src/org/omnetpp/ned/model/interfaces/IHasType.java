/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.model.interfaces;

import org.omnetpp.ned.model.INedElement;

/**
 * Elements that are instances of a NED type: submodule and connection.
 *
 * @author rhornig
 */
public interface IHasType extends INedElement {
    /**
     * Returns the type of the object
     */
    public String getType();

    /**
     * Sets the type attribute
     */
    public void setType(String type);

    /**
     * Returns the type info after the "like" keyword
     */
    public String getLikeType();

    /**
     * Sets the like-type
     */
    public void setLikeType(String type);

    /**
     * Returns the like parameter
     */
    public String getLikeParam();

    /**
     * Sets the like parameter
     */
    public void setLikeParam(String type);

    /**
     * Returns the type, or the likeType if type was not specified. For connections with
     * implicit type (i.e. where getType()==""), it returns one of "ned.IdealChannel", 
     * "ned.DelayChannel" or "ned.DatarateChannel", based on the parameter assignments
     * in the connection's "parameters" section.
     */
    public String getEffectiveType();

    /**
     * Returns the typeinfo for the effective type.
     *
     * Returns null if the effective type is not filled in, or is not a valid NED type,
     * or not a type that's accepted at the given place (e.g. a channel for submodule type).
     */
    public INedTypeInfo getNedTypeInfo();

    /**
     * Returns the model element for the effective type. Equivalent to
     * getNedTypeInfo().getNedElement(), but handles nulls.
     *
     * Returns null if the effective type is not filled in, or is not a valid NED type,
     * or not a type that's accepted at the given place (e.g. a channel for submodule type).
     */
    public INedTypeElement getEffectiveTypeRef();
}

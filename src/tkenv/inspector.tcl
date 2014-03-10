#=================================================================
#  INSPECTOR.TCL - part of
#
#                     OMNeT++/OMNEST
#            Discrete System Simulation in C++
#
#=================================================================

#----------------------------------------------------------------#
#  Copyright (C) 1992-2008 Andras Varga
#
#  This file is distributed WITHOUT ANY WARRANTY. See the file
#  `license' for details on this and other legal matters.
#----------------------------------------------------------------#


#
# Invoked from concrete inspector subtypes
#
proc createInspectorToplevel {w geom} {
    global config fonts icons help_tips
    global B2 B3

    # Create window
    toplevel $w -class Toplevel
    wm focusmodel $w passive

    set state "normal"
    regexp {(.*):(.*)} $geom dummy geom state
    catch {wm geometry $w $geom}
    catch {wm state $w $state}

    wm minsize $w 1 1
    wm overrideredirect $w 0
    wm resizable $w 1 1
    wm protocol $w WM_DELETE_WINDOW "inspector:close $w"

    # Stay on top
    if {$config(keep-inspectors-on-top)} {
        makeTransient $w $geom
    }

    # Create toolbar
    frame $w.toolbar -relief raised -bd 1
    pack $w.toolbar -anchor w -side top -fill x -expand 0

    packIconButton $w.toolbar.sep0 -separator
    packIconButton $w.toolbar.back -image $icons(back) -command "inspector:back $w"
    packIconButton $w.toolbar.forward -image $icons(forward) -command "inspector:forward $w"
    packIconButton $w.toolbar.owner -image $icons(parent) -command "inspector:inspectOwner $w"
    packIconButton $w.toolbar.sep01 -separator
    packIconButton $w.toolbar.inspectas -image $icons(inspectas) -command "inspector:inspectAsPopup $w $w.toolbar.inspectas"
    packIconButton $w.toolbar.copyobj -image $icons(copy) -command "inspector:namePopup $w $w.toolbar.copyobj"
    packIconButton $w.toolbar.objs -image $icons(findobj) -command "inspectFilteredObjectList $w"

    set help_tips($w.toolbar.owner) {Inspect owner}
    set help_tips($w.toolbar.inspectas) {Inspect}
    set help_tips($w.toolbar.copyobj) {Copy name, type or pointer}
    set help_tips($w.toolbar.objs) {Find objects (Ctrl+S)}

    # Create info bar
    frame $w.infobar -relief raised -bd 1
    label $w.infobar.icon -anchor w -relief flat -image $icons(cogwheel_vs)
    label $w.infobar.name -anchor w -relief flat -justify left
    pack $w.infobar.icon -anchor n -side left -expand 0 -fill y -pady 1
    pack $w.infobar.name -anchor n -side left -expand 1 -fill both -pady 1
    pack $w.infobar -anchor w -side top -fill x -expand 0

    # Keyboard bindings
    bind $w <Escape>     "catch {.popup unpost}"
    bind $w <Button-1>   "catch {.popup unpost}"
    bind $w <Key-Return> "opp_commitinspector $w; opp_refreshinspectors"

    bindRunCommands $w
    bindOtherCommands $w
}

proc inspector:createInternalToolbar {w {parent ""}} {
    if {$parent==""} {set parent $w}
    frame $w.toolbar -border 2 -relief groove
    place $w.toolbar -in $parent -relx 1.0 -rely 0 -anchor ne -x -2 -y 2
    return $w.toolbar
}

#
# Invoked from C++
#
proc inspector:onSetObject {w} {
    set ptr [opp_inspector_getobject $w]
    set icon [inspector:getIconForObject $ptr]
    if [opp_inspector_istoplevel $w] {
        $w.infobar.icon config -image $icon
    }
    if {$w==".network"} {
        mainWindow:networkViewInputChanged $ptr
    }
}

#
# Invoked from C++
#
proc inspector:refresh {w} {
    global config help_tips

    set ptr [opp_inspector_getobject $w]
    if [opp_isnull $ptr] {
        $w.infobar.name config -text {n/a}
        $w.toolbar.owner config -state disabled
    } else {
        # Info bar
        set typename [opp_getobjectshorttypename $ptr]
        set fullpath [opp_getobjectfullpath $ptr]
        #set info [opp_getobjectinfostring $ptr]
        set str "($typename) $fullpath"   ;#TODO short info?
        $w.infobar.name config -text $str

        # 'Inspect owner' button
        set ownerptr [opp_getobjectowner $ptr]
        if [opp_isnull $ownerptr] {set state disabled} else {set state normal}
        $w.toolbar.owner config -state $state

        if {[opp_inspector_supportsobject $w $ownerptr] && $config(reuse-inspectors)} {
            set help_tips($w.toolbar.owner) {Go up}
        } else {
            set help_tips($w.toolbar.owner) {Inspect owner}
        }
    }

    if [opp_inspectorcommand $w cangoback] {set state normal} else {set state disabled}
    catch {$w.toolbar.back config -state $state} ;#FIXME add proper condition whether button exists

    if [opp_inspectorcommand $w cangoforward] {set state normal} else {set state disabled}
    catch {$w.toolbar.forward config -state $state}  ;#FIXME add proper condition whether button exists

}

# icons used in the tree view and listboxes
set treeicons(cCompoundModule) {compound_vs}
set treeicons(cSimpleModule)   {simple_vs}
set treeicons(cPlaceholderModule) {placeholder_vs}
set treeicons(cGate)           {gate_vs}
set treeicons(cPar)            {param_vs}
set treeicons(cMessage)        {message_vs}
set treeicons(cQueue)          {queue_vs}
set treeicons(cLinkedList)     {queue_vs}
set treeicons(cArray)          {container_vs}
set treeicons(cMessageHeap)    {container_vs}
set treeicons(cChannel)        {chan_vs}
set treeicons(cStatistic)      {stat_vs}
set treeicons(cOutVector)      {outvect_vs}

#
# Returns the icon of an object (for tree view / listbox)
#
proc inspector:getIconForObject {ptr} {
    global icons treeicons
    if [opp_isnull $ptr] {
       return $icons(none_vs)
    }
    set class [opp_getobjectbaseclass $ptr]
    if [info exists treeicons($class)] {
       return $icons($treeicons($class))
    } else {
       return $icons(cogwheel_vs)
    }
}

#
# Invoked by the WM (Window Manager)
#
proc inspector:close {w} {
    # invokes app->deleteInspector(insp)
    opp_deleteinspector $w
}

#
# Called from C++
#
proc inspector:destroy {w} {
    destroy $w
}

#
# Called from C++
#
proc inspector:show {w} {
    showWindow $w
}

proc inspector:back {w} {
    opp_inspectorcommand $w goback
}

proc inspector:forward {w} {
    opp_inspectorcommand $w goforward
}

proc inspector:inspectOwner {w} {
    global config

    set ptr [opp_inspector_getobject $w]
    set ownerptr [opp_getobjectowner $ptr]
    if [opp_isnull $ownerptr] {
        return
    }

    # inspect in current inspector if possible (and allowed), otherwise open a new one
    if {[opp_inspector_supportsobject $w $ownerptr] && $config(reuse-inspectors)} {
        opp_inspector_setobject $w $ownerptr
    } else {
        opp_inspect $ownerptr "(default)"
    }
}

proc inspector:dblClick {w ptr} {
    global config

    # inspect in current inspector if possible (and allowed), otherwise open a new one
    if {[opp_inspector_supportsobject $w $ptr] && $config(reuse-inspectors)} {
        opp_inspector_setobject $w $ptr
    } else {
        opp_inspect $ptr "(default)"
    }
}

proc inspector:namePopup {w toolbutton} {
    set ptr [opp_inspector_getobject $w]
    regsub {^ptr} $ptr {0x} p

    catch {destroy .popup}
    menu .popup -tearoff 0
    .popup add command -label "Copy pointer with cast (for debugger)" -command [list setClipboard "(([opp_getobjectfield $ptr className] *)$p)"]
    .popup add command -label "Copy pointer value (for debugger)" -command [list setClipboard $p]
    .popup add separator
    .popup add command -label "Copy full path" -command [list setClipboard [opp_getobjectfullpath $ptr]]
    .popup add command -label "Copy name" -command [list setClipboard [opp_getobjectfullname $ptr]]
    .popup add command -label "Copy class name" -command [list setClipboard [opp_getobjectfield $ptr className]]

    set x [winfo rootx $toolbutton]
    set y [expr [winfo rooty $toolbutton]+[winfo height $toolbutton]]
    tk_popup .popup $x $y
}

proc inspector:inspectAsPopup {w toolbutton} {
    set ptr [opp_inspector_getobject $w]
    set curtype [opp_inspectortype [opp_inspector_gettype $w]]
    set typelist [opp_supported_insp_types $ptr]

    catch {destroy .popup}
    menu .popup -tearoff 0
    foreach type $typelist {
        if {$type==$curtype} {set state disabled} else {set state normal}
        .popup add command -label "$type" -command [list opp_inspect $ptr $type] -state $state
    }

    set x [winfo rootx $toolbutton]
    set y [expr [winfo rooty $toolbutton]+[winfo height $toolbutton]]
    tk_popup .popup $x $y
}


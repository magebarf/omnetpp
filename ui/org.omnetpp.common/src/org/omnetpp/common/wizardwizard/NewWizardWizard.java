package org.omnetpp.common.wizardwizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.omnetpp.common.wizard.CreationContext;
import org.omnetpp.common.wizard.IContentTemplate;
import org.omnetpp.common.wizard.TemplateBasedWizard;


/**
 * "New Wizard" wizard
 * 
 * @author Andras
 */
public class NewWizardWizard extends TemplateBasedWizard {
    private NewWizardProjectSelectionPage firstPage;
    
    @Override
    public void addPages() {
        setWindowTitle("New Wizard");
        addPage(firstPage = new NewWizardProjectSelectionPage("first page"));
        super.addPages();
    }

    @Override
    protected CreationContext createContext(IContentTemplate selectedTemplate, IContainer folder) {
        CreationContext context = super.createContext(selectedTemplate, folder);
        context.setVariable("newWizardName", firstPage.getWizardName());
        context.setVariable("newWizardProject", firstPage.getProject());
        return context;
    }
    
    @Override
    protected IWizardPage getFirstExtraPage() {
        return null;
    }

    @Override
    protected IContainer getFolder() {
        return firstPage.getWizardFolder();
    }

    public String getWizardType() {
        return "wizard";
    }

}

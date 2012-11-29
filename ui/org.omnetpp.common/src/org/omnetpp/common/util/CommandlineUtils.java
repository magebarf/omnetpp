package org.omnetpp.common.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.omnetpp.common.util.StringUtils;


/**
 * Offers the user to auto-import as project the directory that contains the
 * files listed on the command-line.
 *
 * @author Andras
 */
public class CommandlineUtils {
    /**
     * Create, import or create projects so that the files given as command line args
     * become accessible in the workspace. If the files array is empty, it makes
     * the current working directory accessible in the workspace.
     *
     * @return true on (probable) success, false on failure or user cancellation.
     */
    public static boolean ensureFilesAreAvailableInWorkspace(String[] fileNames) {
        // check if all files are available via the workspace
        if (fileNames.length > 0 && availableInOpenProjects(fileNames))
            return true;

        // find common ancestor of all files
        File workingDir = new File((String) System.getProperties().get("user.dir"));
        File commonDir = fileNames.length == 0 ? workingDir : determineCommonDir(fileNames);
        if (commonDir == null) {
            MessageDialog.openError(getParentShell(), "Error", "Error: Cannot create a project that covers all input files. Please create project(s) manually, or move/soft link files and start this program again.");
            return false;
        }

        // maybe a closed project contains it, offer opening the project
        IProject project = findOpenOrClosedProjectContaining(commonDir); // actually there may be more than one such project (projects may overlap), but oh well...
        if (project != null) {
            if (project.isOpen())
                return true;
            if (MessageDialog.openQuestion(getParentShell(), "Open Project?", "Open existing project '" + project.getName() + "' that covers location " + commonDir.toString() + "?")) {
                return openProject(project);
            }
            return false;
        }

        // bring up "Create Project" dialog to choose between project locations below (stored in IProjectDescription objects)
        IProjectDescription[] descriptions = collectPotentialProjectDescriptions(commonDir);
        if (descriptions.length == 0) {
            if (fileNames.length == 0)
                return true; // no args, don't worry about project creation
            File workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
            if (isPrefixOf(commonDir, workspaceLocation))
                MessageDialog.openError(getParentShell(), "Error", "Error: Cannot create a project to cover directory " + commonDir + ", because it contains the workspace location " + workspaceLocation + ". Project directories must not contain the workspace where administrative information is stored about them.\n\nPlease choose File->Switch Workspace from the menu, and select another workspace location.");
            else
                MessageDialog.openError(getParentShell(), "Error", "Error: Cannot create a project to cover directory " + commonDir + ". Try changing the workspace location."); // we really don't know what's wrong, but suggest something anyway...
            return false;
        }

        // now really bring up the dialog
        String message = fileNames.length == 0 ?
                "Do you want to create a project to make files in the current directory accessible?" :
                    "Create a project to make your files accessible in the workspace.";
        CreateProjectDialog dialog = new CreateProjectDialog(getParentShell(), message, descriptions);
        if (dialog.open() == Dialog.OK) {
            IProjectDescription description = dialog.getResult();
            createOrImportProject(description);
            return true;
        }
        return false;
    }

    private static boolean availableInOpenProjects(String[] args) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        for (String arg : args) {
            // note: findFilesForLocationURI() also finds closed projects, so we need to check if any are open
            IFile[] candidates = root.findFilesForLocationURI(new File(arg).toURI());
            boolean foundOpenProject = false;
            for (IFile candidate : candidates)
                if (candidate.getProject().isOpen())
                    foundOpenProject = true;
            if (!foundOpenProject)
                return false;
        }
        return true;
    }

    /**
     * Determine a common filesystem directory that contains all given files/dirs.
     * Args are filesystem paths (absolute or working directory relative).
     */
    private static File determineCommonDir(String[] args) {
        Assert.isTrue(args.length > 0);

        File[] files = new File[args.length];
        for (int i=0; i<args.length; i++)
            files[i] = new File(args[i]).getAbsoluteFile();

        // find common prefix
        File prefix = files[0];
        if (!prefix.isDirectory())
            prefix = prefix.getParentFile();
        while (prefix != null) {
            // prefix of all files?
            boolean isPrefixOfAll = true;
            for (File file : files)
                if (!isPrefixOf(prefix, file))
                    isPrefixOfAll = false;
            if (isPrefixOfAll)
                return prefix;
            prefix = prefix.getParentFile();
        }

        return null;  // no common prefix (i.e. files are on different drives)
    }

    /**
     * Find first project that covers the given filesystem path.
     */
    private static IProject findOpenOrClosedProjectContaining(File dir) {
        // note: this implementation ignores linked dirs inside the projects
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        for (IProject project : root.getProjects()) {
            IPath location = project.getLocation();
            if (location != null && isPrefixOf(location.toFile(), dir))
                return project;
        }
        return null;
    }

    private static boolean isPrefixOf(File prefix, File file) {
        while (file != null) {
            if (file.equals(prefix))
                return true;
            file = file.getParentFile();
        }
        return false;
    }

    /**
     * Collect a list of places where we'll offer to create a project; this is the
     * "common" directory of files, and all ancestor directories.
     *
     * Invalid locations (such as the dirs that contain the whole workspace)
     * are filtered out.
     *
     * The result may be zero length, e.g. when dir is the workspace dir or its
     * ancestor.
     *
     * We return the result as a list of project descriptions, for convenience.
     * (Essentially we use them as (location, projectName) pairs.)
     */
    private static IProjectDescription[] collectPotentialProjectDescriptions(File dir) {
        List<IProjectDescription> result = new ArrayList<IProjectDescription>();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProjectDescription firstExistingDescription = null;
        while (dir != null) {
            IStatus status = workspace.validateProjectLocation(null, new Path(dir.getAbsolutePath()));
            if (status.getSeverity() != IStatus.ERROR) {
                IProjectDescription description = null;
                try {
                    File projectFile = new File(dir.getPath() + File.separator + IProjectDescription.DESCRIPTION_FILE_NAME);
                    description = workspace.loadProjectDescription(new Path(projectFile.getPath()));
                    if (firstExistingDescription == null)
                        firstExistingDescription = description;
                }
                catch (CoreException e1) { }
                if (description == null)
                    description = workspace.newProjectDescription(suggestProjectNameFor(dir.getAbsolutePath()));
                description.setLocation(new Path(dir.getPath()));
                result.add(description);
            }
            dir = dir.getParentFile();
        }
        return result.toArray(new IProjectDescription[]{});
    }

    /**
     * Suggest a name for a project; should be valid and different from existing project names.
     */
    private static String suggestProjectNameFor(String location) {
        String projectName = new Path(location).lastSegment();
        if (projectName == null)
            projectName = "unnamed";

        // if already exists, tweak the name a little
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        if (root.getProject(projectName).exists()) {
            int k = 2;
            while (root.getProject(projectName + "-" + k).exists())
                k++;
            projectName += "-" + k;
        }
        return projectName;
    }

    /**
     * Create or import a project. Actually, if the project description was obtained
     * by reading it from disk then it's "import", and if it was created from scratch
     * then it is "create".
     *
     * Project will also be opened.
     *
     * This a synchronous operation, i.e. we'll return only when the whole thing is done.
     */
    @SuppressWarnings("deprecation")
    private static void createOrImportProject(final IProjectDescription description) {
        boolean exists = new File(description.getLocation().append(IProjectDescription.DESCRIPTION_FILE_NAME).toOSString()).isFile();
        final String taskLabel = exists ?
                "Importing project" : "Creating project";
        try {
            IRunnableWithProgress op = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask(taskLabel, 100);
                        IWorkspace workspace = ResourcesPlugin.getWorkspace();
                        IProject project = workspace.getRoot().getProject(description.getName());
                        project.create(description, new SubProgressMonitor(monitor, 50));
                        project.open(IResource.NONE, new SubProgressMonitor(monitor, 50));
                    }
                    catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };
            new ProgressMonitorDialog(getParentShell()).run(true, true, op);
        }
        catch (InterruptedException e) {
            // cancelled
        }
        catch (InvocationTargetException e) {
            showErrorDialog("Error creating project " + description.getName(), e.getCause());
        }
    }

    /**
     * Open project in a synchronous operation, i.e. we'll return only when the whole thing is done.
     * Returns true on success, false otherwise.
     */
    private static boolean openProject(final IProject project) {
        try {
            IRunnableWithProgress op = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask("Opening project", 100);
                        project.open(IResource.NONE, new SubProgressMonitor(monitor, 100));
                    }
                    catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };
            new ProgressMonitorDialog(getParentShell()).run(true, true, op);
            return true;
        }
        catch (InterruptedException e) {
            // cancelled
        }
        catch (InvocationTargetException e) {
            showErrorDialog("Error opening project " + project.getName(), e);
        }
        return false;
    }

    private static void showErrorDialog(String message, Throwable e) {
        if (e != null && !StringUtils.isEmpty(e.getMessage()))
            message += ": " + e.getMessage();
        MessageDialog.openError(getParentShell(), "Error", message);
    }

    private static Shell getParentShell() {
        return Display.getCurrent().getActiveShell(); // or: PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), but workbench window may be null
    }
}

/**
 * Offer creating a project in the given directory or one of its ancestors,
 * based on user's choice (combo box).
 *
 * @author Andras
 */
class CreateProjectDialog extends TitleAreaDialog {
    // dialog config
    private String message;
    private IProjectDescription[] descriptions;

    // widgets
    private Text projectNameText;
    private ComboViewer combo;
    private Button okButton;

    // result
    private IProjectDescription descriptionResult;

    private boolean insideValidation;

    public CreateProjectDialog(Shell parentShell, String message, IProjectDescription[] descriptions) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        this.descriptions = descriptions;
        this.message = message;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Create Project");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Create Project");
        setMessage(message);

        Composite dialogArea = (Composite) super.createDialogArea(parent);

        Composite composite = new Composite(dialogArea, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(new GridLayout(2,false));

        createLabel(composite, "Project root:", parent.getFont());
        combo = new ComboViewer(composite, SWT.BORDER | SWT.READ_ONLY);
        combo.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        createLabel(composite, "Name:", parent.getFont());
        projectNameText = new Text(composite, SWT.BORDER);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // create input
        IProjectDescription firstExistingDescription = null;
        for (IProjectDescription description : descriptions)
            if (firstExistingDescription==null && existsOnDisk(description))
                firstExistingDescription = description;

        if (firstExistingDescription != null) {
            Label l = createLabel(composite, "* Project description file (.project) found on disk\n", parent.getFont());
            ((GridData)l.getLayoutData()).horizontalSpan = 2;
        }

        // fill combo
        combo.setContentProvider(new ArrayContentProvider());
        combo.setLabelProvider(new LabelProvider() {
            @Override
            @SuppressWarnings("deprecation")
            public String getText(Object element) {
                IProjectDescription desc = (IProjectDescription)element;
                String location = desc.getLocation().toOSString();
                return existsOnDisk(desc) ? location + " *" : location;
            }
        });
        combo.setInput(descriptions);

        IProjectDescription initialSelection = firstExistingDescription;
        if (initialSelection == null)
            initialSelection = descriptions[0];
        combo.setSelection(new StructuredSelection(initialSelection));

        // set up validation on content changes
        combo.getCombo().addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                comboSelectionChanged();
                validateDialogContents();
            }
        });
        projectNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateDialogContents();
            }
        });

        comboSelectionChanged();  // initialize project name

        // note: do initial validation when OK button is already created, from createButtonsForButtonBar()

        // focus on first field
        combo.getCombo().setFocus();

        return composite;
    }

    @SuppressWarnings("deprecation")
    private boolean existsOnDisk(IProjectDescription desc) {
        return new File(desc.getLocation().append(IProjectDescription.DESCRIPTION_FILE_NAME).toOSString()).exists();
    }

    protected static Label createLabel(Composite parent, String text, Font font) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(text);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        label.setFont(font);
        return label;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

        // do this here because setting the text will set enablement on the OK button
        validateDialogContents();
    }

    protected void comboSelectionChanged() {
        IProjectDescription description = getComboSelection();

        projectNameText.setText(description.getName());
        projectNameText.selectAll();
    }

    protected void validateDialogContents() {
        // prevent notification loops
        if (insideValidation)
            return;
        insideValidation = true;

        boolean hasError = false;

        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        String projectName = projectNameText.getText();
        IStatus status = workspace.validateName(projectName, IResource.PROJECT);
        if (status.getSeverity() == IStatus.ERROR) {
            setErrorMessage(status.getMessage());
            hasError = true;
        }
        else if (workspace.getRoot().getProject(projectName).exists()) {
            setErrorMessage("A project named '" + projectName + "' already exists.");
            hasError = true;
        }
        else {
            // modify name in the project description
            IProjectDescription description = getComboSelection();
            description.setName(projectName);
        }

        if (!hasError)
            setErrorMessage(null);

        if (okButton != null)  // it is initially null
            okButton.setEnabled(!hasError);

        insideValidation = false;
    }

    protected IProjectDescription getComboSelection() {
        IStructuredSelection selection = (IStructuredSelection)combo.getSelection();
        IProjectDescription description = (IProjectDescription) selection.getFirstElement();
        return description;
    }

    @Override
    protected void okPressed() {
        // save dialog state into variables, so that client can retrieve them after the dialog was disposed
        descriptionResult = getComboSelection();
        super.okPressed();
    }

    public IProjectDescription getResult() {
        return descriptionResult;
    }
}

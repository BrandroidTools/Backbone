/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.explorer.commands.shell;

import com.cyanogenmod.explorer.commands.AsyncResultListener;
import com.cyanogenmod.explorer.commands.ChangeCurrentDirExecutable;
import com.cyanogenmod.explorer.commands.ChangeOwnerExecutable;
import com.cyanogenmod.explorer.commands.ChangePermissionsExecutable;
import com.cyanogenmod.explorer.commands.CopyExecutable;
import com.cyanogenmod.explorer.commands.CreateDirExecutable;
import com.cyanogenmod.explorer.commands.CreateFileExecutable;
import com.cyanogenmod.explorer.commands.CurrentDirExecutable;
import com.cyanogenmod.explorer.commands.DeleteDirExecutable;
import com.cyanogenmod.explorer.commands.DeleteFileExecutable;
import com.cyanogenmod.explorer.commands.DiskUsageExecutable;
import com.cyanogenmod.explorer.commands.EchoExecutable;
import com.cyanogenmod.explorer.commands.ExecutableCreator;
import com.cyanogenmod.explorer.commands.FindExecutable;
import com.cyanogenmod.explorer.commands.GroupsExecutable;
import com.cyanogenmod.explorer.commands.IdentityExecutable;
import com.cyanogenmod.explorer.commands.ListExecutable;
import com.cyanogenmod.explorer.commands.ListExecutable.LIST_MODE;
import com.cyanogenmod.explorer.commands.MountExecutable;
import com.cyanogenmod.explorer.commands.MountPointInfoExecutable;
import com.cyanogenmod.explorer.commands.MoveExecutable;
import com.cyanogenmod.explorer.commands.ParentDirExecutable;
import com.cyanogenmod.explorer.commands.ProcessIdExecutable;
import com.cyanogenmod.explorer.commands.QuickFolderSearchExecutable;
import com.cyanogenmod.explorer.commands.ResolveLinkExecutable;
import com.cyanogenmod.explorer.console.CommandNotFoundException;
import com.cyanogenmod.explorer.console.shell.ShellConsole;
import com.cyanogenmod.explorer.model.Group;
import com.cyanogenmod.explorer.model.MountPoint;
import com.cyanogenmod.explorer.model.Permissions;
import com.cyanogenmod.explorer.model.Query;
import com.cyanogenmod.explorer.model.User;

/**
 * A class for create shell {@link "Executable"} objects.
 */
public class ShellExecutableCreator implements ExecutableCreator {

    private final ShellConsole mConsole;

    /**
     * Constructor of <code>ShellExecutableCreator</code>.
     *
     * @param console A shell console that use for create objects
     */
    ShellExecutableCreator(ShellConsole console) {
        super();
        this.mConsole = console;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeCurrentDirExecutable createChangeCurrentDirExecutable(String dir)
            throws CommandNotFoundException {
        try {
            return new ChangeCurrentDirCommand(dir);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("ChangeCurrentDirCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeOwnerExecutable createChangeOwnerExecutable(
            String fso, User newUser, Group newGroup) throws CommandNotFoundException {
        try {
            return new ChangeOwnerCommand(fso, newUser, newGroup);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("ChangeOwnerCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangePermissionsExecutable createChangePermissionsExecutable(
            String fso, Permissions newPermissions) throws CommandNotFoundException {
        try {
            return new ChangePermissionsCommand(fso, newPermissions);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("ChangePermissionsCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CopyExecutable createCopyExecutable(String src, String dst)
            throws CommandNotFoundException {
        try {
            return new CopyCommand(src, dst);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("CopyCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CreateDirExecutable createCreateDirectoryExecutable(String dir)
            throws CommandNotFoundException {
        try {
            return new CreateDirCommand(dir);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("CreateDirCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CreateFileExecutable createCreateFileExecutable(String file)
            throws CommandNotFoundException {
        try {
            return new CreateFileCommand(file);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("CreateFileCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CurrentDirExecutable createCurrentDirExecutable() throws CommandNotFoundException {
        try {
            return new CurrentDirCommand();
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("CurrentDirCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeleteDirExecutable createDeleteDirExecutable(String dir)
            throws CommandNotFoundException {
        try {
            return new DeleteDirCommand(dir);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("DeleteDirCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeleteFileExecutable createDeleteFileExecutable(String file)
            throws CommandNotFoundException {
        try {
            return new DeleteFileCommand(file);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("DeleteFileCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiskUsageExecutable createDiskUsageExecutable() throws CommandNotFoundException {
        try {
            return new DiskUsageCommand();
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("DiskUsageCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiskUsageExecutable createDiskUsageExecutable(String dir)
            throws CommandNotFoundException {
        try {
            return new DiskUsageCommand(dir);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("DiskUsageCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EchoExecutable createEchoExecutable(String msg) throws CommandNotFoundException {
        try {
            return new EchoCommand(msg);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("EchoCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FindExecutable createFindExecutable(
            String directory, Query query, AsyncResultListener asyncResultListener)
            throws CommandNotFoundException {
        try {
            return new FindCommand(directory, query, asyncResultListener);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("FindCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupsExecutable createGroupsExecutable() throws CommandNotFoundException {
        try {
            return new GroupsCommand();
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("GroupsCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdentityExecutable createIdentityExecutable() throws CommandNotFoundException {
        try {
            return new IdentityCommand();
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("IdentityCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListExecutable createListExecutable(String src, LIST_MODE mode)
            throws CommandNotFoundException {
        try {
            return new ListCommand(src, mode, this.mConsole);
        } catch (Throwable throwEx) {
            throw new CommandNotFoundException("ListCommand", throwEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountExecutable createMountExecutable(MountPoint mp, boolean rw)
            throws CommandNotFoundException {
        try {
            return new MountCommand(mp, rw);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("MountCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountPointInfoExecutable createMountPointInfoExecutable()
            throws CommandNotFoundException {
        try {
            return new MountPointInfoCommand();
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("MountPointInfoCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MoveExecutable createMoveExecutable(String src, String dst)
            throws CommandNotFoundException {
        try {
            return new MoveCommand(src, dst);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("MoveCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParentDirExecutable createParentDirExecutable(String fso)
            throws CommandNotFoundException {
        try {
            return new ParentDirCommand(fso);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("ParentDirCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessIdExecutable createProcessIdExecutable(String processName)
            throws CommandNotFoundException {
        try {
            return new ProcessIdCommand(processName);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("ProcessIdCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuickFolderSearchExecutable createQuickFolderSearchExecutable(String regexp)
            throws CommandNotFoundException {
        try {
            return new QuickFolderSearchCommand(regexp);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("QuickFolderSearchCommand", icdEx); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResolveLinkExecutable createResolveLinkExecutable(String fso)
            throws CommandNotFoundException {
        try {
            return new ResolveLinkCommand(fso);
        } catch (InvalidCommandDefinitionException icdEx) {
            throw new CommandNotFoundException("ResolveLinkCommand", icdEx); //$NON-NLS-1$
        }
    }

}
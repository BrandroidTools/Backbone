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

package com.cyanogenmod.filemanager.console.java;

import android.content.Context;
import android.util.Log;
import com.cyanogenmod.filemanager.commands.ChangeCurrentDirExecutable;
import com.cyanogenmod.filemanager.commands.Executable;
import com.cyanogenmod.filemanager.commands.ExecutableFactory;
import com.cyanogenmod.filemanager.commands.SIGNAL;
import com.cyanogenmod.filemanager.commands.java.JavaExecutableFactory;
import com.cyanogenmod.filemanager.commands.java.Program;
import com.cyanogenmod.filemanager.console.*;
import com.cyanogenmod.filemanager.model.FileSystemStorageVolume;
import com.cyanogenmod.filemanager.model.Identity;
import com.cyanogenmod.filemanager.util.StorageHelper;

/**
 * An implementation of a {@link Console} based on a java implementation.<br/>
 * <br/>
 * This console is a non-privileged console an many of the functionality is not implemented
 * because can't be obtain from java api.
 */
public final class JavaConsole extends Console {

    private static final String TAG = "JavaConsole"; //$NON-NLS-1$

    private boolean mActive;
    private String mCurrentDir;

    private final Context mCtx;
    private final int mBufferSize;

    /**
     * Constructor of <code>JavaConsole</code>
     *
     * @param ctx The current context
     * @param initialDir The initial directory
     * @param bufferSize The buffer size
     */
    public JavaConsole(Context ctx, String initialDir, int bufferSize) {
        super();
        this.mCtx = ctx;
        this.mBufferSize = bufferSize;
        this.mCurrentDir = initialDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void alloc() throws ConsoleAllocException {
        try {
            if (isTrace()) {
                Log.v(TAG, "Allocating Java console"); //$NON-NLS-1$
            }

            //Retrieve the current directory from the first storage volume
            FileSystemStorageVolume[] vols = StorageHelper.getStorageVolumes(this.mCtx);
            if (vols == null || vols.length == 0) {
                throw new ConsoleAllocException("Can't stat any directory"); //$NON-NLS-1$
            }

            // Test to change to current directory
            ChangeCurrentDirExecutable currentDirCmd =
                    getExecutableFactory().
                        newCreator().createChangeCurrentDirExecutable(this.mCurrentDir);
            execute(currentDirCmd);

            // Tested. Is not active
            this.mCurrentDir = vols[0].getPath();
            this.mActive = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to allocate Java console", e); //$NON-NLS-1$
            throw new ConsoleAllocException("failed to build console", e); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dealloc() {
        if (isTrace()) {
            Log.v(TAG, "Deallocating Java console"); //$NON-NLS-1$
        }
        this.mActive = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void realloc() throws ConsoleAllocException {
        dealloc();
        alloc();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutableFactory getExecutableFactory() {
        return new JavaExecutableFactory(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity getIdentity() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPrivileged() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return this.mActive;
    }

    /**
     * Method that returns the current directory of the console
     *
     * @return String The current directory
     */
    public String getCurrentDir() {
        return this.mCurrentDir;
    }

    /**
     * Method that sets the current directory of the console
     *
     * @param currentDir The current directory
     */
    public void setCurrentDir(String currentDir) {
        this.mCurrentDir = currentDir;
    }

    /**
     * Method that returns the current context
     *
     * @return Context The current context
     */
    public Context getCtx() {
        return this.mCtx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void execute(Executable executable) throws ConsoleAllocException,
                                InsufficientPermissionsException, NoSuchFileOrDirectory,
                                OperationTimeoutException, ExecutionException,
                                CommandNotFoundException, ReadOnlyFilesystemException {
        // Check that the program is a java program
        try {
            Program p = (Program)executable;
            p.isTrace();
        } catch (Throwable e) {
            Log.e(TAG, String.format("Failed to resolve program: %s", //$NON-NLS-1$
                    executable.getClass().toString()), e);
            throw new CommandNotFoundException("executable is not a program", e); //$NON-NLS-1$
        }

        //Auditing program execution
        if (isTrace()) {
            Log.v(TAG, String.format("Executing program: %s", //$NON-NLS-1$
                    executable.getClass().toString()));
        }

        // Execute the program
        final Program program = (Program)executable;
        program.setTrace(isTrace());
        program.setBufferSize(this.mBufferSize);
        if (program.isAsynchronous()) {
            // Execute in a thread
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        program.execute();
                    } catch (Exception e) {
                        // Program must use onException to communicate exceptions
                        Log.v(TAG,
                                String.format("Async execute failed program: %s", //$NON-NLS-1$
                                program.getClass().toString()));
                    }
                }
            };
            t.start();

        } else {
            // Synchronous execution
            program.execute();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCancel() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onSendSignal(SIGNAL signal) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onEnd() {
        return false;
    }

}
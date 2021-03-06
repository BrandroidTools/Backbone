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

package com.brandroidtools.filemanager.ui.policy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.Spanned;

import com.brandroidtools.filemanager.R;
import com.brandroidtools.filemanager.console.NoSuchFileOrDirectory;
import com.brandroidtools.filemanager.console.RelaunchableException;
import com.brandroidtools.filemanager.listeners.OnCopyMoveListener;
import com.brandroidtools.filemanager.listeners.OnRequestRefreshListener;
import com.brandroidtools.filemanager.listeners.OnSelectionListener;
import com.brandroidtools.filemanager.model.FileSystemObject;
import com.brandroidtools.filemanager.util.CommandHelper;
import com.brandroidtools.filemanager.util.DialogHelper;
import com.brandroidtools.filemanager.util.ExceptionUtil;
import com.brandroidtools.filemanager.util.ExceptionUtil.OnRelaunchCommandResult;
import com.brandroidtools.filemanager.util.FileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class with the convenience methods for resolve copy/move related actions
 */
public final class CopyMoveActionPolicy extends ActionsPolicy {

    /**
     * @hide
     */
    public enum COPY_MOVE_OPERATION {
        COPY,
        MOVE,
        RENAME,
        CREATE_COPY,
    }


    /**
     * A class that holds a relationship between a source {@link File} and
     * his destination {@link File}
     */
    public static class LinkedResource implements Comparable<LinkedResource> {
        final File mSrc;
        final File mDst;

        /**
         * Constructor of <code>LinkedResource</code>
         *
         * @param src The source file system object
         * @param dst The destination file system object
         */
        public LinkedResource(File src, File dst) {
            super();
            this.mSrc = src;
            this.mDst = dst;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(LinkedResource another) {
            return this.mSrc.compareTo(another.mSrc);
        }
    }

    /**
     * Method that remove an existing file system object.
     *
     * @param ctx The current context
     * @param fso The file system object
     * @param newName The new name of the object
     * @param onSelectionListener The listener for obtain selection information (required)
     * @param onRequestRefreshListener The listener for request a refresh (optional)
     */
    public static void renameFileSystemObject(
            final Context ctx,
            final FileSystemObject fso,
            final String newName,
            final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener) {

        // Create the destination filename
        File dst = new File(fso.getParent(), newName);
        File src = new File(fso.getFullPath());

        // Create arguments
        LinkedResource linkRes = new LinkedResource(src, dst);
        List<LinkedResource> files = new ArrayList<LinkedResource>(1);
        files.add(linkRes);

        // Internal copy
        copyOrMoveFileSystemObjects(
                ctx,
                COPY_MOVE_OPERATION.RENAME,
                files,
                onSelectionListener,
                onRequestRefreshListener);
    }

    /**
     * Method that marks a selection of file system object for copy-type paste.
     *
     * @param selection The list of files to be marked for copying (required)
     * @param onCopyMoveListener The listener for request a refresh (optional)
     */
    public static void createCopyFileSystemObject(
            final List<FileSystemObject> selection,
            final OnCopyMoveListener onCopyMoveListener) {

        onCopyMoveListener.onMarkFilesForPaste(
                selection,
                COPY_MOVE_OPERATION.COPY);
    }


    /**
     * Method that marks a selection of file system object for move-type paste.
     *
     * @param selection The list of files to be marked for copying (required)
     * @param onCopyMoveListener The listener for request a refresh (optional)
     */
    public static void createMoveFileSystemObject(
            final List<FileSystemObject> selection,
            final OnCopyMoveListener onCopyMoveListener) {

        onCopyMoveListener.onMarkFilesForPaste(
                selection,
                COPY_MOVE_OPERATION.MOVE);
    }

    /**
     * Method that triggers the stored move or copy operation.
     *
     * @param ctx The current context
     * @param files The list of files to copy
     * @param operationMode the operation mode (COPY/MOVE) of the paste operation
     * @param onSelectionListener The listener for obtain selection information (required)
     * @param onRequestRefreshListener The listener for request a refresh (optional)
     */
    public static void triggerCopyMoveFileSystemObjects(
            final Context ctx,
            final List<FileSystemObject> files,
            final COPY_MOVE_OPERATION operationMode,
            final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener,
            final OnCopyMoveListener onCopyMoveListener) {
        // Internal copy
        copyOrMoveFileSystemObjects(
                ctx,
                operationMode,
                createLinkedResource(files, onCopyMoveListener.onRequestDestinationDir()),
                onSelectionListener,
                onRequestRefreshListener);
    }

    /**
     * Method that copy an existing file system object.
     *
     * @param ctx The current context
     * @param operation Indicates the operation to do
     * @param files The list of source/destination files to copy
     * @param onSelectionListener The listener for obtain selection information (required)
     * @param onRequestRefreshListener The listener for request a refresh (optional)
     */
    private static void copyOrMoveFileSystemObjects(
            final Context ctx,
            final COPY_MOVE_OPERATION operation,
            final List<LinkedResource> files,
            final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener) {

        // Some previous checks prior to execute
        // 1.- Listener couldn't be null
        if (onSelectionListener == null) {
            AlertDialog dialog =
                    DialogHelper.createErrorDialog(ctx,
                            R.string.error_title,
                            R.string.msgs_illegal_argument);
            DialogHelper.delegateDialogShow(ctx, dialog);
            return;
        }
        // 2.- All the destination files must have the same parent and it must be currentDirectory,
        // and not be null
        final String currentDirectory = onSelectionListener.onRequestCurrentDir();
        int cc = files.size();
        for (int i = 0; i < cc; i++) {
            LinkedResource linkedRes = files.get(i);
            if (linkedRes.mSrc == null || linkedRes.mDst == null) {
                AlertDialog dialog =
                        DialogHelper.createErrorDialog(ctx,
                                R.string.error_title,
                                R.string.msgs_illegal_argument);
                DialogHelper.delegateDialogShow(ctx, dialog);
                return;
            }
            if (linkedRes.mDst.getParent() == null ||
                linkedRes.mDst.getParent().compareTo(currentDirectory) != 0) {
                AlertDialog dialog =
                        DialogHelper.createErrorDialog(ctx,
                                R.string.error_title,
                                R.string.msgs_illegal_argument);
                DialogHelper.delegateDialogShow(ctx, dialog);
                return;
            }
        }
        // 3.- Check the operation consistency
        if (operation.compareTo(COPY_MOVE_OPERATION.MOVE) == 0) {
            if (!checkMoveConsistency(ctx, files, currentDirectory)) {
                return;
            }
        }

        // The callable interface
        final BackgroundCallable callable = new BackgroundCallable() {
            // The current items
            private int mCurrent = 0;
            final Context mCtx = ctx;
            final COPY_MOVE_OPERATION mOperation = operation;
            final List<LinkedResource> mFiles = files;
            final OnRequestRefreshListener mOnRequestRefreshListener = onRequestRefreshListener;

            final Object mSync = new Object();
            Throwable mCause;

            @Override
            public int getDialogTitle() {
                return this.mOperation.compareTo(COPY_MOVE_OPERATION.MOVE) == 0 ||
                       this.mOperation.compareTo(COPY_MOVE_OPERATION.RENAME) == 0 ?
                        R.string.waiting_dialog_moving_title :
                        R.string.waiting_dialog_copying_title;
            }
            @Override
            public int getDialogIcon() {
                return 0;
            }
            @Override
            public boolean isDialogCancellable() {
                return false;
            }

            @Override
            public Spanned requestProgress() {
                File src = this.mFiles.get(this.mCurrent).mSrc;
                File dst = this.mFiles.get(this.mCurrent).mDst;

                // Return the current operation
                String progress =
                      this.mCtx.getResources().
                          getString(
                              this.mOperation.compareTo(COPY_MOVE_OPERATION.MOVE) == 0 ||
                              this.mOperation.compareTo(COPY_MOVE_OPERATION.RENAME) == 0 ?
                                   R.string.waiting_dialog_moving_msg :
                                   R.string.waiting_dialog_copying_msg,
                              src.getAbsolutePath(),
                              dst.getAbsolutePath());
                return Html.fromHtml(progress);
            }

            @Override
            public void onSuccess() {
                //Operation complete. Refresh
                if (this.mOnRequestRefreshListener != null) {
                  // The reference is not the same, so refresh the complete navigation view
                  this.mOnRequestRefreshListener.onRequestRefresh(null, true);
                }
                ActionsPolicy.showOperationSuccessMsg(ctx);
            }

            @Override
            public void doInBackground(Object... params) throws Throwable {
                this.mCause = null;

                // This method expect to receive
                // 1.- BackgroundAsyncTask
                BackgroundAsyncTask task = (BackgroundAsyncTask)params[0];

                int cc2 = this.mFiles.size();
                for (int i = 0; i < cc2; i++) {
                    File src = this.mFiles.get(i).mSrc;
                    File dst = this.mFiles.get(i).mDst;

                    doOperation(this.mCtx, src, dst, this.mOperation);

                    // Next file
                    this.mCurrent++;
                    if (this.mCurrent < this.mFiles.size()) {
                        task.onRequestProgress();
                    }
                }
            }

            /**
             * Method that copy or move the file to another location
             *
             * @param ctx The current context
             * @param src The source file
             * @param dst The destination file
             * @param operation Indicates the operation to do
             */
            @SuppressWarnings("hiding")
            private void doOperation(
                    Context ctx, File src, File dst, COPY_MOVE_OPERATION operation)
                    throws Throwable {
                // If the source is the same as destiny then don't do the operation
                if (src.compareTo(dst) == 0) return;

                try {
                    // Copy or move?
                    if (operation.compareTo(COPY_MOVE_OPERATION.MOVE) == 0 ||
                            operation.compareTo(COPY_MOVE_OPERATION.RENAME) == 0) {
                        CommandHelper.move(
                                ctx,
                                src.getAbsolutePath(),
                                dst.getAbsolutePath(),
                                null);
                    } else {
                        CommandHelper.copy(
                                ctx,
                                src.getAbsolutePath(),
                                dst.getAbsolutePath(),
                                null);
                    }
                } catch (Exception e) {
                    // Need to be relaunched?
                    if (e instanceof RelaunchableException) {
                        OnRelaunchCommandResult rl = new OnRelaunchCommandResult() {
                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            public void onSuccess() {
                                synchronized (mSync) {
                                    mSync.notify();
                                }
                            }

                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            public void onFailed(Throwable cause) {
                                mCause = cause;
                                synchronized (mSync) {
                                    mSync.notify();
                                }
                            }
                            @Override
                            @SuppressWarnings("unqualified-field-access")
                            public void onCancelled() {
                                synchronized (mSync) {
                                    mSync.notify();
                                }
                            }
                        };

                        // Translate the exception (and wait for the result)
                        ExceptionUtil.translateException(ctx, e, false, true, rl);
                        synchronized (this.mSync) {
                            this.mSync.wait();
                        }

                        // Persist the exception?
                        if (this.mCause != null) {
                            // The exception must be elevated
                            throw this.mCause;
                        }

                    } else {
                        // The exception must be elevated
                        throw e;
                    }
                }

                // Check that the operation was completed retrieving the fso modified
                FileSystemObject fso =
                        CommandHelper.getFileInfo(ctx, dst.getAbsolutePath(), false, null);
                if (fso == null) {
                    throw new NoSuchFileOrDirectory(dst.getAbsolutePath());
                }
            }
        };
        final BackgroundAsyncTask task = new BackgroundAsyncTask(ctx, callable);

        // Prior to execute, we need to check if some of the files will be overwritten
        List<FileSystemObject> curFiles = onSelectionListener.onRequestCurrentItems();
        if (curFiles != null) {
            // Is necessary to ask the user?
            if (isOverwriteNeeded(files, curFiles)) {
                //Show a dialog asking the user for overwrite the files
                AlertDialog dialog =
                        DialogHelper.createTwoButtonsQuestionDialog(
                                ctx,
                                android.R.string.cancel,
                                R.string.overwrite,
                                R.string.confirm_overwrite,
                                ctx.getString(R.string.msgs_overwrite_files),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface alertDialog, int which) {
                                        // NEGATIVE (overwrite)  POSITIVE (cancel)
                                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                                            // Execute background task
                                            task.execute(task);
                                        }
                                    }
                               });
                DialogHelper.delegateDialogShow(ctx, dialog);
                return;
            }
        }

        // Execute background task
        task.execute(task);
    }

    /**
     * Method that check if is needed to prompt the user for overwrite prior to do
     * the operation.
     *
     * @param files The list of source/destination files.
     * @param currentFiles The list of the current files in the destination directory.
     * @return boolean If is needed to prompt the user for overwrite
     */
    private static boolean isOverwriteNeeded(
            List<LinkedResource> files, List<FileSystemObject> currentFiles) {
        boolean askUser = false;
        int cc = currentFiles.size();
        for (int i = 0; i < cc; i++) {
            int cc2 = files.size();
            for (int j = 0; j < cc2; j++) {
                FileSystemObject dst1 =  currentFiles.get(i);
                File dst2 = files.get(j).mDst;

                // The file exists in the destination directory
                if (dst1.getFullPath().compareTo(dst2.getAbsolutePath()) == 0) {
                    askUser = true;
                    break;
                }
            }
            if (askUser) break;
        }
        return askUser;
    }


    /**
     * Method that check the consistency of move operations.<br/>
     * <br/>
     * The method checks the following rules:<br/>
     * <ul>
     * <li>Any of the files of the move operation can not include the
     * current directory.</li>
     * <li>Any of the files of the move operation can not include the
     * current directory.</li>
     * </ul>
     *
     * @param ctx The current context
     * @param files The list of source/destination files
     * @param currentDirectory The current directory
     * @return boolean If the consistency is validate successfully
     */
    private static boolean checkMoveConsistency(
            Context ctx, List<LinkedResource> files, String currentDirectory) {
        int cc = files.size();
        for (int i = 0; i < cc; i++) {
            LinkedResource linkRes = files.get(i);
            String src = linkRes.mSrc.getAbsolutePath();
            String dst = linkRes.mDst.getAbsolutePath();

            // 1.- Current directory can't be moved
            if (currentDirectory != null && currentDirectory.startsWith(src)) {
                // Operation not allowed
                AlertDialog dialog =
                        DialogHelper.createErrorDialog(
                                ctx,
                                R.string.error_title,
                                R.string.msgs_unresolved_inconsistencies);
                DialogHelper.delegateDialogShow(ctx, dialog);
                return false;
            }

            // 2.- Destination can't be a child of source
            if (dst.startsWith(src)) {
                // Operation not allowed
                AlertDialog dialog =
                        DialogHelper.createErrorDialog(
                                ctx,
                                R.string.error_title,
                                R.string.msgs_operation_not_allowed_in_current_directory);
                DialogHelper.delegateDialogShow(ctx, dialog);
                return false;
            }
        }
        return true;
    }

    /**
     * Method that creates a {@link com.brandroidtools.filemanager.ui.policy.CopyMoveActionPolicy.LinkedResource} for the list of object to the
     * destination directory
     *
     * @param items The list of the source items
     * @param destinationDirectory The destination directory
     */
    private static List<CopyMoveActionPolicy.LinkedResource> createLinkedResource(
            List<FileSystemObject> items, String destinationDirectory) {

        List<CopyMoveActionPolicy.LinkedResource> resources =
                new ArrayList<CopyMoveActionPolicy.LinkedResource>(items.size());
        int cc = items.size();
        for (int i = 0; i < cc; i++) {
            FileSystemObject fso = items.get(i);
            File src = new File(fso.getFullPath());
            File dst = new File(destinationDirectory, fso.getName());
            resources.add(new CopyMoveActionPolicy.LinkedResource(src, dst));
        }
        return resources;
    }
}
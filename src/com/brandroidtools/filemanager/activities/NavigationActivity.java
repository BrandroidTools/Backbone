/*
 * Copyright (C) 2012 The CyanogenMod Project
 * Copyright (C) 2013 BrandroidTools
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

package com.brandroidtools.filemanager.activities;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import com.brandroidtools.filemanager.FileManagerApplication;
import com.brandroidtools.filemanager.R;
import com.brandroidtools.filemanager.activities.preferences.SettingsPreferences;
import com.brandroidtools.filemanager.adapters.BookmarksAdapter;
import com.brandroidtools.filemanager.adapters.MenuSettingsAdapter;
import com.brandroidtools.filemanager.adapters.NavigationFragmentPagerAdapter;
import com.brandroidtools.filemanager.bus.BusProvider;
import com.brandroidtools.filemanager.bus.events.BookmarkDeleteEvent;
import com.brandroidtools.filemanager.bus.events.BookmarkOpenEvent;
import com.brandroidtools.filemanager.bus.events.BookmarkRefreshEvent;
import com.brandroidtools.filemanager.commands.shell.InvalidCommandDefinitionException;
import com.brandroidtools.filemanager.console.*;
import com.brandroidtools.filemanager.fragments.BookmarksFragment;
import com.brandroidtools.filemanager.fragments.BookmarksFragment.OnBookmarkSelectedListener;
import com.brandroidtools.filemanager.fragments.HistoryFragment;
import com.brandroidtools.filemanager.fragments.NavigationFragment;
import com.brandroidtools.filemanager.fragments.NavigationFragment.OnNavigationRequestMenuListener;
import com.brandroidtools.filemanager.listeners.OnCopyMoveListener;
import com.brandroidtools.filemanager.listeners.OnHistoryListener;
import com.brandroidtools.filemanager.listeners.OnRequestRefreshListener;
import com.brandroidtools.filemanager.model.*;
import com.brandroidtools.filemanager.parcelables.HistoryNavigable;
import com.brandroidtools.filemanager.parcelables.NavigationViewInfoParcelable;
import com.brandroidtools.filemanager.parcelables.SearchInfoParcelable;
import com.brandroidtools.filemanager.preferences.AccessMode;
import com.brandroidtools.filemanager.preferences.Bookmarks;
import com.brandroidtools.filemanager.preferences.FileManagerSettings;
import com.brandroidtools.filemanager.preferences.NavigationLayoutMode;
import com.brandroidtools.filemanager.preferences.ObjectIdentifier;
import com.brandroidtools.filemanager.preferences.Preferences;
import com.brandroidtools.filemanager.ui.ThemeManager;
import com.brandroidtools.filemanager.ui.ThemeManager.Theme;
import com.brandroidtools.filemanager.ui.dialogs.FilesystemInfoDialog;
import com.brandroidtools.filemanager.ui.dialogs.FilesystemInfoDialog.OnMountListener;
import com.brandroidtools.filemanager.ui.dialogs.InputNameDialog;
import com.brandroidtools.filemanager.ui.policy.BookmarksActionPolicy;
import com.brandroidtools.filemanager.ui.policy.CopyMoveActionPolicy;
import com.brandroidtools.filemanager.ui.policy.CopyMoveActionPolicy.COPY_MOVE_OPERATION;
import com.brandroidtools.filemanager.ui.policy.InfoActionPolicy;
import com.brandroidtools.filemanager.ui.policy.NewActionPolicy;
import com.brandroidtools.filemanager.ui.widgets.*;
import com.brandroidtools.filemanager.util.*;
import com.brandroidtools.filemanager.util.ExceptionUtil.OnRelaunchCommandResult;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The main navigation activity. This activity is the center of the application.
 * From this the user can navigate, search, make actions.<br/>
 * This activity is singleTop, so when it is displayed no other activities exists in
 * the stack.<br/>
 * This cause an issue with the saved instance of this class, because if another activity
 * is displayed, and the process is killed, NavigationActivity is started and the saved
 * instance gets corrupted.<br/>
 * For this reason the methods {link {@link android.app.Activity#onSaveInstanceState(android.os.Bundle)} and
 * {@link android.app.Activity#onRestoreInstanceState(android.os.Bundle)} are not implemented, and every time
 * the app is killed, is restarted from his initial state.
 */
public class NavigationActivity extends Activity
    implements OnRequestRefreshListener, OnCopyMoveListener,
        OnNavigationRequestMenuListener, OnPageChangeListener,
        BreadcrumbListener {

    private static final String TAG = "NavigationActivity"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    /**
     * Intent code for request a history selection.
     */
    public static final int INTENT_REQUEST_HISTORY = 20001;

    /**
     * Intent code for request a search.
     */
    public static final int INTENT_REQUEST_SEARCH = 30001;


    /**
     * Constant for extra information about selected bookmark.
     */
    public static final String EXTRA_BOOKMARK_SELECTION =
            "extra_bookmark_selection"; //$NON-NLS-1$

    /**
     * Constant for extra information about selected history entry.
     */
    public static final String EXTRA_HISTORY_ENTRY_SELECTION =
            "extra_history_entry_selection"; //$NON-NLS-1$

    /**
     * Constant for extra information about clear selection action.
     */
    public static final String EXTRA_HISTORY_CLEAR =
            "extra_history_clear_history"; //$NON-NLS-1$

    /**
     * Constant for extra information about selected search entry.
     */
    public static final String EXTRA_SEARCH_ENTRY_SELECTION =
            "extra_search_entry_selection"; //$NON-NLS-1$

    /**
     * Constant for extra information about last search data.
     */
    public static final String EXTRA_SEARCH_LAST_SEARCH_DATA =
            "extra_search_last_search_data"; //$NON-NLS-1$

    /**
     * Constant for extra information for request a navigation to the passed path.
     */
    public static final String EXTRA_NAVIGATE_TO =
            "extra_navigate_to"; //$NON-NLS-1$

    // The timeout needed to reset the exit status for back button
    // After this time user need to tap 2 times the back button to
    // exit, and the toast is shown again after the first tap.
    private static final int RELEASE_EXIT_CHECK_TIMEOUT = 3500;

    // The flag indicating whether or not the application is just starting up.
    // Used in initializing the action bar's breadcrumb once the fragments have
    // finished loading.
    private boolean mFirstRun = true;

    private final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().compareTo(FileManagerSettings.INTENT_SETTING_CHANGED) == 0) {
                    // The settings has changed
                    String key = intent.getStringExtra(FileManagerSettings.EXTRA_SETTING_CHANGED_KEY);
                    if (key != null) {
                        // Disk usage warning level
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_DISK_USAGE_WARNING_LEVEL.getId()) == 0) {

                            // Set the free disk space warning level of the breadcrumb widget
                            Breadcrumb breadcrumb = getCurrentNavigationFragment().getBreadcrumb();
                            String fds = Preferences.getSharedPreferences().getString(
                                    FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getId(),
                                    (String)FileManagerSettings.
                                        SETTINGS_DISK_USAGE_WARNING_LEVEL.getDefaultValue());
                            breadcrumb.setFreeDiskSpaceWarningLevel(Integer.parseInt(fds));
                            breadcrumb.updateMountPointInfo();
                            return;
                        }

                        // Case sensitive sort
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_CASE_SENSITIVE_SORT.getId()) == 0) {
                            getCurrentNavigationFragment().refresh();
                            return;
                        }

                        // Use flinger
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_USE_FLINGER.getId()) == 0) {
                            boolean useFlinger =
                                    Preferences.getSharedPreferences().getBoolean(
                                            FileManagerSettings.SETTINGS_USE_FLINGER.getId(),
                                                ((Boolean)FileManagerSettings.
                                                        SETTINGS_USE_FLINGER.
                                                            getDefaultValue()).booleanValue());
                            getCurrentNavigationFragment().setUseFlinger(useFlinger);
                            return;
                        }

                        // Access mode
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_ACCESS_MODE.getId()) == 0) {
                            // Is it necessary to create or exit of the ChRooted?
                            boolean chRooted =
                                    FileManagerApplication.
                                        getAccessMode().compareTo(AccessMode.SAFE) == 0;
                            if (chRooted != NavigationActivity.this.mChRooted) {
                                if (chRooted) {
                                    createChRooted();
                                } else {
                                    exitChRooted();
                                }
                            }
                            // Update bookmarks to reflect access mode change
                            BusProvider.getInstance().post(new BookmarkRefreshEvent());
                        }

                        // Filetime format mode
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_FILETIME_FORMAT_MODE.getId()) == 0) {
                            // Refresh the data
                            synchronized (FileHelper.DATETIME_SYNC) {
                                FileHelper.sReloadDateTimeFormats = true;
                                NavigationActivity.this.getCurrentNavigationFragment().refresh();
                            }
                        }
                    }

                } else if (intent.getAction().compareTo(
                        FileManagerSettings.INTENT_FILE_CHANGED) == 0) {
                    // Retrieve the file that was changed
                    String file =
                            intent.getStringExtra(FileManagerSettings.EXTRA_FILE_CHANGED_KEY);
                    try {
                        FileSystemObject fso = CommandHelper.getFileInfo(context, file, null);
                        if (fso != null) {
                            getCurrentNavigationFragment().refresh(fso);
                        }
                    } catch (Exception e) {
                        ExceptionUtil.translateException(context, e, true, false);
                    }

                } else if (intent.getAction().compareTo(
                        FileManagerSettings.INTENT_THEME_CHANGED) == 0) {
                    applyTheme();

                } else if (intent.getAction().compareTo(Intent.ACTION_TIME_CHANGED) == 0 ||
                           intent.getAction().compareTo(Intent.ACTION_DATE_CHANGED) == 0 ||
                           intent.getAction().compareTo(Intent.ACTION_TIMEZONE_CHANGED) == 0) {
                    // Refresh the data
                    synchronized (FileHelper.DATETIME_SYNC) {
                        FileHelper.sReloadDateTimeFormats = true;
                        NavigationActivity.this.getCurrentNavigationFragment().refresh();
                    }
                }
            }
        }
    };

    /**
     * @hide
     */
    private ActionBar mActionBar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private BookmarksListView mDrawerList;
    private View mTitleLayout;
    private NavigationCustomTitleView mTitle;
    private Breadcrumb mBreadcrumb;

    public NavigationFragmentPagerAdapter mPagerAdapter;
    public ViewPager mViewPager;

    private boolean mExitFlag = false;
    private long mExitBackTimeout = -1;

    /**
     * @hide
     */
    boolean mChRooted;

    /**
     * @hide
     */
    Handler mHandler;

    /**
     * @hide
     */
    private List<FileSystemObject> mFilesForPaste;
    /**
     * @hide
     */
    private COPY_MOVE_OPERATION mPasteOperationType;

    /**
     * {@inheritDoc}
     */
    @TargetApi(16)
	@Override
    protected void onCreate(Bundle state) {

        if (DEBUG) {
            Log.d(TAG, "NavigationActivity.onCreate"); //$NON-NLS-1$
        }

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileManagerSettings.INTENT_SETTING_CHANGED);
        filter.addAction(FileManagerSettings.INTENT_FILE_CHANGED);
        filter.addAction(FileManagerSettings.INTENT_THEME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(this.mNotificationReceiver, filter);

        // Initialize NFC adapter
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null && Build.VERSION.SDK_INT > 16) {
            mNfcAdapter.setBeamPushUrisCallback(new NfcAdapter.CreateBeamUrisCallback() {
                @Override
                public Uri[] createBeamUris(NfcEvent event) {
                    List<FileSystemObject> selectedFiles =
                            getCurrentNavigationFragment().getSelectedFiles();
                    if (selectedFiles.size() > 0) {
                        List<Uri> fileUri = new ArrayList<Uri>();
                        for (FileSystemObject f : selectedFiles) {
                            //Beam ignores folders and system files
                            if (!FileHelper.isDirectory(f) && !FileHelper.isSystemFile(f)) {
                                fileUri.add(Uri.fromFile(new File(f.getFullPath())));
                            }
                        }
                        if (fileUri.size() > 0) {
                            return fileUri.toArray(new Uri[fileUri.size()]);
                        }
                    }
                    return null;
                }
            }, this);
        }

        setContentView(R.layout.navigation);
        
        //Initialize activity console
        init();
        
        //Initialize viewPager
        initViewPager();

        //Initialize action bar
        mActionBar = getActionBar();
        initTitleActionBar();

        // Create ActionBar drawer toggle drawable
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                // Reload the fragments current dir into the breadcrumb
                if (mBreadcrumb != null) {
                    mBreadcrumb.changeBreadcrumbPath(getCurrentNavigationFragment().getCurrentDir(), mChRooted);
                }
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(R.string.bookmarks);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerList = (BookmarksListView) findViewById(R.id.left_drawer);

        // Apply the theme
        applyTheme();

        // Show welcome message
        showWelcomeMsg();

        //Save state
        super.onCreate(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(Intent intent) {
        //Initialize navigation
        NavigationActivity.this.getCurrentNavigationFragment().initNavigation(true, intent);

        //Check the intent action
        checkIntent(intent);
    }
      
    @Override 
    protected void onResume() {
        super.onResume();
        mViewPager.setOnPageChangeListener(this);
        
        // Register ourselves so that we can provide the initial value.
        BusProvider.getInstance().register(this);
        BusProvider.getInstance().register(mDrawerList);

        BusProvider.getInstance().post(new BookmarkRefreshEvent());
    }

    @Override 
    protected void onPause() {
        super.onPause();

        // Always unregister when an object no longer should be on the bus.
        BusProvider.getInstance().unregister(this);
        BusProvider.getInstance().unregister(mDrawerList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "NavigationActivity.onDestroy"); //$NON-NLS-1$
        }

        // Unregister the receiver
        try {
            unregisterReceiver(this.mNotificationReceiver);
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }

        //All destroy. Continue
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Method that returns the current navigation view.
     *
     * @return NavigationFragment The current navigation view
     */
    public NavigationFragment getCurrentNavigationFragment() {
        return mPagerAdapter.getFragment(mViewPager, mViewPager.getCurrentItem());
    }

    /**
     * Method that returns the requested navigation view.
     *
     * @param fragmentNum The fragment to return
     * @return NavigationFragment The requested navigation view
     */
    public NavigationFragment getNavigationFragment(int fragmentNum) {
        return mPagerAdapter.getFragment(mViewPager, fragmentNum);
    }

    /**
     * Method that initializes the activity.
     */
    private void init() {
        this.mChRooted = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0;

        this.mHandler = new Handler();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                //Create the default console (from the preferences)
                try {
                    Console console = ConsoleBuilder.getConsole(NavigationActivity.this);
                    if (console == null) {
                        throw new ConsoleAllocException("console == null"); //$NON-NLS-1$
                    }
                } catch (Throwable ex) {
                    if (!NavigationActivity.this.isChRooted()) {
                        //Show exception and exists
                        Log.e(TAG, getString(R.string.msgs_cant_create_console), ex);
                        // We don't have any console
                        // Show exception and exists
                        DialogHelper.showToast(
                                NavigationActivity.this,
                                R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                        NavigationActivity.this.exit();
                        return;
                    }

                    // We are in a trouble (something is not allowing creating the console)
                    // Ask the user to return to prompt or root access mode mode with a
                    // non-privileged console, prior to make crash the application
                    NavigationActivity.this.askOrExit();
                    return;
                }
            }
        });
    }

    /**
     * Method that displays a welcome message the first time the user
     * access the application
     */
    private void showWelcomeMsg() {
        boolean firstUse = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_FIRST_USE.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_FIRST_USE.getDefaultValue()).booleanValue());

        //Display the welcome message?
        if (firstUse) {
            AlertDialog dialog = DialogHelper.createAlertDialog(
                this, R.drawable.ic_launcher,
                R.string.welcome_title, getString(R.string.welcome_msg), false);
            DialogHelper.delegateDialogShow(this, dialog);

            // Don't display again this dialog
            try {
                Preferences.savePreference(
                        FileManagerSettings.SETTINGS_FIRST_USE, Boolean.FALSE, true);
            } catch (Exception e) {/**NON BLOCK**/}
        }
    }

    /**
     * Method that initializes the ViewPager and NavigationPagerAdapter
     */
    private void initViewPager(){
        mViewPager = (ViewPager)findViewById(R.id.navigation_pager);

        // Plug the ViewPager into the Pager Adapter and set the number of pages
        mPagerAdapter = new NavigationFragmentPagerAdapter(this, getFragmentManager(), 2);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setHorizontalScrollBarEnabled(true);

        // TODO this is a pretty much a hack job for now, and proper state
        // retaining needs to be implemented for tablets
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
    }

    /**
     * Method that initializes the titlebar of the activity.
     */
    private void initTitleActionBar() {
        //Inflate the view and associate breadcrumb
        mTitleLayout = getLayoutInflater().inflate(
                R.layout.navigation_view_customtitle, null, false);
        mTitle = (NavigationCustomTitleView)mTitleLayout.findViewById(R.id.navigation_title_flipper);
        mBreadcrumb = (Breadcrumb)mTitle.findViewById(R.id.breadcrumb_view);

        // Set the free disk space warning level of the breadcrumb widget
        String fds = Preferences.getSharedPreferences().getString(
                FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getId(),
                (String)FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getDefaultValue());
        mBreadcrumb.setFreeDiskSpaceWarningLevel(Integer.parseInt(fds));
        //Configure the action bar options
        mActionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        mActionBar.setCustomView(mTitleLayout);
    }

    /**
     * Method that updates the titlebar of the activity.
     */
    public void updateTitleActionBar() {

        NavigationFragment navigationFragment = getCurrentNavigationFragment();
        mTitle.setOnHistoryListener(navigationFragment);
        navigationFragment.setBreadcrumb(mBreadcrumb);
        navigationFragment.setOnHistoryListener(navigationFragment);
        navigationFragment.setOnNavigationOnRequestMenuListener(this);
        navigationFragment.setCustomTitle(mTitle);
    }

    /**
     * Method that verifies the intent passed to the activity, and checks
     * if a request is made like Search.
     *
     * @param intent The intent to check
     * @hide
     */
    void checkIntent(Intent intent) {
        //Search action
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Intent searchIntent = new Intent(this, SearchActivity.class);
            searchIntent.setAction(Intent.ACTION_SEARCH);
            //- SearchActivity.EXTRA_SEARCH_DIRECTORY
            searchIntent.putExtra(
                    SearchActivity.EXTRA_SEARCH_DIRECTORY,
                    getCurrentNavigationFragment().getCurrentDir());
            //- SearchManager.APP_DATA
            if (intent.getBundleExtra(SearchManager.APP_DATA) != null) {
                Bundle bundle = new Bundle();
                bundle.putAll(intent.getBundleExtra(SearchManager.APP_DATA));
                searchIntent.putExtra(SearchManager.APP_DATA, bundle);
            }
            //-- SearchManager.QUERY
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null) {
                searchIntent.putExtra(SearchManager.QUERY, query);
            }
            //- android.speech.RecognizerIntent.EXTRA_RESULTS
            ArrayList<String> extraResults =
                    intent.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (extraResults != null) {
                searchIntent.putStringArrayListExtra(
                        android.speech.RecognizerIntent.EXTRA_RESULTS, extraResults);
            }
            startActivityForResult(searchIntent, INTENT_REQUEST_SEARCH);
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (checkBackAction()) {
                return true;
            }

            // An exit event has occurred, force the destroy the consoles
            exit();
        }
        return super.onKeyUp(keyCode, event);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.navigation, menu);

        // Calling super after populating the menu is necessary here to ensure
        // that the action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Make paste action visible if there are files available for pasting
        menu.findItem(R.id.mnu_actions_paste_selection).setVisible(this.onAreFilesMarkedForPaste());
        return super.onPrepareOptionsMenu(menu);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Drawer Toggle pass-off
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Action Items
        switch (item.getItemId()) {
            case R.id.mnu_history:
                openHistory();
                break;

            case R.id.mnu_search:
                openSearch();
                break;

            case R.id.mnu_actions_refresh:
                getCurrentNavigationFragment().refresh();
                break;

            //- Create new object
            case R.id.mnu_actions_new_directory:
            case R.id.mnu_actions_new_file:
                showInputNameDialog(item);
                break;

            // Paste selection
            case R.id.mnu_actions_paste_selection:
                if (true) {
                    CopyMoveActionPolicy.triggerCopyMoveFileSystemObjects(
                            NavigationActivity.this,
                            this.onRequestFilesMarkedForPaste(),
                            this.onRequestPasteOperationType(),
                            getCurrentNavigationFragment(),
                            getCurrentNavigationFragment(),
                            NavigationActivity.this);
                }
                break;

            case R.id.mnu_actions_properties_current_folder:
                openPropertiesDialog(getCurrentNavigationFragment().getCurrentDir());
                break;

            //- Add to bookmarks
            case R.id.mnu_actions_add_to_bookmarks:
                try {
                    FileSystemObject bookmarkFso = CommandHelper.getFileInfo(this,
                            getCurrentNavigationFragment().getCurrentDir(),
                            null);
                    BookmarksActionPolicy.addToBookmarks(this, bookmarkFso);
                    BusProvider.getInstance().post(new BookmarkRefreshEvent());
                } catch (Exception e) {
                    ExceptionUtil.translateException(this, e, true, false);
                }
                break;

            case R.id.mnu_settings:
                //Settings
                Intent settings = new Intent(
                        NavigationActivity.this, SettingsPreferences.class);
                startActivity(settings);
                break;

            default:
               return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Method invoked when a custom action item is clicked. This does not handle action items populated by the default
     * action bar menu inflater.  It does handle the custom action buttons from the "Navigation View" as views instead
     * of MenuItems.  The Navigation View is the custom view inserted into the top action bar.
     *
     * @param view The button pushed
     */
    public void onActionBarItemClick(View view) {
        switch (view.getId()) {
            //######################
            //Navigation Custom Title
            //######################
            case R.id.ab_configuration:
                //Show navigation view configuration toolbar
                getCurrentNavigationFragment().getCustomTitle().showConfigurationView();
                break;
            case R.id.ab_close:
                //Hide navigation view configuration toolbar
                getCurrentNavigationFragment().getCustomTitle().hideConfigurationView();
                break;

            //######################
            //Breadcrumb Actions
            //######################
            case R.id.ab_filesystem_info:
                //Show information of the filesystem
                MountPoint mp = getCurrentNavigationFragment().getBreadcrumb().getMountPointInfo();
                DiskUsage du = getCurrentNavigationFragment().getBreadcrumb().getDiskUsageInfo();
                showMountPointInfo(mp, du);
                break;

            //######################
            //Navigation view options
            //######################
            case R.id.ab_sort_mode:
                showSettingsPopUp(view,
                        Arrays.asList(
                                new FileManagerSettings[]{
                                        FileManagerSettings.SETTINGS_SORT_MODE}));
                break;
            case R.id.ab_layout_mode:
                showSettingsPopUp(view,
                        Arrays.asList(
                                new FileManagerSettings[]{
                                        FileManagerSettings.SETTINGS_LAYOUT_MODE}));
                break;
            case R.id.ab_view_options:
                // If we are in ChRooted mode, then don't show non-secure items
                if (this.mChRooted) {
                    showSettingsPopUp(view,
                            Arrays.asList(new FileManagerSettings[]{
                                    FileManagerSettings.SETTINGS_SHOW_DIRS_FIRST}));
                } else {
                    showSettingsPopUp(view,
                            Arrays.asList(new FileManagerSettings[]{
                                    FileManagerSettings.SETTINGS_SHOW_DIRS_FIRST,
                                    FileManagerSettings.SETTINGS_SHOW_HIDDEN,
                                    FileManagerSettings.SETTINGS_SHOW_SYSTEM,
                                    FileManagerSettings.SETTINGS_SHOW_SYMLINKS}));
                }

                break;

            //######################
            //Selection Actions
            //######################
            case R.id.ab_selection_done:
                // Show information of the filesystem
                getCurrentNavigationFragment().onDeselectAll();
                break;

            case R.id.ab_select_all:
                // Select all items in the visible navigation fragment
                getCurrentNavigationFragment().onSelectAllVisibleItems();
                break;

            default:
                break;
        }
    }
    
    @Subscribe
    public void onBookmarkOpenEvent(BookmarkOpenEvent event) {
    	String path = event.path;
    	// Check that the bookmark exists
        try {
            FileSystemObject fso = CommandHelper.getFileInfo(this, path, null);
            if (fso != null) {
            	getCurrentNavigationFragment().open(fso);
                mDrawerLayout.closeDrawers();
            } else {
                // The bookmark not exists, delete the user-defined bookmark
                try {
                	BusProvider.getInstance().post(new BookmarkDeleteEvent(path));
                	Bookmark b = Bookmarks.getBookmark(getContentResolver(), path);
                    Bookmarks.removeBookmark(this, b);
                    BusProvider.getInstance().post(new BookmarkRefreshEvent());
                } catch (Exception ex) {/**NON BLOCK**/}
            }
        } catch (Exception e) {
            // Capture the exception
            ExceptionUtil.translateException(this, e);
            if (e instanceof NoSuchFileOrDirectory || e instanceof FileNotFoundException) {
                // The bookmark not exists, delete the user-defined bookmark
                try {
                	BusProvider.getInstance().post(new BookmarkDeleteEvent(path));
                } catch (Exception ex) {/**NON BLOCK**/}
            }
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            switch (requestCode) {
                case INTENT_REQUEST_HISTORY:
                    if (resultCode == RESULT_OK) {
                        //Change current directory
                        History history =
                                (History)data.getSerializableExtra(EXTRA_HISTORY_ENTRY_SELECTION);
                        navigateToHistory(history);
                    } else if (resultCode == RESULT_CANCELED) {
                        boolean clear = data.getBooleanExtra(EXTRA_HISTORY_CLEAR, false);
                        if (clear) {
                            clearHistory();
                        }
                    }
                    break;

                case INTENT_REQUEST_SEARCH:
                    if (resultCode == RESULT_OK) {
                        //Change directory?
                        FileSystemObject fso =
                                (FileSystemObject)data.
                                    getSerializableExtra(EXTRA_SEARCH_ENTRY_SELECTION);
                        SearchInfoParcelable searchInfo =
                                data.getParcelableExtra(EXTRA_SEARCH_LAST_SEARCH_DATA);
                        if (fso != null) {
                            //Goto to new directory
                            getCurrentNavigationFragment().open(fso, searchInfo);
                        }
                    } else if (resultCode == RESULT_CANCELED) {
                        SearchInfoParcelable searchInfo =
                                data.getParcelableExtra(EXTRA_SEARCH_LAST_SEARCH_DATA);
                        if (searchInfo != null && searchInfo.isSuccessNavigation()) {
                            //Navigate to previous history
                            back();
                        } else {
                            // I don't know is the search view was changed, so try to do a refresh
                            // of the navigation view
                            getCurrentNavigationFragment().refresh(true);
                        }
                    }
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRefresh(Object o, boolean clearSelection) {
        if (o instanceof FileSystemObject) {
            // Refresh only the item
            this.getCurrentNavigationFragment().refresh((FileSystemObject) o);
        } else if (o == null) {
            // Refresh all
            getCurrentNavigationFragment().refresh();
        }
        if (clearSelection) {
            this.getCurrentNavigationFragment().onDeselectAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRemove(Object o, boolean clearSelection) {
        if (o instanceof FileSystemObject) {
            // Remove from view
            this.getCurrentNavigationFragment().removeItem((FileSystemObject) o);

            //Remove from history
            getCurrentNavigationFragment().removeFromHistory((FileSystemObject) o);
        } else {
            onRequestRefresh(null, clearSelection);
        }
        if (clearSelection) {
            this.getCurrentNavigationFragment().onDeselectAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBreadcrumbItemClick(BreadcrumbItem item) {
        getCurrentNavigationFragment().changeCurrentDir(item.getItemPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNavigateTo(Object o) {
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestMenu(NavigationFragment navFragment, FileSystemObject item) {

    }

    /**
     * Method that shows a popup with a menu associated a {@link com.brandroidtools.filemanager.preferences.FileManagerSettings}.
     *
     * @param anchor The action button that was pressed
     * @param settings The array of settings associated with the action button
     */
    private void showSettingsPopUp(View anchor, List<FileManagerSettings> settings) {
        //Create the adapter
        final MenuSettingsAdapter adapter = new MenuSettingsAdapter(this, settings);

        //Create a show the popup menu
        final ListPopupWindow popup = DialogHelper.createListPopupWindow(this, adapter, anchor);
        popup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                FileManagerSettings setting =
                        ((MenuSettingsAdapter)parent.getAdapter()).getSetting(position);
                final int value = ((MenuSettingsAdapter)parent.getAdapter()).getId(position);
                popup.dismiss();
                try {
                    if (setting.compareTo(FileManagerSettings.SETTINGS_LAYOUT_MODE) == 0) {
                        //Need to change the layout
                        getCurrentNavigationFragment().changeViewMode(
                                NavigationLayoutMode.fromId(value));
                    } else {
                        //Save and refresh
                        if (setting.getDefaultValue() instanceof Enum<?>) {
                            //Enumeration
                            Preferences.savePreference(setting, new ObjectIdentifier() {
                                @Override
                                public int getId() {
                                    return value;
                                }
                            }, false);
                        } else {
                            //Boolean
                            boolean newval =
                                    Preferences.getSharedPreferences().
                                        getBoolean(
                                            setting.getId(),
                                            ((Boolean)setting.getDefaultValue()).booleanValue());
                            Preferences.savePreference(setting, Boolean.valueOf(!newval), false);
                        }
                        getCurrentNavigationFragment().refresh();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error applying navigation option", e); //$NON-NLS-1$
                    NavigationActivity.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            DialogHelper.showToast(
                                    NavigationActivity.this,
                                    R.string.msgs_settings_save_failure, Toast.LENGTH_SHORT);
                        }
                    });

                } finally {
                    adapter.dispose();
                    getCurrentNavigationFragment().getCustomTitle().restoreView();
                }

            }
        });
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                adapter.dispose();
            }
        });
        popup.show();
    }

    /**
     * Method that show the information of a filesystem mount point.
     *
     * @param mp The mount point info
     * @param du The disk usage of the mount point
     */
    private void showMountPointInfo(MountPoint mp, DiskUsage du) {
        //Has mount point info?
        if (mp == null) {
            //There is no information
            AlertDialog alert =
                    DialogHelper.createWarningDialog(
                            this,
                            R.string.filesystem_info_warning_title,
                            R.string.filesystem_info_warning_msg);
            DialogHelper.delegateDialogShow(this, alert);
            return;
        }

        //Show a the filesystem info dialog
        FilesystemInfoDialog dialog = new FilesystemInfoDialog(this, mp, du);
        dialog.setOnMountListener(new OnMountListener() {
            @Override
            public void onRemount(MountPoint mountPoint) {
                //Update the statistics of breadcrumb, only if mount point is the same
                Breadcrumb breadcrumb = getCurrentNavigationFragment().getBreadcrumb();
                if (breadcrumb.getMountPointInfo().compareTo(mountPoint) == 0) {
                    breadcrumb.updateMountPointInfo();
                }
            }
        });
        dialog.show();
    }

    /**
     * Method that checks the action that must be realized when the
     * back button is pushed.
     *
     * @return boolean Indicates if the action must be intercepted
     */
    private boolean checkBackAction() {
        // We need a basic structure to check this
        if (getCurrentNavigationFragment() == null) return false;

        //Check if the configuration view is showing. In this case back
        //action must be "close configuration"
        if (getCurrentNavigationFragment().getCustomTitle().isConfigurationViewShowing()) {
            getCurrentNavigationFragment().getCustomTitle().restoreView();
            return true;
        }

        //Do back operation over the navigation history
        boolean flag = this.mExitFlag;

        this.mExitFlag = !back();

        // Retrieve if the exit status timeout has expired
        long now = System.currentTimeMillis();
        boolean timeout = (this.mExitBackTimeout == -1 ||
                            (now - this.mExitBackTimeout) > RELEASE_EXIT_CHECK_TIMEOUT);

        //Check if there no history and if the user was advised in the last back action
        if (this.mExitFlag && (this.mExitFlag != flag || timeout)) {
            //Communicate the user that the next time the application will be closed
            this.mExitBackTimeout = System.currentTimeMillis();
            DialogHelper.showToast(this, R.string.msgs_push_again_to_exit, Toast.LENGTH_SHORT);
            return true;
        }

        //Back action not applied
        return !this.mExitFlag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onSearchRequested() {
        Bundle bundle = new Bundle();
        bundle.putString(
                SearchActivity.EXTRA_SEARCH_DIRECTORY,
                getCurrentNavigationFragment().getCurrentDir());
        startSearch(Preferences.getLastSearch(), true, bundle, false);
        return true;
    }

    /**
     * Method that clears the fragment history.
     */
    private void clearHistory() {
        getCurrentNavigationFragment().mHistory.clear();
        getCurrentNavigationFragment().onCheckHistory();
    }

    /**
     * Method that navigates to the passed history reference.
     *
     * @param history The history reference
     * @return boolean A problem occurs while navigate
     */
    public boolean navigateToHistory(History history) {
        try {
            NavigationFragment currentNavFragment = getCurrentNavigationFragment();
            //Gets the history
            History realHistory = currentNavFragment.mHistory.get(history.getPosition());

            //Navigate to item. Check what kind of history is
            if (realHistory.getItem() instanceof NavigationViewInfoParcelable) {
                //Navigation
                NavigationViewInfoParcelable info =
                        (NavigationViewInfoParcelable)realHistory.getItem();
                // Selected items must not be restored from on history navigation
                info.setSelectedFiles(currentNavFragment.getSelectedFiles());
                currentNavFragment.onRestoreState(info);

            } else if (realHistory.getItem() instanceof SearchInfoParcelable) {
                //Search (open search with the search results)
                SearchInfoParcelable info = (SearchInfoParcelable)realHistory.getItem();
                Intent searchIntent = new Intent(this, SearchActivity.class);
                searchIntent.setAction(SearchActivity.ACTION_RESTORE);
                searchIntent.putExtra(SearchActivity.EXTRA_SEARCH_RESTORE, (Parcelable)info);
                startActivityForResult(searchIntent, INTENT_REQUEST_SEARCH);
            } else {
                //The type is unknown
                throw new IllegalArgumentException("Unknown history type"); //$NON-NLS-1$
            }

            //Remove the old history
            int cc = realHistory.getPosition();
            for (int i = currentNavFragment.mHistory.size() - 1; i >= cc; i--) {
                currentNavFragment.mHistory.remove(i);
            }

            //Navigate
            return true;

        } catch (Throwable ex) {
            if (history != null) {
                Log.e(TAG,
                        String.format("Failed to navigate to history %d: %s", //$NON-NLS-1$
                                Integer.valueOf(history.getPosition()),
                                history.getItem().getTitle()), ex);
            } else {
                Log.e(TAG,
                        String.format("Failed to navigate to history: null", ex)); //$NON-NLS-1$
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DialogHelper.showToast(
                            NavigationActivity.this,
                            R.string.msgs_history_unknown, Toast.LENGTH_LONG);
                }
            });

            //Not change directory
            return false;
        }
    }

    /**
     * Method that request a back action over the navigation history.
     *
     * @return boolean If a back action was applied
     */
    public boolean back() {
        NavigationFragment currentNavFragment = getCurrentNavigationFragment();
        // Check that has valid history
        while (currentNavFragment.mHistory.size() > 0) {
            History h = currentNavFragment.mHistory.get(currentNavFragment.mHistory.size() - 1);
            if (h.getItem() instanceof NavigationViewInfoParcelable) {
                // Verify that the path exists
                String path = ((NavigationViewInfoParcelable)h.getItem()).getCurrentDir();

                try {
                    FileSystemObject info = CommandHelper.getFileInfo(this, path, null);
                    if (info != null) {
                        break;
                    }
                    currentNavFragment.mHistory.remove(currentNavFragment.mHistory.size() - 1);
                } catch (Exception e) {
                    ExceptionUtil.translateException(this, e, true, false);
                    currentNavFragment.mHistory.remove(currentNavFragment.mHistory.size() - 1);
                }
            } else {
                break;
            }
        }

        //Extract a history from the
        if (currentNavFragment.mHistory.size() > 0) {
            //Navigate to history
            return navigateToHistory(currentNavFragment.mHistory.get(currentNavFragment.mHistory.size() - 1));
        }

        //Nothing to apply
        return false;
    }

    /**
     * Method that show a new dialog for input a name.
     *
     * @param menuItem The item menu associated
     */
    private void showInputNameDialog(final MenuItem menuItem) {

        //Show the input name dialog
        final InputNameDialog inputNameDialog =
                new InputNameDialog(
                        this,
                        getCurrentNavigationFragment().onRequestCurrentItems(),
                        menuItem.getTitle().toString());
        inputNameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //Retrieve the name an execute the action
                try {
                    String name = inputNameDialog.getName();
                    createNewFileSystemObject(menuItem.getItemId(), name);

                } catch (InflateException e) {
                    //TODO handle this exception properly
                }
            }
        });
        inputNameDialog.show();
    }

    /**
     * Method that create the a new file system object.
     *
     * @param menuId The menu identifier (need to determine the fso type)
     * @param name The name of the file system object
     * @hide
     */
    void createNewFileSystemObject(final int menuId, final String name) {
        switch (menuId) {
            case R.id.mnu_actions_new_directory:
                NewActionPolicy.createNewDirectory(this, name, getCurrentNavigationFragment(), this);
                break;
            case R.id.mnu_actions_new_file:
                NewActionPolicy.createNewFile(this, name, getCurrentNavigationFragment(), this);
                break;
            default:
                break;
        }
    }

    private void openPropertiesDialog(Object item) {
        // Resolve the full path
        String path = String.valueOf(item);
        if (item instanceof FileSystemObject) {
            path = ((FileSystemObject)item).getFullPath();
        }

        // Prior to show the dialog, refresh the item reference
        FileSystemObject fso = null;
        try {
            fso = CommandHelper.getFileInfo(this, path, false, null);
            if (fso == null) {
                throw new NoSuchFileOrDirectory(path);
            }

        } catch (Exception e) {
            // Notify the user
            ExceptionUtil.translateException(this, e);

            // Remove the object
            if (e instanceof FileNotFoundException || e instanceof NoSuchFileOrDirectory) {
                // If have a FileSystemObject reference then there is no need to search
                // the path (less resources used)
                if (item instanceof FileSystemObject) {
                    getCurrentNavigationFragment().removeItem((FileSystemObject) item);
                } else {
                    getCurrentNavigationFragment().removeItem((String) item);
                }
            }
            return;
        }

        InfoActionPolicy.showPropertiesDialog(this, fso, this);

    }

    /**
     * Method that opens the history activity.
     * @hide
     */
    void openHistory() {
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        historyIntent.putExtra(HistoryFragment.EXTRA_HISTORY_LIST, (Serializable)this.getCurrentNavigationFragment().mHistory);
        historyIntent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivityForResult(historyIntent, INTENT_REQUEST_HISTORY);
    }

    /**
     * Method that opens the search activity.
     * @hide
     */
    void openSearch() {
        onSearchRequested();
    }

    /**
     * Method that ask the user to change the access mode prior to crash.
     * @hide
     */
    public void askOrExit() {
        //Show a dialog asking the user
        AlertDialog dialog =
            DialogHelper.createYesNoDialog(
                this,
                R.string.msgs_change_to_prompt_access_mode_title,
                R.string.msgs_change_to_prompt_access_mode_msg,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface alertDialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            // We don't have any console
                            // Show exception and exit
                            DialogHelper.showToast(
                                    NavigationActivity.this,
                                    R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                            exit();
                            return;
                        }

                        // Ok. Now try to change to prompt mode. Any crash
                        // here is a fatal error. We won't have any console to operate.
                        try {
                            // Change console
                            ConsoleBuilder.changeToNonPrivilegedConsole(NavigationActivity.this);

                            // Save preferences
                            Preferences.savePreference(
                                    FileManagerSettings.SETTINGS_ACCESS_MODE,
                                    AccessMode.PROMPT, true);

                        } catch (Exception e) {
                            // Displays an exception and exit
                            Log.e(TAG, getString(R.string.msgs_cant_create_console), e);
                            DialogHelper.showToast(
                                    NavigationActivity.this,
                                    R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                            exit();
                        }
                    }
               });
        DialogHelper.delegateDialogShow(this, dialog);
    }

    /**
     * Method that creates a ChRooted environment, protecting the user to break anything in
     * the device
     * @hide
     */
    void createChRooted() {
        // If we are in a ChRooted mode, then do nothing
        if (this.mChRooted) return;
        this.mChRooted = true;

        //Change to first storage volume
        FileSystemStorageVolume[] volumes =
                StorageHelper.getStorageVolumes(this);
        if (volumes != null && volumes.length > 0) {
            for (int x = 0; x < mPagerAdapter.getCount();x++) {
                mPagerAdapter.getFragment(mViewPager, x).enterChRooted(volumes[0].getPath());
            }
        }

        // Remove the history (don't allow to access to previous data)
        clearHistory();
    }

    /**
     * Method that exits from a ChRooted
     * @hide
     */
    void exitChRooted() {
        // If we aren't in a ChRooted mode, then do nothing
        if (!this.mChRooted) return;
        this.mChRooted = false;

        for (int x = 0; x < mPagerAdapter.getCount();x++) {
            NavigationFragment navigationFragment = (NavigationFragment) mPagerAdapter.getItem(x);
            navigationFragment.exitChRooted();

        }
    }

    /**
     * Method that returns whether the activity is ChRooted
     */
    public boolean isChRooted(){
        return this.mChRooted;
    }

    /**
     * Method called when a controlled exit is required
     * @hide
     */
    public void exit() {
        try {
            FileManagerApplication.destroyBackgroundConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
        try {
            ConsoleBuilder.destroyConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * A part of the PageChangeListener interface. Since there are numerous UI
     * elements that are contextually based on the currently selected page, we
     * need to trigger UI updates after the user swipes to a new page. The pager
     * is contained within the main activity, but the relevant data must be
     * pushed from the currently selected list fragment. Therefore, the changeBreadcrumbPath
     * method is called so that the fragment's current dir can be reflected in the breadcrumbs.
     *
     * @param position the integer index of the currently selected page
     * @return nothing
     */
    @Override
    public void onPageSelected(int position) {

    	// Load the new fragments current dir into the breadcrumb
        if (this.mBreadcrumb != null) {
            this.mBreadcrumb.changeBreadcrumbPath(getCurrentNavigationFragment().getCurrentDir(), this.mChRooted);
        }

        // Tell the breadcrumb that the new fragment will now be the one sending dir changes
        NavigationFragment navigationFragment = getCurrentNavigationFragment();
        navigationFragment.setBreadcrumb(mBreadcrumb);
        navigationFragment.setOnHistoryListener(navigationFragment);
        navigationFragment.setOnNavigationOnRequestMenuListener(this);
        navigationFragment.setCustomTitle(mTitle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrollStateChanged(int state) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Determine whether the "just started up" flag is true or not.
     */
    public boolean isFirstRun() {
        return mFirstRun;
    }

    /**
     * Change the app's "just started up" flag
     */
    public void setFirstRun(boolean firstRun) {
        this.mFirstRun = firstRun;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMarkFilesForPaste(List<FileSystemObject> filesForPaste, COPY_MOVE_OPERATION pasteOperationType) {
        this.mFilesForPaste = filesForPaste;
        this.mPasteOperationType = pasteOperationType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onAreFilesMarkedForPaste() {
        return this.mFilesForPaste != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClearFilesMarkedForPaste() {
        this.mFilesForPaste = null;
        this.mPasteOperationType = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileSystemObject> onRequestFilesMarkedForPaste() {
        return this.mFilesForPaste;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public COPY_MOVE_OPERATION onRequestPasteOperationType() {
        return this.mPasteOperationType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String onRequestDestinationDir() {
        return getCurrentNavigationFragment().getCurrentDir();
    }
    
    /**
     * Method that applies the current theme to the activity
     * @hide
     */
    void applyTheme() {
        Theme theme = ThemeManager.getCurrentTheme(this);
        theme.setBaseTheme(this, false);

        View v;

        /*
        TODO: Either find a way to update action item icons via current methods or ensure the theme update mechanism
        can trigger a normal theme update.
        */

        /*        View v = findViewById(R.id.ab_overflow);
        theme.setImageDrawable(this, (ImageView)v, "ab_overflow_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_actions);
        theme.setImageDrawable(this, (ImageView)v, "ab_actions_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_search);
        theme.setImageDrawable(this, (ImageView)v, "ab_search_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_bookmarks);
        theme.setImageDrawable(this, (ImageView)v, "ab_bookmarks_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_history);
        theme.setImageDrawable(this, (ImageView)v, "ab_history_drawable"); //$NON-NLS-1$*/
        //- Expanders
        v = findViewById(R.id.ab_configuration);
        theme.setImageDrawable(this, (ImageView)v, "expander_open_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_close);
        theme.setImageDrawable(this, (ImageView)v, "expander_close_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_sort_mode);
        theme.setImageDrawable(this, (ImageView)v, "ab_sort_mode_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_layout_mode);
        theme.setImageDrawable(this, (ImageView)v, "ab_layout_mode_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_view_options);
        theme.setImageDrawable(this, (ImageView)v, "ab_view_options_drawable"); //$NON-NLS-1$
    }
}

package com.pdt.plume;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.util.Calendar;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pdt.plume.data.DbContract;
import com.pdt.plume.data.DbHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.pdt.plume.StaticRequestCodes.REQUEST_FILE_GET;
import static com.pdt.plume.StaticRequestCodes.REQUEST_IMAGE_CAPTURE;
import static com.pdt.plume.StaticRequestCodes.REQUEST_IMAGE_GET_ICON;
import static com.pdt.plume.StaticRequestCodes.REQUEST_IMAGE_GET_PHOTO;
import static com.pdt.plume.StaticRequestCodes.REQUEST_NOTIFICATION_ALARM;
import static com.pdt.plume.StaticRequestCodes.REQUEST_NOTIFICATION_INTENT;
import static com.pdt.plume.StaticRequestCodes.REQUEST_PERMISSION_READ_EXTERNAL_STORAGE;

public class NewTaskActivity extends AppCompatActivity
        implements IconPromptDialog.iconDialogListener, EasyPermissions.PermissionCallbacks {

    // Constantly used variables
    String LOG_TAG = NewTaskActivity.class.getSimpleName();
    Utility utility = new Utility();
    Handler handler = new Handler();
    int i = 0;
    boolean active = true;
    boolean isTablet = false;

    // UI Elements
    EditText fieldTitle;
    View fieldShared;
    View fieldCalendar;
    CheckBox fieldSharedCheckbox;
    CheckBox fieldAddCalendar;
    EditText fieldDescription;
    ImageView fieldIcon;

    View fieldClassDropdown;
    TextView fieldClassTextview;
    View fieldTypeDropdown;
    TextView fieldTypeTextview;

    TextView fieldTakePhotoText;
    ImageView fieldTakePhotoIcon;
    View fieldDueDate;
    TextView fieldDueDateTextView;
    TextView fieldAttachFile;
    View fieldSetReminderDate;
    TextView fieldSetReminderDateTextview;
    View fieldSetReminderTime;
    TextView fieldSetReminderTimeTextview;

    // UI Data
    String iconUriString = "android.resource://com.pdt.plume/drawable/art_task_64dp";
    ArrayList<String> classTitleArray = new ArrayList<>();
    ArrayList<String> classTypeArray = new ArrayList<>();
    String classTitle = "None";
    String classType = "None";

    long dueDateMillis;
    long reminderDateMillis;
    long reminderTimeMillis;

    long oldReminderDateMillis;
    long oldReminderTimeMillis;

    ArrayList<Uri> photoUriList = new ArrayList();
    ArrayList<ImageView> photos = new ArrayList<>();
    Uri mTempPhotoUri;

    // Theme Variables
    int mPrimaryColor;
    int mDarkColor;
    int mSecondaryColor;

    String attachedFileUriString = "";

    // Firebase Variables
    String name;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser mFirebaseUser;
    String mUserId;

    // Google Calendar Variables
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private Button mCallApiButton;
    ProgressDialog mProgress;
    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
//    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };


    // Built-in Icons
    private Integer[] mThumbIds = {
            R.drawable.art_arts_64dp,
            R.drawable.art_biology_64dp,
            R.drawable.art_business_64dp,
            R.drawable.art_chemistry_64dp,
            R.drawable.art_childdevelopment_64dp,
            R.drawable.art_class_64dp,
            R.drawable.art_computing_64dp,
            R.drawable.art_cooking_64dp,
            R.drawable.art_creativestudies_64dp,
            R.drawable.art_dance_64dp,
            R.drawable.art_drama_64dp,
            R.drawable.art_electronics_64dp,
            R.drawable.art_engineering_64dp,
            R.drawable.art_english_64dp,
            R.drawable.art_environment_64dp,
            R.drawable.art_french_64dp,
            R.drawable.art_geography_64dp,
            R.drawable.art_graphics_64dp,
            R.drawable.art_history_64dp,
            R.drawable.art_hospitality_64dp,
            R.drawable.art_ict_64dp,
            R.drawable.art_maths_64dp,
            R.drawable.art_media_64dp,
            R.drawable.art_music_64dp,
            R.drawable.art_music2_64dp,
            R.drawable.art_pe_64dp,
            R.drawable.art_physics_64dp,
            R.drawable.art_psychology_64dp,
            R.drawable.art_re_64dp,
            R.drawable.art_science_64dp,
            R.drawable.art_spanish_64dp,
            R.drawable.art_task_64dp,
            R.drawable.art_woodwork_64dp
    };

    // Intent Data
    boolean FLAG_EDIT = false;
    int editId = -1;
    String firebaseEditId = "";
    boolean customIconUploaded = false;

    boolean LAUNCHED_NEW_CLASS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);
        getSupportActionBar().setElevation(0f);
        isTablet = getResources().getBoolean(R.bool.isTablet);
        int minHeight = getWindowManager().getDefaultDisplay().getHeight();
        if (isTablet) findViewById(R.id.master_layout).setMinimumHeight(minHeight);

        // Initialise Firebase and SQLite
        DbHelper dbHelper = new DbHelper(this);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser != null) {
            mUserId = mFirebaseUser.getUid();
            FirebaseDatabase.getInstance().getReference()
                    .child("users").child(mUserId).child("nickname").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    name = dataSnapshot.getValue(String.class);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else name = "";

        // Get references to the UI elements
        mOutputText = (TextView) findViewById(R.id.output_text);
        mCallApiButton = (Button) findViewById(R.id.calendar_button);
        fieldTitle = (EditText) findViewById(R.id.field_new_task_title);
        fieldIcon = (ImageView) findViewById(R.id.field_new_task_icon);
        fieldClassDropdown = findViewById(R.id.field_class_dropdown);
        fieldClassTextview = (TextView) findViewById(R.id.field_class_textview);
        fieldShared = findViewById(R.id.field_shared_layout);
        fieldSharedCheckbox = (CheckBox) findViewById(R.id.field_shared_checkbox);
        fieldCalendar = findViewById(R.id.field_calendar);
        fieldAddCalendar = (CheckBox) findViewById(R.id.field_add_to_calendar);
        fieldTypeDropdown = findViewById(R.id.field_type_dropdown);
        fieldTypeTextview = (TextView) findViewById(R.id.field_type_textview);
        fieldDescription = (EditText) findViewById(R.id.field_new_task_description);
        fieldTakePhotoText = (TextView) findViewById(R.id.take_photo_text);
        fieldTakePhotoIcon = (ImageView) findViewById(R.id.take_photo_icon);
        if (!isTablet) fieldDueDate = findViewById(R.id.field_new_task_duedate);
        else fieldDueDate = findViewById(R.id.field_new_task_duedate_textview);
        fieldDueDateTextView = (TextView) findViewById(R.id.field_new_task_duedate_textview);
        if (!isTablet) fieldSetReminderDate = findViewById(R.id.field_new_task_reminder_date);
        else fieldSetReminderDate = findViewById(R.id.field_new_task_reminder_date_textview);
        fieldSetReminderDateTextview = (TextView) findViewById(R.id.field_new_task_reminder_date_textview);
        if (!isTablet) fieldSetReminderTime = findViewById(R.id.field_new_task_reminder_time);
        else fieldSetReminderTime = findViewById(R.id.field_new_task_reminder_time_textview);
        fieldSetReminderTimeTextview = (TextView) findViewById(R.id.field_new_task_reminder_time_textview);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");

        // Initialize credentials and service object.
//        mCredential = GoogleAccountCredential.usingOAuth2(
//                getApplicationContext(), Arrays.asList(SCOPES))
//                .setBackOff(new ExponentialBackOff());

        if (mFirebaseUser == null)
            if (!isTablet) fieldSharedCheckbox.setVisibility(View.GONE);
            else fieldShared.setVisibility(View.GONE);
        if (isTablet)
            fieldShared.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fieldSharedCheckbox.toggle();
                }
            });

        // Initialise the dropdown box default data
        classTitle = getString(R.string.none);
        classType = getString(R.string.none);

        // Initialise the Calendar checkbox
        fieldAddCalendar.setChecked(PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.KEY_SETTINGS_TASK_NOTIFICATION), false));

        // Set the listeners of the UI
        fieldIcon.setOnClickListener(showIconPrompt());
        fieldClassDropdown.setOnClickListener(listener());
        fieldTypeDropdown.setOnClickListener(listener());
        fieldTakePhotoText.setOnClickListener(listener());
        if (fieldTakePhotoIcon != null)
            fieldTakePhotoIcon.setOnClickListener(listener());
//        fieldAttachFile.setOnClickListener(listener());
        fieldSetReminderDate.setOnClickListener(listener());
        fieldSetReminderTime.setOnClickListener(listener());
        fieldDueDate.setOnClickListener(listener());


        // Initialise the class dropdown data
        if (mFirebaseUser != null) {
            // Get schedule data from Firebase
            final DatabaseReference classesRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(mUserId).child("classes");
            classesRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    classTitleArray.add(dataSnapshot.getKey());
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else {
            // Get schedule data from SQLite
            Cursor scheduleCursor = dbHelper.getAllScheduleData();

            // Scan through the cursor and add in each class category into the array list
            if (scheduleCursor.moveToFirst()) {
                for (int i = 0; i < scheduleCursor.getCount(); i++) {
                    String classTitle = scheduleCursor.getString(scheduleCursor.getColumnIndex(DbContract.ScheduleEntry.COLUMN_TITLE));
                    if (!classTitleArray.contains(classTitle))
                        classTitleArray.add(classTitle);
                    scheduleCursor.moveToNext();
                }
            }
            scheduleCursor.close();
        }

        // Initialise the taskType dropdown data
        classTypeArray.add(getString(R.string.field_dropdown_type_menu_item_homework));
        classTypeArray.add(getString(R.string.field_dropdown_type_menu_item_test));
        classTypeArray.add(getString(R.string.field_dropdown_type_menu_item_revision));
        classTypeArray.add(getString(R.string.field_dropdown_type_menu_item_project));
        classTypeArray.add(getString(R.string.field_dropdown_type_menu_item_detention));
        classTypeArray.add(getString(R.string.field_dropdown_type_menu_item_other));

        // Check if the activity was started by an edit action
        // If the intent is not null the activity must have
        // been started through an edit action
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                // Get the task data sent through the intent
                String icon = extras.getString("icon");
                if (icon == null) icon = iconUriString;
                String title = extras.getString(getString(R.string.INTENT_EXTRA_TITLE));
                String classTitle = extras.getString(getString(R.string.INTENT_EXTRA_CLASS));
                String classType = extras.getString(getString(R.string.INTENT_EXTRA_TYPE));
                String description = extras.getString(getString(R.string.INTENT_EXTRA_DESCRIPTION));
                String attachment = "";
                long dueDate = ((long) extras.getFloat(getString(R.string.INTENT_EXTRA_DUEDATE)));
                long reminderDate = extras.getLong(getString(R.string.INTENT_EXTRA_ALARM_DATE));
                long reminderTime = extras.getLong(getString(R.string.INTENT_EXTRA_ALARM_TIME));

                int position = extras.getInt("position");
                FLAG_EDIT = extras.getBoolean(getString(R.string.INTENT_FLAG_EDIT), false);

                if (FLAG_EDIT) {
                    // Get the id depending on where the data came from
                    if (mFirebaseUser != null)
                        firebaseEditId = intent.getStringExtra("id");
                    else editId = intent.getIntExtra(getString(R.string.INTENT_EXTRA_ID), -1);

                    fieldIcon.setImageURI(Uri.parse(icon));
                    fieldTitle.setText(title);
                    fieldTitle.setSelection(fieldTitle.getText().length());
                    fieldDescription.setText(description);

                    // Set the current state of the due date
                    if (dueDate > 0f) {
                        this.dueDateMillis = dueDate;

                        // Format duedate using the utility method
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(((long) dueDate));
                        fieldDueDateTextView.setText(utility.formatDateString(this, c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)));
                    }

                    // Set the current state of the dropdown text views
                    if (classTitle.equals(""))
                        fieldClassTextview.setText(getString(R.string.none));
                    else fieldClassTextview.setText(classTitle);
                    this.classTitle = classTitle;
                    if (classType.equals(""))
                        fieldTypeTextview.setText(getString(R.string.none));
                    else fieldTypeTextview.setText(classType);
                    this.classType = classType;
                    this.iconUriString = icon;

                    // Set the current state of the reminder date and time
                    if (reminderDate != 0f) {
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis((long) reminderDate);
                        float reminderDateYear = c.get(Calendar.YEAR);
                        float reminderDateMonth = c.get(Calendar.MONTH);
                        float reminderDateDay = c.get(Calendar.DAY_OF_MONTH);

                        Calendar today = Calendar.getInstance();
                        if (today.get(Calendar.DAY_OF_MONTH) == reminderDateDay && today.get(Calendar.MONTH) == reminderDateMonth
                                && today.get(Calendar.YEAR) == reminderDateYear)
                            fieldSetReminderDateTextview.setText(getString(R.string.today));
                        else {
                            today.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH) + 1);
                            if (today.get(Calendar.DAY_OF_MONTH) == reminderDateDay && today.get(Calendar.MONTH) == reminderDateMonth
                                    && today.get(Calendar.YEAR) == reminderDateYear)
                                fieldSetReminderDateTextview.setText(getString(R.string.tomorrow));
                            else
                                fieldSetReminderDateTextview.setText(utility.formatDateString(this, ((int) reminderDateYear),
                                        ((int) reminderDateMonth), ((int) reminderDateDay)));
                        }
                        this.reminderDateMillis = c.getTimeInMillis();
                    } else {
                        fieldSetReminderDateTextview.setText(getString(R.string.none));
                        fieldSetReminderTime.setEnabled(false);
                        fieldSetReminderTimeTextview.setAlpha(0.5f);
                    }


                    if (reminderTime != 0f) {
                        fieldSetReminderTimeTextview.setText(utility.millisToHourTime(this, reminderTime));
                        this.reminderTimeMillis = reminderTime;
                    } else {
                        fieldSetReminderTimeTextview.setText(getString(R.string.none));
                    }

                    oldReminderDateMillis = reminderDate;
                    oldReminderTimeMillis = reminderTime;

                    // Get photo data
                    if (mFirebaseUser != null) {
                        DatabaseReference photosRef = FirebaseDatabase.getInstance().getReference()
                                .child("users").child(mUserId).child("tasks").child(firebaseEditId)
                                .child("photos");
                        photosRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot uriSnapshot : dataSnapshot.getChildren()) {
                                    String photoPath = uriSnapshot.getKey()
                                            .replace("'dot'", ".")
                                            .replace("'slash'", "/")
                                            .replace("'hash'", "#")
                                            .replace("'ampers'", "&");
                                    photoUriList.add(Uri.parse(photoPath));
                                }
                                // Add in the views for the photos
                                for (int i = 0; i < photoUriList.size(); i++) {
                                    addPhotoView(photoUriList.get(i));
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }

                    // Photo data from SQLite
                    String photoString = intent.getStringExtra("photo");
                    if (photoString != null && !photoString.equals("")) {
                        String[] photos = photoString.split("#seperate#");
                        // Add in the views for the photos
                        for (int i = 0; i < photos.length; i++) {
                            // Add the photo as a new image view
                            final Uri photoUri = Uri.parse(photos[i]);
                            photoUriList.add(photoUri);
                            final RelativeLayout relativeLayout = new RelativeLayout(NewTaskActivity.this);
                            relativeLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

                            final ImageView photo = new ImageView(NewTaskActivity.this);
                            this.photos.add(photo);
                            photo.setImageURI(photoUri);
                            int width = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics()));
                            photo.setLayoutParams(new RelativeLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
                            photo.setPadding(4, 0, 4, 0);
                            photo.setId(Utility.generateUniqueId());

                            final LinearLayout photosLayout = (LinearLayout) findViewById(R.id.photos_layout);
                            photosLayout.setVisibility(View.VISIBLE);
                            photosLayout.addView(relativeLayout);
                            relativeLayout.addView(photo);

                            // Add the cancel button to remove the photo
                            final ImageView cancel = new ImageView(NewTaskActivity.this);
                            cancel.setImageResource(R.drawable.ic_cancel);
                            int wh = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(wh, wh);
                            params.addRule(RelativeLayout.ALIGN_END, photo.getId());
                            params.addRule(RelativeLayout.ALIGN_RIGHT, photo.getId());
                            params.addRule(RelativeLayout.ALIGN_TOP, photo.getId());
                            cancel.setLayoutParams(params);
                            relativeLayout.addView(cancel);
                            cancel.setVisibility(View.GONE);
                            cancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    photosLayout.removeView(relativeLayout);
                                    photoUriList.remove(photoUri);
                                    NewTaskActivity.this.photos.remove(photo);
                                }
                            });

                            // Add the click listener of the photo
                            cancel.setTag("null");
                            final Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    cancel.setTag("null");
                                    cancel.setVisibility(View.GONE);
                                }
                            };
                            photo.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (cancel.getTag().equals("cancelVisible")) {
                                        cancel.setTag("null");
                                        cancel.setVisibility(View.GONE);
                                        handler.removeCallbacks(runnable);
                                    } else {
                                        cancel.setVisibility(View.VISIBLE);
                                        Runnable runnable1 = new Runnable() {
                                            @Override
                                            public void run() {
                                                View otherCancels = photosLayout.findViewWithTag("cancelVisible");
                                                if (otherCancels != null) {
                                                    otherCancels.setVisibility(View.GONE);
                                                    otherCancels.setTag("null");
                                                    handler.post(this);
                                                } else cancel.setTag("cancelVisible");
                                            }
                                        };

                                        handler.post(runnable1);
                                        handler.postDelayed(runnable, 2000);
                                    }
                                }
                            });
                        }
                    }
                } else {
                    setDefaultData();
                    // Check if there is any data sent through the intent
                    classTitle = intent.getStringExtra(getString(R.string.INTENT_EXTRA_CLASS));
                    if (classTitle != null) {
                        this.classTitle = classTitle;
                        fieldClassTextview.setText(classTitle);
                        fieldTitle.setText(classTitle);
                        fieldTitle.setSelection(0, classTitle.length());
                    }
                }
            } else setDefaultData();
        } else setDefaultData();
    }

    private void setDefaultData() {
        // Set any default data
        Resources resources = getResources();
        int resId = R.drawable.art_task_64dp;
        Uri drawableUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + resources.getResourcePackageName(resId)
                + '/' + resources.getResourceTypeName(resId) + '/' + resources.getResourceEntryName(resId));
        iconUriString = drawableUri.toString();

        // Reminder Date and Time
        fieldSetReminderDateTextview.setText(getString(R.string.none));
        if (!isTablet)
            fieldSetReminderTime.setEnabled(false);
        else fieldSetReminderTimeTextview.setEnabled(false);
        fieldSetReminderTimeTextview.setAlpha(0.5f);
        fieldSetReminderTimeTextview.setText(getString(R.string.none));

        // Dates
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + 7);
        dueDateMillis = c.getTimeInMillis();
        fieldDueDateTextView.setText(utility.formatDateString(this,
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)));
    }


    @Override
    protected void onStart() {
        super.onStart();

        // Initialise the theme variables
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPrimaryColor = preferences.getInt(getString(R.string.KEY_THEME_PRIMARY_COLOR), getResources().getColor(R.color.colorPrimary));
        float[] hsv = new float[3];
        int tempColor = mPrimaryColor;
        Color.colorToHSV(tempColor, hsv);
        hsv[2] *= 0.8f; // value component
        mDarkColor = Color.HSVToColor(hsv);
        mSecondaryColor = preferences.getInt(getString(R.string.KEY_THEME_SECONDARY_COLOR), getResources().getColor(R.color.colorAccent));
        int backgroundColor = preferences.getInt(getString(R.string.KEY_THEME_BACKGROUND_COLOUR), getResources().getColor(R.color.backgroundColor));
        findViewById(R.id.master_layout).setBackgroundColor(backgroundColor);

        int textColor = preferences.getInt(getString(R.string.KEY_THEME_TEXT_COLOUR), getResources().getColor(R.color.gray_900));
        Color.colorToHSV(textColor, hsv);
        hsv[2] *= 0.8f;
        int darkTextColor = Color.HSVToColor(hsv);

        ((TextView) findViewById(R.id.field_class_textview)).setTextColor(darkTextColor);
        ((ImageView) findViewById(R.id.imageView)).setColorFilter(darkTextColor);
        findViewById(R.id.divider1).setBackgroundColor(darkTextColor);
        ((TextView) findViewById(R.id.field_type_textview)).setTextColor(darkTextColor);
        ((ImageView) findViewById(R.id.imageView2)).setColorFilter(darkTextColor);
        findViewById(R.id.divider2).setBackgroundColor(darkTextColor);
        fieldSharedCheckbox.setTextColor(darkTextColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            fieldSharedCheckbox.setButtonTintList(ColorStateList.valueOf(darkTextColor));
        fieldAddCalendar.setTextColor(darkTextColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            fieldAddCalendar.setButtonTintList(ColorStateList.valueOf(darkTextColor));
        ((ImageView) findViewById(R.id.field_new_task_duedate_icon)).setColorFilter(darkTextColor);
        ((EditText) findViewById(R.id.field_new_task_description)).setTextColor(textColor);
        ((EditText) findViewById(R.id.field_new_task_description)).setHintTextColor(darkTextColor);
        ((TextView) findViewById(R.id.field_new_task_duedate_textview)).setTextColor(textColor);
        ((ImageView) findViewById(R.id.field_new_task_duedate_icon)).setColorFilter(textColor);
        ((TextView) findViewById(R.id.field_new_task_reminder_date_textview)).setTextColor(darkTextColor);
        ((ImageView) findViewById(R.id.field_new_task_reminder_date_icon)).setColorFilter(darkTextColor);

        if (!isTablet) {
            ((TextView) findViewById(R.id.link_to_class)).setTextColor(textColor);
            ((TextView) findViewById(R.id.task_type)).setTextColor(textColor);
            ((TextView) findViewById(R.id.textView3)).setTextColor(darkTextColor);
            ((TextView) findViewById(R.id.due_date)).setTextColor(darkTextColor);
            findViewById(R.id.divider3).setBackgroundColor(darkTextColor);
            findViewById(R.id.divider4).setBackgroundColor(darkTextColor);
            findViewById(R.id.divider5).setBackgroundColor(darkTextColor);
            ((TextView) findViewById(R.id.textView7)).setTextColor(darkTextColor);
            ((TextView) findViewById(R.id.field_new_task_reminder_time_textview)).setTextColor(darkTextColor);
            ((ImageView) findViewById(R.id.field_new_task_reminder_time_icon)).setColorFilter(darkTextColor);
            ((TextView) findViewById(R.id.take_photo_text)).setTextColor(darkTextColor);
            ((ImageView) findViewById(R.id.take_photo_icon)).setColorFilter(darkTextColor);
        } else {
            ((TextView) findViewById(R.id.share_tasks_with_peers)).setTextColor(darkTextColor);
            ((ImageView) findViewById(R.id.imageView3)).setColorFilter(darkTextColor);
            ((ImageView) findViewById(R.id.imageView4)).setColorFilter(darkTextColor);
            ((ImageView) findViewById(R.id.imageView6)).setColorFilter(darkTextColor);
            ((ImageView) findViewById(R.id.imageView7)).setColorFilter(darkTextColor);
        }


        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(mPrimaryColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(mDarkColor);
        }
        if (!isTablet)
            fieldTitle.setBackgroundColor(mPrimaryColor);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            fieldTitle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray_700)));

        if (LAUNCHED_NEW_CLASS) {
            DbHelper dbHelper = new DbHelper(this);
            Cursor cursor = dbHelper.getAllScheduleData();
            if (cursor.moveToLast()) {
                String newClassTitle = cursor.getString(cursor.getColumnIndex(DbContract.ScheduleEntry.COLUMN_TITLE));
                fieldClassTextview.setText(newClassTitle);
                classTitle = newClassTitle;
                LAUNCHED_NEW_CLASS = false;
            }

            // Auto-fill the category editText if there isn't any user-inputted category yet
            String titleText = fieldTitle.getText().toString();
            if (titleText.equals(""))
                if (NewTaskActivity.this.classTitle.equals(getString(R.string.none)))
                    fieldTitle.setText("");
                else fieldTitle.setText(NewTaskActivity.this.classTitle);
            if (titleText.equals(classType) && NewTaskActivity.this.classTitle.equals(getString(R.string.none)))
                fieldTitle.setText(classType);
                // Check if another class was set before
            else if (classTitleArray.contains(titleText))
                if (NewTaskActivity.this.classTitle.equals(getString(R.string.none)))
                    fieldTitle.setText("");
                else fieldTitle.setText(NewTaskActivity.this.classTitle);
                // Check if the taskType was set before the class
            else if (classTypeArray.contains(titleText))
                fieldTitle.setText(NewTaskActivity.this.classTitle + " " + titleText);
                // Check if the category editText contains text as a result
                // of previously using the dropdown lists
            else {
                String[] splitFieldTitle = titleText.split(" ");
                if (splitFieldTitle.length == 2 && classTitleArray.contains(splitFieldTitle[0]) && classTypeArray.contains(splitFieldTitle[1])) {
                    if (NewTaskActivity.this.classTitle.equals(getString(R.string.none)))
                        fieldTitle.setText(splitFieldTitle[1]);
                    else
                        fieldTitle.setText(NewTaskActivity.this.classTitle + " " + splitFieldTitle[1]);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_confirm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Without this, the up button will not do anything and return the error 'Cancelling event due to no window focus'
            case android.R.id.home:
                finish();
                break;

            // Insert inputted data into the database and terminate the activity
            case R.id.action_done:
                // Validate that the category field is not empty
                if (fieldTitle.getText().toString().equals("")) {
                    Toast.makeText(NewTaskActivity.this, getString(R.string.new_tasks_toast_validation_title_not_found), Toast.LENGTH_SHORT).show();
                    return false;
                }
                Intent intent = new Intent(this, MainActivity.class);

                // Determine where the NewTaskActivity was started from and return to the corresponding activity
                if (getIntent().hasExtra(getString(R.string.INTENT_FLAG_RETURN_TO_SCHEDULE))) {
                    intent.putExtra(getString(R.string.INTENT_FLAG_RETURN_TO_SCHEDULE), getString(R.string.INTENT_FLAG_RETURN_TO_SCHEDULE));
                    intent.putExtra(getString(R.string.INTENT_EXTRA_POSITION),
                            getIntent().getIntExtra(getString(R.string.INTENT_EXTRA_POSITION), 0));
                } else
                    intent.putExtra(getString(R.string.INTENT_FLAG_RETURN_TO_TASKS), getString(R.string.INTENT_FLAG_RETURN_TO_TASKS));
                try {
                    if (insertTaskDataIntoDatabase()) {
                        startActivity(intent);
                        return true;
                    } else {
                        Log.w(LOG_TAG, "Error creating new task");
                        finish();
                        return true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(NewTaskActivity.this);
                    builder.setTitle(getString(R.string.field_new_photo_dialog_title))
                            .setItems(R.array.field_new_photo_dialog_items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
                                            try {
                                                dispatchTakePictureIntent();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        case 1:
                                            dispatchSelectPhotoIntent();
                                            break;
                                    }
                                }
                            }).show();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    new AlertDialog.Builder(this)
                            .setMessage(getString(R.string.dialog_permission_denied_take_photo))
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                EasyPermissions.onRequestPermissionsResult(
                        requestCode, permissions, grantResults, this);
                break;
        }
    }

    // This method is called when a file is selected after the ACTION_GET
    // intent was called from the attach file action
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Attachment Upload
        if (requestCode == REQUEST_FILE_GET && resultCode == RESULT_OK) {
            // Get the Uri and UriString from the intent and save its global variable
            Uri filePathUri = data.getData();
            attachedFileUriString = data.getDataString();

            // Get the filename of the file and set the field's text to that
            Cursor returnCursor = getContentResolver().query(filePathUri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            String fileName = returnCursor.getString(nameIndex);
            returnCursor.close();
            fieldAttachFile.setText(fileName);
        }

        // Custom Icon Upload
        if (requestCode == REQUEST_IMAGE_GET_ICON && resultCode == RESULT_OK) {
            Uri dataUri = data.getData();
            Bitmap setImageBitmap = null;

            iconUriString = dataUri.toString();

            try {
                setImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dataUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            fieldIcon.setImageBitmap(setImageBitmap);
            customIconUploaded = true;
        }

        // Take/Pick Photo
        if (requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_GET_PHOTO && resultCode == RESULT_OK) {
            // Add the uri to the array list
            final Uri imageData;
            if (requestCode == REQUEST_IMAGE_GET_PHOTO)
                imageData = data.getData();
            else {
                imageData = mTempPhotoUri;
            }

            photoUriList.add(imageData);
            addPhotoView(imageData);
        }

    }

    private void addPhotoView(final Uri imageUri) {
        // Add the photo as a new image view
        final RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final ImageView photo = new ImageView(this);
        photos.add(photo);
        photo.setImageURI(imageUri);
        int width = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics()));
        photo.setLayoutParams(new RelativeLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        photo.setPadding(4, 0, 4, 0);
        photo.setId(Utility.generateUniqueId());

        final LinearLayout photosLayout = (LinearLayout) findViewById(R.id.photos_layout);
        photosLayout.setVisibility(View.VISIBLE);
        photosLayout.addView(relativeLayout);
        relativeLayout.addView(photo);

        // Add the cancel button to remove the photo
        final ImageView cancel = new ImageView(this);
        cancel.setImageResource(R.drawable.ic_cancel);
        int wh = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(wh, wh);
        params.addRule(RelativeLayout.ALIGN_END, photo.getId());
        params.addRule(RelativeLayout.ALIGN_RIGHT, photo.getId());
        params.addRule(RelativeLayout.ALIGN_TOP, photo.getId());
        cancel.setLayoutParams(params);
        relativeLayout.addView(cancel);
        cancel.setVisibility(View.GONE);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photosLayout.removeView(relativeLayout);
                photoUriList.remove(imageUri);
                photos.remove(photo);
            }
        });

        // Add the click listener of the photo
        cancel.setTag("null");
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                cancel.setTag("null");
                cancel.setVisibility(View.GONE);
            }
        };
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cancel.getTag().equals("cancelVisible")) {
                    cancel.setTag("null");
                    cancel.setVisibility(View.GONE);
                    handler.removeCallbacks(runnable);
                } else {
                    cancel.setVisibility(View.VISIBLE);
                    Runnable runnable1 = new Runnable() {
                        @Override
                        public void run() {
                            View otherCancels = photosLayout.findViewWithTag("cancelVisible");
                            if (otherCancels != null) {
                                otherCancels.setVisibility(View.GONE);
                                otherCancels.setTag("null");
                                handler.post(this);
                            } else cancel.setTag("cancelVisible");
                        }
                    };

                    handler.post(runnable1);
                    handler.postDelayed(runnable, 2000);
                }
            }
        });
    }

    private void showClassDropdownMenu() {
        // Set up the dropdown menu on both views
        // Set up the class dropdown menu
        PopupMenu popupMenu = new PopupMenu(this, fieldClassDropdown);

        // Add the titles to the menu as well as the item to add a new class
        popupMenu.getMenu().add(getString(R.string.none));
        for (int i = 0; i < classTitleArray.size(); i++)
            popupMenu.getMenu().add(classTitleArray.get(i));
        popupMenu.getMenu().add(getString(R.string.add_new_class));

        // Set the ItemClickListener for the menu items
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Set the data to be later saved into the database
                NewTaskActivity.this.classTitle = item.getTitle().toString();

                // If Add New Class was selected, start NewScheduleActivity
                if (NewTaskActivity.this.classTitle.equals(getString(R.string.add_new_class))) {
                    LAUNCHED_NEW_CLASS = true;
                    Intent intent = new Intent(NewTaskActivity.this, NewScheduleActivity.class);
                    intent.putExtra("STARTED_BY_NEWTASKACTIVITY", true);
                    startActivity(intent);
                    return true;
                }

                // Auto-fill the category editText if there isn't any user-inputted category yet
                String titleText = fieldTitle.getText().toString();
                if (titleText.equals(""))
                    if (NewTaskActivity.this.classTitle.equals(getString(R.string.none)))
                        fieldTitle.setText("");
                    else fieldTitle.setText(NewTaskActivity.this.classTitle);
                if (titleText.equals(classType) && NewTaskActivity.this.classTitle.equals(getString(R.string.none)))
                    fieldTitle.setText(classType);
                    // Check if another class was set before
                else if (classTitleArray.contains(titleText))
                    if (NewTaskActivity.this.classTitle.equals(getString(R.string.none)))
                        fieldTitle.setText("");
                    else fieldTitle.setText(NewTaskActivity.this.classTitle);
                    // Check if the taskType was set before the class
                else if (classTypeArray.contains(titleText))
                    fieldTitle.setText(NewTaskActivity.this.classTitle + " " + titleText);
                    // Check if the category editText contains text as a result
                    // of previously using the dropdown lists
                else {
                    String[] splitFieldTitle = titleText.split(" ");
                    if (splitFieldTitle.length == 2 && classTitleArray.contains(splitFieldTitle[0]) && classTypeArray.contains(splitFieldTitle[1])) {
                        if (NewTaskActivity.this.classTitle.equals(getString(R.string.none)))
                            fieldTitle.setText(splitFieldTitle[1]);
                        else
                            fieldTitle.setText(NewTaskActivity.this.classTitle + " " + splitFieldTitle[1]);
                    }
                }
                // Set the dropdown list text to the selected item
                fieldClassTextview.setText(NewTaskActivity.this.classTitle);

                if (mFirebaseUser != null) {
                    // Enable the Share checkbox accordingly
                    if (classTitle.equals(getString(R.string.none))) {
                        fieldSharedCheckbox.setEnabled(true);
                        fieldSharedCheckbox.setClickable(true);
                    } else {
                        fieldSharedCheckbox.setEnabled(false);
                        fieldSharedCheckbox.setClickable(false);
                        FirebaseDatabase.getInstance().getReference()
                                .child("users").child(mUserId).child("peers")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for (DataSnapshot peerSnapshot : dataSnapshot.getChildren()) {
                                            for (DataSnapshot classSnapshot : peerSnapshot.child("classes").getChildren()) {
                                                if (classSnapshot.getKey().equals(fieldTitle.getText().toString())) {
                                                    fieldSharedCheckbox.setEnabled(true);
                                                    fieldSharedCheckbox.setClickable(true);
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                    }
                }

                return true;
            }
        });

        popupMenu.show();
    }

    private void showTypeDropdownMenu() {
        // Initialise and inflate the menu
        PopupMenu popupMenu = new PopupMenu(NewTaskActivity.this, fieldTypeDropdown);
        popupMenu.getMenuInflater().inflate(R.menu.menu_popup_type, popupMenu.getMenu());

        // Set the menu's ItemClickListener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Set the data to be later saved into the database
                NewTaskActivity.this.classType = item.getTitle().toString();

                // Auto-fill the category editText if there isn't any user-inputted category yet
                String titleText = fieldTitle.getText().toString();
                if (titleText.equals("") && !NewTaskActivity.this.classType.equals(getString(R.string.none)))
                    fieldTitle.setText(NewTaskActivity.this.classType);
                if (titleText.contains(classTitle) && NewTaskActivity.this.classType.equals(getString(R.string.none)))
                    fieldTitle.setText(classTitle);
                    // Check if another taskType was set before
                else if (classTypeArray.contains(titleText))
                    if (NewTaskActivity.this.classType.equals(getString(R.string.none)))
                        fieldTitle.setText("");
                    else fieldTitle.setText(NewTaskActivity.this.classType);
                    // Check if the taskType was set before the class
                else if (classTitleArray.contains(titleText)) {
                    fieldTitle.setText(titleText + " " + NewTaskActivity.this.classType);
                }
                // Check if the category editText contains text as a result
                // of previously using the dropdown lists
                else {
                    String[] splitFieldTitle = titleText.split(" ");
                    if (splitFieldTitle.length == 2 && classTitleArray.contains(splitFieldTitle[0]) && classTypeArray.contains(splitFieldTitle[1])) {
                        if (NewTaskActivity.this.classType.equals(getString(R.string.none)))
                            fieldTitle.setText(splitFieldTitle[0]);
                        else
                            fieldTitle.setText(splitFieldTitle[0] + " " + NewTaskActivity.this.classType);
                    }
                }

                // Set the dropdown list text to the selected item
                fieldTypeTextview.setText(NewTaskActivity.this.classType);

                return true;
            }
        });

        popupMenu.show();
    }

    private void showReminderDateDropdownMenu() {
        PopupMenu popupMenu = new PopupMenu(this, fieldSetReminderDate);
        popupMenu.getMenuInflater().inflate(R.menu.popup_reminder_date, popupMenu.getMenu());
        final Calendar c = Calendar.getInstance();
        final Calendar now = Calendar.getInstance();
        if (reminderDateMillis <= 0) c.setTimeInMillis(now.getTimeInMillis());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.dropdown_reminder_date_none:
                        fieldSetReminderDateTextview.setText(getString(R.string.none));
                        reminderDateMillis = 0;
                        fieldSetReminderTime.setEnabled(false);
                        fieldSetReminderTimeTextview.setAlpha(0.5f);
                        break;
                    case R.id.dropdown_reminder_date_today:
                        // Set the reminder date to 'today' with no time
                        c.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 0, 0);
                        fieldSetReminderDateTextview.setText(getString(R.string.today));
                        reminderDateMillis = c.getTimeInMillis();

                        // Enable the reminder time field and set its colour accordingly
                        fieldSetReminderTime.setEnabled(true);
                        fieldSetReminderTimeTextview.setAlpha(1f);

                        // If reminderTime has not been previously set, set it to 1 hour later
                        if (reminderTimeMillis == 0) {
                            int hourOfDay = now.get(Calendar.HOUR_OF_DAY) + 1;
                            int minute = now.get(Calendar.MINUTE);

                            Calendar c = Calendar.getInstance();
                            c.set(0, 0, 0, hourOfDay, minute);
                            reminderTimeMillis = utility.timeToMillis(hourOfDay, minute);

                            if (minute < 10)
                                fieldSetReminderTimeTextview.setText(hourOfDay + ":0" + minute);
                            else
                                fieldSetReminderTimeTextview.setText(hourOfDay + ":" + minute);
                        }
                        break;

                    case R.id.dropdown_reminder_date_tomorrow:
                        // Set the reminder date to 'tomorrow' and enable the reminder time field
                        c.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH) + 1);
                        fieldSetReminderDateTextview.setText(getString(R.string.tomorrow));
                        reminderDateMillis = c.getTimeInMillis();

                        // Set the reminder time colour accordingly
                        fieldSetReminderTime.setEnabled(true);
                        fieldSetReminderTimeTextview.setAlpha(1f);

                        // If the reminder time has not been previously set, set the time to 1 hour later
                        if (reminderTimeMillis == 0) {
                            int hourOfDay = now.get(Calendar.HOUR_OF_DAY) + 1;
                            int minute = now.get(Calendar.MINUTE);
                            reminderTimeMillis = utility.timeToMillis(hourOfDay, minute);
                            if (minute < 10)
                                fieldSetReminderTimeTextview.setText(hourOfDay + ":0" + minute);
                            else
                                fieldSetReminderTimeTextview.setText(hourOfDay + ":" + minute);
                        }
                        break;

                    case R.id.dropdown_reminder_date_setdate:
                        // Enable the reminder time field and set its colour accordingly
                        fieldSetReminderTime.setEnabled(true);
                        fieldSetReminderTimeTextview.setAlpha(1);

                        // Pass the current reminder date to the picker
                        int year = c.get(Calendar.YEAR);
                        int month = c.get(Calendar.MONTH);
                        int day = c.get(Calendar.DAY_OF_MONTH) + 1;
                        DatePickerDialog datePickerDialog =
                                new DatePickerDialog(NewTaskActivity.this, reminderDateSetListener(),
                                        year, month, day);
                        datePickerDialog.show();
                        break;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private void showReminderTimeDropdownMenu() {
        PopupMenu popupMenu = new PopupMenu(this, fieldSetReminderTime);
        popupMenu.getMenuInflater().inflate(R.menu.popup_reminder_time, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.none:
                        fieldSetReminderTimeTextview.setText(getString(R.string.none));
                        reminderTimeMillis = 0;
                        break;
                    case R.id.morning:
                        fieldSetReminderTimeTextview.setText(getString(R.string.morning));
                        reminderTimeMillis = 3600 * 9 * 1000;
                        break;
                    case R.id.afternoon:
                        fieldSetReminderTimeTextview.setText(getString(R.string.afternoon));
                        reminderTimeMillis = 3600 * 14 * 1000;
                        break;
                    case R.id.evening:
                        fieldSetReminderTimeTextview.setText(getString(R.string.evening));
                        reminderTimeMillis = 3600 * 18 * 1000;
                        break;
                    case R.id.custom:
                        // If reminder time millis is set to none, pass the time +1 hour
                        int hour, minute;
                        if (reminderTimeMillis <= 0) {
                            Calendar c = Calendar.getInstance();
                            c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + 1);
                            hour = c.get(Calendar.HOUR_OF_DAY);
                            minute = c.get(Calendar.MINUTE);
                        } else {
                            hour = (int) reminderTimeMillis / 1000 / 3600;
                            minute = (int) ((reminderTimeMillis / 1000) % 3600) / 60;
                        }

                        TimePickerDialog timePickerFragment = new TimePickerDialog(NewTaskActivity.this, onTimeSetListener(), hour, minute, true);
                        timePickerFragment.show();
                        break;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private View.OnClickListener listener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.field_class_dropdown:
                        showClassDropdownMenu();
                        break;
                    case R.id.field_type_dropdown:
                        showTypeDropdownMenu();
                        break;
                    case R.id.field_new_task_duedate:
                    case R.id.field_new_task_duedate_textview:
                        Calendar c_duedate = Calendar.getInstance();
                        c_duedate.setTimeInMillis(dueDateMillis);
                        int year_duedate = c_duedate.get(Calendar.YEAR);
                        int month_duedate = c_duedate.get(Calendar.MONTH);
                        int day_duedate = c_duedate.get(Calendar.DAY_OF_MONTH);
                        DatePickerDialog datePickerDialog_duedate = new DatePickerDialog(NewTaskActivity.this, dueDateSetListener(), year_duedate, month_duedate, day_duedate);
                        datePickerDialog_duedate.show();
                        break;

                    case R.id.take_photo_text:
                    case R.id.take_photo_icon:
                        // Request all permissions (for API 23+)
                        int permissionCheckStorage = ContextCompat.checkSelfPermission(NewTaskActivity.this,
                                android.Manifest.permission.READ_EXTERNAL_STORAGE);
                        int permissionCheckCamera = ContextCompat.checkSelfPermission(NewTaskActivity.this,
                                Manifest.permission.CAMERA);

                        if (permissionCheckStorage != PackageManager.PERMISSION_GRANTED
                                || permissionCheckCamera != PackageManager.PERMISSION_GRANTED) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(NewTaskActivity.this, Manifest.permission.CAMERA)) {
                                //Show permission explanation dialog...
                                new AlertDialog.Builder(NewTaskActivity.this)
                                        .setMessage(getString(R.string.dialog_permission_rationale_take_photo))
                                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                ActivityCompat.requestPermissions(NewTaskActivity.this,
                                                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                                                                Manifest.permission.CAMERA},
                                                        REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
                                            }
                                        })
                                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                return;
                                            }
                                        })
                                        .show();
                            } else {
                                ActivityCompat.requestPermissions(NewTaskActivity.this,
                                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                                                Manifest.permission.CAMERA},
                                        REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
                            }
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NewTaskActivity.this);
                            builder.setTitle(getString(R.string.field_new_photo_dialog_title))
                                    .setItems(R.array.field_new_photo_dialog_items, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case 0:
                                                    try {
                                                        dispatchTakePictureIntent();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    break;
                                                case 1:
                                                    dispatchSelectPhotoIntent();
                                                    break;
                                            }
                                        }
                                    }).show();
                        }
                        break;
                    case R.id.field_new_task_reminder_date:
                    case R.id.field_new_task_reminder_date_textview:
                        showReminderDateDropdownMenu();
                        break;
                    case R.id.field_new_task_reminder_time_textview:
                    case R.id.field_new_task_reminder_time:
                        showReminderTimeDropdownMenu();
                        break;
                }
            }
        };
    }

    // Special ItemClickListener for when dueDate is set from dialog
    private DatePickerDialog.OnDateSetListener dueDateSetListener() {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar c = Calendar.getInstance();
                c.set(year, monthOfYear, dayOfMonth);
                dueDateMillis = c.getTimeInMillis();
                fieldDueDateTextView.setText(utility.formatDateString(NewTaskActivity.this, year, monthOfYear, dayOfMonth));
            }
        };
    }

    // This method is called when a date for the reminding notification is set
    private DatePickerDialog.OnDateSetListener reminderDateSetListener() {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Set reminder date to the chosen date
                Calendar c = Calendar.getInstance();
                c.set(year, monthOfYear, dayOfMonth, 0, 0);
                reminderDateMillis = c.getTimeInMillis();

                // Determine if the date corresponds to today or tomorrow
                Calendar currentDate = Calendar.getInstance();
                if (currentDate.getTimeInMillis() == reminderDateMillis)
                    fieldSetReminderDateTextview.setText(getString(R.string.today));
                else {
                    currentDate.set(Calendar.DAY_OF_MONTH, currentDate.get(Calendar.DAY_OF_MONTH) + 1);
                    if (currentDate.getTimeInMillis() == reminderDateMillis)
                        fieldSetReminderDateTextview.setText(getString(R.string.tomorrow));
                    else
                        fieldSetReminderDateTextview.setText(utility.formatDateString(NewTaskActivity.this, year, monthOfYear, dayOfMonth));
                }
            }
        };
    }

    // Special ItemClickListener used for reminder time
    public TimePickerDialog.OnTimeSetListener onTimeSetListener() {
        return new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                reminderTimeMillis = utility.timeToMillis(hourOfDay, minute);
                fieldSetReminderTimeTextview.setText(utility.millisToHourTime(NewTaskActivity.this, reminderTimeMillis));
            }
        };
    }

    // Action called when the Icon ImageView is clicked
    private View.OnClickListener showIconPrompt() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialog = new IconPromptDialog();
                dialog.show(getSupportFragmentManager(), "dialog");
            }
        };
    }

    // Action called when previewed photo is clicked
    private View.OnClickListener photoListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(NewTaskActivity.this);
                builder.setTitle(getString(R.string.field_existing_photo_dialog_title))
                        .setItems(R.array.field_existing_photo_dialog_items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        try {
                                            dispatchTakePictureIntent();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    case 1:
                                        dispatchSelectPhotoIntent();
                                        break;
                                    case 2:
                                        break;
                                }
                            }
                        }).show();
            }
        };
    }

    private void showBuiltInIconsDialog() {
        // Prepare grid view
        GridView gridView = new GridView(this);
        final AlertDialog dialog;

        int[] builtinIcons = getResources().getIntArray(R.array.builtin_icons);
        List<Integer> mList = new ArrayList<>();
        for (int i = 1; i < builtinIcons.length; i++) {
            mList.add(builtinIcons[i]);
        }

        gridView.setAdapter(new BuiltInSubjectIconsAdapter(this));
        gridView.setNumColumns(4);
        gridView.setPadding(0, 16, 0, 16);
        gridView.setGravity(Gravity.CENTER);
        // Set grid view to alertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(gridView);
        builder.setTitle(getString(R.string.new_schedule_icon_builtin_title));
        dialog = builder.show();
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int resId = mThumbIds[position];
                fieldIcon.setImageResource(resId);
                Resources resources = getResources();
                Uri drawableUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + resources.getResourcePackageName(resId)
                        + '/' + resources.getResourceTypeName(resId) + '/' + resources.getResourceEntryName(resId));
                iconUriString = drawableUri.toString();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private boolean insertTaskDataIntoDatabase() throws IOException {
        // Get the data
        final String title = fieldTitle.getText().toString();
        final String description = fieldDescription.getText().toString();

        if (classTitle.equals(getString(R.string.none)))
            classTitle = "";
        if (classType.equals(getString(R.string.none)))
            classType = "";

        // Insert the data based on the user
        if (mFirebaseUser != null) {
            // Insert into Firebase
            debounce("duetasksonline");
            final DatabaseReference taskRef;
            if (FLAG_EDIT)
                taskRef = FirebaseDatabase.getInstance().getReference()
                        .child("users").child(mUserId).child("tasks").child(firebaseEditId);
            else taskRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(mUserId).child("tasks").push();
            Map<String, Long> duedateMap = new HashMap<>();
            duedateMap.put("duedate", dueDateMillis);
            taskRef.setValue(duedateMap);
            taskRef.child("title").setValue(title);
            taskRef.child("class").setValue(classTitle);
            taskRef.child("type").setValue(classType);
            taskRef.child("sharer").setValue("");
            taskRef.child("description").setValue(description);
            taskRef.child("completed").setValue(false);

            // If FLAG_EDIT, attempt to delete custom icon if previously used
            if (FLAG_EDIT) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                StorageReference iconRef = storageRef.child(mUserId + "/tasks/" + taskRef.getKey());
                iconRef.delete();
            }

            if (customIconUploaded) {
                // Copy the file to the app's directory
                Uri imageUri = Uri.parse(iconUriString);
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                String filename = taskRef.getKey() + ".jpg";

                byte[] data = getBytes(inputStream);
                File file = new File(getFilesDir(), filename);
                FileOutputStream outputStream;
                try {
                    outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    outputStream.write(data);
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Image compression
                Bitmap decodedBitmap = decodeFile(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                decodedBitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
                byte[] bitmapData = baos.toByteArray();
                file.delete();
                File file1 = new File(getFilesDir(), filename);
                FileOutputStream fos = new FileOutputStream(file1);
                fos.write(bitmapData);
                fos.flush();
                fos.close();

                // Get the uri of the file so it can be saved
                iconUriString = Uri.fromFile(file1).toString();
            }

            taskRef.child("icon").setValue(iconUriString);

            // Upload the custom icon and photos
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference iconRef = storageRef.child(mUserId + "/tasks/" + taskRef.getKey());

            if (customIconUploaded) {
                fieldIcon.setDrawingCacheEnabled(true);
                fieldIcon.buildDrawingCache();
                Bitmap bitmap = fieldIcon.getDrawingCache();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                UploadTask uploadTask = iconRef.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // TODO: Handle unsuccessful uploads
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // TODO: Handle successful upload if any action is required
                    }
                });
            }

            // If FLAG_EDIT, delete all previous photos from database and storage
            if (FLAG_EDIT) {
                taskRef.child("photos").removeValue();
                storageRef.child(mUserId + "/tasks/" + title + "/photos/").delete();
            }

            // Photos upload
            for (int i = 0; i < photoUriList.size(); i++) {
                // Copy the photo into the app's local directory
                Uri imageUri = photoUriList.get(i);
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                String filename = title + "safechar" + i + ".jpg";
                byte[] data = getBytes(inputStream);
                File file = new File(getFilesDir(), filename);
                FileOutputStream outputStream;
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(data);
                outputStream.close();

                // Image compression
                Bitmap decodedBitmap = decodeFile(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                decodedBitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
                byte[] bitmapData = baos.toByteArray();
                file.delete();
                File file1 = new File(getFilesDir(), filename);
                FileOutputStream fos = new FileOutputStream(file1);
                fos.write(bitmapData);
                fos.flush();
                fos.close();

                // Get the uri of the file so it can be saved
                Uri uri = Uri.fromFile(file1);
                photoUriList.remove(i);
                photoUriList.add(i, uri);

                // Save the photos uri in the database
                taskRef.child("photos").child(Integer.toString(i)).setValue(uri);

                // Upload the and photos
                StorageReference iconRef1 = storageRef.child(mUserId + "/tasks/" + title + "/photos/" + String.valueOf(i));
                Bitmap bitmap = decodedBitmap;
                ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos1);
                byte[] data1 = baos1.toByteArray();

                UploadTask uploadTask = iconRef1.putBytes(data1);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // TODO: Handle unsuccessful uploads
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // TODO: Handle successful upload if any action is required
                    }
                });

            }

            // Set the alarm for the notification
            if (reminderDateMillis > 0)
                ScheduleNotification(reminderDateMillis + reminderTimeMillis, -1, taskRef.getKey(), getString(R.string.notification_message_reminder), title, false);

            // Add to Google Calendar if checked
            if (fieldAddCalendar.isChecked()) {
                // TODO: ADD CALENDAR FUNCTIONS
                // Add title, description, and due date to calendar
            }


            // Share the task to peers if checked shared
            if (fieldSharedCheckbox.isChecked()) {
                final ArrayList<String> peerUIDs = new ArrayList<>();
                DatabaseReference classPeersRef;
                if (classTitle.equals(getString(R.string.none)) || classTitle.equals(""))
                    classPeersRef = FirebaseDatabase.getInstance().getReference()
                            .child("users").child(mUserId).child("peers");
                else classPeersRef = FirebaseDatabase.getInstance().getReference()
                        .child("users").child(mUserId).child("classes").child(classTitle).child("peers");
                classPeersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        long snapshotCount = dataSnapshot.getChildrenCount();
                        for (DataSnapshot classSnapshot : dataSnapshot.getChildren()) {
                            peerUIDs.add(classSnapshot.getKey());
                            for (int i = 0; i < peerUIDs.size(); i++) {
                                // All the values are set here per peer
                                String peerUid = peerUIDs.get(i);
                                DatabaseReference peerTask = FirebaseDatabase.getInstance().getReference()
                                        .child("users").child(peerUid).child("tasks").push();
                                peerTask.child("title").setValue(title);
                                peerTask.child("class").setValue(classTitle);
                                peerTask.child("type").setValue(classType);
                                peerTask.child("sharer").setValue(name);
                                peerTask.child("description").setValue(description);
                                peerTask.child("duedate").setValue(dueDateMillis);
                                peerTask.child("icon").setValue(iconUriString);
                                peerTask.child("completed").setValue(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }
            return true;
        } else {
            // Insert into SQLite and get the id of the task
            DbHelper dbHelper = new DbHelper(this);
            int id;

            if (FLAG_EDIT) {
                // Update database row
                if (dbHelper.updateTaskItem(this, editId, title, classTitle, classType, description, attachedFileUriString,
                        dueDateMillis, reminderDateMillis, reminderTimeMillis, iconUriString, photoUriList, false)) {
                    id = editId;

                    // Cancel the old notification
                    if (oldReminderDateMillis > 0)
                        ScheduleNotification(reminderDateMillis + reminderTimeMillis, id, "", getString(R.string.notification_message_reminder), title, true);

                    // Set the alarm for the notification
                    if (reminderDateMillis > 0)
                        ScheduleNotification(reminderDateMillis + reminderTimeMillis, editId, "",
                                getString(R.string.notification_message_reminder), title, false);

                    return true;
                }
            } else {
                // Insert a new database row
                debounce("duetasksoffline");
                id = (int) dbHelper.insertTask(this, title, classTitle, classType, description, attachedFileUriString,
                        dueDateMillis, reminderDateMillis, reminderTimeMillis, iconUriString, photoUriList, false);

                // Schedule a notification
                if (reminderDateMillis > 0)
                    ScheduleNotification(reminderDateMillis + reminderTimeMillis, id, "", getString(R.string.notification_message_reminder), title, false);

                if (id > -1)
                    return true;
                else Log.d(LOG_TAG, "Task not inserted");
            }

            // Copy the icon into a seperate file if custom
            if (customIconUploaded) {
                Uri imageUri = Uri.parse(iconUriString);
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                String filename = editId + ".jpg";
                byte[] data = getBytes(inputStream);
                File file = new File(getFilesDir(), filename);
                FileOutputStream outputStream;
                try {
                    outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    outputStream.write(data);
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Image compression
                Bitmap decodedBitmap = decodeFile(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                decodedBitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
                byte[] bitmapData = baos.toByteArray();
                file.delete();
                File file1 = new File(getFilesDir(), filename);
                FileOutputStream fos = new FileOutputStream(file1);
                fos.write(bitmapData);
                fos.flush();
                fos.close();

                // Get the uri of the file so it can be saved
                iconUriString = Uri.fromFile(file1).toString();
            }

            // Copy over photos
            for (int i = 0; i < photoUriList.size(); i++) {
                // Copy the photo into the app's local directory
                Uri imageUri = photoUriList.get(i);
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                String filename = title + "safechar" + i + ".jpg";
                byte[] data = getBytes(inputStream);
                File file = new File(getFilesDir(), filename);
                FileOutputStream outputStream;
                try {
                    outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    outputStream.write(data);
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Image compression
                Bitmap decodedBitmap = decodeFile(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                decodedBitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
                byte[] bitmapData = baos.toByteArray();
                file.delete();
                File file1 = new File(getFilesDir(), filename);
                FileOutputStream fos = new FileOutputStream(file1);
                fos.write(bitmapData);
                fos.flush();
                fos.close();

                // Get the uri of the file so it can be saved
                Uri uri = Uri.fromFile(file1);
                photoUriList.remove(i);
                photoUriList.add(i, uri);
            }

            return false;
        }
    }

    private Bitmap decodeFile(File f) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            // The new size we want to scale to
            final int REQUIRED_SIZE = 300;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
        }
        return null;
    }

    private void ScheduleNotification(final long millis, final int ID, final String firebaseID,
                                      final String title, final String message, final boolean cancel) {
        // TEST SEQUENCE
        Calendar test = Calendar.getInstance();
        test.setTimeInMillis(millis);
        Log.v(LOG_TAG, "Notification scheduled for " + test.get(Calendar.DAY_OF_MONTH) + "/"
                + test.get(Calendar.MONTH) + "/"
                + test.get(Calendar.YEAR) + " "
                + test.get(Calendar.HOUR_OF_DAY) + ":"
                + test.get(Calendar.MINUTE));

        final android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Bitmap largeIcon = null;
        try {
            largeIcon = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(iconUriString));
        } catch (IOException e) {
            e.printStackTrace();
        }
        final android.support.v4.app.NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender()
                .setBackground(largeIcon);

        Intent contentIntent = new Intent(this, TasksDetailActivity.class);
        if (ID > -1)
            contentIntent.putExtra("_ID", ID);
        else contentIntent.putExtra("id", firebaseID);
        contentIntent.putExtra("icon", iconUriString);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(TasksDetailActivity.class);
        stackBuilder.addNextIntent(contentIntent);
        final PendingIntent contentPendingIntent = stackBuilder.getPendingIntent
                (REQUEST_NOTIFICATION_INTENT, PendingIntent.FLAG_UPDATE_CURRENT);

        Palette.generateAsync(largeIcon, new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                builder.setContentIntent(contentPendingIntent)
                        .setSmallIcon(R.drawable.ic_assignment)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentText(title)
                        .setContentTitle(message)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .extend(wearableExtender)
                        .setDefaults(Notification.DEFAULT_ALL);

                Notification notification = builder.build();

                Intent notificationIntent = new Intent(NewTaskActivity.this, NotificationPublisher.class);
                notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 60);
                notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(NewTaskActivity.this, REQUEST_NOTIFICATION_ALARM,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (cancel)
                    alarmManager.cancel(pendingIntent);
                else
                    alarmManager.set(AlarmManager.RTC, millis, pendingIntent);
            }
        });
    }

    private void dispatchTakePictureIntent() throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = createImageFile();
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.pdt.plume.fileprovider",
                        photoFile);
                mTempPhotoUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void dispatchSelectPhotoIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, REQUEST_IMAGE_GET_PHOTO);
    }

    private File createImageFile() throws IOException {
        // Create an image file category
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    // Interface method when icon is selected from built-in icons
    @Override
    public void OnIconListItemSelected(int item) {
        switch (item) {
            case 0:
                showBuiltInIconsDialog();
                break;
            case 1:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                if (intent.resolveActivity(getPackageManager()) != null)
                    startActivityForResult(intent, REQUEST_IMAGE_GET_ICON);
                break;
        }
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    boolean debounce(String tag) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        long lastCheckDate = preferences.getLong("lastCheckDate$tag", 0);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(lastCheckDate);
        int second = c.get(Calendar.SECOND);
        Calendar cc = Calendar.getInstance();
        cc.setTimeInMillis(System.currentTimeMillis());
        int cSecond = cc.get(Calendar.SECOND);

        preferences.edit().putLong("lastCheckDate" + tag, System.currentTimeMillis()).apply();

        return cSecond > second + 5;
    }

    @Override
    public void onPermissionsGranted(int i, List<String> list) {

    }

    @Override
    public void onPermissionsDenied(int i, List<String> list) {

    }

}

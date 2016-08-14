package com.pdt.plume;


import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import org.w3c.dom.Text;

import java.util.Calendar;


public class ClassTimeThreeFragmentTime extends Fragment
        implements TimePickerDialog.OnTimeSetListener{

    // Constantly used variables
    Utility utility = new Utility();

    // UI Elements
    EditText fieldTimeIn;
    EditText fieldTimeOut;
    EditText fieldTimeInAlt;
    EditText fieldTimeOutAlt;

    // Days and time data variables
    int[] isButtonChecked = {0, 0, 0, 0, 0, 0, 0};
    int timeInSeconds;
    int timeOutSeconds;
    int timeInAltSeconds;
    int timeOutAltSeconds;

    public static int timeInHour;
    public static int timeOutHour;
    public static int timeInAltHour;
    public static int timeOutAltHour;

    // View IDs passed along activities
    int resourceId = -1;

    // Interface variables
    onDaysSelectedListener daysSelectedListener;
    onTimeSelectedListener timeSelectedListener;
    onBasisTextviewSelectedListener basisTextviewSelectedListener;
    onWeektypeTextviewSelectedListener weektypeTextviewSelectedListener;

    // Required empty public constructor
    public ClassTimeThreeFragmentTime() {
        // Required empty public constructor
    }

    // Interfaces used to pass data to NewScheduleActivity
    public interface onDaysSelectedListener {
        //Pass all data through input params here
        public void onDaysSelected(String classDays, int timeInSeconds, int timeOutSeconds, int timeInAltSeconds, int timeOutAltSeconds, String periods);
    }
    public interface onTimeSelectedListener {
        public void onTimeSelected(int resourceId, int previousTimeInSeconds, int previousTimeOutSeconds, int previousTimeInAltSeconds, int previousTimeOutAltSeconds, int[] buttonsChecked);
    }
    public interface onBasisTextviewSelectedListener {
        //Pass all data through input params here
        public void onBasisTextviewSelected();
    }
    public interface onWeektypeTextviewSelectedListener {
        //Pass all data through input params here
        public void onWeektypeTextViewSelectedListener(String basis);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            daysSelectedListener = (onDaysSelectedListener) context;
            timeSelectedListener = (onTimeSelectedListener) context;
            basisTextviewSelectedListener = (onBasisTextviewSelectedListener) context;
            weektypeTextviewSelectedListener = (onWeektypeTextviewSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onSomeEventListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.class_time_three_time, container, false);

        // Get references to each UI element
        TextView basisTextView = (TextView) rootView.findViewById(R.id.class_time_one_value);
        TextView weekTypeTextView = (TextView) rootView.findViewById(R.id.class_time_two_value);

        ImageView sunday = (ImageView) rootView.findViewById(R.id.class_three_sunday);
        ImageView monday = (ImageView) rootView.findViewById(R.id.class_three_monday);
        ImageView tuesday = (ImageView) rootView.findViewById(R.id.class_three_tuesday);
        ImageView wednesday = (ImageView) rootView.findViewById(R.id.class_three_wednesday);
        ImageView thursday = (ImageView) rootView.findViewById(R.id.class_three_thursday);
        ImageView friday = (ImageView) rootView.findViewById(R.id.class_three_friday);
        ImageView saturday = (ImageView) rootView.findViewById(R.id.class_three_saturday);
        fieldTimeIn = (EditText) rootView.findViewById(R.id.field_new_schedule_timein);
        fieldTimeOut = (EditText) rootView.findViewById(R.id.field_new_schedule_timeout);
        Button done = (Button) rootView.findViewById(R.id.class_three_done);

        ImageView sundayAlt = (ImageView) rootView.findViewById(R.id.class_three_sunday_alt);
        ImageView mondayAlt = (ImageView) rootView.findViewById(R.id.class_three_monday_alt);
        ImageView tuesdayAlt = (ImageView) rootView.findViewById(R.id.class_three_tuesday_alt);
        ImageView wednesdayAlt = (ImageView) rootView.findViewById(R.id.class_three_wednesday_alt);
        ImageView thursdayAlt = (ImageView) rootView.findViewById(R.id.class_three_thursday_alt);
        ImageView fridayAlt = (ImageView) rootView.findViewById(R.id.class_three_friday_alt);
        ImageView saturdayAlt = (ImageView) rootView.findViewById(R.id.class_three_saturday_alt);
        fieldTimeInAlt = (EditText) rootView.findViewById(R.id.field_new_schedule_timein_alt);
        fieldTimeOutAlt = (EditText) rootView.findViewById(R.id.field_new_schedule_timeout_alt);

        // Set OnClickListeners to the UI elements
         basisTextView.setOnClickListener(listener());
        weekTypeTextView.setOnClickListener(listener());

        sunday.setOnClickListener(listener());
        monday.setOnClickListener(listener());
        tuesday.setOnClickListener(listener());
        wednesday.setOnClickListener(listener());
        thursday.setOnClickListener(listener());
        friday.setOnClickListener(listener());
        saturday.setOnClickListener(listener());
        fieldTimeIn.setOnClickListener(showTimePickerDialog());
        fieldTimeOut.setOnClickListener(showTimePickerDialog());
        done.setOnClickListener(listener());

        sundayAlt.setOnClickListener(listener());
        mondayAlt.setOnClickListener(listener());
        tuesdayAlt.setOnClickListener(listener());
        wednesdayAlt.setOnClickListener(listener());
        thursdayAlt.setOnClickListener(listener());
        fridayAlt.setOnClickListener(listener());
        saturdayAlt.setOnClickListener(listener());
        fieldTimeInAlt.setOnClickListener(showTimePickerDialog());
        fieldTimeOutAlt.setOnClickListener(showTimePickerDialog());

        // Get the arguments of the fragment.
        // Hide the alternate layout if the week type selected is 0 (Same time every week) and set the hyperlink week type text to the selected week type text
        Bundle args = getArguments();
        if (args != null){
            if (!args.getString("weekType", "-1").equals("1")){
                // If weekType
                rootView.findViewById(R.id.class_time_three_week_type_alt_layout).setVisibility(View.GONE);
                weekTypeTextView.setText(getString(R.string.class_time_two_sameweek));
            } else  weekTypeTextView.setText(getString(R.string.class_time_two_altweeks));

            // Check if the fragment was launched from the OnTimeSet override method in NewScheduleActivity
            // If it is, get the fragment's previous state data and update the fragment data and UI accordingly
            // If the fragment contains the 'hourOfDay' string, it must contain other previous state data
            if (args.containsKey("hourOfDay")){
                // Get previous state data
                int hourOfDay = args.getInt("hourOfDay");
                int minute = args.getInt("minute");
                int previousTimeInSeconds = args.getInt("timeInSeconds");
                int previousTimeOutSeconds = args.getInt("timeOutSeconds");
                int previousTimeInAltSeconds = args.getInt("timeInAltSeconds");
                int previousTimeOutAltSeconds = args.getInt("timeOutAltSeconds");
                isButtonChecked = args.getIntArray("buttonsChecked");

                // Set the default values of the time fields accordingly
                // as well as update the fragment's global variables of time
                // Global variables updated: timeInSeconds, timeOutSeconds, timeInAltSeconds, timeOutAltSeconds
                switch (args.getInt("resourceId")){
                    case R.id.field_new_schedule_timein:
                        timeInSeconds = utility.timeToSeconds(hourOfDay, minute);
                        timeOutSeconds = previousTimeOutSeconds;
                        timeInAltSeconds = previousTimeInAltSeconds;
                        timeOutAltSeconds = previousTimeOutAltSeconds;
                        if (minute < 10)
                            fieldTimeIn.setText(hourOfDay + ":0" + minute);
                        else
                            fieldTimeIn.setText(hourOfDay + ":" + minute);
                        fieldTimeOut.setText(utility.secondsToTime(previousTimeOutSeconds));
                        fieldTimeInAlt.setText(utility.secondsToTime(previousTimeInAltSeconds));
                        fieldTimeOutAlt.setText(utility.secondsToTime(previousTimeOutAltSeconds));
                        break;

                    case R.id.field_new_schedule_timeout:
                        timeInSeconds = previousTimeInSeconds;
                        timeOutSeconds = utility.timeToSeconds(hourOfDay, minute);
                        timeInAltSeconds = previousTimeInAltSeconds;
                        timeOutAltSeconds = previousTimeOutAltSeconds;
                        if (minute < 10)
                            fieldTimeOut.setText(hourOfDay + ":0" + minute);
                        else
                            fieldTimeOut.setText(hourOfDay + ":" + minute);
                        fieldTimeIn.setText(utility.secondsToTime(previousTimeInSeconds));
                        fieldTimeInAlt.setText(utility.secondsToTime(previousTimeInAltSeconds));
                        fieldTimeOutAlt.setText(utility.secondsToTime(previousTimeOutAltSeconds));
                        break;

                    case R.id.field_new_schedule_timein_alt:
                        timeInSeconds = previousTimeInSeconds;
                        timeOutSeconds = previousTimeOutSeconds;
                        timeInAltSeconds = utility.timeToSeconds(hourOfDay, minute);
                        timeOutAltSeconds = previousTimeOutAltSeconds;
                        if (minute < 10)
                            fieldTimeInAlt.setText(hourOfDay + ":0" + minute);
                        else
                            fieldTimeInAlt.setText(hourOfDay + ":" + minute);
                        fieldTimeIn.setText(utility.secondsToTime(previousTimeInSeconds));
                        fieldTimeOut.setText(utility.secondsToTime(previousTimeOutSeconds));
                        fieldTimeOutAlt.setText(utility.secondsToTime(previousTimeOutAltSeconds));
                        break;

                    case R.id.field_new_schedule_timeout_alt:
                        timeInSeconds = previousTimeInSeconds;
                        timeOutSeconds = previousTimeOutSeconds;
                        timeInAltSeconds = previousTimeInAltSeconds;
                        timeOutAltSeconds = utility.timeToSeconds(hourOfDay, minute);
                        if (minute < 10)
                            fieldTimeOutAlt.setText(hourOfDay + ":0" + minute);
                        else
                            fieldTimeOutAlt.setText(hourOfDay + ":" + minute);
                        fieldTimeIn.setText(utility.secondsToTime(previousTimeInSeconds));
                        fieldTimeOut.setText(utility.secondsToTime(previousTimeOutSeconds));
                        fieldTimeInAlt.setText(utility.secondsToTime(previousTimeInAltSeconds));
                        break;
                }
            }
            // If the fragment was not restarted from the OnTimeSet override method in NewScheduleActivity
            // Set the global variables for the time based on the current time
            // And update the UI elements accordingly
            else {
                Calendar c = Calendar.getInstance();
                // Set the default value of the global time variables
                timeInHour = c.get(Calendar.HOUR_OF_DAY) + 1;
                timeOutHour = c.get(Calendar.HOUR_OF_DAY) + 2;
                timeInAltHour = c.get(Calendar.HOUR_OF_DAY) + 1;
                timeOutAltHour = c.get(Calendar.HOUR_OF_DAY) + 2;
                timeInSeconds = utility.timeToSeconds(timeInHour, 0);
                timeOutSeconds = utility.timeToSeconds(timeOutHour, 0);
                timeInAltSeconds = utility.timeToSeconds(timeInAltHour, 0);
                timeOutAltSeconds = utility.timeToSeconds(timeOutAltHour, 0);
                fieldTimeIn.setText(timeInHour + ":00");
                fieldTimeOut.setText(timeOutHour + ":00");
                fieldTimeInAlt.setText(timeInAltHour + ":00");
                fieldTimeOutAlt.setText(timeOutAltHour + ":00");
            }
        }

        // Set the text of the hyperlink basis text to the time based string annotation
        basisTextView.setText(getString(R.string.class_time_one_timebased));

        return rootView;
    }

    private View.OnClickListener listener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    // In the case that it's one of the day buttons
                    case R.id.class_three_sunday:
                        if (isButtonChecked[0] == 0){
                            isButtonChecked[0] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_selected);
                        }
                        else if (isButtonChecked[0] == 1){
                            isButtonChecked[0] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_unselected);
                        }
                        else if (isButtonChecked[0] == 2){
                            isButtonChecked[0] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_selected);
                        }
                        else if (isButtonChecked[0] == 3){
                            isButtonChecked[0] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_unselected);
                        }
                        break;
                    case R.id.class_three_monday:
                        if (isButtonChecked[1] == 0){
                            isButtonChecked[1] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_monday_selected);
                        }
                        else if (isButtonChecked[1] == 1){
                            isButtonChecked[1] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_monday_unselected);
                        }
                        else if (isButtonChecked[1] == 2){
                            isButtonChecked[1] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_monday_selected);
                        }
                        else if (isButtonChecked[1] == 3){
                            isButtonChecked[1] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_monday_unselected);
                        }
                        break;
                    case R.id.class_three_tuesday:
                        if (isButtonChecked[2] == 0){
                            isButtonChecked[2] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_selected);
                        }
                        else if (isButtonChecked[2] == 1){
                            isButtonChecked[2] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_unselected);
                        }
                        else if (isButtonChecked[2] == 2) {
                            isButtonChecked[2] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_selected);
                        }
                        else if (isButtonChecked[2] == 3){
                            isButtonChecked[2] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_unselected);
                        }
                        break;
                    case R.id.class_three_wednesday:
                        if (isButtonChecked[3] == 0) {
                            isButtonChecked[3] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_wednesday_selected);
                        }
                        else if (isButtonChecked[3] == 1){
                            isButtonChecked[3] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_wednesday_unselected);
                        }
                        else if (isButtonChecked[3] == 2){
                            isButtonChecked[3] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_wednesday_selected);
                        }
                        else if (isButtonChecked[3] == 3){
                            isButtonChecked[3] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_wednesday_unselected);
                        }
                        break;
                    case R.id.class_three_thursday:
                        if (isButtonChecked[4] == 0){
                            isButtonChecked[4] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_selected);
                        }
                        else if (isButtonChecked[4] == 1){
                            isButtonChecked[4] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_unselected);
                        }
                        else if (isButtonChecked[4] == 2){
                            isButtonChecked[4] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_selected);
                        }
                        else if (isButtonChecked[4] == 3){
                            isButtonChecked[4] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_unselected);
                        }
                        break;
                    case R.id.class_three_friday:
                        if (isButtonChecked[5] == 0){
                            isButtonChecked[5] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_friday_selected);
                        }
                        else if (isButtonChecked[5] == 1){
                            isButtonChecked[5] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_friday_unselected);
                        }
                        else if (isButtonChecked[5] == 2){
                            isButtonChecked[5] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_friday_selected);
                        }
                        else if (isButtonChecked[5] == 3){
                            isButtonChecked[5] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_friday_unselected);
                        }
                        break;
                    case R.id.class_three_saturday:
                        if (isButtonChecked[6] == 0){
                            isButtonChecked[6] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_selected);
                        }
                        else if (isButtonChecked[6] == 1){
                            isButtonChecked[6] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_unselected);
                        }
                        else if (isButtonChecked[6] == 2){
                            isButtonChecked[6] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_selected);
                        }
                        else if (isButtonChecked[6] == 3){
                            isButtonChecked[6] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_unselected);
                        }
                        break;

                    // In the case that it's one of the alternate day buttons
                    case R.id.class_three_sunday_alt:
                        if (isButtonChecked[0] == 0){
                            isButtonChecked[0] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_selected);
                        }
                        else if (isButtonChecked[0] == 1){
                            isButtonChecked[0] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_selected);
                        }
                        else if (isButtonChecked[0] == 2){
                            isButtonChecked[0] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_unselected);
                        }
                        else if (isButtonChecked[0] == 3){
                            isButtonChecked[0] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_unselected);
                        }
                        break;
                    case R.id.class_three_monday_alt:
                        if (isButtonChecked[1] == 0){
                            isButtonChecked[1] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_monday_selected);
                        }
                        else if (isButtonChecked[1] == 1){
                            isButtonChecked[1] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_monday_selected);
                        }
                        else if (isButtonChecked[1] == 2){
                            isButtonChecked[1] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_monday_unselected);
                        }
                        else if (isButtonChecked[1] == 3){
                            isButtonChecked[1] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_monday_unselected);
                        }
                        break;
                    case R.id.class_three_tuesday_alt:
                        if (isButtonChecked[2] == 0){
                            isButtonChecked[2] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_selected);
                        }
                        else if (isButtonChecked[2] == 1){
                            isButtonChecked[2] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_selected);
                        }
                        else if (isButtonChecked[2] == 2){
                            isButtonChecked[2] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_unselected);
                        }
                        else if (isButtonChecked[2] == 3){
                            isButtonChecked[2] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_unselected);
                        }
                        break;
                    case R.id.class_three_wednesday_alt:
                        if (isButtonChecked[3] == 0){
                            isButtonChecked[3] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_wednesday_selected);
                        }
                        else if (isButtonChecked[3] == 1){
                            isButtonChecked[3] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_wednesday_selected);
                        }
                        else if (isButtonChecked[3] == 2){
                            isButtonChecked[3] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_wednesday_unselected);
                        }
                        else if (isButtonChecked[3] == 3){
                            isButtonChecked[3] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_wednesday_unselected);
                        }
                        break;
                    case R.id.class_three_thursday_alt:
                        if (isButtonChecked[4] == 0){
                            isButtonChecked[4] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_selected);
                        }
                        else if (isButtonChecked[4] == 1){
                            isButtonChecked[4] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_selected);
                        }
                        else if (isButtonChecked[4] == 2){
                            isButtonChecked[4] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_unselected);
                        }
                        else if (isButtonChecked[4] == 3){
                            isButtonChecked[4] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_tuesday_thursday_unselected);
                        }
                        break;
                    case R.id.class_three_friday_alt:
                        if (isButtonChecked[5] == 0){
                            isButtonChecked[5] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_friday_selected);
                        }
                        else if (isButtonChecked[5] == 1){
                            isButtonChecked[5] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_friday_selected);
                        }
                        else if (isButtonChecked[5] == 2){
                            isButtonChecked[5] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_friday_unselected);
                        }
                        else if (isButtonChecked[5] == 3){
                            isButtonChecked[5] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_friday_unselected);
                        }
                        break;
                    case R.id.class_three_saturday_alt:
                        if (isButtonChecked[6] == 0){
                            isButtonChecked[6] = 2;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_selected);
                        }
                        else if (isButtonChecked[6] == 1){
                            isButtonChecked[6] = 3;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_selected);
                        }
                        else if (isButtonChecked[6] == 2){
                            isButtonChecked[6] = 0;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_unselected);
                        }
                        else if (isButtonChecked[6] == 3){
                            isButtonChecked[6] = 1;
                            ((ImageView) v).setImageResource(R.drawable.ui_saturday_sunday_unselected);
                        }
                        break;

                    // In the case that it's one of the hyperlink text views to the
                    // previous stages of the add class time process
                    case R.id.class_time_one_value:
                        basisTextviewSelectedListener.onBasisTextviewSelected();
                        break;

                    // 0 is the fixed value passed as the basis because the activity itself
                    // (TimeBased) was launched as a result of the basis being 0
                    case R.id.class_time_two_value:
                        weektypeTextviewSelectedListener.onWeektypeTextViewSelectedListener("0");
                        break;

                    // In the case that the 'Done' button was clicked.
                    // This runs the interface that leads to the insertion of data into the database
                    // and into the list view in the NewScheduleActivity
                    case R.id.class_three_done:
                        String classDays = processClassDaysString();
                        daysSelectedListener.onDaysSelected(classDays, timeInSeconds, timeOutSeconds, timeInAltSeconds, timeOutAltSeconds, "-1");
                        break;
                }
            }
        };
    }

    private View.OnClickListener showTimePickerDialog() {
        // OnClickListener set to launch a TimePickerFragment to input the time on a particular field
        // When this happens, data of the current state of the fragment is sent to the NewScheduleActivity
        // As the TimeSetListener can only be implemented in an activity and not a fragment
        // Therefore upon Time Set, the fragment is restarted along with data sent through the interface
        // and data from the TimePickerDialog and the UI elements are updated with the corresponding data

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the id of the view clicked. This id is the view whose text will be updated
                // Upon restart of the fragment
                resourceId = v.getId();

                // Launch a new TimePickerFragment
                DialogFragment timePickerFragment = new TimePickerFragment();
                if (resourceId != -1)
                    timePickerFragment.show(getActivity().getSupportFragmentManager(), "time picker");

                // Launch the interface to send the fragment's current state data to the activity
                timeSelectedListener.onTimeSelected(resourceId, timeInSeconds, timeOutSeconds, timeInAltSeconds, timeOutAltSeconds, isButtonChecked);
            }
        };
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        String timeString;
        if (minute < 10)
            timeString = hourOfDay + ":0" + minute;
        else
            timeString = hourOfDay + ":" + minute;
        switch (resourceId) {
            case R.id.field_new_schedule_timein:
                timeInSeconds = utility.timeToSeconds(hourOfDay, minute);
                fieldTimeIn.setText(timeString);
                break;
            case R.id.field_new_schedule_timeout:
                timeOutSeconds = utility.timeToSeconds(hourOfDay, minute);
                fieldTimeOut.setText(timeString);
                break;
            case R.id.field_new_schedule_timein_alt:
                timeInAltSeconds = utility.timeToSeconds(hourOfDay, minute);
                fieldTimeInAlt.setText(timeString);
                break;
            case R.id.field_new_schedule_timeout_alt:
                timeOutAltSeconds = utility.timeToSeconds(hourOfDay, minute);
                fieldTimeOutAlt.setText(timeString);
                break;

        }
    }

    private String processClassDaysString(){
        return isButtonChecked[0] + ":"
                + isButtonChecked[1] + ":"
                + isButtonChecked[2] + ":"
                + isButtonChecked[3] + ":"
                + isButtonChecked[4] + ":"
                + isButtonChecked[5] + ":"
                + isButtonChecked[6];
    }

}
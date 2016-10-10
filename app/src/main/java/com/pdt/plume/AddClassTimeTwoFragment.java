package com.pdt.plume;


import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class AddClassTimeTwoFragment extends DialogFragment {

    onBasisTextviewSelectedListener basisTextviewSelectedListener;

    // Public Constructor
    public static AddClassTimeTwoFragment newInstance(int title) {
        AddClassTimeTwoFragment fragment = new AddClassTimeTwoFragment();
        Bundle args = new Bundle();
        args.putInt("title", title);
        fragment.setArguments(args);
        return fragment;
    }

    public interface onWeekTypeSelectedListener {
        public void onWeekTypeSelected(String weekType);
    }

    onWeekTypeSelectedListener weekTypeSelectedListener;

    public interface onBasisTextviewSelectedListener {
        //Pass all data through input params here
        public void onBasisTextviewSelected();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            weekTypeSelectedListener = (onWeekTypeSelectedListener) context;
            basisTextviewSelectedListener = (onBasisTextviewSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onSomeEventListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set the fragment's window size to match the screen
        Window window = this.getDialog().getWindow();
        window.setLayout(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
     }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.add_class_time_two, container, false);
        Bundle args = getArguments();

        // Get references to each UI element
        LinearLayout weekSameButton = (LinearLayout) rootView.findViewById(R.id.class_two_weeksame);
        LinearLayout weekAltButton = (LinearLayout) rootView.findViewById(R.id.class_two_weekalt);

        // Set the OnClickListener of each UI element
        weekSameButton.setOnClickListener(listener());
        weekAltButton.setOnClickListener(listener());

        // Set the basis text above the header
        TextView basisTextview = (TextView) rootView.findViewById(R.id.basis_textview);
        basisTextview.setOnClickListener(listener());
        basisTextview.setTextColor(PreferenceManager.getDefaultSharedPreferences(getContext()).
                getInt(getString(R.string.KEY_THEME_SECONDARY_COLOR), getResources().getColor(R.color.colorAccent)));
        String basis = args.getString("basis");
        if (basis.equals("0"))
            basisTextview.setText(getString(R.string.class_time_one_timebased));
        else if (basis.equals("1"))
            basisTextview.setText(getString(R.string.class_time_one_periodbased));

        return rootView;
    }

    private View.OnClickListener listener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    // Run the interface sending the week classType parameter
                    // based on the button selected
                    case R.id.class_two_weeksame:
                        weekTypeSelectedListener.onWeekTypeSelected("0");
                        break;
                    case R.id.class_two_weekalt:
                        weekTypeSelectedListener.onWeekTypeSelected("1");
                        break;
                    case R.id.basis_textview:
                        basisTextviewSelectedListener.onBasisTextviewSelected();
                        break;

                }
            }
        };
    }

}

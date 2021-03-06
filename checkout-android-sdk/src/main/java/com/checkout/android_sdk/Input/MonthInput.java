package com.checkout.android_sdk.Input;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.checkout.android_sdk.R;
import com.checkout.android_sdk.Store.DataStore;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom Spinner with handling of card expiration month input
 */
public class MonthInput extends androidx.appcompat.widget.AppCompatSpinner {

    public interface MonthListener {
        void onMonthInputFinish(String month);
    }

    // enum with month is different formats
    public enum Months {
        JANUARY("يناير", 1, "01"),
        FEBRUARY("فبراير", 2, "02"),
        MARCH("مارس", 3, "03"),
        APRIL("ابريل", 4, "04"),
        MAY("مايو", 5, "05"),
        JUNE("يونيو", 6, "06"),
        JULY("يوليو", 7, "07"),
        AUGUST("اغسطس", 8, "08"),
        SEPTEMBER("سبتمبر", 9, "09"),
        OCTOBER("اكتوبر", 10, "10"),
        NOVEMBER("نوفمبر", 11, "11"),
        DECEMBER("ديسمبر", 12, "12");

        public final String name;
        public final int number;
        public final String numberString;


        Months(String name, int number, String numberString) {
            this.name = name;
            this.number = number;
            this.numberString = numberString;
        }

    }

    private @Nullable
    MonthInput.MonthListener mMonthInputListener;
    private Context mContext;
    private DataStore mDatastore = DataStore.getInstance();

    public MonthInput(Context context) {
        this(context, 0);
    }

    public MonthInput(Context context, int mode) {
        this(context, null);
    }


    public MonthInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    /**
     * The UI initialisation
     * <p>
     * Used to initialise element as well as setting up appropriate listeners
     */
    private void init() {
        // Options needed for focus context switching
        setFocusable(true);
        setFocusableInTouchMode(true);

        populateSpinner();

        setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MonthInput.Months[] months = MonthInput.Months.values();

                mDatastore.setCardMonth(months[position].numberString);

                for (int i = 0; i < 12; i++) {
                    if (mMonthInputListener != null && months[i].number - 1 == position) {
                        mMonthInputListener.onMonthInputFinish(months[i].numberString);
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    performClick();
                    @Nullable InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        });

        // Remove extra padding left
        setPadding(this.getPaddingLeft(), this.getPaddingTop(), 0, this.getPaddingBottom());

    }

    /**
     * Populate the spinner with all the month of the year
     */
    public void populateSpinner() {
        MonthInput.Months[] months = MonthInput.Months.values();

        List<String> monthElements = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            monthElements.add(months[i].name + " - " + months[i].numberString);
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(mContext,
                R.layout.simple_spinner_item, monthElements);
        dataAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        setAdapter(dataAdapter);
    }

    /**
     * Used to set the callback listener for when the month input is completed
     */
    public void setMonthListener(MonthInput.MonthListener listener) {
        this.mMonthInputListener = listener;
    }
}

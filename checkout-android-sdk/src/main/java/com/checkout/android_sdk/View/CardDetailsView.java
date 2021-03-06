package com.checkout.android_sdk.View;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.widget.Toolbar;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.checkout.android_sdk.Input.BillingInput;
import com.checkout.android_sdk.Input.CardInput;
import com.checkout.android_sdk.Input.DefaultInput;
import com.checkout.android_sdk.Input.MonthInput;
import com.checkout.android_sdk.Input.YearInput;
import com.checkout.android_sdk.R;
import com.checkout.android_sdk.Store.DataStore;
import com.checkout.android_sdk.Utils.CardUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The controller of the card details view page
 * <p>
 * This class handles interaction with the custom inputs in the card details form.
 * The state of the view is handled here, so are action like focus changes, full form
 * validation, listeners, persistence over orientation.
 */
public class CardDetailsView extends LinearLayout {

    /**
     * The callback used to indicate the form submission
     * <p>
     * After the user completes their details and the form is valid this callback will
     * be used to communicate to the parent and start the necessary API call(s).
     */
    public interface DetailsCompleted {
        void onDetailsCompleted();
    }

    /**
     * The callback used to indicate the view needs to moved to the billing details page
     * <p>
     * When the user selects the option to add billing details this callback will be used
     * to communicate to the parent the focus change is requested
     */
    public interface GoToBillingListener {
        void onGoToBillingPressed();
    }

    /**
     * The callback is used to communicate with the card input
     * <p>
     * The custom {@link CardInput} takes care of the validation and it uses a callback
     * to indicate this controller if there is any error or if the error state needs to
     * be cleared. State is also updates based on the outcome of the input.
     */
    private final CardInput.Listener mCardInputListener = new CardInput.Listener() {
        @Override
        public void onCardInputFinish(String number) {
            mDataStore.setValidCardNumber(true);
        }

        @Override
        public void onCardError() {
            mCardLayout.setError(getResources().getString(R.string.error_card_number));
            mDataStore.setValidCardNumber(false);
        }

        @Override
        public void onClearCardError() {
            mCardLayout.setError(null);
            mCardLayout.setErrorEnabled(false);
        }
    };

    /**
     * The callback is used to communicate with the month input
     * <p>
     * The custom {@link MonthInput} takes care of populating the values in the spinner
     * and will trigger this callback when the user selects a new option. State is update
     * accordingly.
     */
    private final MonthInput.MonthListener mMonthInputListener = new MonthInput.MonthListener() {
        @Override
        public void onMonthInputFinish(String month) {
            mDataStore.setCardMonth(month);
            mDataStore.setValidCardMonth(true);
        }
    };

    /**
     * The callback is used to communicate with the year input
     * <p>
     * The custom {@link YearInput} takes care of populating the values in the spinner
     * and will trigger this callback when the user selects a new option. State is update
     * accordingly.
     */
    private final YearInput.YearListener mYearInputListener = new YearInput.YearListener() {
        @Override
        public void onYearInputFinish(String year) {
            mDataStore.setCardYear(year);
            mDataStore.setValidCardYear(true);
            ((TextView) mMonthInput.getSelectedView()).setError(null);
        }
    };

    /**
     * The callback is used to communicate with the cvv input
     * <p>
     * The custom {@link DefaultInput} takes care of the validation and it uses a callback
     * to indicate this controller if there is any error or if the error state needs to
     * be cleared. State is also updates based on the outcome of the input.
     */
    private final DefaultInput.Listener mCvvInputListener = new DefaultInput.Listener() {
        @Override
        public void onInputFinish(String value) {
            mDataStore.setCardCvv(value);
            if (value.length() == mDataStore.getCvvLength()) {
                mDataStore.setValidCardCvv(true);
            } else {
                mDataStore.setValidCardCvv(false);
            }
        }

        @Override
        public void clearInputError() {
            mCvvLayout.setError(null);
            mCvvLayout.setErrorEnabled(false);
        }
    };

    /**
     * The callback is used to trigger the focus change to the billing page
     */
    private final BillingInput.BillingListener mBillingInputListener = new BillingInput.BillingListener() {
        @Override
        public void onGoToBilling() {
            if (mGotoBillingListener != null) {
                mGotoBillingListener.onGoToBillingPressed();
            }
        }
    };

    DataStore mDataStore = DataStore.getInstance();
    private @Nullable
    CardDetailsView.GoToBillingListener mGotoBillingListener;
    private @Nullable
    CardDetailsView.DetailsCompleted mDetailsCompletedListener;
    private Context mContext;

    private CardInput mCardInput;
    private MonthInput mMonthInput;
    private YearInput mYearInput;
    private BillingInput mGoToBilling;
    private DefaultInput mCvvInput;
    private TextInputLayout mCardLayout;
    private TextInputLayout mCvvLayout;
    private Button mPayButton;
    private TextView mBillingHelper;
    private Toolbar mToolbar;
    private LinearLayout mAcceptedCardsView;
    private AttributeSet attrs;

    public CardDetailsView(Context context) {
        this(context, null);
    }

    public CardDetailsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    /**
     * The UI initialisation
     * <p>
     * Used to initialise element and pass callbacks as well as setting up appropriate listeners
     */
    private void init() {
        inflate(mContext, R.layout.card_details, this);

        mToolbar = findViewById(R.id.my_toolbar);

        mCardInput = findViewById(R.id.card_input);
        mCardLayout = findViewById(R.id.card_input_layout);
        mCardInput.setCardListener(mCardInputListener);

        mMonthInput = findViewById(R.id.month_input);
        mMonthInput.setMonthListener(mMonthInputListener);

        mYearInput = findViewById(R.id.year_input);
        mYearInput.setYearListener(mYearInputListener);

        mCvvInput = findViewById(R.id.cvv_input);
        mCvvLayout = findViewById(R.id.cvv_input_layout);
        mCvvInput.setListener(mCvvInputListener);

        mBillingHelper = findViewById(R.id.billing_helper_text);
        mGoToBilling = findViewById(R.id.go_to_billing);

        // Hide billing details options based on the module initialisation option
        if (!mDataStore.getBillingVisibility()) {
            mBillingHelper.setVisibility(GONE);
            mGoToBilling.setVisibility(GONE);
        } else {
            mGoToBilling.setBillingListener(mBillingInputListener);
        }

        mPayButton = findViewById(R.id.pay_button);

        mPayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // hide keyboard
                try{
                    InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    if(imm != null)
                     imm.hideSoftInputFromWindow(getWindowToken(), 0);
                    mCardInput.clearFocus();
                    mMonthInput.clearFocus();
                    mYearInput.clearFocus();
                    mCvvInput.clearFocus();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (mDetailsCompletedListener != null && isValidForm()) {
                    mDetailsCompletedListener.onDetailsCompleted();
                }
            }
        });

        // Restore state in case the orientation changes
        repopulateField();

        // Populate accepted cards
        mAcceptedCardsView = findViewById(R.id.card_icons_layout);
        setAcceptedCards();
    }

    /**
     * Used to restore state on orientation changes
     * <p>
     * The method will repopulate all the card input fields with the latest state they were in
     * if the device orientation changes, and therefore avoiding the text inputs to be cleared.
     */
    private void repopulateField() {
        // Repopulate card number
        if (DataStore.getInstance().getCardNumber() != null) {
            if (mDataStore.getCardNumber() != null) {
                // Get card type based on the saved card number
                CardUtils.Cards cardType = CardUtils.getType(DataStore.getInstance().getCardNumber());
                // Set the CardInput maximum length based on the type of card
                mCardInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(cardType.maxCardLength)});
                // Set the CardInput icon based on the type of card
                mCardInput.setCardTypeIcon(cardType);
                // Check if the card is valid
                mCardInput.checkIfCardIsValid(mDataStore.getCardNumber(), cardType);
                // Update the card field with the last input value
                String formattedCard = CardUtils.getFormattedCardNumber(mDataStore.getCardNumber());
                mCardInput.setText(formattedCard);
                // Set the cursor to the end of the input
                mCardInput.setSelection(formattedCard.length());
            }
        }

        //Repopulate card month
        if (DataStore.getInstance().getCardMonth() != null) {
            MonthInput.Months[] months = MonthInput.Months.values();
            try {
                mMonthInput.setSelection(months[Integer.parseInt(DataStore.getInstance().getCardMonth()) - 1].number - 1);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //Repopulate card year
        if (DataStore.getInstance().getCardYear() != null) {
            try {
                mYearInput.setSelection(((ArrayAdapter<String>) mYearInput.getAdapter()).getPosition(DataStore.getInstance().getCardYear()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Repopulate card cvv
        if (DataStore.getInstance().getCardCvv() != null) {
            // Update the cvv field with the last input value
            mCvvInput.setText(mDataStore.getCardCvv());
        }

        //Repopulate billing spinner
        updateBillingSpinner();
    }

    /**
     * Used to indicate the validity of the full card from
     * <p>
     * The method will check if the inputs are valid and also check the relation between the inputs
     * to ensure validity (e.g. month to year relation).
     * This method will also populate the field error accordingly
     *
     * @return boolean abut form validity
     */
    private boolean isValidForm() {

        boolean outcome = true;

        checkFullDate();

        if (!mDataStore.isValidCardMonth()) {
            outcome = false;
        }

        if (!mDataStore.isValidCardNumber()) {
            mCardLayout.setError(getResources().getString(R.string.error_card_number));
            outcome = false;
        }

        if (mCvvInput.getText().length() == mDataStore.getCvvLength()) {
            mDataStore.setValidCardCvv(true);
        } else {
            mDataStore.setValidCardCvv(false);
        }

        if (!mDataStore.isValidCardCvv()) {
            mCvvLayout.setError(getResources().getString(R.string.error_cvv));
            outcome = false;
        } else {
            mCvvLayout.setError(null);
            mCvvLayout.setErrorEnabled(false);
        }

        return outcome;
    }

    /**
     * Used to indicate the validity of the date
     * <p>
     * The method will check if the values from the {@link MonthInput} and {@link YearInput} are
     * not representing a date in the past.
     *
     * @return boolean abut form validity of the date
     */
    private boolean checkFullDate() {

        // Check is the state contain the date and if it is check if the current selected
        // values are valid. Display error if applicable.
        if (mDataStore.getCardYear() != null &&
                mDataStore.getCardYear() != null &&
                !CardUtils.isValidDate(mDataStore.getCardMonth(), mDataStore.getCardYear())) {
            mDataStore.setValidCardMonth(false);
            ((TextView) mMonthInput.getSelectedView()).setError(getResources()
                    .getString(R.string.error_expiration_date));
            return false;
        }
        mDataStore.setValidCardMonth(true);
        return true;
    }

    /**
     * Used to clear/reset the billing details spinner
     * <p>
     * The method will be used to clear/reset the billing details spinner in case the user
     * has decide to clear their details from the {@link BillingDetailsView}
     */
    public void clearBillingSpinner() {
        List<String> billingElement = new ArrayList<>();

        // Set the default value fo the spinner
        billingElement.add(getResources().getString(R.string.select_billing_details));
        billingElement.add(getResources().getString(R.string.billing_details_add));

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_item, billingElement);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGoToBilling.setAdapter(dataAdapter);
        mGoToBilling.setSelection(0);
    }

    /**
     * Used to populate the billing spinner with the user billing details
     * <p>
     * The method will be called when the user has successfully saved their billing details and
     * to visually confirm that, the spinner is populated with the details and the default ADD
     * button is replaced by a EDIT button
     */
    public void updateBillingSpinner() {

        String address = mDataStore.getCustomerAddress1() +
                ", " + mDataStore.getCustomerAddress2() +
                ", " + mDataStore.getCustomerCity() +
                ", " + mDataStore.getCustomerState();

        // Avoid updates for there are no values set
        if (address.length() > 6) {
            List<String> billingElement = new ArrayList<>();

            billingElement.add(address);
            billingElement.add("Edit");

            ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(mContext,
                    android.R.layout.simple_spinner_item, billingElement);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mGoToBilling.setAdapter(dataAdapter);
            mGoToBilling.setSelection(0);
        }
    }

    /**
     * Used to clear the text and state of the fields
     * <p>
     */
    public void resetFields() {
        clearBillingSpinner();
        mCvvInput.setText("");
        mCvvLayout.setError(null);
        mCvvLayout.setErrorEnabled(false);
        mYearInput.setSelection(0);
        mMonthInput.setSelection(0);
        mCardInput.setText("");
        mCardLayout.setError(null);
        mCardLayout.setErrorEnabled(false);
    }

    /**
     * Used dynamically populate the accepted cards view is the option is used
     */
    public void setAcceptedCards() {

        CardUtils.Cards[] allCards = mDataStore.getAcceptedCards() != null
                ? mDataStore.getAcceptedCards()
                : (CardUtils.Cards[]) Arrays.asList(CardUtils.Cards.values()).toArray();

        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());

        for (CardUtils.Cards card : allCards) {
            ImageView image = new ImageView(mContext);
            image.setLayoutParams(new android.view.ViewGroup.LayoutParams(size, size));
            image.setImageResource(card.resourceId);
            MarginLayoutParams marginParams = new MarginLayoutParams(image.getLayoutParams());
            marginParams.setMargins(0, 0, margin, 0);

            // Adds the view to the layout
            mAcceptedCardsView.addView(image);
        }

    }

    /**
     * Used to set the callback listener for when the form is submitted
     */
    public void setDetailsCompletedListener(CardDetailsView.DetailsCompleted listener) {
        mDetailsCompletedListener = listener;
    }

    /**
     * Used to set the callback listener for when the billing details page is requested
     */
    public void setGoToBillingListener(GoToBillingListener listener) {
        mGotoBillingListener = listener;
    }
}

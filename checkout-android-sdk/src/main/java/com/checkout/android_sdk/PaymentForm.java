package com.checkout.android_sdk;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.checkout.android_sdk.Models.BillingModel;
import com.checkout.android_sdk.Models.PhoneModel;
import com.checkout.android_sdk.Request.CardTokenisationRequest;
import com.checkout.android_sdk.Store.DataStore;
import com.checkout.android_sdk.Utils.CardUtils;
import com.checkout.android_sdk.Utils.CustomAdapter;
import com.checkout.android_sdk.View.BillingDetailsView;
import com.checkout.android_sdk.View.CardDetailsView;

/**
 * Contains helper methods dealing with the tokenisation or payment from customisation
 * <p>
 * Most of the methods that include interaction with the Checkout.com API rely on
 * callbacks to communicate outcomes. Please make sure you set the key/environment
 * and appropriate  callbacks to a ensure successful interaction
 */
public class PaymentForm extends FrameLayout {

    private static ProgressDialog mProgressDialog;

    /**
     * This is interface used as a callback for when the 3D secure functionality is used
     */
    public interface On3DSFinished {
        void onSuccess(String token);

        void onError(String errorMessage);
    }

    /**
     * This is interface used as a callback for when the form is completed and the user pressed the
     * pay button. You can use this to potentially display a loader.
     */
    public interface OnSubmitForm {
        void onSubmit(CardTokenisationRequest request);
    }

    // Indexes for the pages
    private static int CARD_DETAILS_PAGE_INDEX = 0;
    private static int BILLING_DETAILS_PAGE_INDEX = 1;

    /**
     * This is a callback used to generate a payload with the user details and pass them to the
     * mSubmitFormListener so the user can act upon them. The next step will most likely include using
     * this payload to generate a token in  the CheckoutAPIClient
     */
    private final CardDetailsView.DetailsCompleted mDetailsCompletedListener = new CardDetailsView.DetailsCompleted() {
        @Override
        public void onDetailsCompleted() {
            mSubmitFormListener.onSubmit(generateRequest());
            //customAdapter.clearFields();
            //mDataStore.cleanState();
        }
    };

    /**
     * This is a callback used to go back to the card details view from the billing page
     * and based on the action used decide is the billing spinner will be updated
     */
    private BillingDetailsView.Listener mBillingListener = new BillingDetailsView.Listener() {
        @Override
        public void onBillingCompleted() {
            customAdapter.updateBillingSpinner();
            mPager.setCurrentItem(CARD_DETAILS_PAGE_INDEX);
        }

        @Override
        public void onBillingCanceled() {
            customAdapter.clearBillingSpinner();
            mPager.setCurrentItem(CARD_DETAILS_PAGE_INDEX);
        }
    };

    /**
     * This is a callback used to navigate to the billing details page
     */
    private CardDetailsView.GoToBillingListener mCardListener = new CardDetailsView.GoToBillingListener() {
        @Override
        public void onGoToBillingPressed() {
            mPager.setCurrentItem(BILLING_DETAILS_PAGE_INDEX);
        }
    };


    private Context mContext;
    public On3DSFinished m3DSecureListener;
    public OnSubmitForm mSubmitFormListener;
    public CheckoutAPIClient.OnTokenGenerated mTokenListener;

    private CustomAdapter customAdapter;
    public ViewPager mPager;
    private AttributeSet attrs;
    private DataStore mDataStore = DataStore.getInstance();

    /**
     * This is the constructor used when the module is used without the UI.
     */
    public PaymentForm(@NonNull Context context) {
        this(context, null);
    }

    public PaymentForm(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.attrs = attrs;
        initView();
    }


    /**
     * This method is used to initialise the UI of the module
     */
    private void initView() {
        // Set up the layout
        inflate(mContext, R.layout.payment_form, this);

        mPager = findViewById(R.id.view_pager);
        // Use a custom adapter for the viewpager
        customAdapter = new CustomAdapter(mContext);
        // Set up the callbacks
        customAdapter.setCardDetailsListener(mCardListener);
        customAdapter.setBillingListener(mBillingListener);
        customAdapter.setTokenDetailsCompletedListener(mDetailsCompletedListener);
        mPager.setAdapter(customAdapter);
        mPager.setEnabled(false);

        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage("الرجاء الانتظار...");

    }

    /**
     * This method is used set the accepted card schemes
     *
     * @param cards array of accepted cards
     */
    public PaymentForm setAcceptedCard(CardUtils.Cards[] cards) {
        mDataStore.setAcceptedCards(cards);
        return this;
    }

    /**
     * This method is used to handle 3D Secure URLs.
     * <p>
     * It wil programmatically generate a WebView and listen for when the url changes
     * in either the success url or the fail url.
     *
     * @param url        the 3D Secure url
     * @param successUrl the Redirection url set up in the Checkout.com HUB
     * @param failsUrl   the Redirection Fail url set up in the Checkout.com HUB
     */
    WebView web;
    public void handle3DS(String url, final String successUrl, final String failsUrl) {
        if (mPager != null) {
            mPager.setVisibility(GONE); // dismiss the card form UI
        }
        web = new WebView(mContext);
        web.loadUrl(url);
        //web.getSettings().setJavaScriptEnabled(true);
        //String newUA= "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";
        //web.getSettings().setUserAgentString(newUA);

        web.getSettings().setJavaScriptEnabled(true);
        //wv.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setLoadsImagesAutomatically(true);
        web.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        web.getSettings().setPluginState(WebSettings.PluginState.ON);
        web.getSettings().setUseWideViewPort(true);
        web.getSettings().setLoadWithOverviewMode(true);
        //@SuppressWarnings("unused")
        //WebSettings settings= web.getSettings();
        if (Build.VERSION.SDK_INT >= 21) {
            web.getSettings().setMixedContentMode( WebSettings.MIXED_CONTENT_ALWAYS_ALLOW );
        }

        web.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {

                Log.e("Error in url...", description);
                Log.e("Error in failingUrl...", failingUrl);

            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.e("webview onPageStarted","Url: " + url);
                if(mProgressDialog != null && !mProgressDialog.isShowing()){
                    mProgressDialog.show();
                }

            }

            // Listen for when teh URL changes and match t with either the success of fail url
            @Override
            public void onPageFinished(WebView view, String url) {

                try {
                    if(mProgressDialog != null){
                        mProgressDialog.hide();
                    }
                    Log.e("webview onPageFinished", "Url: " + url);

                    if (url.contains(successUrl)) {
                        Uri uri = Uri.parse(url);
                        String paymentToken = uri.getQueryParameter("cko-payment-token");
                        m3DSecureListener.onSuccess(paymentToken);
                        if (mPager != null) {
                            mPager.setVisibility(VISIBLE); // dismiss the card form UI
                        }
                        removeView(web);
                    } else if (url.contains(failsUrl)) {
                        Uri uri = Uri.parse(url);
                        String paymentToken = uri.getQueryParameter("cko-payment-token");
                        m3DSecureListener.onError(paymentToken);
                        if (mPager != null) {
                            mPager.setVisibility(VISIBLE); // dismiss the card form UI
                        }
                        removeView(web);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });


        // Make WebView fill the layout
        web.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        addView(web);
    }

    public void removeWebView(){
        if(web != null){
            removeView(web);
        }
    }

    /**
     * This method used to generate a {@link CardTokenisationRequest} with the details
     * completed by the user in the payment from
     * displayed in the payment form.
     *
     * @return CardTokenisationRequest
     */
    private CardTokenisationRequest generateRequest() {
        CardTokenisationRequest request;
        if (mDataStore.isBillingCompleted()) {
            request = new CardTokenisationRequest(
                    sanitizeEntry(mDataStore.getCardNumber()),
                    mDataStore.getCustomerName(),
                    mDataStore.getCardMonth(),
                    mDataStore.getCardYear(),
                    mDataStore.getCardCvv(),
                    new BillingModel(
                            mDataStore.getCustomerAddress1(),
                            mDataStore.getCustomerAddress2(),
                            mDataStore.getCustomerZipcode(),
                            mDataStore.getCustomerCountry(),
                            mDataStore.getCustomerCity(),
                            mDataStore.getCustomerState(),
                            new PhoneModel(
                                    mDataStore.getCustomerPhonePrefix(),
                                    mDataStore.getCustomerPhone()
                            )
                    )
            );
        } else {
            request = new CardTokenisationRequest(
                    sanitizeEntry(mDataStore.getCardNumber()),
                    mDataStore.getCustomerName(),
                    mDataStore.getCardMonth(),
                    mDataStore.getCardYear(),
                    mDataStore.getCardCvv(),
                    null
            );
        }

        return request;
    }

    /**
     * This method used to decide if the billing details option will be
     * displayed in the payment form.
     *
     * @param include boolean showing if the billing should be used
     */
    public void includeBilling(Boolean include) {
        if (!include) {
            mDataStore.setShowBilling(false);
        } else {
            mDataStore.setShowBilling(true);
        }
    }

    /**
     * Returns a String without any spaces
     * <p>
     * This method used to take a card number input String and return a
     * String that simply removed all whitespace, keeping only digits.
     *
     * @param entry the String value of a card number
     */
    private String sanitizeEntry(String entry) {
        return entry.replaceAll("\\D", "");
    }

    /**
     * This method used to set a callback for when the 3D Secure handling.
     */
    public PaymentForm set3DSListener(On3DSFinished listener) {
        this.m3DSecureListener = listener;
        return this;
    }

    /**
     * This method used to set a callback for when the form is submitted
     */
    public PaymentForm setSubmitListener(OnSubmitForm listener) {
        this.mSubmitFormListener = listener;
        return this;
    }

}

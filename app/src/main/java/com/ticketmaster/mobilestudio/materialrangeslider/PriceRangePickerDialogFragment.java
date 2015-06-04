package com.ticketmaster.mobilestudio.materialrangeslider;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ticketmaster.mobilestudio.materialrangeslider.MaterialRangeSlider.RangeSliderListener;

import java.util.Currency;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PriceRangePickerDialogFragment extends DialogFragment implements RangeSliderListener {

    public static final String MIN_PRICE_KEY = "min_price_key";
    public static final String MAX_PRICE_KEY = "max_price_key";
    public static final String SELECTED_MINIMUM_KEY = "selected_minimum_key";
    public static final String SELECTED_MAXIMUM_KEY = "selected_maximum_key";
    public static final String CURRENCY_SYMBOL_KEY = "currency_symbol_key";

    static final int PADDING = 36;

    private String currencyCode;

    @InjectView(R.id.min_price_txt) TextView minPriceTxt;
    @InjectView(R.id.max_price_txt) TextView maxPriceTxt;
    @InjectView(R.id.price_slider) MaterialRangeSlider priceSlider;

    public static PriceRangePickerDialogFragment newInstance(int minPrice,
                                                        int maxPrice,
                                                        Integer selectedMinPrice,
                                                        Integer selectedMaxPrice,
                                                        @NonNull String currencySymbol) {
        PriceRangePickerDialogFragment fragment = new PriceRangePickerDialogFragment();
        fragment.setCancelable(false);
        Bundle bundle = new Bundle();
        bundle.putInt(MIN_PRICE_KEY, minPrice);
        bundle.putInt(MAX_PRICE_KEY, maxPrice);
        bundle.putString(CURRENCY_SYMBOL_KEY, currencySymbol);

        if (selectedMinPrice != null && selectedMaxPrice != null) {
            bundle.putInt(SELECTED_MINIMUM_KEY, selectedMinPrice);
            bundle.putInt(SELECTED_MAXIMUM_KEY, selectedMaxPrice);
        }

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        currencyCode = getArguments().getString(CURRENCY_SYMBOL_KEY);
        final AlertDialog dial = new Builder(getActivity())
                .setPositiveButton("Ok", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Snackbar.make(getActivity().findViewById(R.id.root),
                                "Price range of " + minPriceTxt.getText().toString()
                                        + " - " + maxPriceTxt.getText().toString() + " selected.", Snackbar.LENGTH_LONG)
                                .show();
                    }
                })
                .setNeutralButton("Reset", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing - OnClickListener will be overridden in onStart()
                    }
                })
                .setCancelable(false)
                .create();
        dial.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                dial.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.tms_blue));
                dial.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.tms_blue));
            }
        });
        TextView title = new TextView(getActivity());
        title.setText(getString(R.string.price_dialog_title));
        title.setPadding(PADDING, PADDING, PADDING, PADDING);
        title.setTextSize(18);
        title.setTextAppearance(getActivity(), R.style.TitleText);
        dial.setCustomTitle(title);
        View slider = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_price_range, null);
        ButterKnife.inject(this, slider);
        priceSlider.setRangeSliderListener(this);
        priceSlider.setMin(getArguments().getInt(MIN_PRICE_KEY));
        priceSlider.setMax(getArguments().getInt(MAX_PRICE_KEY));
        dial.setView(slider);
        currencyCode = Currency.getInstance(Locale.getDefault()).getSymbol();

        int startingMin = getArguments().getInt(SELECTED_MINIMUM_KEY, getArguments().getInt(MIN_PRICE_KEY));
        int startingMax = getArguments().getInt(SELECTED_MAXIMUM_KEY, getArguments().getInt(MAX_PRICE_KEY));

        priceSlider.setStartingMinMax(startingMin, startingMax);

        return dial;
    }

    @Override
    public void onMinChanged(int newValue) {
        minPriceTxt.setText(currencyCode + String.valueOf(newValue));
    }

    @Override
    public void onMaxChanged(int newValue) {
        maxPriceTxt.setText(currencyCode + String.valueOf(newValue));
    }

    @Override
    public void onStart() {
        super.onStart();

//         Override onClickListener so dialog is not closed when user clicks reset
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button negativeButton = d.getButton(Dialog.BUTTON_NEUTRAL);
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    priceSlider.reset();
                    priceSlider.invalidate();
                }
            });
        }
    }

    public static class PriceRangeSelectedEvent {
        final int selectedMin;
        final int selectedMax;

        public PriceRangeSelectedEvent(int selectedMin, int selectedMax) {
            this.selectedMin = selectedMin;
            this.selectedMax = selectedMax;
        }

        public int getSelectedMin() {
            return selectedMin;
        }

        public int getSelectedMax() {
            return selectedMax;
        }
    }
}

package com.ticketmaster.android.sample.materialrangeslider;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.Currency;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity {


    @OnClick(R.id.btn_launch_picker)
    public void onLaunchSlider() {
        PricePickerDialogFragment.newInstance(0, 100, 0, 100, Currency.getInstance(Locale.getDefault()).getSymbol())
                .show(getSupportFragmentManager(), "slider");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
    }

}

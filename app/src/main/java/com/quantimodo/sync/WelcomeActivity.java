package com.quantimodo.sync;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.quantimodo.android.sdk.Quantimodo;
import com.quantimodo.android.sdk.QuantimodoApi;
import com.stericson.RootTools.RootTools;

public class WelcomeActivity extends Activity {
    private ViewFlipper viewFlipper;
    private ViewFlipper buttonFlipper;
    private ImageButton btNext;
    private ImageButton btBrowser;
    private ImageButton btPlaystore;
    private ImageButton btRetry;

    //Page 2
    private TextView tvRootResult;

    //Page 3
    private LinearLayout lnQuantimodoAccounts;

    private String selectedAccount;

    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

        tvRootResult = (TextView) findViewById(R.id.tvRootResult);

        lnQuantimodoAccounts = (LinearLayout) findViewById(R.id.lnQuantimodoAccounts);

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);

        buttonFlipper = (ViewFlipper) findViewById(R.id.buttonFlipper);
        buttonFlipper.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        buttonFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));

        btRetry = (ImageButton) findViewById(R.id.button_retry);
        btRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkSystem();
            }
        });
        btBrowser = (ImageButton) findViewById(R.id.button_browser);
        btBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "http://www.androidauthority.com/rooting-for-dummies-a-beginners-guide-to-root-your-android-phone-or-tablet-10915/";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                WelcomeActivity.this.startActivity(i);
                WelcomeActivity.this.setResult(RESULT_CANCELED);
                WelcomeActivity.this.finish();
            }
        });
        btNext = (ImageButton) findViewById(R.id.button_next);
        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                currentPage++;
                if (currentPage == 1) {
                    viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(WelcomeActivity.this, R.anim.flipper_next_out));
                    viewFlipper.setInAnimation(AnimationUtils.loadAnimation(WelcomeActivity.this, R.anim.flipper_next_in));
                    checkSystem();
                } else if (currentPage == 2) {
                    btNext.setVisibility(View.GONE);
                }

                viewFlipper.setDisplayedChild(currentPage);
            }
        });

        getAccountNames();
    }

    private void getAccountNames() {
        Account[] accounts = Quantimodo.getAccounts(this.getApplicationContext());

        final String[] names = new String[accounts.length];
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < names.length; i++) {
            names[i] = accounts[i].name;
            View newView = inflater.inflate(R.layout.activity_tutorial_accounts_row, null);
            Button accountButton = (Button) newView.findViewById(R.id.btAccount);
            accountButton.setId(i);
            accountButton.setTag(names[i]);
            accountButton.setText(names[i]);
            accountButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedAccount = (String) v.getTag();
                    connectToAccount();
                }
            });
            lnQuantimodoAccounts.addView(newView, i + 3);
        }
    }

    private boolean rootAvailable;
    private boolean rootAccess;

    private void checkSystem() {
        final Handler handler = new Handler();
        Runnable run = new Runnable() {
            @Override
            public void run() {
                if (RootTools.isRootAvailable()) {
                    rootAvailable = true;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvRootResult.setTextColor(0xFFFFBB33);
                            tvRootResult.setText("Root available...");

                        }
                    });

                    if (RootTools.isAccessGiven()) {
                        Log.i("Says rooted");
                        rootAccess = true;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvRootResult.setTextColor(0xFFFFBB33);
                                tvRootResult.setText("Root available, access granted");
                            }
                        });
                    } else {
                        rootAccess = false;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvRootResult.setTextColor(0xffff0000);
                                tvRootResult.setText("QuantiModo Sync was denied root access.");
                            }
                        });
                    }
                }

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (rootAvailable && rootAccess) {
                            btNext.performClick();
                        } else if (rootAvailable) {
                            // Show refresh button
                            buttonFlipper.setDisplayedChild(3);
                        } else {
                            // Show browser button for more root info
                            buttonFlipper.setDisplayedChild(1);
                        }
                    }
                });
            }
        };
        new Thread(run).start();
    }

    private void connectToAccount() {
        QuantimodoApi qmClient = QuantimodoApi.getInstance();
        Account account = Quantimodo.getAccount(WelcomeActivity.this.getApplicationContext(), selectedAccount);
        qmClient.getAccessToken(WelcomeActivity.this, account, Global.QM_ID, Global.QM_SECRET, Global.QM_SCOPES, new QuantimodoApi.OnAuthenticationDoneListener() {
            @Override
            public void onSuccess(String authenticationToken) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WelcomeActivity.this);
                prefs.edit().putString("qmAccountName", selectedAccount).commit();
                Global.init(WelcomeActivity.this);
                WelcomeActivity.this.setResult(RESULT_OK);
                WelcomeActivity.this.finish();
            }

            @Override
            public void onFailed(String reason) {
                Toast.makeText(WelcomeActivity.this, "Authorization failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

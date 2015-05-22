package com.quantimodo.sync.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewFlipper;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.quantimodo.sync.AuthHelper;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.Log;
import com.quantimodo.sync.R;
import com.stericson.RootTools.RootTools;

import javax.inject.Inject;

public class WelcomeActivity extends Activity {

    @InjectView(R.id.viewFlipper)
    ViewFlipper viewFlipper;

    @InjectView(R.id.buttonFlipper)
    ViewFlipper buttonFlipper;

    @InjectView(R.id.button_next)
    ImageButton btNext;

    @InjectView(R.id.button_browser)
    ImageButton btBrowser;

    @InjectView(R.id.button_retry)
    ImageButton btRetry;

    //Page 2
    @InjectView(R.id.tvRootResult)
    TextView tvRootResult;

    //Page 3
    @InjectView(R.id.btLogIn)
    Button mLoginButton;

    private boolean rootAvailable;
    private boolean rootAccess;

    @Inject
    AuthHelper mAuthHelper;

    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.inject(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);


        buttonFlipper.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        buttonFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));

        btRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkSystem();
            }
        });

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

        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                currentPage++;
                if (currentPage == 1) {
                    viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(WelcomeActivity.this, R.anim.flipper_next_out));
                    viewFlipper.setInAnimation(AnimationUtils.loadAnimation(WelcomeActivity.this, R.anim.flipper_next_in));
                    checkSystem();
                } else if (currentPage == 2) {
                    btNext.setVisibility(View.INVISIBLE);
                    btNext.setEnabled(false);
                }

                viewFlipper.setDisplayedChild(currentPage);
            }
        });

    }

    @OnClick(R.id.btLogIn)
    public void onLogginButtonClick(){
        Intent intent = new Intent(this,QuantimodoWebAuthenticatorActivity.class);
        startActivityForResult(intent,QuantimodoWebAuthenticatorActivity.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == QuantimodoWebAuthenticatorActivity.REQUEST_CODE && resultCode == RESULT_OK){
            mLoginButton.setEnabled(false);
            Global.init(this);
            setResult(RESULT_OK);
            finish();
        }
    }

    private void checkSystem() {
        btNext.setVisibility(View.GONE);
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
                            tvRootResult.setText(R.string.tutorial_message_root_avail);

                        }
                    });

                    if (RootTools.isAccessGiven()) {
                        Log.i("Says rooted");
                        rootAccess = true;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvRootResult.setTextColor(0xFFFFBB33);
                                tvRootResult.setText(R.string.tutorial_message_access_granted);
                            }
                        });
                    } else {
                        rootAccess = false;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvRootResult.setTextColor(0xffff0000);
                                tvRootResult.setText(R.string.tutorial_message_access_denied);
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
                            tvRootResult.setTextColor(0xffff0000);
                            tvRootResult.setText(R.string.tutorial_message_no_root);
                            buttonFlipper.setDisplayedChild(1);
                        }
                    }
                });
            }
        };
        new Thread(run).start();
    }
}

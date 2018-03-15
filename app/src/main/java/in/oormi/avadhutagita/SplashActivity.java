package in.oormi.avadhutagita;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.VideoView;

public class SplashActivity extends AppCompatActivity {
    MediaPlayer mp = new MediaPlayer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //not reliable below API 24
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            StartMainActivity();
        }

        setContentView(R.layout.activity_splash);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        View decorView = this.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        Animation animation = AnimationUtils.loadAnimation(SplashActivity.this, R.anim.title);
        TextView textView1 = (TextView) findViewById(R.id.textViewTitleSanskrit);
        TextView textView2 = (TextView) findViewById(R.id.textViewTitleEnglish);
        textView1.startAnimation(animation);
        textView2.startAnimation(animation);

        try {
            final VideoView vidHolder = (VideoView) findViewById(R.id.videoViewSplash);
            Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sage4);
            vidHolder.setVideoURI(video);
            vidHolder.setZOrderOnTop(true);
            //vidHolder.setBackgroundColor(Color.TRANSPARENT);
            //vidHolder.start();

            vidHolder.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    StartMainActivity();
                }
            });

            vidHolder.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    StartMainActivity();
                    return false;
                }
            });

            vidHolder.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(false); //make sure to use this statement
                    vidHolder.start();
                    //Toast.makeText(mContext,String.valueOf(vtime),Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception ex) {
            StartMainActivity();
        }

        //just in case video completion listener is not called
        //5500 milli seconds is total time, 1000 milli seconds is time interval
        new CountDownTimer(5500, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                StartMainActivity();
            }
        }.start();
    }

    private void StartMainActivity() {
        if (isFinishing())
            return;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

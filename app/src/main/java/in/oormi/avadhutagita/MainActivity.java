package in.oormi.avadhutagita;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<String> {

    TextToSpeech tts;
    String alerttone;
    boolean alertenable;
    boolean soundenable;
    boolean vibeenable;
    boolean ttsenable;
    float speechrate;
    int freq;
    boolean nonightrem;
    boolean automode;
    int randenable = 0;

    PendingIntent pi;
    BroadcastReceiver br;
    AlarmManager am;

    boolean animrunning = false;
    boolean isTimerOn = false;

    private float x1;
    static final int MIN_DISTANCE = 150;
    //static final int MAX_VERSE = 298;
    int Verse = 0;
    //public static String [] allverses = new String[MAX_VERSE];
    public static ArrayList<String> allVerses = new ArrayList<>();
    final int [] chapMap = {76,40,46,25,32,27,15,10};
    Animation animation1;
    Animation animation2;
    Animation animation3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        getSupportLoaderManager().initLoader(1, null, (LoaderManager.LoaderCallbacks<String>)this).forceLoad();

        Setup();
        setupTimer();


//        TextView tv1 = (TextView)findViewById(R.id.textwhosaid);
//        Typeface type = Typeface.createFromAsset(getAssets(),"kokila.ttf");
//        tv1.setTypeface(type);

        Verse = 0;
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Verse = preferences.getInt("LastVerse", 0); //restart at the last verse
        ShowVerse(false);
        //Toast.makeText(MainActivity.this, R.string.touchtostart, Toast.LENGTH_LONG).show();
        final TextView tvs = (TextView)findViewById(R.id.textstatus);

        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButtonStartStop);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Setup();
                    Verse++;
                    ShowVerse(true);
                    animrunning = false;
                    startTimer();
                    if (automode) {
                        tvs.setText(R.string.status_auto);
                    } else {
                        tvs.setText(R.string.status_sch);
                    }
                } else {
                    stopTimer(true);
                    toggle.clearAnimation();
                    tvs.setText(R.string.touchtostart);
                }
            }
        });

        ImageButton mbuttonNext = (ImageButton) findViewById(R.id.imageButtonNext);
        mbuttonNext.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View view){
                        Verse++;
                        ShowVerse(true);
                        if (isTimerOn) stopTimer(true);
                        tvs.setText(getString(R.string.status_user));
                    }
                });

        ImageButton mbuttonPrev = (ImageButton) findViewById(R.id.imageButtonPrev);
        mbuttonPrev.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View view){
                        Verse--;
                        ShowVerse(true);
                        if (isTimerOn) stopTimer(true);
                        tvs.setText(getString(R.string.status_user));
                    }
                });

        //mbuttonNext.setImageAlpha(180);
        //mbuttonPrev.setImageAlpha(180);

    }

    public void ShowVerse(boolean sounds){
        String mLine = getString(R.string.cover);
        TextView tv1 = (TextView)findViewById(R.id.textwhosaid);
        TextView tv2 = (TextView)findViewById(R.id.textversenum);
        TextView tv3 = (TextView)findViewById(R.id.textverse);
        TextView tv4 = (TextView)findViewById(R.id.textViewComments);

        tv1.setText("");
        tv2.setText("");
        tv3.setText("");
        tv4.setText("");


        if(randenable > 0){
            Random rand = new Random();
            Verse = rand.nextInt(allVerses.size());
        }

        if (Verse >= allVerses.size()) Verse = 0;
        if (Verse < 0) Verse = allVerses.size()-1;

        mLine = allVerses.get(Verse);
        if (mLine == null){
            mLine = getString(R.string.texterror);
        }

        if(Verse == 0){
            tv1.setGravity(Gravity.CENTER);
            tv1.setTextSize(40.0f);
        }else {
            tv1.setGravity(Gravity.CENTER_VERTICAL);
            tv1.setTextSize(18.0f);
        }

        String[] versecontent;
        versecontent = mLine.split("@");

        if(versecontent.length > 0) {
            tv1.setText(versecontent[0]);
            if(versecontent.length > 1) tv2.setText(versecontent[1]);
            if(versecontent.length > 2) {
                versecontent[2] = versecontent[2].replace("~", "\n");
                tv3.setText(versecontent[2]);
            }
            if(versecontent.length > 3) {
                versecontent[3] = versecontent[3].replace("~", "\n");
                tv4.setText(versecontent[3]);
            }
            if (sounds) {
                if(versecontent.length > 2) {
                    versecontent[2] = versecontent[2].replace("-", " ");
                    versecontent[2] = versecontent[2].replace("\n\n", "\n");
                    SoundAlert(versecontent[2]);
                }
            }
            // Store values between instances here
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();  // Put the values from the UI
            editor.putInt("LastVerse", Verse); // value to store
            // Commit to storage
            editor.commit();

            tv1.startAnimation(animation1);
            tv3.startAnimation(animation2);
            tv4.startAnimation(animation3);

        }
        else{
            tv3.setText(R.string.errorbadformat);
        }


    }

    private void Setup(){
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_headers, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        alerttone = prefs.getString("notifications_new_message_ringtone",
                "content://settings/system/notification_sound");
        soundenable = prefs.getBoolean("notifications_tone", false);
        alertenable = prefs.getBoolean("notifications_new_message", true);
        vibeenable = prefs.getBoolean("notifications_new_message_vibrate", false);
        ttsenable = prefs.getBoolean("notifications_new_message_speak", true);

        speechrate = 1.0f;
        String strrate = prefs.getString("rate_list", "1.0");
        if ((strrate!=null)&&(strrate.length()>0)) speechrate = Float.parseFloat(strrate);

        String remfreqstr = prefs.getString("freq_list", "60");
        if (remfreqstr.length()<1) remfreqstr = "60";
        freq = 60 * Integer.parseInt(remfreqstr);
        //freq = 10; //testing

        nonightrem = prefs.getBoolean("night_switch", true);
        automode = prefs.getBoolean("auto_switch", false);
        if (automode) {
            nonightrem = false;//autoread in night too
            freq = 20;
        }
        String srandenable = prefs.getString("order_list", "0");
        randenable = Integer.parseInt(srandenable);

        animation1 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.remzoom);
        animation2 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fromleft);
        animation3 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fromright);
    }

    private void SoundAlert(final String speakme){
        if (alertenable) {
            if (soundenable) {
                Uri uri = Uri.parse(alerttone);
                PlayAlert(this, uri);
            }

            if (vibeenable) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(500);
            }

            if (ttsenable) {
                tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            int result = tts.setLanguage(Locale.UK);
                            if (result == TextToSpeech.LANG_MISSING_DATA ||
                                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.e("TTS Error", "This language is not supported");
                            } else {
                                tts.setSpeechRate(speechrate);
                                tts.speak(speakme, TextToSpeech.QUEUE_FLUSH, null,
                                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                            }
                        } else
                            Log.e("TTS Error", "Initialization Failed!");
                    }
                });
            }
        }
    }

    private void PlayAlert(Context context, Uri alert) {
        MediaPlayer mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(context, alert);
            final AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            Log.e("Media Error", "Failed to play alert.");
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void startTimer() {
        int delay = 1000 * freq;

        if(delay>60000) {
            String toastinfo = String.valueOf(delay / 60000) + getString(R.string.nxtremmintoast);
            //Toast.makeText(this, getString(R.string.nxtremtoast) + toastinfo, Toast.LENGTH_SHORT).show();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delay, pi );
        }
        else{
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                    + delay, pi );
        }

        if (!animrunning) {
            final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButtonStartStop);
            final Animation animation = AnimationUtils.loadAnimation(MainActivity.this,
                    R.anim.rotate_around_center_point);
            AlphaAnimation fade_in = new AlphaAnimation(0.2f, 1.0f);
            fade_in.setDuration(1000);
            fade_in.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation arg0) {
                }

                public void onAnimationRepeat(Animation arg0) {
                }

                public void onAnimationEnd(Animation arg0) {
                    toggle.setVisibility(View.VISIBLE);
                    toggle.startAnimation(animation);
                }
            });
            toggle.startAnimation(fade_in);
            animrunning = true;
            isTimerOn = true;
        }
    }

    public void stopTimer(boolean stopanim) {
        if(am!=null){
            if(pi!=null){
                am.cancel(pi);
                isTimerOn = false;
            }
        }
        if(stopanim) {
            final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButtonStartStop);
            toggle.clearAnimation();
        }

    }

    private void setupTimer() {
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {

                boolean enabled = false;
                if (nonightrem) {
                    //get the current timeStamp
                    Calendar calendar = Calendar.getInstance();
                    //calendar.set(Calendar.HOUR_OF_DAY, 17);
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    boolean day = (hour<22) && (hour>5);
                    if (day){enabled = true;}
                }
                else {enabled = true;}

                if (enabled) {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP, "AshtavakraAppTag");
                    wl.acquire();

                    Intent startIntent = c.getPackageManager()
                            .getLaunchIntentForPackage(c.getPackageName());
                    startIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //bring to foreground
                    c.startActivity(startIntent);

                    // Show next verse
                    Verse++;
                    ShowVerse(true);
                    stopTimer(false);
                    startTimer();//for next verse
                    wl.release();
                }
                else {//keep looping anyway
                    stopTimer(false);
                    startTimer();
                }

            }
        };
        registerReceiver(br, new IntentFilter("in.oormi.agita") );
        pi = PendingIntent.getBroadcast( this, 0, new Intent("in.oormi.agita"), 0 );
        am = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
    }

    @Override
    protected void onDestroy() {
        if(am!=null){
            if(pi!=null){
                am.cancel(pi);
            }
        }
        if(br!=null){
            unregisterReceiver(br);
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                moveTaskToBack(true);
                return true;
        }
        return false;
    }
/*

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        float x2;
        TextView tvs = (TextView)findViewById(R.id.textstatus);

        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Left to Right swipe action
                    if (x2 > x1)
                    {
                        Verse++;
                        if(Verse > allVerses.size()){Verse = 0;}
                    }

                    // Right to left swipe action
                    else
                    {
                        Verse--;
                        if(Verse < 0){Verse = allVerses.size();}
                    }

                    stopTimer(true);
                    ShowVerse(false);
                    tvs.setText(R.string.status_user);
                }
                else
                {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        return super.onTouchEvent(event);
    }
*/

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {
        Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

    @Override
    public Loader onCreateLoader(int arg0, Bundle arg1) {
        return new FetchData(this);
    }

    private static class FetchData extends AsyncTaskLoader<String> {

        public FetchData(Context context) {
            super(context);
        }

        @Override
        public String loadInBackground() {
            String str = "";
            String strBlock = "";
            //int nstr;

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(getContext().getAssets().open("avg01.txt"), "Unicode"));
                int v = 0;
                while (str != null) {
                    str = reader.readLine();
                    if (str != null) {
                        if (!str.isEmpty()) {
                            if (str.contains("^")) {
                                str = str.replace("^", "");
                                allVerses.add(strBlock);
                                v++;
                                strBlock = "";
                            }
                            strBlock = strBlock + str + "\n";
                        }
                    }

                }
                str = String.format("Loading finished. Count: %d", v);
            } catch (IOException e) {
                str = "Loading error.";
                Log.e("File Error", "Could not open verse file.");
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e("File Error", "Could not close verse file.");
                    }
                }
            }
            return str;
        }

        @Override
        public void deliverResult(String data) {
            super.deliverResult(data);
        }
    }

    private void gotoDialog() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(params);

        layout.setGravity(Gravity.CLIP_VERTICAL);
        layout.setPadding(10,10,10,10);

        final TextView tv = new TextView(this);
        tv.setText(R.string.jumptitle);
        tv.setPadding(40, 40, 40, 40);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(20);

        alertDialogBuilder.setView(layout);
        alertDialogBuilder.setCustomTitle(tv);

        final TextView tvChapter = new TextView(this);
        tvChapter.setText(R.string.gotochapter);
        tvChapter.setPadding(80,10,10,10);
        layout.addView(tvChapter);

        final SeekBar chapterBar = new SeekBar(this);
        chapterBar.setMax(7);
        chapterBar.setProgress(0);
        layout.addView(chapterBar);

        final TextView tvVerse = new TextView(this);
        tvVerse.setText(R.string.versenum);
        tvVerse.setPadding(80,10,10,10);
        layout.addView(tvVerse);

        final SeekBar verseBar = new SeekBar(this);
        verseBar.setMax(75);
        verseBar.setProgress(0);
        layout.addView(verseBar);

        alertDialogBuilder.setNegativeButton(R.string.jumpcancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        alertDialogBuilder.setPositiveButton(R.string.jumpok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setGotoVerse(chapterBar.getProgress(), verseBar.getProgress());
                ShowVerse(true);
            }
        });

        chapterBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvChapter.setText(getString(R.string.gotochapter)+ " " + String.valueOf(progress+1));
                verseBar.setMax(chapMap[progress] - 1);
                verseBar.setProgress(0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        verseBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVerse.setText(getString(R.string.versenum) + " " + String.valueOf(progress+1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        AlertDialog edSetDialog = alertDialogBuilder.create();
        tvVerse.setText(getString(R.string.versenum) + " 1");
        tvChapter.setText(getString(R.string.gotochapter) + " 1");

        try {
            edSetDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setGotoVerse(int nchap, int nverse) {
        int skip = 0;
        for (int c=0;c<nchap;c++) skip = skip + chapMap[c];
        Verse = 4 + skip + nverse + 2*nchap;
    }

    private ShareActionProvider mShareActionProvider;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.infomenu, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider)  MenuItemCompat.getActionProvider(item);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=in.oormi.avadhutagita");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this app!");
        setShareIntent(shareIntent);
        return true;
    }

    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.gotoverse:
                gotoDialog();
                break;
            case R.id.info:
                Intent resIntent = new Intent(this, ResourceShow.class);
                startActivity(resIntent);
                break;
            case R.id.settings:
                Intent setIntent = new Intent(this, SettingsActivity.class);
                startActivity(setIntent);
                break;
        }
        return true;
    }
}

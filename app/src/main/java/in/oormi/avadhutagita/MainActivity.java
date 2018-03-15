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
import android.widget.ScrollView;
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

    TextToSpeech tts, ttsHindi;

    String alerttone;
    String freqStr;
    Locale locale;

    boolean alertenable;
    boolean soundenable;
    boolean vibeenable;
    boolean ttsenable;
    float speechrate;
    int freq;
    boolean nonightrem;
    boolean automode;
    int randenable = 0;
    int lang = 2;

    PendingIntent pi;
    BroadcastReceiver br;
    AlarmManager am;

    boolean animrunning = false;
    boolean isTimerOn = false;

//    private float x1;
//    static final int MIN_DISTANCE = 150;

    int Verse = 0;
    public static ArrayList<String> allVerses = new ArrayList<>();
    final int[] chapMap = {76, 40, 46, 25, 32, 27, 15, 10};

    Animation animation1;
    Animation animation2;
    Animation animation3;
    Animation animation4;

    TextView tv1, tv2, tv3, tv4;
    ScrollView sv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        getSupportLoaderManager().initLoader(1, null,
                (LoaderManager.LoaderCallbacks<String>) this).forceLoad();

        Setup();
        setupTimer();


        TextView tv1 = (TextView)findViewById(R.id.textwhosaid);
        Typeface type = Typeface.createFromAsset(getAssets(),"Sanskrit.ttf");
        tv1.setTypeface(type);

        Verse = 0;
        //SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        //Verse = preferences.getInt("LastVerse", 0); //restart at the last verse
        //ShowVerse(true);
        //Toast.makeText(MainActivity.this, R.string.touchtostart, Toast.LENGTH_LONG).show();
        final TextView tvs = (TextView) findViewById(R.id.textstatus);

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
                        tvs.setText(getString(R.string.reading) + freqStr);
                    }
                } else {
                    stopTimer(true);
                    toggle.clearAnimation();
                    tvs.setText(R.string.touchtostart);
                    StopTTS();
                    //tts.stop();
                }
            }
        });

        final ImageButton mbuttonNext = (ImageButton) findViewById(R.id.imageButtonNext);
        mbuttonNext.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Verse++;
                        ShowVerse(true);
                        //if (isTimerOn) stopTimer(true);
                        //tvs.setText(getString(R.string.status_user));
                        mbuttonNext.startAnimation(animation4);
                    }
                });

        final ImageButton mbuttonPrev = (ImageButton) findViewById(R.id.imageButtonPrev);
        mbuttonPrev.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Verse--;
                        ShowVerse(true);
                        //if (isTimerOn) stopTimer(true);
                        //tvs.setText(getString(R.string.status_user));
                        mbuttonPrev.startAnimation(animation4);
                    }
                });

        //mbuttonNext.setImageAlpha(180);
        //mbuttonPrev.setImageAlpha(180);

    }

    public void ShowVerse(boolean sounds) {
        if (allVerses.size() < 1) return;

        StopTTS();

        String mLine = getString(R.string.cover);
        tv1.setText("");
        tv2.setText("");
        tv3.setText("");
        tv4.setText("");

        sv.scrollTo(0, 0);

        if (randenable > 0) {
            Random rand = new Random();
            Verse = rand.nextInt(allVerses.size());
        }

        if (Verse >= allVerses.size()) Verse = 0;
        if (Verse < 0) Verse = allVerses.size() - 1;

        mLine = allVerses.get(Verse);
        if (mLine == null) {
            mLine = getString(R.string.texterror);
        }

        if (Verse == 0) {
            tv1.setGravity(Gravity.CENTER);
            tv1.setTextSize(30.0f);
        } else {
            tv1.setGravity(Gravity.CENTER_VERTICAL);
            tv1.setTextSize(16.0f);
        }

        String[] verseContent;
        verseContent = mLine.split("@");

        if (verseContent.length > 0) {
            tv1.setText(verseContent[0]);
            if (verseContent.length > 1) tv2.setText(verseContent[1]);
            if (verseContent.length > 2) {
                verseContent[2] = verseContent[2].replace("~", "\n");
                tv3.setText(verseContent[2]);
            }
            if (verseContent.length > 3) {
                verseContent[3] = verseContent[3].replace("~", "\n");
                tv4.setText(verseContent[3]);
            }
            if ((sounds) && (Verse > 2)) {
                if (verseContent.length > 2) {
                    if (verseContent[2].length() > 26) {//do not speak chapter pages
                        if ((lang == 0) || (lang == 2)) {
                            int endIndex = verseContent[0].indexOf("।।");
                            if (endIndex >= 0) {
                                verseContent[0] = verseContent[0].subSequence(0, endIndex).toString();
                                verseContent[0] = verseContent[0].replace("।", " ");
                            }
                            verseContent[0] = verseContent[0].replace("\n", ". \n");
                            SoundAlert(verseContent[0], true);
                        }
                        if ((lang == 1) || (lang == 2)) {
                            verseContent[2] = verseContent[2].replace("-", " ");
                            verseContent[2] = verseContent[2].replace("\n\n", "\n");
                            SoundAlert(verseContent[2], false);
                        }
                    }
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

        } else {
            tv3.setText(R.string.errorbadformat);
        }


    }

    private void Setup() {
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_headers, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        animation1 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.remzoom);
        animation2 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fromleft);
        animation3 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fromright);
        animation4 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.press);

        tv1 = (TextView) findViewById(R.id.textwhosaid);
        tv2 = (TextView) findViewById(R.id.textversenum);
        tv3 = (TextView) findViewById(R.id.textverse);
        tv4 = (TextView) findViewById(R.id.textViewComments);
        sv = (ScrollView) findViewById(R.id.scrollViewVc);
    }

    private void SoundAlert(final String speakme, final boolean hindi) {
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
                if (hindi) {
                    ttsHindi = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                        @Override
                        public void onInit(int status) {
                            if (status == TextToSpeech.SUCCESS) {
                                Locale loc = null;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    loc = Locale.forLanguageTag("hi-IN");
                                }
                                int result = ttsHindi.setLanguage(loc);
                                if (result == TextToSpeech.LANG_MISSING_DATA ||
                                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    Log.e("TTS Error", "Language 0 is not supported");
                                } else {
                                    ttsHindi.setSpeechRate(speechrate);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        ttsHindi.speak(speakme, TextToSpeech.QUEUE_FLUSH, null,
                                                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                                    }else {
                                        ttsHindi.speak(speakme, TextToSpeech.QUEUE_ADD, null);
                                    }
                                }
                            } else
                                Log.e("TTS Error", "Initialization Failed!");
                        }
                    });
                } else {
                    tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                        @Override
                        public void onInit(int status) {
                            if (status == TextToSpeech.SUCCESS) {
                                int result = tts.setLanguage(locale);
                                if (result == TextToSpeech.LANG_MISSING_DATA ||
                                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    Log.e("TTS Error", "Language 1 is not supported");
                                } else {
                                    tts.setSpeechRate(speechrate);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        tts.speak(speakme, TextToSpeech.QUEUE_FLUSH, null,
                                                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                                    }else {
                                        tts.speak(speakme, TextToSpeech.QUEUE_ADD, null);
                                    }
                                }
                            } else
                                Log.e("TTS Error", "Initialization Failed!");
                        }
                    });
                }

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
    protected void onPause() {
        SharedPreferences preferences = getSharedPreferences("TimerOn", MODE_PRIVATE);
        isTimerOn = preferences.getBoolean("TimerOn", false);
        //Log.d("TIMER", "Timer is " + String.valueOf(isTimerOn));

        if (!isTimerOn) StopTTS(); //stop if user closes app and timer not running

        super.onPause();
    }

    private void StopTTS() {
        if (tts != null) {
            tts.stop();
        }
        if (ttsHindi != null) {
            ttsHindi.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        UpdateSettings();
    }

    public void startTimer() {
        int delay = 1000 * freq;

        if (delay > 60000) {
            String toastinfo = String.valueOf(delay / 60000) + getString(R.string.nxtremmintoast);
            //Toast.makeText(this, getString(R.string.nxtremtoast) + toastinfo, Toast.LENGTH_SHORT).show();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delay, pi);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                        + delay, pi);
            }else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                        + delay, pi );
            }
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

        }

        isTimerOn = true;
        SharedPreferences preferences = getSharedPreferences("TimerOn", MODE_PRIVATE);
        preferences.edit().putBoolean("TimerOn", isTimerOn).commit();
    }

    public void stopTimer(boolean stopanim) {
        if (am != null) {
            if (pi != null) {
                am.cancel(pi);
                isTimerOn = false;
                SharedPreferences preferences = getSharedPreferences("TimerOn", MODE_PRIVATE);
                preferences.edit().putBoolean("TimerOn", isTimerOn).commit();
            }
        }
        if (stopanim) {
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
                    boolean day = (hour < 22) && (hour > 5);
                    if (day) {
                        enabled = true;
                    }
                } else {
                    enabled = true;
                }

                if (enabled) {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK |
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
                } else {//keep looping anyway
                    stopTimer(false);
                    startTimer();
                }

            }
        };
        registerReceiver(br, new IntentFilter("in.oormi.agita"));
        pi = PendingIntent.getBroadcast(this, 0,
                new Intent("in.oormi.agita"), 0);
        am = (AlarmManager) (this.getSystemService(Context.ALARM_SERVICE));
    }

    @Override
    protected void onDestroy() {
        StopTTS();
        if (tts != null) tts.shutdown();
        if (ttsHindi != null) ttsHindi.shutdown();

        if (am != null) {
            if (pi != null) {
                am.cancel(pi);
            }
        }
        if (br != null) {
            unregisterReceiver(br);
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
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
        Verse = 0;
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Verse = preferences.getInt("LastVerse", 0); //restart at the last verse
        ShowVerse(true);
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

    /////////////




    //space for line num matching with A gita code





    ///////////

    private void gotoDialog() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(params);

        layout.setGravity(Gravity.CLIP_VERTICAL);
        layout.setPadding(10, 10, 10, 10);

        final TextView tv = new TextView(this);
        tv.setText(R.string.jumptitle);
        tv.setPadding(40, 40, 40, 40);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(20);

        alertDialogBuilder.setView(layout);
        alertDialogBuilder.setCustomTitle(tv);

        final TextView tvChapter = new TextView(this);
        tvChapter.setText(R.string.gotochapter);
        tvChapter.setPadding(80, 10, 10, 10);
        layout.addView(tvChapter);

        final SeekBar chapterBar = new SeekBar(this);
        chapterBar.setMax(7);
        chapterBar.setProgress(0);
        layout.addView(chapterBar);

        final TextView tvVerse = new TextView(this);
        tvVerse.setText(R.string.versenum);
        tvVerse.setPadding(80, 10, 10, 10);
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
                tvChapter.setText(getString(R.string.gotochapter) + " " + String.valueOf(progress + 1));
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
                tvVerse.setText(getString(R.string.versenum) + " " + String.valueOf(progress + 1));
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
        for (int c = 0; c < nchap; c++) skip = skip + chapMap[c];
        Verse = 4 + skip + nverse + 2 * nchap;
    }

    private ShareActionProvider mShareActionProvider;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.infomenu, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=in.oormi.avadhutagita");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                getString(R.string.shareSubject));
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
            case R.id.mute:
                StopTTS();
                ttsenable = !ttsenable;
                if (ttsenable) item.setIcon(android.R.drawable.ic_lock_silent_mode_off);
                else item.setIcon(android.R.drawable.ic_lock_silent_mode);
                break;
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

    private void UpdateSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        alerttone = prefs.getString("notifications_new_message_ringtone",
                "content://settings/system/notification_sound");
        soundenable = prefs.getBoolean("notifications_tone", false);
        alertenable = prefs.getBoolean("notifications_new_message", true);
        vibeenable = prefs.getBoolean("notifications_new_message_vibrate", false);
        ttsenable = prefs.getBoolean("notifications_new_message_speak", true);

        speechrate = 1.0f;
        String strrate = prefs.getString("rate_list", "1");
        if ((strrate == null) || (strrate.length() < 1)) strrate = "1";
        if (strrate.equals("0")) speechrate = 0.85f;
        if (strrate.equals("1")) speechrate = 1.0f;
        if (strrate.equals("2")) speechrate = 1.25f;

        String localestr = prefs.getString("locale_list", "0");
        if ((localestr == null) || (localestr.length() < 1)) localestr = "0";
        if (localestr.equals("0")) locale = Locale.getDefault();
        if (localestr.equals("1")) locale = Locale.US;
        if (localestr.equals("2")) locale = Locale.UK;

        String langstr = prefs.getString("lang_list", "2");
        if ((langstr == null) || (langstr.length() < 1)) langstr = "2";
        if (langstr.equals("0")) lang = 0;
        if (langstr.equals("1")) lang = 1;
        if (langstr.equals("2")) lang = 2;

        freqStr = prefs.getString("freq_list", "60");
        if (freqStr.length() < 1) freqStr = "60";
        freq = 60 * Integer.parseInt(freqStr);
        freqStr = freqStr + getString(R.string.nxtremmintoast);

        nonightrem = prefs.getBoolean("night_switch", true);
        if (freq < 1) {
            freq = 20;
            freqStr = getString(R.string.autoreadtimedisp);
            nonightrem = false;//autoread in night too
        }

        automode = prefs.getBoolean("auto_switch", false);
        if (automode) {
            nonightrem = false;//autoread in night too
            freq = 20;
        }
        String srandenable = prefs.getString("order_list", "0");
        randenable = Integer.parseInt(srandenable);
    }
}

package by.klnvch.link5dots;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import by.klnvch.link5dots.settings.SettingsUtils;

public class MainActivity extends AppCompatActivity {

    private GameView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_board);

        //restore view
        view = (GameView) findViewById(R.id.game_view);

        view.setOnGameEventListener(new GameView.OnGameEventListener() {
            @Override
            public void onMoveDone(Dot currentDot, Dot previousDot) {
                if (previousDot == null || previousDot.getType() == Dot.OPPONENT) {
                    // set user dot
                    currentDot.setType(Dot.USER);
                    view.setDot(currentDot);
                    // set bot dot
                    Dot botDot = Bot.findAnswer(view.getCopyOfNet());
                    botDot.setType(Dot.OPPONENT);
                    view.setDot(botDot);
                }
            }

            @Override
            public void onGameEnd(HighScore highScore) {
                showAlertDialog(highScore);
            }
        });

        String username = SettingsUtils.getUserName(this, null);
        if (username != null) {
            TextView tvUsername = (TextView) findViewById(R.id.user_name);
            tvUsername.setText(username);
        }

        ((App) getApplication()).getTracker();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
        //
        FirebaseAuth.getInstance().signInAnonymously();
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.restore(getPreferences(MODE_PRIVATE));
        view.invalidate();
        view.isOver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.save(getPreferences(MODE_PRIVATE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_undo:
                undoLastMove();
                return true;
            case R.id.menu_new_game:
                newGame();
                return true;
            case R.id.menu_search:
                searchLastMove();
                return true;
        }
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        searchLastMove();
        return true;
    }

    private void showAlertDialog(final HighScore highScore) {
        //final long gameStatus = data.getLong(GAME_STATUS);
        //final long numberOfMoves = data.getLong(NUMBER_OF_MOVES);
        //final long timeElapsed = data.getLong(ELAPSED_TIME);

        final String title;
        if (highScore.getStatus() == HighScore.WON) {
            title = getString(R.string.end_win);
        } else {
            title = getString(R.string.end_lose);
        }
        String str = getString(R.string.end_move, highScore.getScore(), highScore.getTime());

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(str)
                .setPositiveButton(R.string.end_new_game, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        newGame();
                    }
                })
                .setNeutralButton(R.string.scores_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        moveToScores();
                    }
                })
                .setNegativeButton(R.string.end_undo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        undoLastMove();
                    }
                })
                .show();
    }

    private void undoLastMove() {
        HighScore highScore = view.getHighScore();
        if (highScore != null) publishScore(highScore);
        view.undoLastMove(2);
        //
        Tracker tracker = ((App) getApplication()).getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Main")
                .setAction("Undo")
                .build());
    }

    private void newGame() {
        HighScore highScore = view.getHighScore();
        if (highScore != null) publishScore(highScore);
        view.resetGame();
        //
        Tracker tracker = ((App) getApplication()).getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Main")
                .setAction("New")
                .build());
    }

    private void moveToScores() {
        startActivity(new Intent(this, ScoresActivity.class));
        //
        Tracker tracker = ((App) getApplication()).getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Main")
                .setAction("Scores")
                .build());
    }

    @SuppressLint("HardwareIds")
    private void publishScore(@NonNull HighScore highScore) {
        String userId;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            highScore.setUserId(userId);
        }
        highScore.setAndroidId(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        highScore.setUserName(SettingsUtils.getUserName(this, null));

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        String key = mDatabase.child("high_scores").push().getKey();
        Map<String, Object> postValues = highScore.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/high_scores/" + key, postValues);
        mDatabase.updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                //Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    private void searchLastMove() {
        view.switchHideArrow();
        //
        Tracker tracker = ((App) getApplication()).getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Main")
                .setAction("Search")
                .build());
    }
}

package by.klnvch.link5dots;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class MainActivity extends Activity {
	
	private GameView view;
	private Bot bot;
    private AlertDialog alertDialog = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.play);
		
		//restore view
		view = (GameView)findViewById(R.id.game_view);

        bot = new Bot(view.getPointerToNet());

        view.setOnGameEventListener(new GameView.OnGameEventListener() {
            @Override
            public void onUserMoveDone(Offset dot) {
                Offset botDot = bot.findAnswer();
                view.setOpponentDot(botDot);
            }

            @Override
            public void onGameEnd(HighScore highScore) {
                showAlertDialog(highScore);
            }
        });

        ((MyApplication)getApplication()).getTracker();
	}

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
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
		
		switch(item.getItemId()){
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

    private void showAlertDialog(final HighScore highScore){
        if(alertDialog == null || !alertDialog.isShowing()){
            //final long gameStatus = data.getLong(GAME_STATUS);
            //final long numberOfMoves = data.getLong(NUMBER_OF_MOVES);
            //final long timeElapsed = data.getLong(ELAPSED_TIME);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if(highScore.getStatus() == HighScore.WON){
                builder.setTitle(R.string.end_win);
            }else{
                builder.setTitle(R.string.end_lose);
            }
            String str = getString(R.string.end_move, highScore.getScore(), highScore.getTime());
            builder.setMessage(str);
            builder.setPositiveButton(R.string.end_new_game, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    newGame();
                }
            });
            builder.setNeutralButton(R.string.end_publish, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    publishScore(highScore);
                }
            });
            builder.setNegativeButton(R.string.end_undo, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    undoLastMove();
                }
            });
            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    private void undoLastMove(){
        view.undoLastMove();
        //
        Tracker tracker = ((MyApplication)getApplication()).getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Main")
                .setAction("Undo")
                .build());
    }

    private void newGame(){
        view.resetGame();
        //
        Tracker tracker = ((MyApplication)getApplication()).getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Main")
                .setAction("New")
                .build());
    }

    private void publishScore(HighScore highScore){
        Intent i = new Intent(MainActivity.this, ScoresActivity.class);
        Bundle data = new Bundle();
        data.putLong(HighScore.GAME_STATUS, highScore.getStatus());
        data.putLong(HighScore.NUMBER_OF_MOVES, highScore.getScore());
        data.putLong(HighScore.ELAPSED_TIME, highScore.getTime());
        i.putExtras(data);
        startActivity(i);
        //
        Tracker tracker = ((MyApplication)getApplication()).getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Main")
                .setAction("Publish")
                .build());
    }

    private void searchLastMove(){
        view.switchHideArrow();
        //
        Tracker tracker = ((MyApplication)getApplication()).getTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Main")
                .setAction("Search")
                .build());
    }
}

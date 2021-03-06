package by.klnvch.link5dots;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

public class Game {

    private static final String TAG = "Game";

    private static final String START_TIME = "START_TIME";
    private static final String MOVES_DONE = "MOVES_DONE";
    private static final String HIGH_SCORE = "HIGH_SCORE";

    // json variables
    private static final String VERSION = "VERSION";
    private static final String GAME_ID = "GAME_ID";
    private static final String HOST_NAME = "HOST_NAME";
    private static final String GUEST_NAME = "GUEST_NAME";
    private static final String NET = "NET";
    private static final String TURN_COUNTER = "TURN_COUNTER";


    private static final int N = 20;
    private static final int M = 20;
    public final Dot[][] net = new Dot[N][M];
    // important data
    private final long id;
    private String hostName;
    private String guestName;
    private HighScore currentScore = null;
    private ArrayList<Dot> winningLine = null;
    private int movesDone = 0;
    private long startTime;
    public int turnCounter = 0;


    public Game(String hostName, String guestName) {

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                net[i][j] = new Dot(i, j);
            }
        }

        id = (new Random()).nextLong();

        this.hostName = hostName;
        this.guestName = guestName;
    }

    private Game(long id) {
        this.id = id;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public void restore(SharedPreferences pref) {
        //
        startTime = pref.getLong(START_TIME, 0);
        //
        movesDone = pref.getInt(MOVES_DONE, 0);
        //restore dots table
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                int type = pref.getInt("dottype(" + Integer.toString(i) + "," + Integer.toString(j) + ")", Dot.EMPTY);
                int number = pref.getInt("dotnum(" + Integer.toString(i) + "," + Integer.toString(j) + ")", -1);

                net[i][j].changeStatus(type, number);
            }
        }
        //
        final long currentScoreCode = pref.getLong(HIGH_SCORE, -1);
        if (currentScoreCode != -1) {
            currentScore = new HighScore(currentScoreCode);
        }

        //other
        winningLine = isOver();
    }

    public void restore(Bundle bundle) {
        //
        startTime = bundle.getLong(START_TIME, 0);
        //
        movesDone = bundle.getInt(MOVES_DONE, 0);
        //restore dots table
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                int type = bundle.getInt("dottype(" + Integer.toString(i) + "," + Integer.toString(j) + ")", Dot.EMPTY);
                int number = bundle.getInt("dotnum(" + Integer.toString(i) + "," + Integer.toString(j) + ")", -1);

                net[i][j].changeStatus(type, number);
            }
        }
        //
        final long currentScoreCode = bundle.getLong(HIGH_SCORE, -1);
        if (currentScoreCode != -1) {
            currentScore = new HighScore(currentScoreCode);
        }

        //other
        winningLine = isOver();
    }

    public void save(SharedPreferences pref) {
        Editor editor = pref.edit();
        //
        editor.putLong(START_TIME, startTime);
        //
        editor.putInt(MOVES_DONE, movesDone);
        //save info about dots
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                editor.putInt("dottype(" + Integer.toString(i) + "," + Integer.toString(j) + ")", net[i][j].getType());
                editor.putInt("dotnum(" + Integer.toString(i) + "," + Integer.toString(j) + ")", net[i][j].getNumber());
            }
        }
        //
        if (currentScore != null) {
            editor.putLong(HIGH_SCORE, currentScore.code());
        } else {
            editor.putLong(HIGH_SCORE, -1);
        }
        //
        editor.apply();
    }

    public void save(Bundle bundle) {
        bundle.putLong(START_TIME, startTime);
        //
        bundle.putInt(MOVES_DONE, movesDone);
        //save info about dots
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                bundle.putInt("dottype(" + Integer.toString(i) + "," + Integer.toString(j) + ")", net[i][j].getType());
                bundle.putInt("dotnum(" + Integer.toString(i) + "," + Integer.toString(j) + ")", net[i][j].getNumber());
            }
        }
        //
        if (currentScore != null) {
            bundle.putLong(HIGH_SCORE, currentScore.code());
        } else {
            bundle.putLong(HIGH_SCORE, -1);
        }
    }

    public void reset() {

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                net[i][j].changeStatus(Dot.EMPTY, -1);
            }
        }

        movesDone = 0;

        winningLine = null;
    }

    private void prepareScore() {
        long time = System.currentTimeMillis() / 1000 - startTime;

        if (winningLine != null) {

            if (winningLine.get(0).getType() == Dot.USER) {
                currentScore = new HighScore(movesDone, time, HighScore.WON);
            } else {
                currentScore = new HighScore(getNumberOfMoves(), time, HighScore.LOST);
            }

        }
    }

    public void setDot(int x, int y, int type) {

        Dot theLastDot = getLastDot();
        if (!checkCorrectness(x, y) || (theLastDot != null && theLastDot.getType() == type)) {
            return;
        }

        if (getNumberOfMoves() == 0) {//it is the first move, start stop watch
            startTime = System.currentTimeMillis() / 1000;
        }
        if (type == Dot.USER) {
            movesDone++;
        }
        net[x][y].changeStatus(type, getNumberOfMoves());
        winningLine = isOver();

        if (winningLine != null) {
            prepareScore();
        }
    }

    private int getNumberOfMoves() {
        Dot dot = getLastDot();
        if (dot != null) {
            return dot.getNumber() + 1;
        }
        return 0;
    }

    public boolean checkCorrectness(int x, int y) {
        return isInBound(x, y) && net[x][y].getType() == Dot.EMPTY && winningLine == null;
    }

    private boolean isInBound(int x, int y) {
        return x >= 0 && y >= 0 && x < N && y < M;
    }

    private ArrayList<Dot> getDotsNumber(Dot dot, int dx, int dy) {

        int x = dot.getX();
        int y = dot.getY();

        ArrayList<Dot> result = new ArrayList<>();
        result.add(dot);

        for (int k = 1; (k < 5) && isInBound(x + dx * k, y + dy * k) && net[x + dx * k][y + dy * k].getType() == dot.getType(); k++) {
            result.add(net[x + dx * k][y + dy * k]);
        }
        for (int k = 1; (k < 5) && isInBound(x - dx * k, y - dy * k) && net[x - dx * k][y - dy * k].getType() == dot.getType(); k++) {
            result.add(0, net[x - dx * k][y - dy * k]);
        }

        return result;
    }

    /**
     * Checks if five dots line has been built
     *
     * @return null or array of five dots
     */
    public ArrayList<Dot> isOver() {

        if (winningLine == null) {
            if (getNumberOfMoves() < 5) return null;

            Dot lastDot = getLastDot();

            if (lastDot == null) return null;

            ArrayList<Dot> result;

            result = getDotsNumber(lastDot, 1, 0);
            if (result.size() >= 5) {
                winningLine = result;
                return result;
            }

            result = getDotsNumber(lastDot, 1, 1);
            if (result.size() >= 5) {
                winningLine = result;
                return result;
            }

            result = getDotsNumber(lastDot, 0, 1);
            if (result.size() >= 5) {
                winningLine = result;
                return result;
            }

            result = getDotsNumber(lastDot, -1, 1);
            if (result.size() >= 5) {
                winningLine = result;
                return result;
            }

            return null;
        } else {
            return winningLine;
        }
    }

    public void undo(int moves) {

        for (int i = 0; i != moves; ++i) {
            Dot d = getLastDot();
            if (d != null) {
                d.changeStatus(Dot.EMPTY, -1);
            }
        }

        winningLine = null;
        winningLine = isOver();

    }

    public HighScore getCurrentScore() {
        return currentScore;
    }

    public Dot getLastDot() {

        Dot result = net[0][0];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                if (result.getNumber() < net[i][j].getNumber()) {
                    result = net[i][j];
                }
            }
        }

        if (result.getNumber() == -1) return null;
        else return result;
    }

    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                result += net[i][j] + " ";
            }
            result += "\n";
        }
        return result;
    }

    public byte[] toByteArray() {

        try {
            JSONObject result = new JSONObject();
            result.put(VERSION, 1);
            result.put(GAME_ID, id);
            result.put(HOST_NAME, hostName);
            result.put(GUEST_NAME, guestName);
            result.put(TURN_COUNTER, turnCounter);

            JSONArray netArray = new JSONArray();
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    JSONObject dot = new JSONObject();
                    dot.put(Dot.TYPE, net[i][j].getType());
                    dot.put(Dot.TIMESTAMP, net[i][j].getTimestamp());
                    netArray.put(dot);
                }
            }

            result.put(NET, netArray);

            return result.toString().getBytes("UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "toByteArray: " + e.getMessage());
        }
        return null;
    }


    public static Game parseByteArray(byte[] bytes) {

        try {
            JSONObject jsonObject = new JSONObject(new String(bytes, "UTF-8"));

            long gameId = jsonObject.getLong(GAME_ID);

            Game game = new Game(gameId);

            game.setHostName(jsonObject.getString(HOST_NAME));
            game.setGuestName(jsonObject.getString(GUEST_NAME));
            game.turnCounter = jsonObject.getInt(TURN_COUNTER);
            JSONArray netArray = jsonObject.getJSONArray(NET);
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    JSONObject dot = netArray.getJSONObject(i * N + j);
                    int type = dot.getInt(Dot.TYPE);
                    long timestamp = dot.getLong(Dot.TIMESTAMP);
                    game.net[i][j] = new Dot(i, j, type, timestamp);
                }
            }

            return game;
        } catch (Exception e) {
            Log.e(TAG, "parseByteArray: " + e.getMessage());
        }
        return null;
    }
}
package by.klnvch.link5dots;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScoresActivity extends AppCompatActivity {

    private static final String TAG = "ScoresActivity";

    private RecyclerView mRecyclerView;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scores);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    updateData();
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }
        };

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateData();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInAnonymously", task.getException());
                            Toast.makeText(ScoresActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scores_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.scores_update:
                updateData();
                return true;
        }
        return false;
    }

    private void updateData() {
        mSwipeRefreshLayout.setRefreshing(true);
        //
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        Query query = mDatabase.child("high_scores").orderByChild("score");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<HighScore> scores = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    HighScore highScore = child.getValue(HighScore.class);
                    highScore.decode(highScore.getScore());
                    scores.add(highScore);
                }
                mRecyclerView.setAdapter(new MyAdapter(scores));
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView mPosition;
        TextView mUserName;
        TextView mMoves;
        TextView mTime;
        TextView mStatus;

        ViewHolder(View v) {
            super(v);
            mPosition = (TextView) v.findViewById(R.id.position);
            mUserName = (TextView) v.findViewById(R.id.user_name);
            mMoves = (TextView) v.findViewById(R.id.moves);
            mTime = (TextView) v.findViewById(R.id.time);
            mStatus = (TextView) v.findViewById(R.id.status);
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<ViewHolder> {
        private List<HighScore> mDataset;

        MyAdapter(List<HighScore> dataset) {
            mDataset = dataset;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_high_score, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            HighScore highScore = mDataset.get(position);
            holder.mPosition.setText(String.format(Locale.getDefault(), "%d.", position + 1));
            holder.mUserName.setText(highScore.getUserName());
            holder.mMoves.setText(String.format(Locale.getDefault(), "%d", highScore.getScore()));
            holder.mTime.setText(String.format(Locale.getDefault(), "%d", highScore.getTime()));
            switch ((int) highScore.getStatus()) {
                case (int) HighScore.WON:
                    holder.mStatus.setText(R.string.scores_won);
                    break;
                case (int) HighScore.LOST:
                    holder.mStatus.setText(R.string.scores_lost);
                    break;
                default:
                    holder.mStatus.setText("");
                    break;
            }

        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
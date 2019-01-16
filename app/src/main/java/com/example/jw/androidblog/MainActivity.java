package com.example.jw.androidblog;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDataBase;
    private DatabaseReference mDataBaseUsers;
    private DatabaseReference mDataBaseLike;
    private DatabaseReference mDatabaseCurrentUser;
    private Query mQueryCurrentUser;
    private RecyclerView mBlogList;
    private FirebaseRecyclerAdapter<Blog, BlogViewHolder> mAdapter;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private boolean mProcessLike = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() == null){
                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(loginIntent);
                }
            }
        };

        mDataBase = FirebaseDatabase.getInstance().getReference().child("Blog");
        mDataBaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        mDataBaseLike = FirebaseDatabase.getInstance().getReference().child("Likes");


        String currentUserId = mAuth.getCurrentUser().getUid();
        mDatabaseCurrentUser =  FirebaseDatabase.getInstance().getReference().child("Blog");

        mQueryCurrentUser = mDatabaseCurrentUser.orderByChild("uid").equalTo(currentUserId);


        mDataBaseUsers.keepSynced(true);
        mDataBaseLike.keepSynced(true);
        mDataBase.keepSynced(true);

        mBlogList = (RecyclerView) findViewById(R.id.blogid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        mBlogList.setHasFixedSize(true);
        mBlogList.setLayoutManager(layoutManager);

        checkUserExists();

    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mAuth.addAuthStateListener(mAuthListener);

        FirebaseRecyclerOptions<Blog> options;
        options = new FirebaseRecyclerOptions.Builder<Blog>()
                .setQuery(mQueryCurrentUser, Blog.class)
                .build();

        mAdapter = new FirebaseRecyclerAdapter<Blog, BlogViewHolder>(
                options
        ) {
            @Override
            protected void onBindViewHolder(@NonNull BlogViewHolder holder, int position, @NonNull Blog model) {

                final String post_key = getRef(position).getKey();

                holder.setTitle(model.getTitle());
                holder.setDesc(model.getDesc());
                holder.setImage(model.getImage());
                holder.setUsername(model.getUsername());
                holder.setLikeBtn(post_key);

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(MainActivity.this,post_key, Toast.LENGTH_LONG).show();
                        Intent singleBlogIntent = new Intent(MainActivity.this, BlogSingleActivity.class);
                        singleBlogIntent.putExtra("blog_id", post_key);
                        startActivity(singleBlogIntent);
                    }
                });

                holder.mLikeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mProcessLike = true;


                            mDataBaseLike.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(mProcessLike) {
                                        if (dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())) {
                                            mDataBaseLike.child(post_key).child(mAuth.getCurrentUser().getUid()).removeValue();
                                            mProcessLike = false;

                                        } else {
                                            mDataBaseLike.child(post_key).child(mAuth.getCurrentUser().getUid()).setValue("RandomValue");
                                            mProcessLike = false;
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                    }
                });
            }

            @NonNull
            @Override
            public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_row, parent, false);

                return new BlogViewHolder(view);
            }
        };

        mAdapter.startListening();

        mBlogList.setAdapter(mAdapter);
    }


    private void checkUserExists() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            //Go to login
            Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
            setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(setupIntent);
        }
        else {
           final String user_id = mAuth.getCurrentUser().getUid();
            mDataBaseUsers.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.hasChild(user_id)){
                        Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(setupIntent);

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }


    public static class BlogViewHolder extends RecyclerView.ViewHolder{

        View mView;
        ImageButton mLikeBtn;
        DatabaseReference mDataBaseLike;
        FirebaseAuth mAuth;

        public BlogViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

            mLikeBtn = (ImageButton) mView.findViewById(R.id.like_btn);
            mDataBaseLike = FirebaseDatabase.getInstance().getReference().child("Likes");
            mAuth = FirebaseAuth.getInstance();

            mDataBaseLike.keepSynced(true);

        }

        public void setLikeBtn(final String post_key){
            mDataBaseLike.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if(dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())){
                        mLikeBtn.setImageResource(R.mipmap.baseline_thumb_up_alt_red_36);
                    }else{
                        mLikeBtn.setImageResource(R.mipmap.baseline_thumb_up_alt_gray_36);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }

        public void setTitle(String title){
            TextView post_title = (TextView) mView.findViewById(R.id.post_title);
            post_title.setText(title);
        }

        public void setDesc(String desc){
            TextView post_desc = (TextView) mView.findViewById(R.id.post_desc);
            post_desc.setText(desc);
        }

        public void setUsername(String username){
            TextView post_username = (TextView) mView.findViewById(R.id.post_username);
            post_username.setText(username);
        }

        public void setImage(final String image){
           final ImageView post_image  = mView.findViewById(R.id.post_image);
            //Picasso.get().load(image).into(post_image);

            Picasso.get().load(image).centerCrop().resize(390, 185).into(post_image, new Callback() {
                @Override
                public void onSuccess() {
                    Log.d("TAG", "onSuccess");
                }

                @Override
                public void onError(Exception e) {
                    Log.d("TAG", "image:failure " + image);
                    Picasso.get().load(image).resize(333, 185).into(post_image);
                    //Picasso.get().load("http://i.imgur.com/DvpvklR.png").into(post_image);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.action_add)
        {
            startActivity(new Intent(MainActivity.this, PostActivity.class));
        }

        if(item.getItemId() == R.id.action_logout)
        {
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        mAuth.signOut();
    }
}


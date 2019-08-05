package com.pierratonoc.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


public class AdminActivity extends AppCompatActivity {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private static final int PICTURE_RESULT = 42;
    EditText txtTitle;
    EditText txtPrice;
    EditText txtDescription;
    TravelDeal deal;
    Button btnImage;
    ImageView image;
    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        //FirebaseUtil.openFbReference("traveldeals");
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        txtTitle = findViewById(R.id.txtTitle);
        image = findViewById(R.id.image);
        txtDescription = findViewById(R.id.txtDescription);
        txtPrice = findViewById(R.id.txtPrice);
        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal == null) {
            deal = new TravelDeal();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());
        btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent,
                        "Insert Picture"), PICTURE_RESULT);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal Saved", Toast.LENGTH_LONG).show();
                clean();
                backTolist();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal Deleted", Toast.LENGTH_LONG).show();
                backTolist();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void clean() {
        txtTitle.setText("");
        txtDescription.setText("");
        txtPrice.setText("");
        txtTitle.requestFocus();

    }

    private void saveDeal() {
        deal.setTitle(txtTitle.getText().toString());
        deal.setDescription(txtDescription.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        if (deal.getId() == null) {
            mDatabaseReference.push().setValue(deal);
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }
    }

    private void deleteDeal() {
        if (deal == null) {
            Toast.makeText(this, "please save deal before deleting", Toast.LENGTH_LONG).show();

        }
        mDatabaseReference.child(deal.getId()).removeValue();
        if (deal.getImageUrl() != null && !deal.getImageUrl().isEmpty()) {
            StorageReference picref = FirebaseUtil.mStorage.getReference().child(deal.getImageUrl());
            picref.delete().addOnSuccessListener(this, new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete image", "Image Successfully Deleted");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image", e.getMessage());

                }
            });
        }
    }

    private void backTolist() {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtil.isAdmin) {
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enabledEditTexts(true);
            findViewById(R.id.btnImage).setEnabled(true);
        } else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enabledEditTexts(false);
            findViewById(R.id.btnImage).setEnabled(false);


        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            mImageUri = data.getData();

            Toast.makeText(this, "Image Selected", Toast.LENGTH_SHORT).show();

//            Picasso.get().load(mImageUri).into(image);

            final StorageReference ref = FirebaseUtil.mStorageRef.child(mImageUri.getLastPathSegment());
            UploadTask uploadTask = ref.putFile(mImageUri);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return ref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String url = downloadUri.toString();
                        String pictureName = downloadUri.getPath();
                        deal.setImageUrl(url);
                        deal.setImageName(pictureName);
                        Log.d("Url: ", url);
                        Log.d("Name", pictureName);
                        showImage(url);

                        Toast.makeText(AdminActivity.this, "Image Uploaded", Toast.LENGTH_SHORT).show();

                    } else {

                        // Handle failures
                        // ...
                    }
                }
            });


        }
    }

    private void showImage(String url) {
        if (url != null && url.isEmpty() == false) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get()
                    .load(url)
                    .resize(width, width * 2 / 3)
                    .centerCrop()
                    .into(image);

        }
    }

    private void enabledEditTexts(boolean isEnabled) {
        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
        if (isEnabled == false) {
            btnImage.setVisibility(View.GONE);
        }

    }
}

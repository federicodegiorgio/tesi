package fede.tesi.mqttplantanalyzer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import fede.tesi.mqttplantanalyzer.databinding.ActivityMainBinding;

public class ImageActivity extends Activity {

    FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storageRef = storage.getReference();
        setContentView(R.layout.image_alert);
        ImageView image= (ImageView) findViewById(R.id.dialog_imageview);
        Intent i =getIntent();
        String res=i.getStringExtra("imm");
        StorageReference pathReference=storageRef.child(res);
        final long ONE_MEGABYTE = 1024 * 1024;
        pathReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/island.jpg" is returns, use this as needed
                image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }
}

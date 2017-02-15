package com.ff.imageupload;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private ImageView image;
    private Button uploadButton;
    private Bitmap bitmap;
    private Button selectImageButton;

    // number of images to select
    private static final int PICK_IMAGE = 1;

    /**
     * called when the activity is first created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // find the views
        image = (ImageView) findViewById(R.id.uploadImage);
        uploadButton = (Button) findViewById(R.id.uploadButton);

        // on click select an image
        selectImageButton = (Button) findViewById(R.id.selectImageButton);
        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "이미지 선택 버튼 클릭", Toast.LENGTH_SHORT).show();
                selectImageFromGallery();
            }
        });

        // when uploadButton is clicked
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ImageUploadTask().execute();
            }
        });
    }

    /**
     * Opens dialog picker, so the user can select image from the gallery. The
     * result is returned in the method <code>onActivityResult()</code>
     */
    public void selectImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "갤러리 선택이욤!!!"), PICK_IMAGE);
    }

    /**
     * Retrives the result returned from selecting image, by invoking the method
     * <code>selectImageFromGallery()</code>
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Log.d("selectedImage ===== ", selectedImage + "입니다");       // selectedImage -> content://com.android.providers.media.documents/document/image%3A407182
            Log.d("filePathColumn ===== ", filePathColumn + "입니다");     // filePathColumn -> [Ljava.lang.String;@41af8ea8

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            Log.d("cursor ===== ", cursor + "입니다");       // cursor -> android.content.ContentResolver$CursorWrapperInner@41af3778
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            Log.d("columnIndex ===== ", columnIndex + "입니다");           // columnIndex -> 0
            String picturePath = cursor.getString(columnIndex);
            Log.d("picturePath ===== ", picturePath + "입니다");           // picturePath -> null
            cursor.close();

            decodeFile(picturePath);
        }
    }

    /**
     * The method decodes the image file to avoid out of memory issues. Sets the
     * selected image in to the ImageView.
     *
     * @param filePath
     */
    public void decodeFile(String filePath) {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 1024;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp < REQUIRED_SIZE && height_tmp < REQUIRED_SIZE)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        bitmap = BitmapFactory.decodeFile(filePath, o2);

        image.setImageBitmap(bitmap);
    }

    /**
     * The class connects with server and uploads the photo
     *
     *
     */
    class ImageUploadTask extends AsyncTask<Void, Void, String> {
        private String webAddressToPost = "http://your-website-here.com";

        // private ProgressDialog dialog;
        private ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Uploading...");
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();
                HttpPost httpPost = new HttpPost(webAddressToPost);

                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                byte[] data = bos.toByteArray();
                String file = Base64.encodeBytes(data);
                entity.addPart("uploaded", new StringBody(file));

                entity.addPart("someOtherStringToSend", new StringBody("your string here"));

                httpPost.setEntity(entity);
                HttpResponse response = httpClient.execute(httpPost, localContext);
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

                String sResponse = reader.readLine();
                return sResponse;
            } catch (Exception e) {
                // something went wrong. connection with the server error
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            dialog.dismiss();
            Toast.makeText(getApplicationContext(), "file uploaded", Toast.LENGTH_LONG).show();
        }
    }
}

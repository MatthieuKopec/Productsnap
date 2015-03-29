package com.staging.shotgun.productsnap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.org.apache.http.concurrent.FutureCallback;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.ion.Ion;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;


public class MainActivity extends Activity {

    String mFileName = "pic.jpg";
    String mDealDir = "";
    String mCurrentPhotoPath;

    EditText mQuantity;
    EditText mDuration;
    EditText mPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mQuantity = (EditText) findViewById(R.id.deal_quantity);
        mDuration = (EditText) findViewById(R.id.deal_duration);
        mPrice = (EditText) findViewById(R.id.deal_price);

        mFileName = settings.getString("filename", null);
        mCurrentPhotoPath = settings.getString("filepath", null);

        File imgFile = new File(mCurrentPhotoPath);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ImageView myImage = (ImageView) findViewById(R.id.imageTest);
            myImage.setImageBitmap(myBitmap);
        }

        askTitleImage();
    }

    private void execTask() {
        UploadFileTask task = new UploadFileTask(this);
        task.execute();
    }

    private void askTitleImage() {
        final File imgFile = new File(mCurrentPhotoPath);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(getString(R.string.dialog_asktitle));

        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Editable value = input.getText();
                if (value.length() > 0)
                    mFileName = value.toString() + ".jpg";
                File to = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), mFileName);
                imgFile.renameTo(to);
                mCurrentPhotoPath = to.getAbsolutePath();
                execTask();
            }
        });

        alert.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                execTask();
            }
        });

        alert.show();
    }

    private class UploadFileTask extends AsyncTask<Void, Void, Integer> {

        Context mContext;

        UploadFileTask(Context c) {
            mContext = c;
        }
        protected Integer doInBackground(Void... param) {

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageDirName = "DEAL_" + timeStamp;
            String directory = "deals";
            mDealDir = imageDirName;

            AWSCredentials credentials = new BasicAWSCredentials("ACCESS_ID", "ACCESS_KEY"); //replace with your own id and key
            AmazonS3 s3client = new AmazonS3Client(credentials);
            Region region = Region.getRegion(Regions.US_WEST_2);
            try {
                s3client.setRegion(region);
                s3client.putObject(new PutObjectRequest("shotgun-staging", directory + "/" + imageDirName + "/" + mFileName,
                        new File(mCurrentPhotoPath)));
            } catch (AmazonClientException ex){
                Log.e("Productsnap", "exception", ex);
            }
            return 1;
        }
    }

    private class UploadDealTask extends AsyncTask<DealFactory, Void, Integer> {

        Context mContext;

        UploadDealTask(Context c) {
            mContext = c;
        }
        protected Integer doInBackground(DealFactory... params) {

            int count = params.length;
            DealFactory current;
            HttpClient client = new DefaultHttpClient();
            HttpResponse response;

            for (int i = 0; i < count; i++) {
                try {
                    current = params[i];
                    HttpPost request = new HttpPost("https://shotgun-api-staging.herokuapp.com/v2/deals");
                    StringEntity body = new StringEntity(current.getMyBody().toString().replace("\\", ""), "UTF-8");
                    request.addHeader("Authorization", "API_TOKEN"); //replace with api Authorization token
                    request.addHeader("Content-Type", "application/json");
                    request.setEntity(body);
                    response = client.execute(request);
                    /*HttpEntity entity = response.getEntity();
                    String responseString = EntityUtils.toString(entity, "UTF-8");*/
           } catch (Exception e) {
                    Log.e("Productsnap", "exception", e);
                } finally {
                    client.getConnectionManager().shutdown();
                }

            }
            return 0;
        }
    }

    private String expireDate(int duration) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, duration);
        return (sdf.format(c.getTime()));
    }

    public void clickSendDeal(View view) {

        String quantity = mQuantity.getText().toString();
        String duration = mDuration.getText().toString();
        String price = mPrice.getText().toString();
        String filename = mFileName;

        if (mFileName.indexOf(".") > 0)
            filename = mFileName.substring(0, mFileName.lastIndexOf("."));
        if (quantity.matches("") || duration.matches("") || price.matches(""))
            Toast.makeText(this, R.string.toast_invalidInput, Toast.LENGTH_SHORT).show();
        else {
            String exp = expireDate(Integer.parseInt(duration));
            Map data = new HashMap();
            data.put("price", Integer.parseInt(price));
            data.put("quantity", Integer.parseInt(quantity));
            data.put("title", filename);
            data.put("dealer_id", 1);
            data.put("pictures", mDealDir + "/" + mFileName);
            data.put("expires_at", exp + "T00:00:00.000Z");
            data.put("use_by", exp + "T00:00:00.000Z");
            DealFactory newDeal = new DealFactory(data);
            if (!newDeal.getError()) {
                UploadDealTask task = new UploadDealTask(this);
                task.execute(newDeal);
            }
            else
                Toast.makeText(this, R.string.toast_processErr, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

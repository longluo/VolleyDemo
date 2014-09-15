package com.longluo.volleydemo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.AuthFailureError;
import com.android.volley.ClientError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.Cache.Entry;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.longluo.volleydemo.toolbox.FadeInImageListener;
import com.longluo.volleydemo.util.BitmapUtil;
import com.longluo.volleydemo.util.iQiyiInterface;

import android.R.integer;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.longluo.volleydemo.R;

public class NetworkDemoActivity extends Activity {
    private static final String TAG = "NetworkDemo";

    private Button mTrigger;
    private RequestQueue mVolleyQueue;
    private ListView mListView;
    private EfficientAdapter mAdapter;
    private ProgressDialog mProgress;
    private List<DataModel> mDataList;

    private ImageLoader mImageLoader;

    private final String TAG_REQUEST = "MY_TAG";

    private class DataModel {
        private String mImageUrl;
        private String mTitle;

        public String getImageUrl() {
            return mImageUrl;
        }

        public void setImageUrl(String mImageUrl) {
            this.mImageUrl = mImageUrl;
        }

        public String getTitle() {
            return mTitle;
        }

        public void setTitle(String mTitle) {
            this.mTitle = mTitle;
        }
    }

    public class BitmapCache extends LruCache<String, Bitmap> implements ImageCache {
        public BitmapCache(int maxSize) {
            super(maxSize);
        }

        @Override
        public Bitmap getBitmap(String url) {
            return (Bitmap) get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            put(url, bitmap);
        }
    }

    /*
     * Extends from DisckBasedCache --> Utility from volley toolbox. Also
     * implements ImageCache, so that we can pass this custom implementation to
     * ImageLoader.
     */
    public class DiskBitmapCache extends DiskBasedCache implements ImageCache {

        public DiskBitmapCache(File rootDirectory, int maxCacheSizeInBytes) {
            super(rootDirectory, maxCacheSizeInBytes);
        }

        public DiskBitmapCache(File cacheDir) {
            super(cacheDir);
        }

        public Bitmap getBitmap(String url) {
            final Entry requestedItem = get(url);

            if (requestedItem == null)
                return null;

            return BitmapFactory.decodeByteArray(requestedItem.data, 0, requestedItem.data.length);
        }

        public void putBitmap(String url, Bitmap bitmap) {

            final Entry entry = new Entry();

            /*
             * //Down size the bitmap.If not done, OutofMemoryError occurs while
             * decoding large bitmaps. // If w & h is set during image request (
             * using ImageLoader ) then this is not required.
             * ByteArrayOutputStream baos = new ByteArrayOutputStream(); Bitmap
             * downSized = BitmapUtil.downSizeBitmap(bitmap, 50);
             * 
             * downSized.compress(Bitmap.CompressFormat.JPEG, 100, baos); byte[]
             * data = baos.toByteArray();
             * 
             * System.out.println("####### Size of bitmap is ######### "+url+" : "
             * +data.length); entry.data = data ;
             */

            entry.data = BitmapUtil.convertBitmapToBytes(bitmap);
            put(url, entry);
        }
    }

    JsonObjectRequest jsonObjRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.json_object_layout);

        actionBarSetup();

        // Initialise Volley Request Queue.
        mVolleyQueue = Volley.newRequestQueue(this);

        int max_cache_size = 1000000;
        mImageLoader = new ImageLoader(mVolleyQueue, new DiskBitmapCache(getCacheDir(),
                max_cache_size));

        // Memory cache is always faster than DiskCache. Check it our for
        // yourself.
        // mImageLoader = new ImageLoader(mVolleyQueue, new
        // BitmapCache(max_cache_size));

        mDataList = new ArrayList<DataModel>();

        mListView = (ListView) findViewById(R.id.image_list);
        mTrigger = (Button) findViewById(R.id.send_http);

        mAdapter = new EfficientAdapter(this);
        mListView.setAdapter(mAdapter);

        mTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress();
                makeSampleHttpRequest();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionBarSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar ab = getActionBar();
            ab.setTitle("Network Demo");
        }
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onStop() {
        super.onStop();
        if (mProgress != null)
            mProgress.dismiss();
        // Keep the list of requests dispatched in a List<Request<T>>
        // mRequestList;
        /*
         * for( Request<T> req : mRequestList) { req.cancel(); }
         */
        // jsonObjRequest.cancel();
        // ( or )
        // mVolleyQueue.cancelAll(TAG_REQUEST);
    }

    private void makeSampleHttpRequest() {

        String url = iQiyiInterface.getURL();

        Uri.Builder builder = Uri.parse(url).buildUpon();
        // must Parameters
        builder.appendQueryParameter("key", iQiyiInterface.oemOppoKey);
        builder.appendQueryParameter("version", iQiyiInterface.UA_VERSION);
        builder.appendQueryParameter("compat", "1");
        builder.appendQueryParameter("platform", iQiyiInterface.PLATFORM);
        builder.appendQueryParameter("category_id", "6,0~0~0~0");
        builder.appendQueryParameter("device_id", "111111111111111");
        builder.appendQueryParameter("os", "4.4");
        builder.appendQueryParameter("ua", "OPPO");

        // have to Parameters
        builder.appendQueryParameter("s", "0");
        builder.appendQueryParameter("pn", "1");
        builder.appendQueryParameter("ps", "30");

        builder.appendQueryParameter("api", "2.2.2");

        Log.d(TAG, "iQiyi JSON url=" + builder.toString());

        jsonObjRequest = new JsonObjectRequest(Request.Method.GET, builder.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, "response=" + response);
                            parseiQiyiInterfaceImageResponse(response);
                            mAdapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                            showToast("JSON parse error");
                        }
                        stopProgress();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle your error types accordingly.For Timeout & No
                        // connection error, you can show 'retry' button.
                        // For AuthFailure, you can re login with user
                        // credentials.
                        // For ClientError, 400 & 401, Errors happening on
                        // client side when sending api request.
                        // In this case you can check how client is forming the
                        // api and debug accordingly.
                        // For ServerError 5xx, you can do retry or handle
                        // accordingly.
                        if (error instanceof NetworkError) {
                        } else if (error instanceof ClientError) {
                        } else if (error instanceof ServerError) {
                        } else if (error instanceof AuthFailureError) {
                        } else if (error instanceof ParseError) {
                        } else if (error instanceof NoConnectionError) {
                        } else if (error instanceof TimeoutError) {
                        }

                        Log.d(TAG, "onErrorResponse, error=" + error);

                        stopProgress();
                        showToast(error.getMessage());
                    }

                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("t", iQiyiInterface.getEncryptTimestamp());
                headers.put("sign", iQiyiInterface.getSign());

                Log.d(TAG, "headers=" + headers);

                return headers;
            }
        };

        // Set a retry policy in case of SocketTimeout & ConnectionTimeout
        // Exceptions. Volley does retry for you if you have specified the
        // policy.
        jsonObjRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        jsonObjRequest.setTag(TAG_REQUEST);
        mVolleyQueue.add(jsonObjRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void showProgress() {
        mProgress = ProgressDialog.show(this, "", "Loading...");
    }

    private void stopProgress() {
        mProgress.cancel();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void parseiQiyiInterfaceImageResponse(JSONObject response) throws JSONException {
        Log.d(TAG, "parseiQiyiInterfaceImageResponse");

        ArrayList<Integer> albumIdArrayList = new ArrayList<Integer>();

        if (response.has("albumIdList")) {
            JSONArray albumIdList = response.getJSONArray("albumIdList");
            Log.d(TAG, "albumIdList=" + albumIdList + ",length=" + albumIdList.length());

            JSONObject idJSONObject = albumIdList.optJSONObject(0);
            Log.d(TAG, "idJSONObject=" + idJSONObject + ",length=" + idJSONObject.length());

            int idNum = idJSONObject.getInt("totalidnum");
            JSONArray idListJsonArray = idJSONObject.getJSONArray("idlist");
            Log.d(TAG, "idNum=" + idNum + ",idList=" + idListJsonArray + ",length="
                    + idListJsonArray.length());

            if (idListJsonArray != null) {
                for (int i = 0; i < idListJsonArray.length(); i++) {
                    albumIdArrayList.add(idListJsonArray.getInt(i));
                }
            }

            // Log.d(TAG, "idArrayList=" + idArrayList);
            Collections.sort(albumIdArrayList);
            Log.d(TAG, "After: idArrayList=" + albumIdArrayList);
        }

        if (response.has("albumArray")) {
            try {
                JSONObject albums = response.getJSONObject("albumArray");
                Log.d(TAG, "albumArray length=" + albums.length());

                mDataList.clear();

                for (int index = 0; index < albums.length(); index++) {
                    Log.d(TAG, "string=" + albumIdArrayList.get(index).toString());

                    JSONObject itemJsonObject = albums.getJSONObject(albumIdArrayList.get(index)
                            .toString());

                    String imageUrl = itemJsonObject.getString("h1_img");
                    Log.d(TAG, "imageUrl=" + imageUrl + ",title=" + itemJsonObject.getString("_t"));

                    DataModel model = new DataModel();
                    model.setImageUrl(imageUrl);
                    model.setTitle(itemJsonObject.getString("_t"));
                    mDataList.add(model);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void parseFlickrImageResponse(JSONObject response) throws JSONException {

        if (response.has("photos")) {
            try {
                JSONObject photos = response.getJSONObject("photos");
                JSONArray items = photos.getJSONArray("photo");

                mDataList.clear();

                for (int index = 0; index < items.length(); index++) {

                    JSONObject jsonObj = items.getJSONObject(index);

                    String farm = jsonObj.getString("farm");
                    String id = jsonObj.getString("id");
                    String secret = jsonObj.getString("secret");
                    String server = jsonObj.getString("server");

                    String imageUrl = "http://farm" + farm + ".static.flickr.com/" + server + "/"
                            + id + "_" + secret + "_t.jpg";

                    Log.d(TAG, "imageUrl=" + imageUrl);

                    DataModel model = new DataModel();
                    model.setImageUrl(imageUrl);
                    model.setTitle(jsonObj.getString("title"));
                    mDataList.add(model);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class EfficientAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public EfficientAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mDataList.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item, null);
                holder = new ViewHolder();
                holder.image = (ImageView) convertView.findViewById(R.id.image);
                holder.title = (TextView) convertView.findViewById(R.id.title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.title.setText(mDataList.get(position).getTitle());
            mImageLoader.get(mDataList.get(position).getImageUrl(), new FadeInImageListener(
                    holder.image, NetworkDemoActivity.this));
            return convertView;
        }

        class ViewHolder {
            TextView title;
            ImageView image;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}

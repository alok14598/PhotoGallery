package com.example.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
private static final String API_KEY="7b22d3f88441f635fe9d3ebc929f5d71";
    private static final String TAG ="flickfetchr" ;
private static final String FETCH_RECENT_METHODS="flickr.photos.getRecent";
private static final String SEARCH_METHOD="flickr.photos.search";
private static final Uri Endpoint=Uri.parse("https://api.flickr.com/services/rest/")
        .buildUpon()
             .appendQueryParameter("api_key",API_KEY).appendQueryParameter("format","json")
        .appendQueryParameter("nojsoncallback","1")
        .appendQueryParameter("extras","url_s")
        .build();
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url=new URL(urlSpec);
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            InputStream in=connection.getInputStream();
            if(connection.getResponseCode()!= HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage()+": with " + urlSpec);
            }
            int bytesRead=0;
            byte[] buffer =new byte[1024];
            while((bytesRead=in.read(buffer))>0) {
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();

        }finally {
            connection.disconnect();
        }
    }
    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }
    public List<GalleryItem> fetchRecentPhotos(){
        String url=buildUrl(FETCH_RECENT_METHODS,null);
        return DownloadGalleryItem(url);
    }
    public List<GalleryItem> searchPhotos(String query){
        String url=buildUrl(SEARCH_METHOD,query);
        return DownloadGalleryItem(url);
    }
    private String buildUrl(String method,String query){
        Uri.Builder uribuilder=Endpoint.buildUpon().appendQueryParameter("method",method);
        if(method.equals(SEARCH_METHOD)){
            uribuilder.appendQueryParameter("text",query);
        }return uribuilder.build().toString();
    }
    private List<GalleryItem> DownloadGalleryItem(String url){
        List<GalleryItem> items=new ArrayList<>();
        try{

            String jsonString=getUrlString(url);
           JSONObject jsonBody=new JSONObject(jsonString);
            parseItems(items,jsonBody);
        } catch (IOException | JSONException e) {
            Log.e(TAG,"Failes to Fetch",e);
        }return items;
    }
    private void parseItems(List<GalleryItem> items,JSONObject jsonBody) throws IOException,JSONException{
        JSONObject jsonObject=jsonBody.getJSONObject("photos");
        JSONArray jsonArray=jsonObject.getJSONArray("photo");
        for(int i=0;i<jsonArray.length();i++){
            JSONObject jsonObject1=jsonArray.getJSONObject(i);
            GalleryItem item=new GalleryItem();
            item.setId(jsonObject1.getString("id"));
            item.setCaption(jsonObject1.getString("title"));
            if(!jsonObject1.has("url_s")){
                continue;
            }
            item.setUri(jsonObject1.getString("url_s"));
            item.setOwner(jsonObject1.getString("owner"));
            items.add(item);
        }
    }
}

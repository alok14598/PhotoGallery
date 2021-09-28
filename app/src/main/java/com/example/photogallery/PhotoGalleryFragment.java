package com.example.photogallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG ="photoGalleryFragment" ;
    private RecyclerView mRecyclerView;
    private List<GalleryItem> mItems=new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
         return new PhotoGalleryFragment();
     }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItem();
       PollService.setServiceAlarm(getActivity(),true);
        Handler responseHandler=new Handler();
        mThumbnailDownloader=new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDonwloaded(PhotoHolder target, Bitmap thumbnail) {
                        Drawable drawable=new BitmapDrawable(getResources(),thumbnail);
                        target.bind(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();;
        mThumbnailDownloader.getLooper();
        Log.i(TAG,"Background Thread Started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG,"Background Thread Destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearqueue();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.photo_gallery_fragment,menu);
        MenuItem searchItem=menu.findItem(R.id.menu_item_search);
        final SearchView  searchView=  (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                QueryPreferences.setStoredQuery(getActivity(),query);
                updateItem();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
searchView.setOnSearchClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        String query=QueryPreferences.getStoredQuery(getActivity());
        searchView.setQuery(query,false);

    }
});
MenuItem toggleItem=menu.findItem(R.id.meu_item_toggle_polling);
if(PollService.isServiceAlarm(getActivity())){
    toggleItem.setTitle(R.string.stop_pollong);
}else{
    toggleItem.setTitle(R.string.start_polling);
}

     }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                updateItem();
                return true;

            case R.id.meu_item_toggle_polling:
                boolean shouldStartAlarm=!PollService.isServiceAlarm(getActivity());
                PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:   return super.onOptionsItemSelected(item);
        }


    }

    private void updateItem() {
         String query=QueryPreferences.getStoredQuery(getActivity());
         new FetchitemsTask(query).execute();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.photo_gallery_fragment,container,false);
        mRecyclerView=(RecyclerView)v.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        setUpAdapter();
        return v;
    }
    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;
        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            mItemImageView= (ImageView) itemView.findViewById(R.id.item_image_view);
            itemView.setOnClickListener(this);
        }

        public void bind(Drawable galleryItem) {
         mItemImageView.setImageDrawable(galleryItem);
        }

         public void bindGallery(GalleryItem galleryItem){
            mGalleryItem=galleryItem;
         }
        @Override
        public void onClick(View v) {
            Intent i=PhotoPageActivity.newIntent(getActivity(),mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> items) {
            mGalleryItems=items;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
          LayoutInflater inflater=LayoutInflater.from(getActivity());
          View view=inflater.inflate(R.layout.list_item_gallery,parent,false);
          return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            GalleryItem galleryItem=mGalleryItems.get(position);
            holder.bindGallery(galleryItem);
            Drawable placeholder=getResources().getDrawable(R.drawable.bill_up_close);
            holder.bind(placeholder);
            mThumbnailDownloader.queueThumbnail(holder,galleryItem.getUri());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }
    private void setUpAdapter() {
        if(isAdded()){
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class FetchitemsTask extends AsyncTask<Void,Void,List<GalleryItem>>{
private String mQuery;

        public FetchitemsTask(String query) {
            mQuery = query;
        }



        @Override
        protected List<GalleryItem> doInBackground(Void... params) {

if(mQuery==null){
    return new FlickrFetchr().fetchRecentPhotos();
}else{
    return new FlickrFetchr().searchPhotos(mQuery);
}
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems=items;
            setUpAdapter();
        }
    }

}

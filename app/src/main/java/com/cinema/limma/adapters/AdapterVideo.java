package com.cinema.limma.adapters;

import static com.cinema.limma.utils.Constant.NATIVE_AD_VIDEO_LIST;
import static com.cinema.limma.utils.Constant.VIDEO_LIST_COMPACT;
import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN_MAX;
import static com.solodroid.ads.sdk.util.Constant.STARTAPP;
import static com.solodroid.ads.sdk.util.Constant.STYLE_VIDEO_LARGE;
import static com.solodroid.ads.sdk.util.Constant.STYLE_VIDEO_SMALL;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.cinema.limma.R;
import com.cinema.limma.config.AppConfig;
import com.cinema.limma.databases.prefs.AdsPref;
import com.cinema.limma.databases.prefs.SharedPref;
import com.cinema.limma.models.Video;
import com.cinema.limma.utils.Constant;
import com.cinema.limma.utils.Tools;
import com.solodroid.ads.sdk.format.NativeAdViewHolder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class AdapterVideo extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_AD = 2;
    private final int VIEW_ITEM = 1;
    private final int VIEW_PROG = 0;

    private List<Video> videos;

    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;

    Context context;
    private OnItemClickListener mOnItemClickListener;
    private OnItemOverflowClickListener mOnItemOverflowClickListener;

    boolean scrolling = false;
    SharedPref sharedPref;
    AdsPref adsPref;

    public interface OnItemClickListener {
        void onItemClick(View view, Video obj, int position);
    }

    public interface OnItemOverflowClickListener {
        void onItemOverflowClick(View view, Video obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public void setOnItemOverflowClickListener(final OnItemOverflowClickListener mItemOverflowClickListener) {
        this.mOnItemOverflowClickListener = mItemOverflowClickListener;
    }

    public AdapterVideo(Context context, RecyclerView view, List<Video> videos) {
        this.videos = videos;
        this.context = context;
        this.sharedPref = new SharedPref(context);
        this.adsPref = new AdsPref(context);
        lastItemViewDetector(view);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    scrolling = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrolling = false;
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {

        public TextView categoryName;
        public TextView videoTitle;
        public TextView videoDuration;
        public TextView totalViews;
        public TextView dateTime;
        public LinearLayout lytView;
        public LinearLayout lytDate;
        public ImageView videoThumbnail;
        public LinearLayout lytParent;
        public ImageButton overflow;

        public OriginalViewHolder(View v) {
            super(v);
            categoryName = v.findViewById(R.id.category_name);
            videoTitle = v.findViewById(R.id.video_title);
            videoDuration = v.findViewById(R.id.video_duration);
            dateTime = v.findViewById(R.id.date_time);
            totalViews = v.findViewById(R.id.total_views);
            lytView = v.findViewById(R.id.lyt_view_count);
            lytDate = v.findViewById(R.id.lyt_date);
            videoThumbnail = v.findViewById(R.id.video_thumbnail);
            lytParent = v.findViewById(R.id.lyt_parent);
            overflow = v.findViewById(R.id.overflow);

        }

    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public ProgressViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.load_more);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM) {
            if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_compact, parent, false);
                vh = new OriginalViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_default, parent, false);
                vh = new OriginalViewHolder(v);
            }
        } else if (viewType == VIEW_AD) {
            if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_native_ad_video_small, parent, false);
                vh = new NativeAdViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_native_ad_video_large, parent, false);
                vh = new NativeAdViewHolder(v);
            }
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_load_more, parent, false);
            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Video video = videos.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.categoryName.setText(video.category_name);
            vItem.videoTitle.setText(video.video_title);
            vItem.videoDuration.setText(video.video_duration);
            if (AppConfig.ENABLE_VIEW_COUNT) {
                vItem.totalViews.setText(Tools.withSuffix(video.total_views) + " " + context.getResources().getString(R.string.views_count));
            } else {
                vItem.lytView.setVisibility(View.GONE);
            }

            if (AppConfig.ENABLE_DATE_DISPLAY && AppConfig.DISPLAY_DATE_AS_TIME_AGO) {
                vItem.dateTime.setText(Tools.getTimeAgo(video.date_time));
            } else if (AppConfig.ENABLE_DATE_DISPLAY && !AppConfig.DISPLAY_DATE_AS_TIME_AGO) {
                vItem.dateTime.setText(Tools.getFormatedDateSimple(video.date_time));
            } else {
                vItem.lytDate.setVisibility(View.GONE);
            }

            if (video.video_type != null && video.video_type.equals("youtube")) {
                if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
                    Picasso.get()
                            .load(Constant.YOUTUBE_IMAGE_FRONT + video.video_id + Constant.YOUTUBE_IMAGE_BACK_MQ)
                            .placeholder(R.drawable.ic_thumbnail)
                            .into(vItem.videoThumbnail);
                } else {
                    Picasso.get()
                            .load(Constant.YOUTUBE_IMAGE_FRONT + video.video_id + Constant.YOUTUBE_IMAGE_BACK_HQ)
                            .placeholder(R.drawable.ic_thumbnail)
                            .into(vItem.videoThumbnail);
                }
            } else {
                Picasso.get()
                        .load(sharedPref.getBaseUrl() + "/upload/" + video.video_thumbnail)
                        .placeholder(R.drawable.ic_thumbnail)
                        .into(vItem.videoThumbnail);
            }

            vItem.lytParent.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, video, position);
                }
            });

            vItem.overflow.setOnClickListener(view -> {
                if (mOnItemOverflowClickListener != null) {
                    mOnItemOverflowClickListener.onItemOverflowClick(view, video, position);
                }
            });

        } else if (holder instanceof NativeAdViewHolder) {

            final NativeAdViewHolder vItem = (NativeAdViewHolder) holder;

            String nativeAdStyle;
            if (sharedPref.getVideoViewType() == VIDEO_LIST_COMPACT) {
                nativeAdStyle = STYLE_VIDEO_SMALL;
            } else {
                nativeAdStyle = STYLE_VIDEO_LARGE;
            }

            vItem.loadNativeAd(context,
                    adsPref.getAdStatus(),
                    NATIVE_AD_VIDEO_LIST,
                    adsPref.getAdType(),
                    adsPref.getBackupAds(),
                    adsPref.getAdMobNativeId(),
                    adsPref.getAppLovinNativeAdManualUnitId(),
                    sharedPref.getIsDarkTheme(),
                    AppConfig.USE_LEGACY_GDPR_EU_CONSENT,
                    nativeAdStyle
            );

            if (sharedPref.getVideoViewType() != VIDEO_LIST_COMPACT) {
                vItem.setNativeAdPadding(
                        context.getResources().getDimensionPixelOffset(R.dimen.gnt_no_size),
                        context.getResources().getDimensionPixelOffset(R.dimen.spacing_small),
                        context.getResources().getDimensionPixelOffset(R.dimen.gnt_no_size),
                        context.getResources().getDimensionPixelOffset(R.dimen.spacing_small)
                );
            }

        } else {
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    @Override
    public int getItemViewType(int position) {
        Video video = videos.get(position);
        if (video != null) {
            if (video.video_title == null || video.video_title.equals("")) {
                return VIEW_AD;
            }
            return VIEW_ITEM;
        } else {
            return VIEW_PROG;
        }
    }

//    public void insertData(List<Video> items) {
//        setLoaded();
//        int positionStart = getItemCount();
//        int itemCount = items.size();
//        this.videos.addAll(items);
//        notifyItemRangeInserted(positionStart, itemCount);
//    }

    public void insertDataWithNativeAd(List<Video> videos) {
        setLoaded();
        int positionStart = getItemCount();
        if (videos.size() >= adsPref.getNativeAdIndex()) {
            videos.add(adsPref.getNativeAdIndex(), new Video());
            Log.d("item ads", "space for Native Ad");
        }
        int itemCount = videos.size();
        this.videos.addAll(videos);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void setLoaded() {
        loading = false;
        for (int i = 0; i < getItemCount(); i++) {
            if (videos.get(i) == null) {
                videos.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    public void setLoading() {
        if (getItemCount() != 0) {
            this.videos.add(null);
            notifyItemInserted(getItemCount() - 1);
            loading = true;
        }
    }

    public void resetListData() {
        this.videos = new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void lastItemViewDetector(RecyclerView recyclerView) {

        if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            final StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int lastPos = getLastVisibleItem(layoutManager.findLastVisibleItemPositions(null));
                    if (!loading && lastPos == getItemCount() - 1 && onLoadMoreListener != null) {
                        if (NATIVE_AD_VIDEO_LIST != 0) {
                            switch (adsPref.getAdType()) {
                                case ADMOB:
                                case STARTAPP:
                                case APPLOVIN:
                                case APPLOVIN_MAX: {
                                    int current_page = getItemCount() / (AppConfig.LOAD_MORE + 1); //posts per page plus 1 Ad
                                    onLoadMoreListener.onLoadMore(current_page);
                                    break;
                                }
                                default: {
                                    int current_page = getItemCount() / (AppConfig.LOAD_MORE);
                                    onLoadMoreListener.onLoadMore(current_page);
                                    break;
                                }
                            }
                        } else {
                            int current_page = getItemCount() / (AppConfig.LOAD_MORE);
                            onLoadMoreListener.onLoadMore(current_page);
                        }
                        loading = true;
                    }
                }
            });
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int current_page);
    }

    private int getLastVisibleItem(int[] into) {
        int lastIdx = into[0];
        for (int i : into) {
            if (lastIdx < i) lastIdx = i;
        }
        return lastIdx;
    }

}
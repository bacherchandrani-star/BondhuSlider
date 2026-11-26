
package com.bondhubazar.slider.bondhuslider;

// ==========================================
// Android Imports
// ==========================================

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

// ==========================================
// App Inventor Imports
// ==========================================

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.util.YailList;

// ==========================================
// Java Imports
// ==========================================

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// ==========================================
// Extension Information
// ==========================================

@DesignerComponent(
        version = 3,
        description = "Bondhu Bazar Image Slider V3",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/extension.png"
)

public class BondhuSlider extends AndroidNonvisibleComponent {

    // ======================================
    // ১. Activity এবং Handler
    // ======================================

    private final Activity activity;

    private final Handler handler;

    // ======================================
    // ২. Background Executor
    // ======================================

    // ছবি Download করার জন্য ২টি Thread
    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    // চলমান Download Task
    private Future<?> currentDownload;

    // ======================================
    // ৩. Slider UI
    // ======================================

    private FrameLayout sliderView;

    private ImageView imageView;

    private TextView titleView;

    private TextView subtitleView;

    private LinearLayout indicatorLayout;

    // বর্তমানে দেখানো Bitmap
    private Bitmap currentBitmap;

    // ======================================
    // ৪. Image Data
    // ======================================

    private final ArrayList<String> imageUrls =
            new ArrayList<>();

    private final ArrayList<String> titles =
            new ArrayList<>();

    private final ArrayList<String> subtitles =
            new ArrayList<>();

    // ======================================
    // ৫. Slider Settings
    // ======================================

    private int currentIndex = 0;

    private int sliderHeight = 160;

    private int interval = 3000;

    private int cornerRadius = 12;

    private boolean autoSlide = false;

    // পুরোনো Request শনাক্ত করার জন্য
    private int requestNumber = 0;

    // সর্বোচ্চ Image Size: 8 MB
    private static final int MAX_IMAGE_SIZE =
            8 * 1024 * 1024;

    // ======================================
    // ৬. Auto Slide Runnable
    // ======================================

    private final Runnable autoRunnable = new Runnable() {

        @Override
        public void run() {

            if (!autoSlide) {
                return;
            }

            if (imageUrls.size() > 1) {

                NextSlide();

                handler.postDelayed(
                        this,
                        interval
                );

            } else {

                StopAutoSlide();
            }
        }
    };

    // ======================================
    // ৭. Constructor
    // ======================================

    public BondhuSlider(ComponentContainer container) {

        super(container.$form());

        Form form = container.$form();

        activity = form;

        handler = new Handler(
                Looper.getMainLooper()
        );
    }

    // ======================================
    // ৮. DP থেকে Pixel
    // ======================================

    private int dp(int value) {

        return (int) (
                value * activity.getResources()
                        .getDisplayMetrics()
                        .density + 0.5f
        );
    }

    // ======================================
    // ৯. Slider Create
    // ======================================

    @SimpleFunction(
            description = "Arrangement-এর ভিতরে Image Slider তৈরি করবে"
    )

    public void CreateSlider(
            AndroidViewComponent arrangement,
            YailList urls,
            YailList slideTitles,
            YailList slideSubtitles,
            int height,
            int slideInterval) {

        try {

            // Arrangement পরীক্ষা
            if (arrangement == null ||
                    urls == null ||
                    slideTitles == null ||
                    slideSubtitles == null) {

                ErrorOccurred("কোনো Input খালি পাওয়া গেছে");

                return;
            }

            if (!(arrangement.getView() instanceof ViewGroup)) {

                ErrorOccurred(
                        "Arrangement অবশ্যই Layout হতে হবে"
                );

                return;
            }

            // পুরোনো Slider সরানো
            RemoveSlider();

            // ==================================
            // List থেকে Array নেওয়া
            // ==================================

            String[] urlArray =
                    urls.toStringArray();

            String[] titleArray =
                    slideTitles.toStringArray();

            String[] subtitleArray =
                    slideSubtitles.toStringArray();

            // ==================================
            // URL, Title, Subtitle একই Index-এ
            // ==================================

            imageUrls.clear();

            titles.clear();

            subtitles.clear();

            for (int i = 0; i < urlArray.length; i++) {

                String url = urlArray[i];

                // খালি URL বাদ
                if (url == null ||
                        url.trim().isEmpty()) {

                    continue;
                }

                // URL যোগ
                imageUrls.add(
                        url.trim()
                );

                // একই Index-এর Title
                String title = "";

                if (i < titleArray.length &&
                        titleArray[i] != null) {

                    title = titleArray[i];
                }

                titles.add(title);

                // একই Index-এর Subtitle
                String subtitle = "";

                if (i < subtitleArray.length &&
                        subtitleArray[i] != null) {

                    subtitle = subtitleArray[i];
                }

                subtitles.add(subtitle);
            }

            // Image না থাকলে Error
            if (imageUrls.isEmpty()) {

                ErrorOccurred(
                        "কোনো Image URL পাওয়া যায়নি"
                );

                return;
            }

            // ==================================
            // Slider Settings
            // ==================================

            sliderHeight = Math.max(
                    60,
                    height
            );

            interval = Math.max(
                    1000,
                    slideInterval
            );

            currentIndex = 0;

            // ==================================
            // Main Slider Layout
            // ==================================

            sliderView = new FrameLayout(activity);

            sliderView.setBackground(
                    CreateRoundedBackground(
                            Color.LTGRAY,
                            cornerRadius
                    )
            );

            sliderView.setClipToOutline(true);

            // ==================================
            // ImageView
            // ==================================

            imageView = new ImageView(activity);

            imageView.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

            FrameLayout.LayoutParams imageParams =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );

            sliderView.addView(
                    imageView,
                    imageParams
            );

            // ==================================
            // Text Container
            // ==================================

            LinearLayout textContainer =
                    new LinearLayout(activity);

            textContainer.setOrientation(
                    LinearLayout.VERTICAL
            );

            textContainer.setGravity(
                    Gravity.BOTTOM
            );

            textContainer.setPadding(
                    dp(12),
                    dp(8),
                    dp(12),
                    dp(30)
            );

            GradientDrawable overlay =
                    new GradientDrawable();

            overlay.setColor(
                    0x88000000
            );

            textContainer.setBackground(
                    overlay
            );

            // ==================================
            // Title
            // ==================================

            titleView = new TextView(activity);

            titleView.setTextColor(
                    Color.WHITE
            );

            titleView.setTextSize(18);

            titleView.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            // ==================================
            // Subtitle
            // ==================================

            subtitleView = new TextView(activity);

            subtitleView.setTextColor(
                    Color.WHITE
            );

            subtitleView.setTextSize(13);

            // Text যোগ
            textContainer.addView(titleView);

            textContainer.addView(subtitleView);

            FrameLayout.LayoutParams textParams =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM
                    );

            sliderView.addView(
                    textContainer,
                    textParams
            );

            // ==================================
            // Indicator Layout
            // ==================================

            indicatorLayout =
                    new LinearLayout(activity);

            indicatorLayout.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            indicatorLayout.setGravity(
                    Gravity.CENTER
            );

            FrameLayout.LayoutParams indicatorParams =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(24),
                            Gravity.BOTTOM |
                                    Gravity.CENTER_HORIZONTAL
                    );

            sliderView.addView(
                    indicatorLayout,
                    indicatorParams
            );

            // ==================================
            // Swipe এবং Click Listener
            // ==================================

            sliderView.setOnTouchListener(
                    new View.OnTouchListener() {

                        private float startX;

                        private float startY;

                        @Override
                        public boolean onTouch(
                                View view,
                                MotionEvent event
                        ) {

                            switch (event.getAction()) {

                                case MotionEvent.ACTION_DOWN:

                                    startX = event.getX();

                                    startY = event.getY();

                                    return true;

                                case MotionEvent.ACTION_UP:

                                    float diffX =
                                            event.getX() - startX;

                                    float diffY =
                                            event.getY() - startY;

                                    // Horizontal Swipe
                                    if (Math.abs(diffX) > dp(40)
                                            && Math.abs(diffX) >
                                            Math.abs(diffY)) {

                                        if (diffX > 0) {

                                            PreviousSlide();

                                        } else {

                                            NextSlide();
                                        }

                                    } else {

                                        // সাধারণ Click
                                        if (currentIndex >= 0 &&
                                                currentIndex < imageUrls.size()) {

                                            SlideClicked(
                                                    currentIndex + 1,
                                                    imageUrls.get(currentIndex)
                                            );
                                        }
                                    }

                                    return true;

                                case MotionEvent.ACTION_CANCEL:

                                    return true;
                            }

                            return true;
                        }
                    }
            );

            // ==================================
            // Parent Arrangement
            // ==================================

            ViewGroup parent =
                    (ViewGroup) arrangement.getView();

            ViewGroup.LayoutParams mainParams;

            // Parent অনুযায়ী LayoutParams
            if (parent instanceof LinearLayout) {

                mainParams =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                dp(sliderHeight)
                        );

            } else if (parent instanceof FrameLayout) {

                mainParams =
                        new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                dp(sliderHeight)
                        );

            } else if (parent instanceof RelativeLayout) {

                mainParams =
                        new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                dp(sliderHeight)
                        );

            } else {

                mainParams =
                        new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                dp(sliderHeight)
                        );
            }

            // Slider Parent-এর ভিতরে যোগ
            parent.addView(
                    sliderView,
                    mainParams
            );

            // প্রথম Slide দেখানো
            ShowCurrentSlide();

            // Event
            SliderCreated();

        } catch (Exception e) {

            ErrorOccurred(
                    "Slider Create Error: " + e.toString()
            );
        }
    }

    // ======================================
    // ১০. Rounded Background
    // ======================================

    private GradientDrawable CreateRoundedBackground(
            int color,
            int radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);

        drawable.setCornerRadius(
                dp(radius)
        );

        return drawable;
    }

    // ======================================
    // ১১. Current Slide দেখানো
    // ======================================

    private void ShowCurrentSlide() {

        if (sliderView == null ||
                imageView == null ||
                imageUrls.isEmpty()) {

            return;
        }

        // Index ঠিক রাখা
        if (currentIndex < 0 ||
                currentIndex >= imageUrls.size()) {

            currentIndex = 0;
        }

        final int index = currentIndex;

        final String imageUrl =
                imageUrls.get(index);

        // Title Update
        titleView.setText(
                titles.get(index)
        );

        // Subtitle Update
        subtitleView.setText(
                subtitles.get(index)
        );

        // Indicator Update
        UpdateIndicators();

        // ==================================
        // পুরোনো Download Cancel
        // ==================================

        if (currentDownload != null) {

            currentDownload.cancel(true);

            currentDownload = null;
        }

        // নতুন Request ID
        final int thisRequest =
                ++requestNumber;

        // পুরোনো Image সরানো
        imageView.setImageBitmap(null);

        // ==================================
        // Background Thread
        // ==================================

        currentDownload = executor.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        Bitmap bitmap =
                                DownloadImage(imageUrl);

                        final Bitmap result =
                                bitmap;

                        // Main Thread
                        handler.post(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        // পুরোনো Request হলে
                                        if (thisRequest != requestNumber) {

                                            RecycleBitmap(result);

                                            return;
                                        }

                                        // Slider সরানো হলে
                                        if (sliderView == null ||
                                                imageView == null) {

                                            RecycleBitmap(result);

                                            return;
                                        }

                                        // Image সফলভাবে Load
                                        if (result != null) {

                                            Bitmap oldBitmap =
                                                    currentBitmap;

                                            currentBitmap = result;

                                            imageView.setImageBitmap(
                                                    result
                                            );

                                            // নতুন Image বসানোর পরে পুরোনোটি পরিষ্কার
                                            RecycleBitmap(oldBitmap);

                                            BannerImageLoaded(
                                                    imageUrl
                                            );

                                        } else {

                                            BannerImageFailed(
                                                    imageUrl
                                            );
                                        }
                                    }
                                }
                        );
                    }
                }
        );

        // Slide Changed Event
        SlideChanged(
                index + 1,
                imageUrl
        );
    }

    // ======================================
    // ১২. Bitmap পরিষ্কার
    // ======================================

    private void RecycleBitmap(Bitmap bitmap) {

        if (bitmap != null &&
                !bitmap.isRecycled()) {

            bitmap.recycle();
        }
    }

    // ======================================
    // ১৩. Image Download
    // ======================================

    private Bitmap DownloadImage(String imageUrl) {

        HttpURLConnection connection = null;

        InputStream inputStream = null;

        ByteArrayOutputStream outputStream = null;

        try {

            URL url = new URL(imageUrl);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(10000);

            connection.setReadTimeout(15000);

            connection.setRequestMethod("GET");

            connection.setInstanceFollowRedirects(true);

            connection.connect();

            // Response Code
            int responseCode =
                    connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {

                return null;
            }

            // Content Length পরীক্ষা
            int contentLength =
                    connection.getContentLength();

            if (contentLength > MAX_IMAGE_SIZE) {

                return null;
            }

            inputStream =
                    connection.getInputStream();

            outputStream =
                    new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];

            int bytesRead;

            int totalBytes = 0;

            // ==================================
            // Image Data পড়া
            // ==================================

            while ((bytesRead = inputStream.read(buffer)) != -1) {

                // Thread Cancel হলে বন্ধ
                if (Thread.currentThread().isInterrupted()) {

                    return null;
                }

                totalBytes += bytesRead;

                // 8 MB-এর বেশি হলে বন্ধ
                if (totalBytes > MAX_IMAGE_SIZE) {

                    return null;
                }

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            byte[] imageData =
                    outputStream.toByteArray();

            // Bitmap Decode
            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inSampleSize = 2;

            options.inPreferredConfig =
                    Bitmap.Config.RGB_565;

            return BitmapFactory.decodeByteArray(
                    imageData,
                    0,
                    imageData.length,
                    options
            );

        } catch (Exception e) {

            return null;

        } finally {

            try {

                if (inputStream != null) {
                    inputStream.close();
                }

            } catch (Exception ignored) {}

            try {

                if (outputStream != null) {
                    outputStream.close();
                }

            } catch (Exception ignored) {}

            if (connection != null) {

                connection.disconnect();
            }
        }
    }

    // ======================================
    // ১৪. Indicator Update
    // ======================================

    private void UpdateIndicators() {

        if (indicatorLayout == null) {
            return;
        }

        indicatorLayout.removeAllViews();

        for (int i = 0; i < imageUrls.size(); i++) {

            final int position = i;

            TextView dot = new TextView(activity);

            dot.setText("●");

            dot.setTextSize(12);

            dot.setGravity(Gravity.CENTER);

            if (i == currentIndex) {

                dot.setTextColor(Color.WHITE);

            } else {

                dot.setTextColor(0x88FFFFFF);
            }

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(
                    dp(4),
                    0,
                    dp(4),
                    0
            );

            indicatorLayout.addView(
                    dot,
                    params
            );

            // Indicator-এ Click করলে Slide পরিবর্তন
            dot.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            GoToSlide(position + 1);
                        }
                    }
            );
        }
    }

    // ======================================
    // ১৫. নির্দিষ্ট Slide দেখানো
    // ======================================

    @SimpleFunction(
            description = "নির্দিষ্ট Slide Number দেখাবে"
    )

    public void GoToSlide(int slideNumber) {

        if (imageUrls.isEmpty()) {
            return;
        }

        if (slideNumber < 1 ||
                slideNumber > imageUrls.size()) {

            ErrorOccurred(
                    "ভুল Slide Number"
            );

            return;
        }

        currentIndex = slideNumber - 1;

        ShowCurrentSlide();
    }

    // ======================================
    // ১৬. Next Slide
    // ======================================

    @SimpleFunction(
            description = "পরবর্তী ছবি দেখাবে"
    )

    public void NextSlide() {

        if (imageUrls.isEmpty()) {
            return;
        }

        currentIndex++;

        if (currentIndex >= imageUrls.size()) {

            currentIndex = 0;
        }

        ShowCurrentSlide();
    }

    // ======================================
    // ১৭. Previous Slide
    // ======================================

    @SimpleFunction(
            description = "আগের ছবি দেখাবে"
    )

    public void PreviousSlide() {

        if (imageUrls.isEmpty()) {
            return;
        }

        currentIndex--;

        if (currentIndex < 0) {

            currentIndex = imageUrls.size() - 1;
        }

        ShowCurrentSlide();
    }

    // ======================================
    // ১৮. Auto Slide চালু
    // ======================================

    @SimpleFunction(
            description = "Auto Slide চালু করবে"
    )

    public void StartAutoSlide() {

        StopAutoSlide();

        if (imageUrls.size() <= 1) {
            return;
        }

        autoSlide = true;

        handler.postDelayed(
                autoRunnable,
                interval
        );
    }

    // ======================================
    // ১৯. Auto Slide বন্ধ
    // ======================================

    @SimpleFunction(
            description = "Auto Slide বন্ধ করবে"
    )

    public void StopAutoSlide() {

        autoSlide = false;

        handler.removeCallbacks(
                autoRunnable
        );
    }

    // ======================================
    // ২০. Current Slide Number
    // ======================================

    @SimpleFunction(
            description = "বর্তমান Slide Number দেবে"
    )

    public int GetCurrentSlide() {

        return currentIndex + 1;
    }

    // ======================================
    // ২১. মোট Slide সংখ্যা
    // ======================================

    @SimpleFunction(
            description = "মোট Slide সংখ্যা দেবে"
    )

    public int GetSlideCount() {

        return imageUrls.size();
    }

    // ======================================
    // ২২. Title পরিবর্তন
    // ======================================

    @SimpleFunction(
            description = "বর্তমান Slide-এর Title পরিবর্তন করবে"
    )

    public void SetCurrentTitle(String title) {

        if (currentIndex >= 0 &&
                currentIndex < titles.size()) {

            titles.set(
                    currentIndex,
                    title == null ? "" : title
            );
        }

        if (titleView != null) {

            titleView.setText(
                    title == null ? "" : title
            );
        }
    }

    // ======================================
    // ২৩. Subtitle পরিবর্তন
    // ======================================

    @SimpleFunction(
            description = "বর্তমান Slide-এর Subtitle পরিবর্তন করবে"
    )

    public void SetCurrentSubtitle(String subtitle) {

        if (currentIndex >= 0 &&
                currentIndex < subtitles.size()) {

            subtitles.set(
                    currentIndex,
                    subtitle == null ? "" : subtitle
            );
        }

        if (subtitleView != null) {

            subtitleView.setText(
                    subtitle == null ? "" : subtitle
            );
        }
    }

    // ======================================
    // ২৪. Slider Show
    // ======================================

    @SimpleFunction(
            description = "Slider দেখাবে"
    )

    public void ShowSlider() {

        if (sliderView != null) {

            sliderView.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // ======================================
    // ২৫. Slider Hide
    // ======================================

    @SimpleFunction(
            description = "Slider লুকাবে"
    )

    public void HideSlider() {

        if (sliderView != null) {

            sliderView.setVisibility(
                    View.GONE
            );
        }
    }

    // ======================================
    // ২৬. Slider Remove
    // ======================================

    @SimpleFunction(
            description = "Slider সম্পূর্ণ সরিয়ে দেবে"
    )

    public void RemoveSlider() {

        // Auto Slide বন্ধ
        StopAutoSlide();

        // পুরোনো Request বাতিল
        requestNumber++;

        // Download Cancel
        if (currentDownload != null) {

            currentDownload.cancel(true);

            currentDownload = null;
        }

        // ImageView থেকে Bitmap সরানো
        if (imageView != null) {

            imageView.setImageBitmap(null);
        }

        // Bitmap পরিষ্কার
        RecycleBitmap(currentBitmap);

        currentBitmap = null;

        // Parent থেকে Slider সরানো
        if (sliderView != null) {

            ViewParentRemove(sliderView);
        }

        // UI Reference পরিষ্কার
        sliderView = null;

        imageView = null;

        titleView = null;

        subtitleView = null;

        indicatorLayout = null;

        // Data পরিষ্কার
        imageUrls.clear();

        titles.clear();

        subtitles.clear();

        currentIndex = 0;
    }

    // ======================================
    // ২৭. Parent থেকে View Remove
    // ======================================

    private void ViewParentRemove(View view) {

        if (view == null) {
            return;
        }

        if (view.getParent() instanceof ViewGroup) {

            ViewGroup parent =
                    (ViewGroup) view.getParent();

            parent.removeView(view);
        }
    }

    // ======================================
    // ২৮. Slider Created Event
    // ======================================

    @SimpleEvent(
            description = "Slider তৈরি হলে এই Event ঘটবে"
    )

    public void SliderCreated() {

        EventDispatcher.dispatchEvent(
                this,
                "SliderCreated"
        );
    }

    // ======================================
    // ২৯. Slide Changed Event
    // ======================================

    @SimpleEvent(
            description = "Slide পরিবর্তন হলে এই Event ঘটবে"
    )

    public void SlideChanged(
            int slideNumber,
            String imageUrl
    ) {

        EventDispatcher.dispatchEvent(
                this,
                "SlideChanged",
                slideNumber,
                imageUrl
        );
    }

    // ======================================
    // ৩০. Image Loaded Event
    // ======================================

    @SimpleEvent(
            description = "ছবি Load হলে এই Event ঘটবে"
    )

    public void BannerImageLoaded(String imageUrl) {

        EventDispatcher.dispatchEvent(
                this,
                "BannerImageLoaded",
                imageUrl
        );
    }

    // ======================================
    // ৩১. Image Failed Event
    // ======================================

    @SimpleEvent(
            description = "ছবি Load ব্যর্থ হলে এই Event ঘটবে"
    )

    public void BannerImageFailed(String imageUrl) {

        EventDispatcher.dispatchEvent(
                this,
                "BannerImageFailed",
                imageUrl
        );
    }

    // ======================================
    // ৩২. Slide Click Event
    // ======================================

    @SimpleEvent(
            description = "Slider-এ Click করলে এই Event ঘটবে"
    )

    public void SlideClicked(
            int slideNumber,
            String imageUrl
    ) {

        EventDispatcher.dispatchEvent(
                this,
                "SlideClicked",
                slideNumber,
                imageUrl
        );
    }

    // ======================================
    // ৩৩. Error Event
    // ======================================

    @SimpleEvent(
            description = "কোনো Error হলে এই Event ঘটবে"
    )

    public void ErrorOccurred(String message) {

        EventDispatcher.dispatchEvent(
                this,
                "ErrorOccurred",
                message
        );
    }
}

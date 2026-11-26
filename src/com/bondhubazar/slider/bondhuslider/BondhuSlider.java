
package com.bondhubazar.slider.bondhuslider;

// ==============================
// Android Imports
// ==============================

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
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

// ==============================
// App Inventor Imports
// ==============================

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

// ==============================
// Java Imports
// ==============================

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// ==============================
// Extension Information
// ==============================

@DesignerComponent(
        version = 3,
        description = "Bondhu Bazar Image Slider V3",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/extension.png"
)

public class BondhuSlider extends AndroidNonvisibleComponent {

    // ==============================
    // ১. Activity ও Handler
    // ==============================

    private final Activity activity;

    private final Handler handler;

    // ==============================
    // ২. Background Thread
    // ==============================

    // ছবি Download করার জন্য ২টি Thread
    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    // বর্তমান Image Download Task
    private Future<?> currentDownload;

    // ==============================
    // ৩. Slider UI
    // ==============================

    private FrameLayout sliderView;

    private ImageView imageView;

    private TextView titleView;

    private TextView subtitleView;

    private LinearLayout indicatorLayout;

    // বর্তমানে Display হওয়া Bitmap
    private Bitmap currentBitmap;

    // ==============================
    // ৪. Image Data
    // ==============================

    private final ArrayList<String> imageUrls =
            new ArrayList<>();

    private final ArrayList<String> titles =
            new ArrayList<>();

    private final ArrayList<String> subtitles =
            new ArrayList<>();

    // ==============================
    // ৫. Slider Settings
    // ==============================

    private int currentIndex = 0;

    private int sliderHeight = 160;

    private int interval = 3000;

    private int cornerRadius = 12;

    private boolean autoSlide = false;

    // Image Request ID
    private int requestNumber = 0;

    // সর্বোচ্চ Image Download Size: 8 MB
    private static final int MAX_IMAGE_SIZE =
            8 * 1024 * 1024;

    // ==============================
    // ৬. Auto Slide Runnable
    // ==============================

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
            }
        }
    };

    // ==============================
    // ৭. Constructor
    // ==============================

    public BondhuSlider(ComponentContainer container) {

        super(container.$form());

        Form form = container.$form();

        activity = form;

        handler = new Handler(
                Looper.getMainLooper()
        );
    }

    // ==============================
    // ৮. DP থেকে Pixel
    // ==============================

    private int dp(int value) {

        return (int) (
                value * activity.getResources()
                        .getDisplayMetrics()
                        .density + 0.5f
        );
    }

    // ==============================
    // ৯. Slider Create
    // ==============================

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

            // পুরোনো Slider সরানো
            RemoveSlider();

            // Arrangement পরীক্ষা
            if (arrangement == null ||
                    !(arrangement.getView() instanceof ViewGroup)) {

                ErrorOccurred(
                        "Arrangement অবশ্যই Layout হতে হবে"
                );

                return;
            }

            // ==============================
            // Image Data পরিষ্কার
            // ==============================

            imageUrls.clear();

            titles.clear();

            subtitles.clear();

            // ==============================
            // List থেকে Array
            // ==============================

            String[] urlArray =
                    urls.toStringArray();

            String[] titleArray =
                    slideTitles.toStringArray();

            String[] subtitleArray =
                    slideSubtitles.toString();

            // ==============================
            // URL ও Title একই Index-এ রাখা
            // ==============================

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

                // Title নেওয়া
                String title = "";

                if (i < titleArray.length) {

                    title = titleArray[i];
                }

                titles.add(title);

                // Subtitle নেওয়া
                String subtitle = "";

                if (i < subtitleArray.length) {

                    subtitle = subtitleArray[i];
                }

                subtitles.add(subtitle);
            }

            // ==============================
            // Image না থাকলে Error
            // ==============================

            if (imageUrls.isEmpty()) {

                ErrorOccurred(
                        "কোনো Image URL পাওয়া যায়নি"
                );

                return;
            }

            // ==============================
            // Slider Settings
            // ==============================

            sliderHeight = Math.max(
                    60,
                    height
            );

            interval = Math.max(
                    1000,
                    slideInterval
            );

            currentIndex = 0;

            // ==============================
            // Main Slider Layout
            // ==============================

            sliderView = new FrameLayout(activity);

            sliderView.setBackground(
                    CreateRoundedBackground(
                            Color.LTGRAY,
                            cornerRadius
                    )
            );

            sliderView.setClipToOutline(true);

            // ==============================
            // ImageView
            // ==============================

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

            // ==============================
            // Title Subtitle Container
            // ==============================

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

            // Semi-transparent Background
            GradientDrawable overlay =
                    new GradientDrawable();

            overlay.setColor(
                    0x88000000
            );

            textContainer.setBackground(
                    overlay
            );

            // ==============================
            // Title
            // ==============================

            titleView = new TextView(activity);

            titleView.setTextColor(
                    Color.WHITE
            );

            titleView.setTextSize(
                    18
            );

            titleView.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            // ==============================
            // Subtitle
            // ==============================

            subtitleView = new TextView(activity);

            subtitleView.setTextColor(
                    Color.WHITE
            );

            subtitleView.setTextSize(
                    13
            );

            // ==============================
            // Text যোগ
            // ==============================

            textContainer.addView(
                    titleView
            );

            textContainer.addView(
                    subtitleView
            );

            // ==============================
            // Text Layout Params
            // ==============================

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

            // ==============================
            // Indicator Layout
            // ==============================

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

            // ==============================
            // Swipe ও Click Listener
            // ==============================

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
                                            && Math.abs(diffX)
                                            > Math.abs(diffY)) {

                                        if (diffX > 0) {

                                            PreviousSlide();

                                        } else {

                                            NextSlide();
                                        }

                                    } else {

                                        // সাধারণ Click
                                        SlideClicked(
                                                currentIndex + 1,
                                                imageUrls.get(
                                                        currentIndex
                                                )
                                        );
                                    }

                                    return true;

                                case MotionEvent.ACTION_CANCEL:

                                    return true;
                            }

                            return true;
                        }
                    }
            );

            // ==============================
            // Parent Arrangement
            // ==============================

            ViewGroup parent =
                    (ViewGroup) arrangement.getView();

            // Parent-এর সাধারণ LayoutParams
            ViewGroup.LayoutParams mainParams =
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(sliderHeight)
                    );

            // Slider যোগ
            parent.addView(
                    sliderView,
                    mainParams
            );

            // ==============================
            // প্রথম Slide দেখানো
            // ==============================

            ShowCurrentSlide();

            // Created Event
            SliderCreated();

        } catch (Exception e) {

            ErrorOccurred(
                    "Slider Create Error: " +
                            e.toString()
            );
        }
    }

    // ==============================
    // ১০. Rounded Background
    // ==============================

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

    // ==============================
    // ১১. Current Slide দেখানো
    // ==============================

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

        // Title
        titleView.setText(
                titles.get(index)
        );

        // Subtitle
        subtitleView.setText(
                subtitles.get(index)
        );

        // Indicator Update
        UpdateIndicators();

        // ==============================
        // পুরোনো Request Cancel
        // ==============================

        if (currentDownload != null) {

            currentDownload.cancel(true);
        }

        // Request ID বৃদ্ধি
        final int thisRequest =
                ++requestNumber;

        // পুরোনো Image সরানো
        imageView.setImageBitmap(null);

        // আগের Bitmap পরিষ্কার
        if (currentBitmap != null) {

            if (!currentBitmap.isRecycled()) {

                currentBitmap.recycle();
            }

            currentBitmap = null;
        }

        // ==============================
        // Background Download
        // ==============================

        currentDownload = executor.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        Bitmap bitmap =
                                DownloadImage(imageUrl);

                        final Bitmap result =
                                bitmap;

                        // Main Thread-এ Result
                        handler.post(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        // পুরোনো Request হলে
                                        if (thisRequest
                                                != requestNumber) {

                                            RecycleBitmap(
                                                    result
                                            );

                                            return;
                                        }

                                        // Slider সরানো হলে
                                        if (sliderView == null ||
                                                imageView == null) {

                                            RecycleBitmap(
                                                    result
                                            );

                                            return;
                                        }

                                        // Image Loaded
                                        if (result != null) {

                                            currentBitmap =
                                                    result;

                                            imageView.setImageBitmap(
                                                    result
                                            );

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

    // ==============================
    // ১২. Bitmap পরিষ্কার
    // ==============================

    private void RecycleBitmap(
            Bitmap bitmap
    ) {

        if (bitmap != null &&
                !bitmap.isRecycled()) {

            bitmap.recycle();
        }
    }

    // ==============================
    // ১৩. Image Download
    // ==============================

    private Bitmap DownloadImage(
            String imageUrl
    ) {

        HttpURLConnection connection = null;

        InputStream inputStream = null;

        ByteArrayOutputStream outputStream = null;

        try {

            // URL তৈরি
            URL url =
                    new URL(imageUrl);

            // Connection
            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setConnectTimeout(
                    10000
            );

            connection.setReadTimeout(
                    15000
            );

            connection.setInstanceFollowRedirects(
                    true
            );

            connection.setRequestMethod(
                    "GET"
            );

            connection.connect();

            // Response Check
            int responseCode =
                    connection.getResponseCode();

            if (responseCode !=
                    HttpURLConnection.HTTP_OK) {

                return null;
            }

            // Content Length Check
            int contentLength =
                    connection.getContentLength();

            if (contentLength > MAX_IMAGE_SIZE) {

                return null;
            }

            // Input Stream
            inputStream =
                    connection.getInputStream();

            outputStream =
                    new ByteArrayOutputStream();

            byte[] buffer =
                    new byte[4096];

            int bytesRead;

            int totalBytes = 0;

            // ==============================
            // সর্বোচ্চ ৮ MB Download
            // ==============================

            while ((bytesRead =
                    inputStream.read(buffer)) != -1) {

                // Thread Cancel হলে
                if (Thread.currentThread()
                        .isInterrupted()) {

                    return null;
                }

                totalBytes += bytesRead;

                // Size Limit
                if (totalBytes >
                        MAX_IMAGE_SIZE) {

                    return null;
                }

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            // Byte Array
            byte[] imageBytes =
                    outputStream.toByteArray();

            // ==============================
            // Bitmap Decode
            // ==============================

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inJustDecodeBounds =
                    true;

            BitmapFactory.decodeByteArray(
                    imageBytes,
                    0,
                    imageBytes.length,
                    options
            );

            // Image Width / Height
            int imageWidth =
                    options.outWidth;

            int imageHeight =
                    options.outHeight;

            if (imageWidth <= 0 ||
                    imageHeight <= 0) {

                return null;
            }

            // ==============================
            // Sample Size
            // ==============================

            int requiredWidth =
                    dp(700);

            int sampleSize = 1;

            while (imageWidth / sampleSize
                    > requiredWidth * 2) {

                sampleSize *= 2;
            }

            options.inJustDecodeBounds =
                    false;

            options.inSampleSize =
                    sampleSize;

            options.inPreferredConfig =
                    Bitmap.Config.RGB_565;

            // RGB_565 কম Memory ব্যবহার করে
            options.inDither =
                    true;

            // Bitmap Decode
            return BitmapFactory.decodeByteArray(
                    imageBytes,
                    0,
                    imageBytes.length,
                    options
            );

        } catch (Exception e) {

            return null;

        } finally {

            // Stream বন্ধ
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

            // Connection বন্ধ
            if (connection != null) {

                connection.disconnect();
            }
        }
    }

    // ==============================
    // ১৪. Indicator Update
    // ==============================

    private void UpdateIndicators() {

        if (indicatorLayout == null) {

            return;
        }

        indicatorLayout.removeAllViews();

        for (int i = 0;
                i < imageUrls.size();
                i++) {

            final int dotIndex = i;

            TextView dot =
                    new TextView(activity);

            dot.setText(
                    "●"
            );

            dot.setTextSize(
                    12
            );

            // Active Dot
            if (i == currentIndex) {

                dot.setTextColor(
                        Color.WHITE
                );

            } else {

                dot.setTextColor(
                        0x88FFFFFF
                );
            }

            // Dot Click
            dot.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(
                                View view
                        ) {

                            GoToSlide(
                                    dotIndex
                            );
                        }
                    }
            );

            // Dot Margin
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(
                    dp(5),
                    0,
                    dp(5),
                    0
            );

            indicatorLayout.addView(
                    dot,
                    params
            );
        }
    }

    // ==============================
    // ১৫. নির্দিষ্ট Slide
    // ==============================

    @SimpleFunction(
            description = "নির্দিষ্ট Slide Number দেখাবে"
    )

    public void GoToSlide(
            int slideNumber
    ) {

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

        currentIndex =
                slideNumber - 1;

        ShowCurrentSlide();
    }

    // ==============================
    // ১৬. Next Slide
    // ==============================

    @SimpleFunction(
            description = "পরবর্তী ছবি দেখাবে"
    )

    public void NextSlide() {

        if (imageUrls.isEmpty()) {

            return;
        }

        currentIndex++;

        if (currentIndex >=
                imageUrls.size()) {

            currentIndex = 0;
        }

        ShowCurrentSlide();
    }

    // ==============================
    // ১৭. Previous Slide
    // ==============================

    @SimpleFunction(
            description = "আগের ছবি দেখাবে"
    )

    public void PreviousSlide() {

        if (imageUrls.isEmpty()) {

            return;
        }

        currentIndex--;

        if (currentIndex < 0) {

            currentIndex =
                    imageUrls.size() - 1;
        }

        ShowCurrentSlide();
    }

    // ==============================
    // ১৮. Auto Slide Start
    // ==============================

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

    // ==============================
    // ১৯. Auto Slide Stop
    // ==============================

    @SimpleFunction(
            description = "Auto Slide বন্ধ করবে"
    )

    public void StopAutoSlide() {

        autoSlide = false;

        handler.removeCallbacks(
                autoRunnable
        );
    }

    // ==============================
    // ২০. বর্তমান Slide Number
    // ==============================

    @SimpleFunction(
            description = "বর্তমান Slide Number দেবে"
    )

    public int GetCurrentSlide() {

        return currentIndex + 1;
    }

    // ==============================
    // ২১. মোট Slide সংখ্যা
    // ==============================

    @SimpleFunction(
            description = "মোট Slide সংখ্যা দেবে"
    )

    public int GetSlideCount() {

        return imageUrls.size();
    }

    // ==============================
    // ২২. বর্তমান Title পরিবর্তন
    // ==============================

    @SimpleFunction(
            description = "বর্তমান Slide-এর Title পরিবর্তন করবে"
    )

    public void SetCurrentTitle(
            String title
    ) {

        if (titleView != null) {

            titleView.setText(
                    title
            );
        }
    }

    // ==============================
    // ২৩. বর্তমান Subtitle পরিবর্তন
    // ==============================

    @SimpleFunction(
            description = "বর্তমান Slide-এর Subtitle পরিবর্তন করবে"
    )

    public void SetCurrentSubtitle(
            String subtitle
    ) {

        if (subtitleView != null) {

            subtitleView.setText(
                    subtitle
            );
        }
    }

    // ==============================
    // ২৪. Slider Show
    // ==============================

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

    // ==============================
    // ২৫. Slider Hide
    // ==============================

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

    // ==============================
    // ২৬. Slider Remove
    // ==============================

    @SimpleFunction(
            description = "Slider সম্পূর্ণ সরিয়ে দেবে"
    )

    public void RemoveSlider() {

        // Auto Slide বন্ধ
        StopAutoSlide();

        // পুরোনো Request Invalid
        requestNumber++;

        // Download Cancel
        if (currentDownload != null) {

            currentDownload.cancel(
                    true
            );

            currentDownload = null;
        }

        // Slider Parent থেকে সরানো
        if (sliderView != null) {

            ViewGroup parent =
                    (ViewGroup)
                            sliderView.getParent();

            if (parent != null) {

                parent.removeView(
                        sliderView
                );
            }
        }

        // Bitmap পরিষ্কার
        if (currentBitmap != null) {

            RecycleBitmap(
                    currentBitmap
            );

            currentBitmap = null;
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

    // ==============================
    // ২৭. Slider Created Event
    // ==============================

    @SimpleEvent(
            description = "Slider তৈরি হলে"
    )

    public void SliderCreated() {

        EventDispatcher.dispatchEvent(
                this,
                "SliderCreated"
        );
    }

    // ==============================
    // ২৮. Slide Changed Event
    // ==============================

    @SimpleEvent(
            description = "Slide পরিবর্তন হলে"
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

    // ==============================
    // ২৯. Image Loaded Event
    // ==============================

    @SimpleEvent(
            description = "ছবি সফলভাবে লোড হলে"
    )

    public void BannerImageLoaded(
            String imageUrl
    ) {

        EventDispatcher.dispatchEvent(
                this,
                "BannerImageLoaded",
                imageUrl
        );
    }

    // ==============================
    // ৩০. Image Failed Event
    // ==============================

    @SimpleEvent(
            description = "ছবি লোড ব্যর্থ হলে"
    )

    public void BannerImageFailed(
            String imageUrl
    ) {

        EventDispatcher.dispatchEvent(
                this,
                "BannerImageFailed",
                imageUrl
        );
    }

    // ==============================
    // ৩১. Slide Click Event
    // ==============================

    @SimpleEvent(
            description = "Slider-এ Click করলে"
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

    // ==============================
    // ৩২. Error Event
    // ==============================

    @SimpleEvent(
            description = "কোনো Error হলে"
    )

    public void ErrorOccurred(
            String message
    ) {

        EventDispatcher.dispatchEvent(
                this,
                "ErrorOccurred",
                message
        );
    }
}

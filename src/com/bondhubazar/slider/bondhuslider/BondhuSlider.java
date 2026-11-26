
package com.bondhubazar.slider.bondhuslider;

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

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.util.YailList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DesignerComponent(
        version = 2,
        description = "Bondhu Bazar Image Slider V2",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/extension.png"
)

public class BondhuSlider extends AndroidNonvisibleComponent {

    private final Form form;
    private final Activity activity;
    private final Handler handler;

    // ছবি ডাউনলোড করার জন্য Background Thread
    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    // Slider-এর মূল Layout
    private FrameLayout sliderView;

    // ছবি দেখানোর View
    private ImageView imageView;

    // Title ও Subtitle
    private TextView titleView;
    private TextView subtitleView;

    // Indicator Dots
    private LinearLayout indicatorLayout;

    // ছবির তথ্য রাখার List
    private final ArrayList<String> imageUrls = new ArrayList<>();
    private final ArrayList<String> titles = new ArrayList<>();
    private final ArrayList<String> subtitles = new ArrayList<>();

    // Slider-এর বর্তমান অবস্থান
    private int currentIndex = 0;

    // Slider-এর উচ্চতা
    private int sliderHeight = 160;

    // Auto Slide-এর সময়
    private int interval = 3000;

    private boolean autoSlide = false;

    // পুরোনো Image Request বাতিল করার জন্য
    private int requestNumber = 0;

    // Auto Slide Runnable
    private final Runnable autoRunnable = new Runnable() {
        @Override
        public void run() {
            if (autoSlide && imageUrls.size() > 1) {
                NextSlide();
                handler.postDelayed(this, interval);
            }
        }
    };

    public BondhuSlider(ComponentContainer container) {
        super(container.$form());

        form = container.$form();
        activity = form;
        handler = new Handler(Looper.getMainLooper());
    }

    // dp-কে pixel-এ পরিবর্তন
    private int dp(int value) {
        return (int) (value * activity.getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    // ==============================
    // ১. Slider তৈরি
    // ==============================

    @SimpleFunction(description = "Image Slider তৈরি করবে")
    public void CreateSlider(
            AndroidViewComponent arrangement,
            YailList urls,
            YailList slideTitles,
            YailList slideSubtitles,
            int height,
            int slideInterval) {

        try {
            RemoveSlider();

            if (!(arrangement.getView() instanceof ViewGroup)) {
                ErrorOccurred("Arrangement অবশ্যই Layout হতে হবে");
                return;
            }

            imageUrls.clear();
            titles.clear();
            subtitles.clear();

            // Image URL List নেওয়া
            String[] urlArray = urls.toStringArray();

            String[] titleArray = slideTitles.toStringArray();

            String[] subtitleArray = slideSubtitles.toStringArray();

            for (String url : urlArray) {
                if (url != null && !url.trim().isEmpty()) {
                    imageUrls.add(url.trim());
                }
            }

            if (imageUrls.size() == 0) {
                ErrorOccurred("কোনো Image URL পাওয়া যায়নি");
                return;
            }

            // Title এবং Subtitle সংরক্ষণ
            for (int i = 0; i < imageUrls.size(); i++) {

                String title = "";
                String subtitle = "";

                if (i < titleArray.length) {
                    title = titleArray[i];
                }

                if (i < subtitleArray.length) {
                    subtitle = subtitleArray[i];
                }

                titles.add(title);
                subtitles.add(subtitle);
            }

            sliderHeight = Math.max(60, height);
            interval = Math.max(1000, slideInterval);

            currentIndex = 0;

            // Main Slider Layout
            sliderView = new FrameLayout(activity);

            sliderView.setBackgroundColor(Color.LTGRAY);

            sliderView.setClipToOutline(true);

            FrameLayout.LayoutParams mainParams =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(sliderHeight)
                    );

            // ImageView
            imageView = new ImageView(activity);

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            FrameLayout.LayoutParams imageParams =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );

            sliderView.addView(imageView, imageParams);

            // Text Container
            LinearLayout textContainer = new LinearLayout(activity);

            textContainer.setOrientation(LinearLayout.VERTICAL);

            textContainer.setPadding(
                    dp(12), dp(8), dp(12), dp(28)
            );

            textContainer.setGravity(Gravity.BOTTOM);

            GradientDrawable overlay = new GradientDrawable();

            overlay.setColor(0x88000000);

            textContainer.setBackground(overlay);

            titleView = new TextView(activity);

            titleView.setTextColor(Color.WHITE);

            titleView.setTextSize(18);

            titleView.setTypeface(null, Typeface.BOLD);

            subtitleView = new TextView(activity);

            subtitleView.setTextColor(Color.WHITE);

            subtitleView.setTextSize(13);

            textContainer.addView(titleView);

            textContainer.addView(subtitleView);

            FrameLayout.LayoutParams textParams =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM
                    );

            sliderView.addView(textContainer, textParams);

            // Indicator Layout
            indicatorLayout = new LinearLayout(activity);

            indicatorLayout.setGravity(Gravity.CENTER);

            indicatorLayout.setOrientation(LinearLayout.HORIZONTAL);

            FrameLayout.LayoutParams dotParams =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(22),
                            Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
                    );

            sliderView.addView(indicatorLayout, dotParams);

            // Swipe Listener
            sliderView.setOnTouchListener(new View.OnTouchListener() {

                private float startX;

                @Override
                public boolean onTouch(View view, MotionEvent event) {

                    switch (event.getAction()) {

                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            return true;

                        case MotionEvent.ACTION_UP:

                            float difference = event.getX() - startX;

                            if (difference > dp(40)) {
                                PreviousSlide();
                            } else if (difference < -dp(40)) {
                                NextSlide();
                            }

                            return true;
                    }

                    return true;
                }
            });

            // Arrangement-এর মধ্যে Slider বসানো
            ViewGroup parent = (ViewGroup) arrangement.getView();

            parent.addView(sliderView, mainParams);

            // প্রথম ছবি দেখানো
            ShowCurrentSlide();

            SliderCreated();

        } catch (Exception e) {
            ErrorOccurred(e.toString());
        }
    }

    // ==============================
    // ২. বর্তমান Slide দেখানো
    // ==============================

    private void ShowCurrentSlide() {

        if (imageUrls.size() == 0 || sliderView == null) {
            return;
        }

        final int index = currentIndex;

        final String imageUrl = imageUrls.get(index);

        titleView.setText(titles.get(index));

        subtitleView.setText(subtitles.get(index));

        UpdateIndicators();

        final int thisRequest = ++requestNumber;

        // পুরোনো ছবি সরানো
        imageView.setImageBitmap(null);

        // Background Thread-এ ছবি ডাউনলোড
        executor.execute(new Runnable() {

            @Override
            public void run() {

                Bitmap bitmap = DownloadImage(imageUrl);

                final Bitmap result = bitmap;

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        // পুরোনো Request হলে দেখাবে না
                        if (thisRequest != requestNumber) {
                            if (result != null) {
                                result.recycle();
                            }
                            return;
                        }

                        if (imageView == null || sliderView == null) {
                            return;
                        }

                        if (result != null) {

                            imageView.setImageBitmap(result);

                            BannerImageLoaded(imageUrl);

                        } else {

                            BannerImageFailed(imageUrl);
                        }
                    }
                });
            }
        });

        SlideChanged(index + 1, imageUrl);
    }

    // ==============================
    // ৩. Image Download
    // ==============================

    private Bitmap DownloadImage(String imageUrl) {

        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {

            URL url = new URL(imageUrl);

            connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(10000);

            connection.setReadTimeout(15000);

            connection.setInstanceFollowRedirects(true);

            connection.connect();

            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int contentLength = connection.getContentLength();

            // 8 MB-এর বেশি হলে ডাউনলোড বন্ধ
            if (contentLength > 8 * 1024 * 1024) {
                return null;
            }

            inputStream = connection.getInputStream();

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inSampleSize = 2;

            return BitmapFactory.decodeStream(
                    inputStream, null, options
            );

        } catch (Exception e) {

            return null;

        } finally {

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ignored) {}

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ==============================
    // ৪. Indicator Dots
    // ==============================

    private void UpdateIndicators() {

        if (indicatorLayout == null) {
            return;
        }

        indicatorLayout.removeAllViews();

        for (int i = 0; i < imageUrls.size(); i++) {

            TextView dot = new TextView(activity);

            dot.setText("●");

            dot.setTextSize(12);

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

            params.setMargins(dp(4), 0, dp(4), 0);

            indicatorLayout.addView(dot, params);
        }
    }

    // ==============================
    // ৫. Next Slide
    // ==============================

    @SimpleFunction(description = "পরবর্তী ছবি দেখাবে")
    public void NextSlide() {

        if (imageUrls.size() == 0) {
            return;
        }

        currentIndex++;

        if (currentIndex >= imageUrls.size()) {
            currentIndex = 0;
        }

        ShowCurrentSlide();
    }

    // ==============================
    // ৬. Previous Slide
    // ==============================

    @SimpleFunction(description = "আগের ছবি দেখাবে")
    public void PreviousSlide() {

        if (imageUrls.size() == 0) {
            return;
        }

        currentIndex--;

        if (currentIndex < 0) {
            currentIndex = imageUrls.size() - 1;
        }

        ShowCurrentSlide();
    }

    // ==============================
    // ৭. Auto Slide চালু
    // ==============================

    @SimpleFunction(description = "Auto Slide চালু করবে")
    public void StartAutoSlide() {

        StopAutoSlide();

        if (imageUrls.size() <= 1) {
            return;
        }

        autoSlide = true;

        handler.postDelayed(autoRunnable, interval);
    }

    // ==============================
    // ৮. Auto Slide বন্ধ
    // ==============================

    @SimpleFunction(description = "Auto Slide বন্ধ করবে")
    public void StopAutoSlide() {

        autoSlide = false;

        handler.removeCallbacks(autoRunnable);
    }

    // ==============================
    // ৯. বর্তমান Slide Number
    // ==============================

    @SimpleFunction(description = "বর্তমান Slide Number দেবে")
    public int GetCurrentSlide() {

        return currentIndex + 1;
    }

    // ==============================
    // ১০. মোট Slide সংখ্যা
    // ==============================

    @SimpleFunction(description = "মোট Slide সংখ্যা দেবে")
    public int GetSlideCount() {

        return imageUrls.size();
    }

    // ==============================
    // ১১. Slide-এর Title পরিবর্তন
    // ==============================

    @SimpleFunction(description = "বর্তমান Slide-এর Title পরিবর্তন করবে")
    public void SetCurrentTitle(String title) {

        if (titleView != null) {
            titleView.setText(title);
        }
    }

    // ==============================
    // ১২. Slide-এর Subtitle পরিবর্তন
    // ==============================

    @SimpleFunction(description = "বর্তমান Slide-এর Subtitle পরিবর্তন করবে")
    public void SetCurrentSubtitle(String subtitle) {

        if (subtitleView != null) {
            subtitleView.setText(subtitle);
        }
    }

    // ==============================
    // ১৩. Slider দেখানো
    // ==============================

    @SimpleFunction(description = "Slider দেখাবে")
    public void ShowSlider() {

        if (sliderView != null) {
            sliderView.setVisibility(View.VISIBLE);
        }
    }

    // ==============================
    // ১৪. Slider লুকানো
    // ==============================

    @SimpleFunction(description = "Slider লুকাবে")
    public void HideSlider() {

        if (sliderView != null) {
            sliderView.setVisibility(View.GONE);
        }
    }

    // ==============================
    // ১৫. Slider মুছে ফেলা
    // ==============================

    @SimpleFunction(description = "Slider সরিয়ে দেবে")
    public void RemoveSlider() {

        StopAutoSlide();

        // চলমান Image Request বাতিল
        requestNumber++;

        if (sliderView != null) {

            ViewGroup parent =
                    (ViewGroup) sliderView.getParent();

            if (parent != null) {
                parent.removeView(sliderView);
            }
        }

        sliderView = null;
        imageView = null;
        titleView = null;
        subtitleView = null;
        indicatorLayout = null;

        imageUrls.clear();
        titles.clear();
        subtitles.clear();

        currentIndex = 0;
    }

    // ==============================
    // ১৬. Events
    // ==============================

    @SimpleEvent(description = "Slider তৈরি হলে")
    public void SliderCreated() {
        EventDispatcher.dispatchEvent(this, "SliderCreated");
    }

    @SimpleEvent(description = "Slide পরিবর্তন হলে")
    public void SlideChanged(int slideNumber, String imageUrl) {
        EventDispatcher.dispatchEvent(
                this, "SlideChanged", slideNumber, imageUrl
        );
    }

    @SimpleEvent(description = "ছবি লোড হলে")
    public void BannerImageLoaded(String imageUrl) {
        EventDispatcher.dispatchEvent(
                this, "BannerImageLoaded", imageUrl
        );
    }

    @SimpleEvent(description = "ছবি লোড ব্যর্থ হলে")
    public void BannerImageFailed(String imageUrl) {
        EventDispatcher.dispatchEvent(
                this, "BannerImageFailed", imageUrl
        );
    }

    @SimpleEvent(description = "Slider-এ ক্লিক করলে")
    public void SlideClicked(int slideNumber, String imageUrl) {
        EventDispatcher.dispatchEvent(
                this, "SlideClicked", slideNumber, imageUrl
        );
    }

    @SimpleEvent(description = "কোনো Error হলে")
    public void ErrorOccurred(String message) {
        EventDispatcher.dispatchEvent(
                this, "ErrorOccurred", message
        );
    }
}

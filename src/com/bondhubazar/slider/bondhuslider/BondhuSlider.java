
package com.bondhubazar.slider.bondhuslider;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.util.TypedValue;

import com.google.appinventor.components.annotations.Extension;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;

@Extension(
    description = "Bondhu Bazar Slider Extension"
)
public class BondhuSlider extends AndroidNonvisibleComponent {

    private final android.content.Context context;
    private ViewFlipper slider;

    public BondhuSlider(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
    }

    // DP থেকে Pixel
    private int dp(int value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.getResources().getDisplayMetrics()
        );
    }

    // =====================================
    // ১. Slider তৈরি
    // =====================================

    @SimpleFunction(description = "Text Slide দিয়ে Slider তৈরি করবে")
    public void CreateSlider(
        AndroidViewComponent arrangement,
        YailList titles,
        YailList subtitles,
        int height,
        int interval
    ) {

        try {

            View parentView = arrangement.getView();

            if (!(parentView instanceof ViewGroup)) {
                ErrorOccurred("Arrangement পাওয়া যায়নি");
                return;
            }

            ViewGroup parent = (ViewGroup) parentView;

            // আগের Slider সরানো
            RemoveSlider();

            slider = new ViewFlipper(context);

            slider.setFlipInterval(
                Math.max(1000, interval)
            );

            int count = Math.min(
                titles.size(),
                subtitles.size()
            );

            if (count == 0) {
                ErrorOccurred("Slide-এর তথ্য পাওয়া যায়নি");
                return;
            }

            // প্রতিটি Slide তৈরি
            for (int i = 0; i < count; i++) {

                String title = String.valueOf(
                    titles.get(i)
                );

                String subtitle = String.valueOf(
                    subtitles.get(i)
                );

                TextView slideView = new TextView(context);

                slideView.setText(
                    title + "\n" + subtitle
                );

                slideView.setTextColor(Color.WHITE);
                slideView.setTextSize(18);
                slideView.setTypeface(null, Typeface.BOLD);
                slideView.setGravity(Gravity.CENTER);
                slideView.setPadding(
                    dp(12), dp(12), dp(12), dp(12)
                );

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.rgb(20, 110, 65));
                bg.setCornerRadius(dp(16));

                slideView.setBackground(bg);

                final int slideIndex = i;

                slideView.setOnClickListener(v -> {
                    SlideClicked(slideIndex);
                });

                slider.addView(
                    slideView,
                    new ViewGroup.LayoutParams(-1, -1)
                );
            }

            parent.addView(
                slider,
                new ViewGroup.LayoutParams(
                    -1,
                    dp(Math.max(80, height))
                )
            );

            SliderCreated(count);

        } catch (Exception e) {
            ErrorOccurred(e.getMessage());
        }
    }

    // =====================================
    // ২. পরের Slide
    // =====================================

    @SimpleFunction(description = "পরের Slide দেখাবে")
    public void NextSlide() {

        if (slider == null) return;

        slider.showNext();

        SlideChanged(slider.getDisplayedChild());
    }

    // =====================================
    // ৩. আগের Slide
    // =====================================

    @SimpleFunction(description = "আগের Slide দেখাবে")
    public void PreviousSlide() {

        if (slider == null) return;

        slider.showPrevious();

        SlideChanged(slider.getDisplayedChild());
    }

    // =====================================
    // ৪. Auto Slide চালু
    // =====================================

    @SimpleFunction(description = "Automatic Slide চালু করবে")
    public void StartAutoSlide() {

        if (slider != null) {
            slider.startFlipping();
        }
    }

    // =====================================
    // ৫. Auto Slide বন্ধ
    // =====================================

    @SimpleFunction(description = "Automatic Slide বন্ধ করবে")
    public void StopAutoSlide() {

        if (slider != null) {
            slider.stopFlipping();
        }
    }

    // =====================================
    // ৬. বর্তমান Slide Number
    // =====================================

    @SimpleFunction(description = "বর্তমান Slide-এর Index দেবে")
    public int GetCurrentSlide() {

        if (slider == null) return -1;

        return slider.getDisplayedChild();
    }

    // =====================================
    // ৭. Slide-এর সংখ্যা
    // =====================================

    @SimpleFunction(description = "মোট Slide সংখ্যা দেবে")
    public int GetSlideCount() {

        if (slider == null) return 0;

        return slider.getChildCount();
    }

    // =====================================
    // ৮. Slider Show / Hide
    // =====================================

    @SimpleFunction(description = "Slider দেখাবে")
    public void ShowSlider() {

        if (slider != null) {
            slider.setVisibility(View.VISIBLE);
        }
    }

    @SimpleFunction(description = "Slider লুকাবে")
    public void HideSlider() {

        if (slider != null) {
            slider.setVisibility(View.GONE);
        }
    }

    // =====================================
    // ৯. Slider Remove
    // =====================================

    @SimpleFunction(description = "Slider সরিয়ে দেবে")
    public void RemoveSlider() {

        if (slider != null) {

            slider.stopFlipping();

            if (slider.getParent() instanceof ViewGroup) {
                ((ViewGroup) slider.getParent())
                    .removeView(slider);
            }

            slider = null;
        }
    }

    // =====================================
    // EVENTS
    // =====================================

    @SimpleEvent(description = "Slider তৈরি হলে")
    public void SliderCreated(int slideCount) {
        EventDispatcher.dispatchEvent(
            this, "SliderCreated", slideCount
        );
    }

    @SimpleEvent(description = "Slide পরিবর্তন হলে")
    public void SlideChanged(int position) {
        EventDispatcher.dispatchEvent(
            this, "SlideChanged", position
        );
    }

    @SimpleEvent(description = "Slide-এ Click করলে")
    public void SlideClicked(int position) {
        EventDispatcher.dispatchEvent(
            this, "SlideClicked", position
        );
    }

    @SimpleEvent(description = "কোনো সমস্যা হলে")
    public void ErrorOccurred(String message) {
        EventDispatcher.dispatchEvent(
            this, "ErrorOccurred", message
        );
    }
}


package com.bondhubazar.banner.bondhubanner;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;

import com.google.appinventor.components.annotations.Extension;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

@Extension(
    description = "Bondhu Bazar Banner Extension"
)
public class BondhuBanner extends AndroidNonvisibleComponent {

    private final android.content.Context context;
    private LinearLayout bannerView;

    public BondhuBanner(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
    }

    // DP থেকে Pixel তৈরি
    private int dp(int value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.getResources().getDisplayMetrics()
        );
    }

    // =====================================
    // ১. Banner তৈরি
    // =====================================

    @SimpleFunction(description = "নতুন Banner তৈরি করবে")
    public void CreateBanner(
        AndroidViewComponent arrangement,
        String title,
        String subtitle,
        int backgroundColor,
        int textColor,
        int height
    ) {

        try {

            View parentView = arrangement.getView();

            if (!(parentView instanceof ViewGroup)) {
                ErrorOccurred("Arrangement পাওয়া যায়নি");
                return;
            }

            ViewGroup parent = (ViewGroup) parentView;

            // আগের Banner থাকলে সরানো
            if (bannerView != null &&
                bannerView.getParent() instanceof ViewGroup) {

                ((ViewGroup) bannerView.getParent())
                    .removeView(bannerView);
            }

            // Banner Layout
            bannerView = new LinearLayout(context);

            bannerView.setOrientation(LinearLayout.VERTICAL);
            bannerView.setGravity(android.view.Gravity.CENTER);
            bannerView.setPadding(
                dp(16), dp(12), dp(16), dp(12)
            );

            // Rounded Background
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(backgroundColor);
            bg.setCornerRadius(dp(16));

            bannerView.setBackground(bg);

            // Title
            TextView titleView = new TextView(context);

            titleView.setText(title);
            titleView.setTextColor(textColor);
            titleView.setTextSize(21);
            titleView.setTypeface(null, Typeface.BOLD);
            titleView.setGravity(android.view.Gravity.CENTER);

            // Subtitle
            TextView subtitleView = new TextView(context);

            subtitleView.setText(subtitle);
            subtitleView.setTextColor(textColor);
            subtitleView.setTextSize(14);
            subtitleView.setGravity(android.view.Gravity.CENTER);

            // View যুক্ত করা
            bannerView.addView(titleView);

            LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(
                    -1, -2
                );

            subtitleParams.topMargin = dp(6);

            bannerView.addView(
                subtitleView,
                subtitleParams
            );

            // Arrangement-এ Banner বসানো
            parent.addView(
                bannerView,
                new ViewGroup.LayoutParams(
                    -1,
                    dp(Math.max(60, height))
                )
            );

            // Banner Click
            bannerView.setOnClickListener(v -> {
                BannerClicked();
            });

            BannerCreated();

        } catch (Exception e) {
            ErrorOccurred(e.getMessage());
        }
    }

    // =====================================
    // ২. Banner-এর লেখা পরিবর্তন
    // =====================================

    @SimpleFunction(description = "Banner-এর Title ও Subtitle পরিবর্তন")
    public void SetBannerText(
        String title,
        String subtitle
    ) {

        if (bannerView == null) return;

        if (bannerView.getChildCount() >= 2) {

            ((TextView) bannerView.getChildAt(0))
                .setText(title);

            ((TextView) bannerView.getChildAt(1))
                .setText(subtitle);
        }
    }

    // =====================================
    // ৩. Banner দেখানো / লুকানো
    // =====================================

    @SimpleFunction(description = "Banner Show করবে")
    public void ShowBanner() {

        if (bannerView != null) {
            bannerView.setVisibility(View.VISIBLE);
        }
    }

    @SimpleFunction(description = "Banner Hide করবে")
    public void HideBanner() {

        if (bannerView != null) {
            bannerView.setVisibility(View.GONE);
        }
    }

    // =====================================
    // ৪. Banner Remove
    // =====================================

    @SimpleFunction(description = "Banner সরিয়ে দেবে")
    public void RemoveBanner() {

        if (bannerView != null &&
            bannerView.getParent() instanceof ViewGroup) {

            ((ViewGroup) bannerView.getParent())
                .removeView(bannerView);
        }

        bannerView = null;
        BannerRemoved();
    }

    // =====================================
    // EVENTS
    // =====================================

    @SimpleEvent(description = "Banner তৈরি হলে")
    public void BannerCreated() {
        EventDispatcher.dispatchEvent(this, "BannerCreated");
    }

    @SimpleEvent(description = "Banner-এ Click করলে")
    public void BannerClicked() {
        EventDispatcher.dispatchEvent(this, "BannerClicked");
    }

    @SimpleEvent(description = "Banner Remove হলে")
    public void BannerRemoved() {
        EventDispatcher.dispatchEvent(this, "BannerRemoved");
    }

    @SimpleEvent(description = "কোনো সমস্যা হলে")
    public void ErrorOccurred(String message) {
        EventDispatcher.dispatchEvent(
            this, "ErrorOccurred", message
        );
    }
}

package mogic.toad;

import android.os.Bundle;
import android.os.Build;
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.PopupMenu;
import android.support.v4.view.ViewCompat;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;
import android.view.View;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.Display;
import android.graphics.Point;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;

import com.just.agentweb.AgentWeb;
import com.just.agentweb.NestedScrollAgentWebView;

public class ToadBaseWebActivity extends AppCompatActivity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    protected CoordinatorLayout mCoordinatorLayout;
    protected AppBarLayout mAppBarLayout;
    protected Toolbar mToolbar;
    protected TextView mTitleTextView;
    protected AppCompatImageButton mLeftButton;
    protected AppCompatImageButton mRightButton;
    protected AppCompatImageButton mForwardButton;
    protected AppCompatImageButton mBackButton;
    protected PopupMenu mLeftPopupMenu;
    protected PopupMenu mRightPopupMenu;

    protected int mIndicatorColor;
    protected int mIndicatorHeight;

    protected AgentWeb mAgentWeb;
    protected AgentWeb.CommonBuilder mAgentWebBuilder;

    protected ToadWebView mWebView;

    protected WebSettings mWebSettings;
    protected String mDefaultUserAgent;
    protected boolean mIsFullscreen;

    public class ToadWebView extends NestedScrollAgentWebView {
        public ToadWebView(Context context) {
            super(context);
        }
    }

    public class ToadWebViewClient extends WebViewClient {
    }

    public class ToadWebChromeClient extends WebChromeClient {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            mTitleTextView.setText(title);
        }
    }

    protected ToadWebView createWebView(Context context) {
        return new ToadWebView(context);
    }

    protected ToadWebViewClient createWebViewClient() {
        return new ToadWebViewClient();
    }

    protected ToadWebChromeClient createWebChromeClient() {
        return new ToadWebChromeClient();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toad_web_activity);
        initView();
        createAgentWeb();
    }

    protected void initView() {
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.main);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mTitleTextView.setMaxWidth(getDisplayWidth() - dpToPx(220));
        mLeftButton = (AppCompatImageButton) findViewById(R.id.left_button);
        mRightButton = (AppCompatImageButton) findViewById(R.id.right_button);
        mForwardButton = (AppCompatImageButton) findViewById(R.id.forward_button);
        mBackButton = (AppCompatImageButton) findViewById(R.id.back_button);
        mLeftButton.setOnClickListener(this);
        mRightButton.setOnClickListener(this);
        mForwardButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
        mIndicatorColor = Color.parseColor("#ff0000");
        mIndicatorHeight = 2;

        if (mLeftPopupMenu == null) {
            mLeftPopupMenu = new PopupMenu(this, mLeftButton);
            mLeftPopupMenu.inflate(R.menu.toad_left_menu);
            mLeftPopupMenu.setOnMenuItemClickListener(this);
        }
        if (mRightPopupMenu == null) {
            mRightPopupMenu = new PopupMenu(this, mRightButton);
            mRightPopupMenu.inflate(R.menu.toad_right_menu);
            mRightPopupMenu.setOnMenuItemClickListener(this);
        }
    }

    protected void beforeCreateAgentWeb() {
        mWebView = createWebView(this);

        mWebSettings = mWebView.getSettings();
        mDefaultUserAgent = mWebSettings.getUserAgentString();

        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(-1, -1);
        params.setBehavior(new AppBarLayout.ScrollingViewBehavior());

        mAgentWebBuilder = AgentWeb.with(this)
                .setAgentWebParent(mCoordinatorLayout, 1, params)
                .useDefaultIndicator(mIndicatorColor, mIndicatorHeight)
                .setWebView(mWebView)
                .setWebViewClient(createWebViewClient())
                .setWebChromeClient(createWebChromeClient());
    }
    
    protected void createAgentWeb() {
        beforeCreateAgentWeb();
        mAgentWeb = mAgentWebBuilder.createAgentWeb().ready().go(null);
        mWebSettings.setUserAgentString(mDefaultUserAgent);
        afterCreateAgentWeb();
    }
    
    protected void afterCreateAgentWeb() {
    }

    public void enterFullscreen() {
        mWebView.setNestedScrollingEnabled(false);
        mAppBarLayout.setExpanded(false, true);
        mIsFullscreen = true;
    }

    public void exitFullScreen() {
        mAppBarLayout.setExpanded(true, true);
        mWebView.setNestedScrollingEnabled(true);
        mIsFullscreen = false;
    }

    public void enableDesktopMode() {
        mWebSettings.setUserAgentString(mDefaultUserAgent.replace("Android", "").replace("Mobile", ""));
    }

    public void disableDesktopMode() {
        mWebSettings.setUserAgentString(mDefaultUserAgent);
    }

    public void copyUrlToClipboard() {
        String url = mWebView.getUrl();
        setClipboard(getApplicationContext(), url);
        Toast.makeText(getApplicationContext(), url, Toast.LENGTH_SHORT).show();
    }

    public void exit() {
        System.exit(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAgentWeb.getWebLifeCycle().onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_button:
                if (mAgentWeb.getWebCreator().getWebView().canGoBack()) {
                    mAgentWeb.back();
                }
                break;
            case R.id.forward_button:
                if (mAgentWeb.getWebCreator().getWebView().canGoForward()) {
                    mAgentWeb.getWebCreator().getWebView().goForward();
                }
                break;
            case R.id.left_button:
                mLeftPopupMenu.show();
                break;
            case R.id.right_button:
                mRightPopupMenu.show();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_refresh:
                mAgentWeb.getUrlLoader().reload();
                return true;
            case R.id.menu_item_fullscreen:
                enterFullscreen();
                return true;
            case R.id.menu_item_desktop_mode:
                if (!item.isChecked()) {
                    enableDesktopMode();
                    item.setChecked(true);
                } else {
                    disableDesktopMode();
                    item.setChecked(false);
                }
                return true;
            case R.id.menu_item_copy_url:
                copyUrlToClipboard();
                return true;
            case R.id.menu_item_exit:
                exit();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mIsFullscreen == true) {
                exitFullScreen();
                return true;
            }
        }
        if (mAgentWeb.handleKeyEvent(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        mAgentWeb.getWebLifeCycle().onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mAgentWeb.getWebLifeCycle().onResume();
        super.onResume();
    }

    private void setClipboard(Context context, String text) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    private DisplayMetrics getDisplayMetrics() {
        Context context = this.getApplicationContext();
        return context.getResources().getDisplayMetrics();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getDisplayMetrics().density + 0.5f);
    }

    private int pxToDp(int px) {
        return (int) (px / getDisplayMetrics().density + 0.5f);
    }

    private Display getDefaultDisplay() {
        Context context = this.getApplicationContext();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return windowManager.getDefaultDisplay();
    }

    private int getDisplayWidth() {
        Display display = getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            return size.x;
        } else {
            return display.getWidth();
        }
    }

    private int getDisplayHeight() {
        Display display = getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            return size.y;
        } else {
            return display.getHeight();
        }
    }
}

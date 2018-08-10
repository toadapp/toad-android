package mogic.toad;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.widget.Toast;
import android.graphics.Bitmap;

import mogic.toad.proxyhandler.ProxyServer;
import mogic.toad.proxyhandler.IProxyListener;
import mogic.toad.proxyhandler.ConnectionHandler;

public class ToadWebActivity extends ToadBaseWebActivity implements IProxyListener {
    protected ProxyServer mProxyServer;
    protected MenuItem mCustomMenuItems[];
    protected String mCustomMenuItemUrls[];
    protected boolean mIsTrustedPage;
    protected ToadStringSet mTrustedUrls;

    protected String getInitialUrl() {
        return "https://hahamama.github.io/";
    }

    protected void setDefaultCustomMenuItems() {
        displayCustomMenuItem(1, "起始页", getInitialUrl());
        displayCustomMenuItem(2, "膜乎", "https://www.mohu.club/");
        displayCustomMenuItem(3, "辱乎", "https://www.ruhu.ml/");
        displayCustomMenuItem(4, "品葱", "https://www.pin-cong.com/");
        displayCustomMenuItem(9, "无法正常使用？", "https://toadbucket.bitbucket.io/");
        displayCustomMenuItem(10, "关于...", "https://github.com/toadapp/toad-android");
    }

    protected void setDefaultHosts() {
        ToadHosts.set("(.+?)\\.netlify\\.com", "netlify.com");
        ToadHosts.set("(.+?)\\.bitbucket\\.io", "bitbucket.io");
        ToadHosts.set("(.+?)\\.github\\.io", "raw.githubusercontent.com");
        ToadHosts.set("(.*\\.|)mohu\\.club", "1.0.0.1");
        ToadHosts.set("(.*\\.|)ruhu\\.ml", "1.0.0.1");
        ToadHosts.set("(.*\\.|)pin-cong\\.com", "1.0.0.1");

        ToadHosts.set("(.*\\.|)googlesyndication\\.com", "0.0.0.0");
        ToadHosts.set("(.*\\.|)google-analytics\\.com", "0.0.0.0");
        ToadHosts.set("(.*\\.|)51\\.la", "0.0.0.0");
        ToadHosts.set("(.*\\.|)51yes\\.com", "0.0.0.0");
        ToadHosts.set("(.*\\.|)cnzz\\.com", "0.0.0.0");
        ToadHosts.set("(.*\\.|)baidu\\.com", "0.0.0.0");
        ToadHosts.set("cpro\\.baidustatic\\.com", "0.0.0.0");
    }

    protected void setDefaultInterceptorUrls() {
        ToadUrlInterceptor.Blacklist.add("https://(www\\.)?mohu\\d?\\..+/matomo/piwik\\.js");
   }

    protected void setDefaultTrustedUrls() {
        mTrustedUrls.add("https://toadbucket\\.bitbucket\\.io/.*");
        mTrustedUrls.add("https://hahamama\\.github\\.io/.*");
        mTrustedUrls.add("https://toadapp\\.github\\.io/.*");
    }

    @Override
    protected void beforeCreateAgentWeb() {
        super.beforeCreateAgentWeb();
        mIsTrustedPage = false;
        mTrustedUrls = new ToadStringSet();
        initCustomMenuItems();
        ConnectionHandler.setForceHttps(true);
        setDefaultHosts();
        setDefaultInterceptorUrls();
        setDefaultTrustedUrls();
    }

    @Override
    protected void afterCreateAgentWeb() {
        mProxyServer = new ProxyServer();
        mProxyServer.setCallback(this);
        mProxyServer.startServer();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mWebView.addJavascriptInterface(new ToadJavascriptInterface.Hosts(this), "ToadHosts");
            mWebView.addJavascriptInterface(new ToadJavascriptInterface.UrlWhitelist(this), "ToadUrlWhitelist");
            mWebView.addJavascriptInterface(new ToadJavascriptInterface.UrlBlacklist(this), "ToadUrlBlacklist");
            mWebView.addJavascriptInterface(new ToadJavascriptInterface.Menu(this), "ToadMenu");
        }
    }

    @Override
    public void onReportProxyPort(int port) {
        ToadWebViewProxyUtil.setProxy(mWebView, "localhost", port);
        mWebView.loadUrl(getInitialUrl());

        setDefaultCustomMenuItems();
    }

    @Override
    protected ToadWebViewClient createWebViewClient() {
        return new ToadWebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                verifyPageUrl(url);
                super.onPageStarted(view, url, favicon);
            }

            @Deprecated
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (!TextUtils.isEmpty(url) && ToadUrlInterceptor.intercept(url)) {
                    return new WebResourceResponse(null, null, null);
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (!TextUtils.isEmpty(url) && ToadUrlInterceptor.intercept(url)) {
                        return new WebResourceResponse(null, null, null);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        };
    }

    protected void verifyPageUrl(String url) {
        mIsTrustedPage = mTrustedUrls.match(url);
    }

    protected boolean isTrustedPage() {
        return mIsTrustedPage;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = fromCustomMenuItemId(item.getItemId());
        if (id != -1) {
            customMenuItemClicked(id);
            return true;
        }
        return super.onMenuItemClick(item);
    }

    protected void customMenuItemClicked(int id) {
        if (id < 1 || id > 10)
            return;
        String url = mCustomMenuItemUrls[id - 1];
        if (!TextUtils.isEmpty(url))
            mWebView.loadUrl(url);
    }

    protected void initCustomMenuItems() {
        Menu rightMenu = mRightPopupMenu.getMenu();
        mCustomMenuItems = new MenuItem[10];
        mCustomMenuItemUrls = new String[10];
        for (int i = 0; i < 10; i++) {
            mCustomMenuItems[i] = rightMenu.findItem(toCustomMenuItemId(i + 1));
            mCustomMenuItemUrls[i] = null;
        }
    }

    protected void displayCustomMenuItem(int id, String text, String url) {
        setCustomMenuItem(id, text, url);
        setCustomMenuItemVisible(id, true);
    }

    protected void setCustomMenuItem(int id, String text, String url) {
        if (id < 1 || id > 10)
            return;
        mCustomMenuItems[id - 1].setTitle(text);
        mCustomMenuItemUrls[id - 1] = url;
    }

    protected void setCustomMenuItemVisible(int id, Boolean visible) {
        if (id < 1 || id > 10)
            return;
        mCustomMenuItems[id - 1].setVisible(visible);
    }

    protected int toCustomMenuItemId(int num) {
        switch (num) {
            case 1:
                return R.id.menu_item_custom_1;
            case 2:
                return R.id.menu_item_custom_2;
            case 3:
                return R.id.menu_item_custom_3;
            case 4:
                return R.id.menu_item_custom_4;
            case 5:
                return R.id.menu_item_custom_5;
            case 6:
                return R.id.menu_item_custom_6;
            case 7:
                return R.id.menu_item_custom_7;
            case 8:
                return R.id.menu_item_custom_8;
            case 9:
                return R.id.menu_item_custom_9;
            case 10:
                return R.id.menu_item_custom_10;
        }
        return -1;
    }

    protected int fromCustomMenuItemId(int menuItemId) {
        switch (menuItemId) {
            case R.id.menu_item_custom_1:
                return 1;
            case R.id.menu_item_custom_2:
                return 2;
            case R.id.menu_item_custom_3:
                return 3;
            case R.id.menu_item_custom_4:
                return 4;
            case R.id.menu_item_custom_5:
                return 5;
            case R.id.menu_item_custom_6:
                return 6;
            case R.id.menu_item_custom_7:
                return 7;
            case R.id.menu_item_custom_8:
                return 8;
            case R.id.menu_item_custom_9:
                return 9;
            case R.id.menu_item_custom_10:
                return 10;
        }
        return -1;
    }

}
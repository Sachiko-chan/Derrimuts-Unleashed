.class public final Lprivacy/WebPolicy;
.super Ljava/lang/Object;

.method public static fileAccess(Landroid/webkit/WebSettings;Z)V
    .locals 0
    const/4 p1, 0x0
    invoke-virtual {p0, p1}, Landroid/webkit/WebSettings;->setAllowFileAccess(Z)V
    invoke-virtual {p0, p1}, Landroid/webkit/WebSettings;->setAllowFileAccessFromFileURLs(Z)V
    invoke-virtual {p0, p1}, Landroid/webkit/WebSettings;->setAllowUniversalAccessFromFileURLs(Z)V
    return-void
.end method

.method public static contentAccess(Landroid/webkit/WebSettings;Z)V
    .locals 0
    const/4 p1, 0x0
    invoke-virtual {p0, p1}, Landroid/webkit/WebSettings;->setAllowContentAccess(Z)V
    return-void
.end method

.method public static mixedContent(Landroid/webkit/WebSettings;I)V
    .locals 0
    const/4 p1, 0x1
    invoke-virtual {p0, p1}, Landroid/webkit/WebSettings;->setMixedContentMode(I)V
    return-void
.end method

.method public static thirdPartyCookies(Landroid/webkit/CookieManager;Landroid/webkit/WebView;Z)V
    .locals 0
    const/4 p2, 0x0
    invoke-virtual {p0, p1, p2}, Landroid/webkit/CookieManager;->setAcceptThirdPartyCookies(Landroid/webkit/WebView;Z)V
    return-void
.end method

.method public static debug(Z)V
    .locals 0
    const/4 p0, 0x0
    invoke-static {p0}, Landroid/webkit/WebView;->setWebContentsDebuggingEnabled(Z)V
    return-void
.end method

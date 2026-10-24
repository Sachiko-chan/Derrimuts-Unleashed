.class public final Lprivacy/CameraPolicy;
.super Ljava/lang/Object;

.method public static resume(Lul/f;)V
    .locals 2
    if-eqz p0, :done
    invoke-virtual {p0}, Landroid/view/View;->getContext()Landroid/content/Context;
    move-result-object v0
    const-string v1, "android.permission.CAMERA"
    invoke-virtual {v0, v1}, Landroid/content/Context;->checkSelfPermission(Ljava/lang/String;)I
    move-result v0
    if-nez v0, :done
    invoke-virtual {p0}, Lul/f;->c()V
    :done
    return-void
.end method

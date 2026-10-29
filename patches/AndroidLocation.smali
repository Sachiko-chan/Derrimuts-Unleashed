.class public final Lprivacy/AndroidLocation;
.super Ljava/lang/Object;

.method public static request(Landroid/support/v4/media/session/r;Landroid/content/Context;Lcom/google/android/material/datepicker/i;)V
    .locals 1
    const/4 v0, 0x0
    invoke-static {p0, p1, p2, v0}, Lprivacy/AndroidLocation;->request(Landroid/support/v4/media/session/r;Landroid/content/Context;Lcom/google/android/material/datepicker/i;Lorg/json/JSONObject;)V
    return-void
.end method

.method public static request(Landroid/support/v4/media/session/r;Landroid/content/Context;Lcom/google/android/material/datepicker/i;Lorg/json/JSONObject;)V
    .locals 1
    .param p3, "gym"

    if-eqz p3, :check_saved
    invoke-static {p1}, Lprivacy/AndroidLocation;->disabled(Landroid/content/Context;)V
    return-void

:check_saved
    invoke-static {p3}, Lprivacy/SavedGyms;->resolveLocation(Lorg/json/JSONObject;)Lorg/json/JSONObject;
    move-result-object v0
    if-eqz v0, :using_saved_gym
    invoke-static {p1}, Lprivacy/AndroidLocation;->disabled(Landroid/content/Context;)V
    return-void

:using_saved_gym
    return-void
.end method

.method public static disabled(Landroid/content/Context;)V
    .locals 3
    instance-of v0, p0, Lcom/innovatise/utils/w;
    if-eqz v0, :done
    check-cast p0, Lcom/innovatise/utils/w;
    invoke-virtual {p0}, Lcom/innovatise/utils/w;->D()V
    new-instance v0, Landroid/app/AlertDialog$Builder;
    invoke-direct {v0, p0}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V
    const-string v1, "Location is disabled"
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    const-string v1, "This operation requested device location. Location collection is disabled, so it cannot continue. No check-in success has been confirmed."
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    const-string v1, "Close"
    const/4 v2, 0x0
    invoke-virtual {v0, v1, v2}, Landroid/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    invoke-virtual {v0}, Landroid/app/AlertDialog$Builder;->show()Landroid/app/AlertDialog;
:done
    return-void
.end method

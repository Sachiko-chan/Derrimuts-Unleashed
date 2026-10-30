.class public final Lprivacy/AndroidLocation;
.super Ljava/lang/Object;

# Consume venue metadata for the current saved-gym attempt.
.method public static request(Landroid/support/v4/media/session/r;Landroid/content/Context;Lcom/google/android/material/datepicker/i;)V
    .locals 1
    invoke-static {p1}, Lprivacy/SavedGyms;->takeLocation(Landroid/content/Context;)Lorg/json/JSONObject;
    move-result-object v0
    invoke-static {p0, p1, p2, v0}, Lprivacy/AndroidLocation;->request(Landroid/support/v4/media/session/r;Landroid/content/Context;Lcom/google/android/material/datepicker/i;Lorg/json/JSONObject;)V
    return-void
.end method

# An explicit Saved Gyms selection supplies venue coordinates; never access Android GPS.
.method public static request(Landroid/support/v4/media/session/r;Landroid/content/Context;Lcom/google/android/material/datepicker/i;Lorg/json/JSONObject;)V
    .locals 6
    .param p3, "gym"
    invoke-static {p3}, Lprivacy/SavedGyms;->resolveLocation(Lorg/json/JSONObject;)Lorg/json/JSONObject;
    move-result-object v0
    if-eqz v0, :disabled
    const-string v1, "lat"
    invoke-virtual {v0, v1}, Lorg/json/JSONObject;->getDouble(Ljava/lang/String;)D
    move-result-wide v2
    const-string v1, "lon"
    invoke-virtual {v0, v1}, Lorg/json/JSONObject;->getDouble(Ljava/lang/String;)D
    move-result-wide v4
    new-instance v1, Lac/h;
    invoke-direct {v1, v2, v3, v4, v5}, Lac/h;-><init>(DD)V
    invoke-static {p1}, Lprivacy/CheckInTrace;->savedLocation(Landroid/content/Context;)V
    invoke-virtual {p2, v1}, Lcom/google/android/material/datepicker/i;->a(Lac/h;)V
    return-void
    :disabled
    invoke-static {p1}, Lprivacy/AndroidLocation;->disabled(Landroid/content/Context;)V
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
    const-string v1, "Unable to check in"
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    const-string v1, "Location details are unavailable for this check-in. Please contact gym staff for assistance."
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    const-string v1, "Close"
    const/4 v2, 0x0
    invoke-virtual {v0, v1, v2}, Landroid/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    invoke-virtual {v0}, Landroid/app/AlertDialog$Builder;->show()Landroid/app/AlertDialog;
    :done
    return-void
.end method

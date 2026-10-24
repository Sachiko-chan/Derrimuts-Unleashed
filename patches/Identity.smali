.class public final Lprivacy/Identity;
.super Ljava/lang/Object;

# A random, persisted installation identity. Never imports an old hardware ID.
.method public static declared-synchronized get(Landroid/content/Context;)Ljava/util/UUID;
    .locals 4
    const-class v0, Lprivacy/Identity;
    monitor-enter v0
    :try_start
    const-string v1, "privacy_identity_v1"
    const/4 v2, 0x0
    invoke-virtual {p0, v1, v2}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
    move-result-object v1
    const-string v2, "uuid"
    const/4 v3, 0x0
    invoke-interface {v1, v2, v3}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v3
    if-eqz v3, :create
    :parse_start
    invoke-static {v3}, Ljava/util/UUID;->fromString(Ljava/lang/String;)Ljava/util/UUID;
    move-result-object p0
    :parse_end
    .catch Ljava/lang/IllegalArgumentException; {:parse_start .. :parse_end} :invalid
    goto :done
    :invalid
    move-exception v3
    :create
    invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;
    move-result-object p0
    invoke-interface {v1}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;
    move-result-object v1
    invoke-virtual {p0}, Ljava/util/UUID;->toString()Ljava/lang/String;
    move-result-object v3
    invoke-interface {v1, v2, v3}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;
    invoke-interface {v1}, Landroid/content/SharedPreferences$Editor;->commit()Z
    :done
    monitor-exit v0
    :try_end
    .catchall {:try_start .. :try_end} :failed
    return-object p0
    :failed
    move-exception p0
    monitor-exit v0
    throw p0
.end method

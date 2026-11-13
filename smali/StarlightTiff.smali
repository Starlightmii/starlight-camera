.class public final Lcom/starlight/camera/StarlightTiff;
.super Ljava/lang/Object;
.source "StarlightTiff.java"


# direct methods
.method private constructor <init>()V
    .locals 0

    .line 7
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static orientation(Ljava/lang/String;)I
    .locals 12

    .line 10
    const/4 v0, 0x0

    if-nez p0, :cond_0

    return v0

    .line 11
    :cond_0
    nop

    .line 13
    const/4 v1, 0x0

    :try_start_0
    new-instance v2, Ljava/io/RandomAccessFile;

    const-string v3, "r"

    invoke-direct {v2, p0, v3}, Ljava/io/RandomAccessFile;-><init>(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_8

    .line 14
    :try_start_1
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->length()J

    move-result-wide v3
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_7

    const-wide/16 v5, 0x8

    cmp-long p0, v3, v5

    if-gez p0, :cond_1

    .line 40
    :try_start_2
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->close()V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    goto :goto_0

    :catchall_0
    move-exception p0

    .line 14
    :goto_0
    return v0

    .line 15
    :cond_1
    :try_start_3
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->read()I

    move-result p0

    .line 16
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->read()I

    move-result v1

    .line 18
    const/16 v3, 0x49

    if-ne p0, v3, :cond_2

    if-ne v1, v3, :cond_2

    const/4 p0, 0x1

    goto :goto_1

    .line 19
    :cond_2
    const/16 v3, 0x4d

    if-ne p0, v3, :cond_a

    if-ne v1, v3, :cond_a

    move p0, v0

    .line 21
    :goto_1
    invoke-static {v2, p0}, Lcom/starlight/camera/StarlightTiff;->readU16(Ljava/io/RandomAccessFile;Z)I

    move-result v1
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_7

    const/16 v3, 0x2a

    if-eq v1, v3, :cond_3

    .line 40
    :try_start_4
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->close()V
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_1

    goto :goto_2

    :catchall_1
    move-exception p0

    .line 21
    :goto_2
    return v0

    .line 22
    :cond_3
    :try_start_5
    invoke-static {v2, p0}, Lcom/starlight/camera/StarlightTiff;->readU32(Ljava/io/RandomAccessFile;Z)J

    move-result-wide v3

    .line 23
    const-wide/16 v5, 0x0

    cmp-long v1, v3, v5

    if-lez v1, :cond_9

    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->length()J

    move-result-wide v5

    cmp-long v1, v3, v5

    if-ltz v1, :cond_4

    goto :goto_7

    .line 24
    :cond_4
    invoke-virtual {v2, v3, v4}, Ljava/io/RandomAccessFile;->seek(J)V

    .line 25
    invoke-static {v2, p0}, Lcom/starlight/camera/StarlightTiff;->readU16(Ljava/io/RandomAccessFile;Z)I

    move-result v1

    .line 26
    move v5, v0

    :goto_3
    if-ge v5, v1, :cond_8

    .line 27
    const-wide/16 v6, 0x2

    add-long/2addr v6, v3

    int-to-long v8, v5

    const-wide/16 v10, 0xc

    mul-long/2addr v8, v10

    add-long/2addr v6, v8

    .line 28
    add-long/2addr v10, v6

    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->length()J

    move-result-wide v8

    cmp-long v8, v10, v8

    if-lez v8, :cond_5

    goto :goto_6

    .line 29
    :cond_5
    invoke-virtual {v2, v6, v7}, Ljava/io/RandomAccessFile;->seek(J)V

    .line 30
    invoke-static {v2, p0}, Lcom/starlight/camera/StarlightTiff;->readU16(Ljava/io/RandomAccessFile;Z)I

    move-result v6

    .line 31
    invoke-static {v2, p0}, Lcom/starlight/camera/StarlightTiff;->readU16(Ljava/io/RandomAccessFile;Z)I

    move-result v7

    .line 32
    invoke-static {v2, p0}, Lcom/starlight/camera/StarlightTiff;->readU32(Ljava/io/RandomAccessFile;Z)J

    move-result-wide v8

    .line 33
    const/16 v10, 0x112

    if-ne v6, v10, :cond_7

    const-wide/16 v10, 0x1

    cmp-long v6, v8, v10

    if-ltz v6, :cond_7

    .line 34
    const/4 v6, 0x3

    if-ne v7, v6, :cond_6

    invoke-static {v2, p0}, Lcom/starlight/camera/StarlightTiff;->readU16(Ljava/io/RandomAccessFile;Z)I

    move-result p0
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_7

    .line 40
    :try_start_6
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->close()V
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_2

    goto :goto_4

    :catchall_2
    move-exception v0

    .line 34
    :goto_4
    return p0

    .line 35
    :cond_6
    const/4 v6, 0x4

    if-ne v7, v6, :cond_7

    :try_start_7
    invoke-static {v2, p0}, Lcom/starlight/camera/StarlightTiff;->readU32(Ljava/io/RandomAccessFile;Z)J

    move-result-wide v0
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_7

    long-to-int p0, v0

    .line 40
    :try_start_8
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->close()V
    :try_end_8
    .catchall {:try_start_8 .. :try_end_8} :catchall_3

    goto :goto_5

    :catchall_3
    move-exception v0

    .line 35
    :goto_5
    return p0

    .line 26
    :cond_7
    add-int/lit8 v5, v5, 0x1

    goto :goto_3

    .line 40
    :cond_8
    :goto_6
    :try_start_9
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->close()V
    :try_end_9
    .catchall {:try_start_9 .. :try_end_9} :catchall_4

    goto :goto_b

    :catchall_4
    move-exception p0

    goto :goto_b

    :cond_9
    :goto_7
    :try_start_a
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->close()V
    :try_end_a
    .catchall {:try_start_a .. :try_end_a} :catchall_5

    goto :goto_8

    :catchall_5
    move-exception p0

    .line 23
    :goto_8
    return v0

    .line 20
    :cond_a
    nop

    .line 40
    :try_start_b
    invoke-virtual {v2}, Ljava/io/RandomAccessFile;->close()V
    :try_end_b
    .catchall {:try_start_b .. :try_end_b} :catchall_6

    goto :goto_9

    :catchall_6
    move-exception p0

    .line 20
    :goto_9
    return v0

    .line 38
    :catchall_7
    move-exception p0

    move-object v1, v2

    goto :goto_a

    :catchall_8
    move-exception p0

    .line 40
    :goto_a
    if-eqz v1, :cond_b

    :try_start_c
    invoke-virtual {v1}, Ljava/io/RandomAccessFile;->close()V
    :try_end_c
    .catchall {:try_start_c .. :try_end_c} :catchall_4

    .line 42
    :cond_b
    :goto_b
    return v0
.end method

.method private static readU16(Ljava/io/RandomAccessFile;Z)I
    .locals 1
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .line 46
    invoke-virtual {p0}, Ljava/io/RandomAccessFile;->read()I

    move-result v0

    .line 47
    invoke-virtual {p0}, Ljava/io/RandomAccessFile;->read()I

    move-result p0

    .line 48
    if-ltz v0, :cond_2

    if-gez p0, :cond_0

    goto :goto_1

    .line 49
    :cond_0
    if-eqz p1, :cond_1

    shl-int/lit8 p0, p0, 0x8

    or-int/2addr p0, v0

    goto :goto_0

    :cond_1
    shl-int/lit8 p1, v0, 0x8

    or-int/2addr p0, p1

    :goto_0
    return p0

    .line 48
    :cond_2
    :goto_1
    const/4 p0, 0x0

    return p0
.end method

.method private static readU32(Ljava/io/RandomAccessFile;Z)J
    .locals 8
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .line 53
    invoke-virtual {p0}, Ljava/io/RandomAccessFile;->read()I

    move-result v0

    .line 54
    invoke-virtual {p0}, Ljava/io/RandomAccessFile;->read()I

    move-result v1

    .line 55
    invoke-virtual {p0}, Ljava/io/RandomAccessFile;->read()I

    move-result v2

    .line 56
    invoke-virtual {p0}, Ljava/io/RandomAccessFile;->read()I

    move-result p0

    .line 57
    or-int v3, v0, v1

    or-int/2addr v3, v2

    or-int/2addr v3, p0

    if-gez v3, :cond_0

    const-wide/16 p0, 0x0

    return-wide p0

    .line 58
    :cond_0
    const/16 v3, 0x8

    const/16 v4, 0x10

    const/16 v5, 0x18

    if-eqz p1, :cond_1

    .line 59
    int-to-long v6, v0

    int-to-long v0, v1

    shl-long/2addr v0, v3

    or-long/2addr v0, v6

    int-to-long v2, v2

    shl-long/2addr v2, v4

    or-long/2addr v0, v2

    int-to-long p0, p0

    shl-long/2addr p0, v5

    or-long/2addr p0, v0

    goto :goto_0

    .line 60
    :cond_1
    int-to-long v6, v0

    shl-long v5, v6, v5

    int-to-long v0, v1

    shl-long/2addr v0, v4

    or-long/2addr v0, v5

    int-to-long v4, v2

    shl-long v2, v4, v3

    or-long/2addr v0, v2

    int-to-long p0, p0

    or-long/2addr p0, v0

    .line 61
    :goto_0
    const-wide v0, 0xffffffffL

    and-long/2addr p0, v0

    return-wide p0
.end method

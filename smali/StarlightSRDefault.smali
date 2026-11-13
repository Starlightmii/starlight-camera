.class public final Lcom/starlight/camera/StarlightSRDefault;
.super Ljava/lang/Object;
.source "StarlightSRDefault.java"


# static fields
.field public static final DEFAULT_QUALITY:I = 0x64

.field public static final TAG:Ljava/lang/String; = "StarlightSRDefault"


# direct methods
.method private constructor <init>()V
    .locals 0

    .line 38
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 39
    return-void
.end method

.method private static clamp(I)I
    .locals 1

    .line 259
    if-gez p0, :cond_0

    const/4 p0, 0x0

    goto :goto_0

    :cond_0
    const/16 v0, 0xff

    if-le p0, v0, :cond_1

    move p0, v0

    :cond_1
    :goto_0
    return p0
.end method

.method private static detailEnhance(Landroid/graphics/Bitmap;FI)V
    .locals 28

    .line 210
    move/from16 v0, p2

    invoke-virtual/range {p0 .. p0}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v9

    .line 211
    invoke-virtual/range {p0 .. p0}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v10

    .line 212
    const/4 v1, 0x3

    if-lt v9, v1, :cond_b

    if-ge v10, v1, :cond_0

    goto/16 :goto_9

    .line 216
    :cond_0
    mul-int/lit8 v1, v9, 0x62

    new-array v11, v1, [I

    .line 217
    mul-int/lit8 v1, v9, 0x60

    new-array v12, v1, [I

    .line 218
    const/4 v13, 0x0

    move v14, v13

    :goto_0
    if-ge v14, v10, :cond_a

    .line 219
    sub-int v1, v10, v14

    const/16 v2, 0x60

    invoke-static {v2, v1}, Ljava/lang/Math;->min(II)I

    move-result v15

    .line 220
    add-int/lit8 v1, v14, -0x1

    invoke-static {v13, v1}, Ljava/lang/Math;->max(II)I

    move-result v16

    .line 221
    add-int v1, v14, v15

    add-int/lit8 v1, v1, 0x1

    invoke-static {v10, v1}, Ljava/lang/Math;->min(II)I

    move-result v1

    .line 222
    sub-int v8, v1, v16

    .line 223
    const/4 v3, 0x0

    const/4 v5, 0x0

    move-object/from16 v1, p0

    move-object v2, v11

    move v4, v9

    move/from16 v6, v16

    move v7, v9

    move/from16 v17, v8

    invoke-virtual/range {v1 .. v8}, Landroid/graphics/Bitmap;->getPixels([IIIIIII)V

    .line 224
    move v1, v13

    :goto_1
    if-ge v1, v15, :cond_9

    .line 225
    add-int v2, v14, v1

    sub-int v2, v2, v16

    .line 226
    mul-int v3, v1, v9

    .line 227
    mul-int v4, v2, v9

    .line 228
    if-lez v2, :cond_1

    add-int/lit8 v5, v2, -0x1

    goto :goto_2

    :cond_1
    move v5, v13

    :goto_2
    mul-int/2addr v5, v9

    .line 229
    add-int/lit8 v2, v2, 0x1

    move/from16 v6, v17

    if-ge v2, v6, :cond_2

    goto :goto_3

    :cond_2
    add-int/lit8 v2, v6, -0x1

    :goto_3
    mul-int/2addr v2, v9

    .line 230
    move v7, v13

    :goto_4
    if-ge v7, v9, :cond_8

    .line 231
    add-int v8, v4, v7

    aget v8, v11, v8

    .line 232
    shr-int/lit8 v13, v8, 0x10

    and-int/lit16 v13, v13, 0xff

    .line 233
    move/from16 v18, v6

    shr-int/lit8 v6, v8, 0x8

    and-int/lit16 v6, v6, 0xff

    .line 234
    move/from16 v19, v10

    and-int/lit16 v10, v8, 0xff

    .line 235
    mul-int/lit8 v20, v13, 0x4d

    move/from16 v21, v15

    mul-int/lit16 v15, v6, 0x96

    add-int v20, v20, v15

    mul-int/lit8 v15, v10, 0x1d

    add-int v20, v20, v15

    shr-int/lit8 v15, v20, 0x8

    .line 236
    if-lez v7, :cond_3

    add-int/lit8 v20, v7, -0x1

    goto :goto_5

    :cond_3
    const/16 v20, 0x0

    .line 237
    :goto_5
    move/from16 v22, v14

    add-int/lit8 v14, v7, 0x1

    if-ge v14, v9, :cond_4

    move/from16 v23, v14

    goto :goto_6

    :cond_4
    add-int/lit8 v23, v9, -0x1

    .line 238
    :goto_6
    add-int v20, v4, v20

    move/from16 v24, v14

    aget v14, v11, v20

    move/from16 v20, v9

    shr-int/lit8 v9, v14, 0x10

    and-int/lit16 v9, v9, 0xff

    mul-int/lit8 v9, v9, 0x4d

    move/from16 v25, v1

    shr-int/lit8 v1, v14, 0x8

    and-int/lit16 v1, v1, 0xff

    mul-int/lit16 v1, v1, 0x96

    add-int/2addr v9, v1

    and-int/lit16 v1, v14, 0xff

    mul-int/lit8 v1, v1, 0x1d

    add-int/2addr v9, v1

    shr-int/lit8 v1, v9, 0x8

    .line 239
    add-int v23, v4, v23

    aget v9, v11, v23

    shr-int/lit8 v14, v9, 0x10

    and-int/lit16 v14, v14, 0xff

    mul-int/lit8 v14, v14, 0x4d

    move/from16 v23, v4

    shr-int/lit8 v4, v9, 0x8

    and-int/lit16 v4, v4, 0xff

    mul-int/lit16 v4, v4, 0x96

    add-int/2addr v14, v4

    and-int/lit16 v4, v9, 0xff

    mul-int/lit8 v4, v4, 0x1d

    add-int/2addr v14, v4

    shr-int/lit8 v4, v14, 0x8

    .line 240
    add-int v9, v5, v7

    aget v9, v11, v9

    shr-int/lit8 v14, v9, 0x10

    and-int/lit16 v14, v14, 0xff

    mul-int/lit8 v14, v14, 0x4d

    move/from16 v26, v5

    shr-int/lit8 v5, v9, 0x8

    and-int/lit16 v5, v5, 0xff

    mul-int/lit16 v5, v5, 0x96

    add-int/2addr v14, v5

    and-int/lit16 v5, v9, 0xff

    mul-int/lit8 v5, v5, 0x1d

    add-int/2addr v14, v5

    shr-int/lit8 v5, v14, 0x8

    .line 241
    add-int v9, v2, v7

    aget v9, v11, v9

    shr-int/lit8 v14, v9, 0x10

    and-int/lit16 v14, v14, 0xff

    mul-int/lit8 v14, v14, 0x4d

    move/from16 v27, v2

    shr-int/lit8 v2, v9, 0x8

    and-int/lit16 v2, v2, 0xff

    mul-int/lit16 v2, v2, 0x96

    add-int/2addr v14, v2

    and-int/lit16 v2, v9, 0xff

    mul-int/lit8 v2, v2, 0x1d

    add-int/2addr v14, v2

    shr-int/lit8 v2, v14, 0x8

    .line 242
    add-int/2addr v1, v4

    add-int/2addr v1, v5

    add-int/2addr v1, v2

    add-int/lit8 v1, v1, 0x2

    shr-int/lit8 v1, v1, 0x2

    .line 243
    sub-int/2addr v15, v1

    .line 244
    if-gez v15, :cond_5

    neg-int v1, v15

    goto :goto_7

    :cond_5
    move v1, v15

    .line 245
    :goto_7
    if-le v1, v0, :cond_7

    .line 246
    if-lez v15, :cond_6

    move v1, v0

    goto :goto_8

    :cond_6
    neg-int v1, v0

    :goto_8
    sub-int/2addr v15, v1

    int-to-float v1, v15

    mul-float v1, v1, p1

    float-to-int v1, v1

    .line 247
    add-int/2addr v13, v1

    invoke-static {v13}, Lcom/starlight/camera/StarlightSRDefault;->clamp(I)I

    move-result v13

    .line 248
    add-int/2addr v6, v1

    invoke-static {v6}, Lcom/starlight/camera/StarlightSRDefault;->clamp(I)I

    move-result v6

    .line 249
    add-int/2addr v10, v1

    invoke-static {v10}, Lcom/starlight/camera/StarlightSRDefault;->clamp(I)I

    move-result v10

    .line 251
    :cond_7
    add-int/2addr v7, v3

    const/high16 v1, -0x1000000

    and-int/2addr v1, v8

    shl-int/lit8 v2, v13, 0x10

    or-int/2addr v1, v2

    shl-int/lit8 v2, v6, 0x8

    or-int/2addr v1, v2

    or-int/2addr v1, v10

    aput v1, v12, v7

    .line 230
    move/from16 v6, v18

    move/from16 v10, v19

    move/from16 v9, v20

    move/from16 v15, v21

    move/from16 v14, v22

    move/from16 v4, v23

    move/from16 v7, v24

    move/from16 v1, v25

    move/from16 v5, v26

    move/from16 v2, v27

    const/4 v13, 0x0

    goto/16 :goto_4

    .line 224
    :cond_8
    move/from16 v25, v1

    move/from16 v18, v6

    move/from16 v20, v9

    move/from16 v19, v10

    move/from16 v22, v14

    move/from16 v21, v15

    add-int/lit8 v1, v25, 0x1

    move/from16 v17, v18

    const/4 v13, 0x0

    goto/16 :goto_1

    .line 254
    :cond_9
    move/from16 v20, v9

    move/from16 v19, v10

    move/from16 v22, v14

    move/from16 v21, v15

    const/4 v3, 0x0

    const/4 v5, 0x0

    move-object/from16 v1, p0

    move-object v2, v12

    move/from16 v4, v20

    move/from16 v6, v22

    move/from16 v7, v20

    move/from16 v8, v21

    invoke-virtual/range {v1 .. v8}, Landroid/graphics/Bitmap;->setPixels([IIIIIII)V

    .line 218
    add-int/lit8 v14, v22, 0x60

    const/4 v13, 0x0

    goto/16 :goto_0

    .line 256
    :cond_a
    return-void

    .line 213
    :cond_b
    :goto_9
    return-void
.end method

.method private static nextOutput(Ljava/io/File;)Ljava/io/File;
    .locals 8

    .line 263
    invoke-virtual {p0}, Ljava/io/File;->getParentFile()Ljava/io/File;

    move-result-object v0

    .line 264
    invoke-virtual {p0}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object p0

    invoke-static {p0}, Lcom/starlight/camera/StarlightSRDefault;->stem(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    .line 265
    new-instance v1, Ljava/io/File;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "Upscaled_"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v4, ".jpg"

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v0, v2}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    .line 266
    const/4 v2, 0x1

    move v5, v2

    .line 267
    :goto_0
    invoke-virtual {v1}, Ljava/io/File;->exists()Z

    move-result v6

    if-eqz v6, :cond_0

    .line 268
    new-instance v1, Ljava/io/File;

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    const-string v7, "_"

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    invoke-direct {v1, v0, v6}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    .line 269
    add-int/2addr v5, v2

    goto :goto_0

    .line 271
    :cond_0
    return-object v1
.end method

.method private static orient(Ljava/io/File;Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;
    .locals 8

    .line 113
    nop

    .line 115
    const/4 v0, 0x1

    const/4 v7, 0x0

    :try_start_0
    new-instance v1, Landroid/media/ExifInterface;

    invoke-virtual {p0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p0

    move-object v7, p0

    invoke-direct {v1, p0}, Landroid/media/ExifInterface;-><init>(Ljava/lang/String;)V

    .line 116
    const-string p0, "Orientation"

    invoke-virtual {v1, p0, v0}, Landroid/media/ExifInterface;->getAttributeInt(Ljava/lang/String;I)I

    move-result p0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 118
    goto :goto_0

    .line 117
    :catchall_0
    move-exception p0

    const/4 p0, 0x0

    .line 119
    :goto_0
    if-lez p0, :do_tiff_fallback

    goto :after_tiff_fallback

    :do_tiff_fallback
    invoke-static {v7}, Lcom/starlight/camera/StarlightTiff;->orientation(Ljava/lang/String;)I

    move-result p0

    :after_tiff_fallback
    if-eq p0, v0, :cond_2

    if-nez p0, :cond_0

    goto :goto_2

    .line 122
    :cond_0
    new-instance v6, Landroid/graphics/Matrix;

    invoke-direct {v6}, Landroid/graphics/Matrix;-><init>()V

    .line 123
    const/high16 v0, 0x43870000    # 270.0f

    const/high16 v1, 0x42b40000    # 90.0f

    const/high16 v2, 0x3f800000    # 1.0f

    const/high16 v3, -0x40800000    # -1.0f

    packed-switch p0, :pswitch_data_0

    .line 148
    return-object p1

    .line 131
    :pswitch_0
    invoke-virtual {v6, v0}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 132
    goto :goto_1

    .line 144
    :pswitch_1
    invoke-virtual {v6, v0}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 145
    invoke-virtual {v6, v3, v2}, Landroid/graphics/Matrix;->postScale(FF)Z

    .line 146
    goto :goto_1

    .line 125
    :pswitch_2
    invoke-virtual {v6, v1}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 126
    goto :goto_1

    .line 140
    :pswitch_3
    invoke-virtual {v6, v1}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 141
    invoke-virtual {v6, v3, v2}, Landroid/graphics/Matrix;->postScale(FF)Z

    .line 142
    goto :goto_1

    .line 137
    :pswitch_4
    invoke-virtual {v6, v2, v3}, Landroid/graphics/Matrix;->postScale(FF)Z

    .line 138
    goto :goto_1

    .line 128
    :pswitch_5
    const/high16 p0, 0x43340000    # 180.0f

    invoke-virtual {v6, p0}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 129
    goto :goto_1

    .line 134
    :pswitch_6
    invoke-virtual {v6, v3, v2}, Landroid/graphics/Matrix;->postScale(FF)Z

    .line 135
    nop

    .line 151
    :goto_1
    const/4 v2, 0x0

    const/4 v3, 0x0

    :try_start_1
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v4

    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v5

    const/4 v7, 0x1

    move-object v1, p1

    invoke-static/range {v1 .. v7}, Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIIILandroid/graphics/Matrix;Z)Landroid/graphics/Bitmap;

    move-result-object p0

    .line 152
    if-eq p0, p1, :cond_1

    .line 153
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->recycle()V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    .line 155
    :cond_1
    return-object p0

    .line 156
    :catchall_1
    move-exception p0

    .line 157
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "orient failed: "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string v0, "StarlightSRDefault"

    invoke-static {v0, p0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 158
    return-object p1

    .line 120
    :cond_2
    :goto_2
    return-object p1

    nop

    :pswitch_data_0
    .packed-switch 0x2
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_3
        :pswitch_2
        :pswitch_1
        :pswitch_0
    .end packed-switch
.end method

.method private static paint(Z)Landroid/graphics/Paint;
    .locals 8

    .line 186
    new-instance v0, Landroid/graphics/Paint;

    const/4 v1, 0x7

    invoke-direct {v0, v1}, Landroid/graphics/Paint;-><init>(I)V

    .line 187
    if-eqz p0, :cond_0

    .line 188
    new-instance p0, Landroid/graphics/ColorMatrix;

    invoke-direct {p0}, Landroid/graphics/ColorMatrix;-><init>()V

    .line 189
    const v2, 0x3f933333    # 1.15f

    invoke-virtual {p0, v2}, Landroid/graphics/ColorMatrix;->setSaturation(F)V

    .line 191
    nop

    .line 192
    nop

    .line 193
    new-instance v3, Landroid/graphics/ColorMatrix;

    const/16 v4, 0x14

    new-array v4, v4, [F

    const/4 v5, 0x0

    aput v2, v4, v5

    const/4 v5, 0x1

    const/4 v6, 0x0

    aput v6, v4, v5

    const/4 v5, 0x2

    aput v6, v4, v5

    const/4 v5, 0x3

    aput v6, v4, v5

    const/4 v5, 0x4

    const v7, -0x3e670002

    aput v7, v4, v5

    const/4 v5, 0x5

    aput v6, v4, v5

    const/4 v5, 0x6

    aput v2, v4, v5

    aput v6, v4, v1

    const/16 v1, 0x8

    aput v6, v4, v1

    const/16 v1, 0x9

    aput v7, v4, v1

    const/16 v1, 0xa

    aput v6, v4, v1

    const/16 v1, 0xb

    aput v6, v4, v1

    const/16 v1, 0xc

    aput v2, v4, v1

    const/16 v1, 0xd

    aput v6, v4, v1

    const/16 v1, 0xe

    aput v7, v4, v1

    const/16 v1, 0xf

    aput v6, v4, v1

    const/16 v1, 0x10

    aput v6, v4, v1

    const/16 v1, 0x11

    aput v6, v4, v1

    const/16 v1, 0x12

    const/high16 v2, 0x3f800000    # 1.0f

    aput v2, v4, v1

    const/16 v1, 0x13

    aput v6, v4, v1

    invoke-direct {v3, v4}, Landroid/graphics/ColorMatrix;-><init>([F)V

    invoke-virtual {p0, v3}, Landroid/graphics/ColorMatrix;->postConcat(Landroid/graphics/ColorMatrix;)V

    .line 199
    new-instance v1, Landroid/graphics/ColorMatrixColorFilter;

    invoke-direct {v1, p0}, Landroid/graphics/ColorMatrixColorFilter;-><init>(Landroid/graphics/ColorMatrix;)V

    invoke-virtual {v0, v1}, Landroid/graphics/Paint;->setColorFilter(Landroid/graphics/ColorFilter;)Landroid/graphics/ColorFilter;

    .line 201
    :cond_0
    return-object v0
.end method

.method public static process(Landroid/content/Context;Ljava/io/File;IIJ)Z
    .locals 7
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Throwable;
        }
    .end annotation

    .line 42
    const/high16 v6, 0x40000000    # 2.0f

    move-object v0, p0

    move-object v1, p1

    move v2, p2

    move v3, p3

    move-wide v4, p4

    invoke-static/range {v0 .. v6}, Lcom/starlight/camera/StarlightSRDefault;->process(Landroid/content/Context;Ljava/io/File;IIJF)Z

    move-result p0

    return p0
.end method

.method public static process(Landroid/content/Context;Ljava/io/File;IIJF)Z
    .locals 17
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Throwable;
        }
    .end annotation

    .line 47
    move-object/from16 v0, p0

    move-object/from16 v1, p1

    move-wide/from16 v2, p4

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v4

    .line 48
    const/4 v6, 0x0

    if-eqz v0, :cond_e

    if-eqz v1, :cond_e

    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->exists()Z

    move-result v7

    if-eqz v7, :cond_e

    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->length()J

    move-result-wide v7

    const-wide/16 v9, 0x0

    cmp-long v7, v7, v9

    if-nez v7, :cond_0

    goto/16 :goto_5

    .line 51
    :cond_0
    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object v7

    .line 52
    const-string v8, "Upscaled_"

    invoke-virtual {v7, v8}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v8

    if-nez v8, :cond_d

    const-string v8, "_Upscaled"

    invoke-virtual {v7, v8}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v7

    if-eqz v7, :cond_1

    goto/16 :goto_4

    .line 56
    :cond_1
    new-instance v7, Landroid/graphics/BitmapFactory$Options;

    invoke-direct {v7}, Landroid/graphics/BitmapFactory$Options;-><init>()V

    .line 57
    const/4 v8, 0x1

    iput-boolean v8, v7, Landroid/graphics/BitmapFactory$Options;->inJustDecodeBounds:Z

    .line 58
    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v9

    invoke-static {v9, v7}, Landroid/graphics/BitmapFactory;->decodeFile(Ljava/lang/String;Landroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;

    .line 59
    iget v9, v7, Landroid/graphics/BitmapFactory$Options;->outWidth:I

    .line 60
    iget v7, v7, Landroid/graphics/BitmapFactory$Options;->outHeight:I

    .line 61
    if-lez v9, :cond_c

    if-gtz v7, :cond_2

    goto/16 :goto_3

    .line 64
    :cond_2
    int-to-long v10, v9

    int-to-long v12, v7

    mul-long/2addr v10, v12

    cmp-long v10, v10, v2

    const-string v11, "x"

    const-string v12, "StarlightSRDefault"

    if-lez v10, :cond_3

    .line 65
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "skip huge source "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v12, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 66
    return v6

    .line 69
    :cond_3
    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v7

    invoke-static {v7}, Landroid/graphics/BitmapFactory;->decodeFile(Ljava/lang/String;)Landroid/graphics/Bitmap;

    move-result-object v7

    .line 70
    if-nez v7, :cond_4

    .line 71
    return v6

    .line 74
    :cond_4
    :try_start_0
    invoke-static {v1, v7}, Lcom/starlight/camera/StarlightSRDefault;->orient(Ljava/io/File;Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;

    move-result-object v7

    .line 75
    const/4 v9, 0x4

    move/from16 v10, p2

    invoke-static {v9, v10}, Ljava/lang/Math;->min(II)I

    move-result v9

    const/4 v10, 0x2

    invoke-static {v10, v9}, Ljava/lang/Math;->max(II)I

    move-result v9

    .line 76
    invoke-virtual {v7}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v10

    mul-int/2addr v10, v9

    .line 77
    invoke-virtual {v7}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v13

    mul-int/2addr v13, v9

    .line 78
    long-to-double v2, v2

    int-to-double v9, v10

    int-to-double v13, v13

    mul-double v15, v9, v13

    div-double/2addr v2, v15

    invoke-static {v2, v3}, Ljava/lang/Math;->sqrt(D)D

    move-result-wide v2

    move-wide/from16 p4, v9

    const-wide/high16 v8, 0x3ff0000000000000L    # 1.0

    invoke-static {v8, v9, v2, v3}, Ljava/lang/Math;->min(DD)D

    move-result-wide v2

    .line 79
    invoke-virtual {v7}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v8

    move-wide/from16 v9, p4

    mul-double/2addr v9, v2

    invoke-static {v9, v10}, Ljava/lang/Math;->round(D)J

    move-result-wide v9

    long-to-int v9, v9

    invoke-static {v8, v9}, Ljava/lang/Math;->max(II)I

    move-result v8

    .line 80
    invoke-virtual {v7}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v9

    mul-double/2addr v13, v2

    invoke-static {v13, v14}, Ljava/lang/Math;->round(D)J

    move-result-wide v2

    long-to-int v2, v2

    invoke-static {v9, v2}, Ljava/lang/Math;->max(II)I

    move-result v2

    .line 81
    invoke-virtual {v7}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v3

    if-le v8, v3, :cond_9

    invoke-virtual {v7}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v3

    if-gt v2, v3, :cond_5

    goto/16 :goto_1

    .line 86
    :cond_5
    invoke-static {v7, v8, v2}, Lcom/starlight/camera/StarlightSRDefault;->resample(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;

    move-result-object v3

    .line 87
    if-eq v3, v7, :cond_6

    .line 88
    invoke-virtual {v7}, Landroid/graphics/Bitmap;->recycle()V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    .line 90
    :cond_6
    nop

    .line 93
    const/4 v7, 0x3

    const/4 v9, 0x0

    move/from16 v10, p6

    :try_start_1
    invoke-static {v3, v10, v7}, Lcom/starlight/camera/StarlightSRDefault;->detailEnhance(Landroid/graphics/Bitmap;FI)V

    .line 95
    invoke-static/range {p0 .. p0}, Lcom/starlight/camera/StarlightPolicy;->photoFormat(Landroid/content/Context;)Ljava/lang/String;

    move-result-object v7

    .line 96
    nop

    .line 97
    if-lez p3, :cond_7

    move/from16 v10, p3

    goto :goto_0

    :cond_7
    const/16 v10, 0x64

    .line 96
    :goto_0
    invoke-static {v0, v3, v1, v7, v10}, Lcom/starlight/camera/StarlightOutput;->save(Landroid/content/Context;Landroid/graphics/Bitmap;Ljava/io/File;Ljava/lang/String;I)Ljava/io/File;

    move-result-object v0

    .line 98
    invoke-virtual {v3}, Landroid/graphics/Bitmap;->recycle()V

    .line 99
    if-nez v0, :cond_8

    .line 100
    nop

    .line 106
    nop

    .line 100
    return v6

    .line 102
    :cond_8
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "done "

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    const-string v3, " "

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v8}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    const-string v2, " bytes="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v0}, Ljava/io/File;->length()J

    move-result-wide v2

    invoke-virtual {v1, v2, v3}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, " fmt="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, " ms="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    .line 103
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v1

    sub-long/2addr v1, v4

    invoke-virtual {v0, v1, v2}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    .line 102
    invoke-static {v12, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    .line 104
    nop

    .line 106
    nop

    .line 104
    const/4 v0, 0x1

    return v0

    .line 106
    :catchall_0
    move-exception v0

    move-object v7, v9

    goto :goto_2

    .line 82
    :cond_9
    :goto_1
    :try_start_2
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "skip, source already at cap "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v7}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v7}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v12, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    .line 83
    nop

    .line 106
    if-eqz v7, :cond_a

    .line 107
    invoke-virtual {v7}, Landroid/graphics/Bitmap;->recycle()V

    .line 83
    :cond_a
    return v6

    .line 106
    :catchall_1
    move-exception v0

    :goto_2
    if-eqz v7, :cond_b

    .line 107
    invoke-virtual {v7}, Landroid/graphics/Bitmap;->recycle()V

    .line 109
    :cond_b
    throw v0

    .line 62
    :cond_c
    :goto_3
    return v6

    .line 53
    :cond_d
    :goto_4
    return v6

    .line 49
    :cond_e
    :goto_5
    return v6
.end method

.method private static resample(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;
    .locals 8

    .line 163
    move-object v0, p0

    .line 165
    :goto_0
    invoke-virtual {v0}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v1

    mul-int/lit8 v1, v1, 0x2

    const/4 v2, 0x0

    const/4 v3, 0x0

    if-gt v1, p1, :cond_1

    invoke-virtual {v0}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v1

    mul-int/lit8 v1, v1, 0x2

    if-gt v1, p2, :cond_1

    .line 166
    invoke-virtual {v0}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v1

    mul-int/lit8 v1, v1, 0x2

    .line 167
    invoke-virtual {v0}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v4

    mul-int/lit8 v4, v4, 0x2

    .line 168
    sget-object v5, Landroid/graphics/Bitmap$Config;->ARGB_8888:Landroid/graphics/Bitmap$Config;

    invoke-static {v1, v4, v5}, Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;

    move-result-object v5

    .line 169
    new-instance v6, Landroid/graphics/Canvas;

    invoke-direct {v6, v5}, Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V

    .line 170
    new-instance v7, Landroid/graphics/Rect;

    invoke-direct {v7, v3, v3, v1, v4}, Landroid/graphics/Rect;-><init>(IIII)V

    invoke-static {v3}, Lcom/starlight/camera/StarlightSRDefault;->paint(Z)Landroid/graphics/Paint;

    move-result-object v1

    invoke-virtual {v6, v0, v2, v7, v1}, Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;Landroid/graphics/Rect;Landroid/graphics/Rect;Landroid/graphics/Paint;)V

    .line 171
    if-eq v0, p0, :cond_0

    .line 172
    invoke-virtual {v0}, Landroid/graphics/Bitmap;->recycle()V

    .line 174
    :cond_0
    nop

    .line 175
    move-object v0, v5

    goto :goto_0

    .line 176
    :cond_1
    sget-object v1, Landroid/graphics/Bitmap$Config;->ARGB_8888:Landroid/graphics/Bitmap$Config;

    invoke-static {p1, p2, v1}, Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;

    move-result-object v1

    .line 177
    new-instance v4, Landroid/graphics/Canvas;

    invoke-direct {v4, v1}, Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V

    .line 178
    new-instance v5, Landroid/graphics/Rect;

    invoke-direct {v5, v3, v3, p1, p2}, Landroid/graphics/Rect;-><init>(IIII)V

    const/4 p1, 0x1

    invoke-static {p1}, Lcom/starlight/camera/StarlightSRDefault;->paint(Z)Landroid/graphics/Paint;

    move-result-object p1

    invoke-virtual {v4, v0, v2, v5, p1}, Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;Landroid/graphics/Rect;Landroid/graphics/Rect;Landroid/graphics/Paint;)V

    .line 179
    if-eq v0, p0, :cond_2

    .line 180
    invoke-virtual {v0}, Landroid/graphics/Bitmap;->recycle()V

    .line 182
    :cond_2
    return-object v1
.end method

.method private static stem(Ljava/lang/String;)Ljava/lang/String;
    .locals 2

    .line 275
    const/16 v0, 0x2e

    invoke-virtual {p0, v0}, Ljava/lang/String;->lastIndexOf(I)I

    move-result v0

    .line 276
    if-lez v0, :cond_0

    .line 277
    const/4 v1, 0x0

    invoke-virtual {p0, v1, v0}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object p0

    .line 279
    :cond_0
    const-string v0, "_Starlight"

    const-string v1, ""

    invoke-virtual {p0, v0, v1}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object p0

    const-string v0, "_Upscaled"

    invoke-virtual {p0, v0, v1}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object p0

    .line 280
    const-string v0, "DSC_"

    invoke-virtual {p0, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v0

    if-nez v0, :cond_2

    const-string v0, "IMG_"

    invoke-virtual {p0, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v0

    if-nez v0, :cond_2

    const-string v0, "VID_"

    invoke-virtual {p0, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v0

    if-nez v0, :cond_2

    const-string v0, "TIMELAPSE_"

    invoke-virtual {p0, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_1

    goto :goto_0

    .line 283
    :cond_1
    return-object p0

    .line 281
    :cond_2
    :goto_0
    const/16 v0, 0x5f

    invoke-virtual {p0, v0}, Ljava/lang/String;->indexOf(I)I

    move-result v0

    add-int/lit8 v0, v0, 0x1

    invoke-virtual {p0, v0}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method

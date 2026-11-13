.class public final Lcom/starlight/camera/StarlightSR;
.super Ljava/lang/Object;
.source "StarlightSR.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/starlight/camera/StarlightSR$RowTask;,
        Lcom/starlight/camera/StarlightSR$FastResizer;
    }
.end annotation


# static fields
.field public static final DEFAULT_QUALITY:I = 0x64

.field private static final LOBE:D = 3.0

.field public static final TAG:Ljava/lang/String; = "StarlightSR"

.field private static final TAPS:I = 0x6


# direct methods
.method private constructor <init>()V
    .locals 0

    .line 46
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 47
    return-void
.end method

.method static synthetic access$000(I)I
    .locals 0

    .line 40
    invoke-static {p0}, Lcom/starlight/camera/StarlightSR;->clamp(I)I

    move-result p0

    return p0
.end method

.method private static applyColorMatrix(Landroid/graphics/Bitmap;)V
    .locals 15

    .line 487
    invoke-virtual {p0}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v8

    .line 488
    invoke-virtual {p0}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v9

    .line 490
    mul-int/lit16 v0, v8, 0x100

    new-array v10, v0, [I

    .line 493
    const/4 v11, 0x0

    move v12, v11

    :goto_0
    if-ge v12, v9, :cond_1

    .line 494
    sub-int v0, v9, v12

    const/16 v1, 0x100

    invoke-static {v1, v0}, Ljava/lang/Math;->min(II)I

    move-result v13

    .line 495
    const/4 v2, 0x0

    const/4 v4, 0x0

    move-object v0, p0

    move-object v1, v10

    move v3, v8

    move v5, v12

    move v6, v8

    move v7, v13

    invoke-virtual/range {v0 .. v7}, Landroid/graphics/Bitmap;->getPixels([IIIIIII)V

    .line 496
    mul-int v0, v8, v13

    .line 497
    move v1, v11

    :goto_1
    if-ge v1, v0, :cond_0

    .line 498
    aget v2, v10, v1

    .line 499
    shr-int/lit8 v3, v2, 0x10

    and-int/lit16 v3, v3, 0xff

    .line 500
    shr-int/lit8 v4, v2, 0x8

    and-int/lit16 v4, v4, 0xff

    .line 501
    and-int/lit16 v5, v2, 0xff

    .line 502
    int-to-float v3, v3

    const v6, 0x3e5a1cac    # 0.213f

    mul-float/2addr v6, v3

    int-to-float v4, v4

    const v7, 0x3f370a3d    # 0.715f

    mul-float/2addr v7, v4

    add-float/2addr v6, v7

    int-to-float v5, v5

    const v7, 0x3d9374bc    # 0.072f

    mul-float/2addr v7, v5

    add-float/2addr v6, v7

    .line 503
    sub-float/2addr v3, v6

    const v7, 0x3f933333    # 1.15f

    mul-float/2addr v3, v7

    add-float/2addr v3, v6

    .line 504
    sub-float/2addr v4, v6

    mul-float/2addr v4, v7

    add-float/2addr v4, v6

    .line 505
    sub-float/2addr v5, v6

    mul-float/2addr v5, v7

    add-float/2addr v6, v5

    .line 506
    mul-float/2addr v3, v7

    const v5, -0x3e670002

    add-float/2addr v3, v5

    const/high16 v14, 0x3f000000    # 0.5f

    add-float/2addr v3, v14

    float-to-int v3, v3

    invoke-static {v3}, Lcom/starlight/camera/StarlightSR;->clamp(I)I

    move-result v3

    .line 507
    mul-float/2addr v4, v7

    add-float/2addr v4, v5

    add-float/2addr v4, v14

    float-to-int v4, v4

    invoke-static {v4}, Lcom/starlight/camera/StarlightSR;->clamp(I)I

    move-result v4

    .line 508
    mul-float/2addr v6, v7

    add-float/2addr v6, v5

    add-float/2addr v6, v14

    float-to-int v5, v6

    invoke-static {v5}, Lcom/starlight/camera/StarlightSR;->clamp(I)I

    move-result v5

    .line 509
    const/high16 v6, -0x1000000

    and-int/2addr v2, v6

    shl-int/lit8 v3, v3, 0x10

    or-int/2addr v2, v3

    shl-int/lit8 v3, v4, 0x8

    or-int/2addr v2, v3

    or-int/2addr v2, v5

    aput v2, v10, v1

    .line 497
    add-int/lit8 v1, v1, 0x1

    goto :goto_1

    .line 511
    :cond_0
    const/4 v2, 0x0

    const/4 v4, 0x0

    move-object v0, p0

    move-object v1, v10

    move v3, v8

    move v5, v12

    move v6, v8

    move v7, v13

    invoke-virtual/range {v0 .. v7}, Landroid/graphics/Bitmap;->setPixels([IIIIIII)V

    .line 493
    add-int/lit16 v12, v12, 0x100

    goto :goto_0

    .line 513
    :cond_1
    return-void
.end method

.method private static buildWeights(II[I[F)V
    .locals 18

    .line 453
    move/from16 v0, p0

    move/from16 v1, p1

    int-to-double v2, v0

    int-to-double v4, v1

    div-double/2addr v2, v4

    .line 454
    const/4 v5, 0x0

    :goto_0
    if-ge v5, v1, :cond_4

    .line 455
    int-to-double v6, v5

    const-wide/high16 v8, 0x3fe0000000000000L    # 0.5

    add-double/2addr v6, v8

    mul-double/2addr v6, v2

    sub-double/2addr v6, v8

    .line 456
    invoke-static {v6, v7}, Ljava/lang/Math;->floor(D)D

    move-result-wide v8

    double-to-int v8, v8

    add-int/lit8 v8, v8, -0x2

    .line 457
    nop

    .line 458
    mul-int/lit8 v9, v5, 0x6

    .line 459
    const-wide/16 v10, 0x0

    move-wide v13, v10

    const/4 v12, 0x0

    :goto_1
    const/4 v15, 0x6

    if-ge v12, v15, :cond_2

    .line 460
    add-int v15, v8, v12

    .line 461
    move/from16 v16, v5

    int-to-double v4, v15

    sub-double v4, v6, v4

    .line 462
    invoke-static {v4, v5}, Lcom/starlight/camera/StarlightSR;->lanczos(D)D

    move-result-wide v4

    .line 463
    add-int v17, v9, v12

    if-gez v15, :cond_0

    const/4 v15, 0x0

    goto :goto_2

    :cond_0
    if-lt v15, v0, :cond_1

    add-int/lit8 v15, v0, -0x1

    :cond_1
    :goto_2
    aput v15, p2, v17

    .line 464
    double-to-float v15, v4

    aput v15, p3, v17

    .line 465
    add-double/2addr v13, v4

    .line 459
    add-int/lit8 v12, v12, 0x1

    move/from16 v5, v16

    goto :goto_1

    .line 467
    :cond_2
    move/from16 v16, v5

    cmpl-double v4, v13, v10

    if-eqz v4, :cond_3

    .line 468
    const/4 v4, 0x0

    :goto_3
    if-ge v4, v15, :cond_3

    .line 469
    add-int v5, v9, v4

    aget v6, p3, v5

    float-to-double v6, v6

    div-double/2addr v6, v13

    double-to-float v6, v6

    aput v6, p3, v5

    .line 468
    add-int/lit8 v4, v4, 0x1

    goto :goto_3

    .line 454
    :cond_3
    add-int/lit8 v5, v16, 0x1

    goto :goto_0

    .line 473
    :cond_4
    return-void
.end method

.method private static clamp(I)I
    .locals 1

    .line 654
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

.method static copyExif(Ljava/io/File;Ljava/io/File;)V
    .locals 6

    .line 566
    :try_start_0
    new-instance v0, Landroid/media/ExifInterface;

    invoke-virtual {p0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p0

    invoke-direct {v0, p0}, Landroid/media/ExifInterface;-><init>(Ljava/lang/String;)V

    .line 567
    new-instance p0, Landroid/media/ExifInterface;

    invoke-virtual {p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Landroid/media/ExifInterface;-><init>(Ljava/lang/String;)V

    .line 568
    const/16 p1, 0x14

    new-array v1, p1, [Ljava/lang/String;

    const-string v2, "Make"

    const/4 v3, 0x0

    aput-object v2, v1, v3

    const-string v2, "Model"

    const/4 v4, 0x1

    aput-object v2, v1, v4

    const-string v2, "DateTime"

    const/4 v5, 0x2

    aput-object v2, v1, v5

    const-string v2, "DateTimeOriginal"

    const/4 v5, 0x3

    aput-object v2, v1, v5

    const-string v2, "DateTimeDigitized"

    const/4 v5, 0x4

    aput-object v2, v1, v5

    const-string v2, "FNumber"

    const/4 v5, 0x5

    aput-object v2, v1, v5

    const-string v2, "ExposureTime"

    const/4 v5, 0x6

    aput-object v2, v1, v5

    const-string v2, "ISOSpeedRatings"

    const/4 v5, 0x7

    aput-object v2, v1, v5

    const-string v2, "FocalLength"

    const/16 v5, 0x8

    aput-object v2, v1, v5

    const-string v2, "Flash"

    const/16 v5, 0x9

    aput-object v2, v1, v5

    const-string v2, "WhiteBalance"

    const/16 v5, 0xa

    aput-object v2, v1, v5

    const-string v2, "Software"

    const/16 v5, 0xb

    aput-object v2, v1, v5

    const-string v2, "GPSLatitude"

    const/16 v5, 0xc

    aput-object v2, v1, v5

    const-string v2, "GPSLatitudeRef"

    const/16 v5, 0xd

    aput-object v2, v1, v5

    const-string v2, "GPSLongitude"

    const/16 v5, 0xe

    aput-object v2, v1, v5

    const-string v2, "GPSLongitudeRef"

    const/16 v5, 0xf

    aput-object v2, v1, v5

    const-string v2, "GPSAltitude"

    const/16 v5, 0x10

    aput-object v2, v1, v5

    const-string v2, "GPSAltitudeRef"

    const/16 v5, 0x11

    aput-object v2, v1, v5

    const-string v2, "GPSTimeStamp"

    const/16 v5, 0x12

    aput-object v2, v1, v5

    const-string v2, "GPSDateStamp"

    const/16 v5, 0x13

    aput-object v2, v1, v5

    .line 590
    nop

    :goto_0
    if-ge v3, p1, :cond_1

    aget-object v2, v1, v3

    .line 591
    invoke-virtual {v0, v2}, Landroid/media/ExifInterface;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 592
    if-eqz v5, :cond_0

    .line 593
    invoke-virtual {p0, v2, v5}, Landroid/media/ExifInterface;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 590
    :cond_0
    add-int/lit8 v3, v3, 0x1

    goto :goto_0

    .line 596
    :cond_1
    const-string p1, "Orientation"

    invoke-static {v4}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v0

    invoke-virtual {p0, p1, v0}, Landroid/media/ExifInterface;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 597
    invoke-virtual {p0}, Landroid/media/ExifInterface;->saveAttributes()V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 600
    goto :goto_1

    .line 598
    :catchall_0
    move-exception p0

    .line 599
    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v0, "exif copy failed: "

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p1

    invoke-virtual {p1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string p1, "StarlightSR"

    invoke-static {p1, p0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 601
    :goto_1
    return-void
.end method

.method private static detailEnhance(Landroid/graphics/Bitmap;FI)V
    .locals 28

    .line 516
    move/from16 v0, p2

    invoke-virtual/range {p0 .. p0}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v9

    .line 517
    invoke-virtual/range {p0 .. p0}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v10

    .line 518
    const/4 v1, 0x3

    if-lt v9, v1, :cond_b

    if-ge v10, v1, :cond_0

    goto/16 :goto_9

    .line 522
    :cond_0
    mul-int/lit8 v1, v9, 0x62

    new-array v11, v1, [I

    .line 523
    mul-int/lit8 v1, v9, 0x60

    new-array v12, v1, [I

    .line 524
    const/4 v13, 0x0

    move v14, v13

    :goto_0
    if-ge v14, v10, :cond_a

    .line 525
    sub-int v1, v10, v14

    const/16 v2, 0x60

    invoke-static {v2, v1}, Ljava/lang/Math;->min(II)I

    move-result v15

    .line 526
    add-int/lit8 v1, v14, -0x1

    invoke-static {v13, v1}, Ljava/lang/Math;->max(II)I

    move-result v16

    .line 527
    add-int v1, v14, v15

    add-int/lit8 v1, v1, 0x1

    invoke-static {v10, v1}, Ljava/lang/Math;->min(II)I

    move-result v1

    .line 528
    sub-int v8, v1, v16

    .line 529
    const/4 v3, 0x0

    const/4 v5, 0x0

    move-object/from16 v1, p0

    move-object v2, v11

    move v4, v9

    move/from16 v6, v16

    move v7, v9

    move/from16 v17, v8

    invoke-virtual/range {v1 .. v8}, Landroid/graphics/Bitmap;->getPixels([IIIIIII)V

    .line 530
    move v1, v13

    :goto_1
    if-ge v1, v15, :cond_9

    .line 531
    add-int v2, v14, v1

    sub-int v2, v2, v16

    .line 532
    mul-int v3, v1, v9

    .line 533
    mul-int v4, v2, v9

    .line 534
    if-lez v2, :cond_1

    add-int/lit8 v5, v2, -0x1

    goto :goto_2

    :cond_1
    move v5, v13

    :goto_2
    mul-int/2addr v5, v9

    .line 535
    add-int/lit8 v2, v2, 0x1

    move/from16 v6, v17

    if-ge v2, v6, :cond_2

    goto :goto_3

    :cond_2
    add-int/lit8 v2, v6, -0x1

    :goto_3
    mul-int/2addr v2, v9

    .line 536
    move v7, v13

    :goto_4
    if-ge v7, v9, :cond_8

    .line 537
    add-int v8, v4, v7

    aget v8, v11, v8

    .line 538
    shr-int/lit8 v13, v8, 0x10

    and-int/lit16 v13, v13, 0xff

    .line 539
    move/from16 v18, v6

    shr-int/lit8 v6, v8, 0x8

    and-int/lit16 v6, v6, 0xff

    .line 540
    move/from16 v19, v10

    and-int/lit16 v10, v8, 0xff

    .line 541
    mul-int/lit8 v20, v13, 0x4d

    move/from16 v21, v15

    mul-int/lit16 v15, v6, 0x96

    add-int v20, v20, v15

    mul-int/lit8 v15, v10, 0x1d

    add-int v20, v20, v15

    shr-int/lit8 v15, v20, 0x8

    .line 542
    if-lez v7, :cond_3

    add-int/lit8 v20, v7, -0x1

    goto :goto_5

    :cond_3
    const/16 v20, 0x0

    .line 543
    :goto_5
    move/from16 v22, v14

    add-int/lit8 v14, v7, 0x1

    if-ge v14, v9, :cond_4

    move/from16 v23, v14

    goto :goto_6

    :cond_4
    add-int/lit8 v23, v9, -0x1

    .line 544
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

    .line 545
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

    .line 546
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

    .line 547
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

    .line 548
    add-int/2addr v1, v4

    add-int/2addr v1, v5

    add-int/2addr v1, v2

    add-int/lit8 v1, v1, 0x2

    shr-int/lit8 v1, v1, 0x2

    .line 549
    sub-int/2addr v15, v1

    .line 550
    if-gez v15, :cond_5

    neg-int v1, v15

    goto :goto_7

    :cond_5
    move v1, v15

    .line 551
    :goto_7
    if-le v1, v0, :cond_7

    .line 552
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

    .line 553
    add-int/2addr v13, v1

    invoke-static {v13}, Lcom/starlight/camera/StarlightSR;->clamp(I)I

    move-result v13

    .line 554
    add-int/2addr v6, v1

    invoke-static {v6}, Lcom/starlight/camera/StarlightSR;->clamp(I)I

    move-result v6

    .line 555
    add-int/2addr v10, v1

    invoke-static {v10}, Lcom/starlight/camera/StarlightSR;->clamp(I)I

    move-result v10

    .line 557
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

    .line 536
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

    .line 530
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

    .line 560
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

    .line 524
    add-int/lit8 v14, v22, 0x60

    const/4 v13, 0x0

    goto/16 :goto_0

    .line 562
    :cond_a
    return-void

    .line 519
    :cond_b
    :goto_9
    return-void
.end method

.method public static hwAccel(Landroid/content/Context;)Z
    .locals 2

    .line 51
    const/4 v0, 0x1

    :try_start_0
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;

    move-result-object p0

    const-string v1, "pref_starlight_hw_accel_key"

    .line 52
    invoke-interface {p0, v1, v0}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result p0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 51
    return p0

    .line 53
    :catchall_0
    move-exception p0

    .line 54
    return v0
.end method

.method private static lanczos(D)D
    .locals 5

    .line 476
    const-wide/16 v0, 0x0

    cmpl-double v2, p0, v0

    if-nez v2, :cond_0

    .line 477
    const-wide/high16 p0, 0x3ff0000000000000L    # 1.0

    return-wide p0

    .line 479
    :cond_0
    const-wide/high16 v2, -0x3ff8000000000000L    # -3.0

    cmpg-double v2, p0, v2

    if-lez v2, :cond_2

    const-wide/high16 v2, 0x4008000000000000L    # 3.0

    cmpl-double v4, p0, v2

    if-ltz v4, :cond_1

    goto :goto_0

    .line 482
    :cond_1
    const-wide v0, 0x400921fb54442d18L    # Math.PI

    mul-double/2addr p0, v0

    .line 483
    invoke-static {p0, p1}, Ljava/lang/Math;->sin(D)D

    move-result-wide v0

    mul-double/2addr v0, v2

    div-double v2, p0, v2

    invoke-static {v2, v3}, Ljava/lang/Math;->sin(D)D

    move-result-wide v2

    mul-double/2addr v0, v2

    mul-double/2addr p0, p0

    div-double/2addr v0, p0

    return-wide v0

    .line 480
    :cond_2
    :goto_0
    return-wide v0
.end method

.method private static nextOutput(Ljava/io/File;)Ljava/io/File;
    .locals 8

    .line 658
    invoke-virtual {p0}, Ljava/io/File;->getParentFile()Ljava/io/File;

    move-result-object v0

    .line 659
    invoke-virtual {p0}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object p0

    invoke-static {p0}, Lcom/starlight/camera/StarlightSR;->stem(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    .line 660
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

    .line 661
    const/4 v2, 0x1

    move v5, v2

    .line 662
    :goto_0
    invoke-virtual {v1}, Ljava/io/File;->exists()Z

    move-result v6

    if-eqz v6, :cond_0

    .line 663
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

    .line 664
    add-int/2addr v5, v2

    goto :goto_0

    .line 666
    :cond_0
    return-object v1
.end method

.method private static orient(Ljava/io/File;Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;
    .locals 8

    .line 604
    nop

    .line 606
    const/4 v0, 0x1

    const/4 v7, 0x0

    :try_start_0
    new-instance v1, Landroid/media/ExifInterface;

    invoke-virtual {p0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p0

    move-object v7, p0

    invoke-direct {v1, p0}, Landroid/media/ExifInterface;-><init>(Ljava/lang/String;)V

    .line 607
    const-string p0, "Orientation"

    invoke-virtual {v1, p0, v0}, Landroid/media/ExifInterface;->getAttributeInt(Ljava/lang/String;I)I

    move-result p0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 609
    goto :goto_0

    .line 608
    :catchall_0
    move-exception p0

    const/4 p0, 0x0

    .line 610
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

    .line 613
    :cond_0
    new-instance v6, Landroid/graphics/Matrix;

    invoke-direct {v6}, Landroid/graphics/Matrix;-><init>()V

    .line 614
    const/high16 v0, 0x43870000    # 270.0f

    const/high16 v1, 0x42b40000    # 90.0f

    const/high16 v2, 0x3f800000    # 1.0f

    const/high16 v3, -0x40800000    # -1.0f

    packed-switch p0, :pswitch_data_0

    .line 639
    return-object p1

    .line 622
    :pswitch_0
    invoke-virtual {v6, v0}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 623
    goto :goto_1

    .line 635
    :pswitch_1
    invoke-virtual {v6, v0}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 636
    invoke-virtual {v6, v3, v2}, Landroid/graphics/Matrix;->postScale(FF)Z

    .line 637
    goto :goto_1

    .line 616
    :pswitch_2
    invoke-virtual {v6, v1}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 617
    goto :goto_1

    .line 631
    :pswitch_3
    invoke-virtual {v6, v1}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 632
    invoke-virtual {v6, v3, v2}, Landroid/graphics/Matrix;->postScale(FF)Z

    .line 633
    goto :goto_1

    .line 628
    :pswitch_4
    invoke-virtual {v6, v2, v3}, Landroid/graphics/Matrix;->postScale(FF)Z

    .line 629
    goto :goto_1

    .line 619
    :pswitch_5
    const/high16 p0, 0x43340000    # 180.0f

    invoke-virtual {v6, p0}, Landroid/graphics/Matrix;->postRotate(F)Z

    .line 620
    goto :goto_1

    .line 625
    :pswitch_6
    invoke-virtual {v6, v3, v2}, Landroid/graphics/Matrix;->postScale(FF)Z

    .line 626
    nop

    .line 642
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

    .line 643
    if-eq p0, p1, :cond_1

    .line 644
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->recycle()V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    .line 646
    :cond_1
    return-object p0

    .line 647
    :catchall_1
    move-exception p0

    .line 648
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "orient failed: "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string v0, "StarlightSR"

    invoke-static {v0, p0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 649
    return-object p1

    .line 611
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

.method public static postProcessVideo(Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;
    .locals 2

    .line 204
    if-nez p0, :cond_0

    .line 205
    const/4 p0, 0x0

    return-object p0

    .line 207
    :cond_0
    invoke-static {p0}, Lcom/starlight/camera/StarlightSR;->applyColorMatrix(Landroid/graphics/Bitmap;)V

    .line 208
    const v0, 0x3eb33333    # 0.35f

    const/4 v1, 0x3

    invoke-static {p0, v0, v1}, Lcom/starlight/camera/StarlightSR;->detailEnhance(Landroid/graphics/Bitmap;FI)V

    .line 209
    return-object p0
.end method

.method public static process(Landroid/content/Context;Ljava/io/File;IIJ)Z
    .locals 7
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Throwable;
        }
    .end annotation

    .line 59
    const/high16 v6, 0x40000000    # 2.0f

    move-object v0, p0

    move-object v1, p1

    move v2, p2

    move v3, p3

    move-wide v4, p4

    invoke-static/range {v0 .. v6}, Lcom/starlight/camera/StarlightSR;->process(Landroid/content/Context;Ljava/io/File;IIJF)Z

    move-result p0

    return p0
.end method

.method public static process(Landroid/content/Context;Ljava/io/File;IIJF)Z
    .locals 16
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Throwable;
        }
    .end annotation

    .line 64
    move-object/from16 v0, p0

    move-object/from16 v8, p1

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v9

    .line 65
    const/4 v11, 0x0

    if-eqz v0, :cond_c

    if-eqz v8, :cond_c

    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->exists()Z

    move-result v1

    if-eqz v1, :cond_c

    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->length()J

    move-result-wide v1

    const-wide/16 v3, 0x0

    cmp-long v1, v1, v3

    if-nez v1, :cond_0

    goto/16 :goto_4

    .line 68
    :cond_0
    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object v1

    .line 69
    const-string v2, "Upscaled_"

    invoke-virtual {v1, v2}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v2

    if-nez v2, :cond_b

    const-string v2, "_Upscaled"

    invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v1

    if-eqz v1, :cond_1

    goto/16 :goto_3

    .line 73
    :cond_1
    new-instance v1, Landroid/graphics/BitmapFactory$Options;

    invoke-direct {v1}, Landroid/graphics/BitmapFactory$Options;-><init>()V

    .line 74
    const/4 v12, 0x1

    iput-boolean v12, v1, Landroid/graphics/BitmapFactory$Options;->inJustDecodeBounds:Z

    .line 75
    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2, v1}, Landroid/graphics/BitmapFactory;->decodeFile(Ljava/lang/String;Landroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;

    .line 76
    iget v2, v1, Landroid/graphics/BitmapFactory$Options;->outWidth:I

    .line 77
    iget v1, v1, Landroid/graphics/BitmapFactory$Options;->outHeight:I

    .line 78
    if-lez v2, :cond_a

    if-gtz v1, :cond_2

    goto/16 :goto_2

    .line 81
    :cond_2
    int-to-long v3, v2

    int-to-long v5, v1

    mul-long/2addr v3, v5

    cmp-long v3, v3, p4

    const-string v13, "x"

    const-string v14, "StarlightSR"

    if-lez v3, :cond_3

    .line 82
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "skip huge source "

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v14, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 83
    return v11

    .line 86
    :cond_3
    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Landroid/graphics/BitmapFactory;->decodeFile(Ljava/lang/String;)Landroid/graphics/Bitmap;

    move-result-object v1

    .line 87
    if-nez v1, :cond_4

    .line 88
    return v11

    .line 90
    :cond_4
    nop

    .line 92
    :try_start_0
    invoke-static {v8, v1}, Lcom/starlight/camera/StarlightSR;->orient(Ljava/io/File;Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;

    move-result-object v15
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    .line 93
    :try_start_1
    invoke-static/range {p0 .. p0}, Lcom/starlight/camera/StarlightSR;->hwAccel(Landroid/content/Context;)Z

    move-result v6

    move-object/from16 v1, p0

    move-object v2, v15

    move/from16 v3, p2

    move-wide/from16 v4, p4

    move/from16 v7, p6

    invoke-static/range {v1 .. v7}, Lcom/starlight/camera/StarlightSR;->upscaleBitmap(Landroid/content/Context;Landroid/graphics/Bitmap;IJZF)Landroid/graphics/Bitmap;

    move-result-object v1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    .line 95
    if-eqz v15, :cond_5

    .line 96
    invoke-virtual {v15}, Landroid/graphics/Bitmap;->recycle()V

    .line 99
    :cond_5
    if-nez v1, :cond_6

    .line 100
    return v11

    .line 103
    :cond_6
    invoke-virtual {v1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v2

    .line 104
    invoke-virtual {v1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v3

    .line 105
    invoke-static/range {p0 .. p0}, Lcom/starlight/camera/StarlightPolicy;->photoFormat(Landroid/content/Context;)Ljava/lang/String;

    move-result-object v4

    .line 106
    nop

    .line 107
    if-lez p3, :cond_7

    move/from16 v5, p3

    goto :goto_0

    :cond_7
    const/16 v5, 0x64

    .line 106
    :goto_0
    invoke-static {v0, v1, v8, v4, v5}, Lcom/starlight/camera/StarlightOutput;->save(Landroid/content/Context;Landroid/graphics/Bitmap;Ljava/io/File;Ljava/lang/String;I)Ljava/io/File;

    move-result-object v0

    .line 108
    invoke-virtual {v1}, Landroid/graphics/Bitmap;->recycle()V

    .line 109
    if-nez v0, :cond_8

    .line 110
    return v11

    .line 112
    :cond_8
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "done "

    invoke-virtual {v1, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v1, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    const-string v5, " "

    invoke-virtual {v1, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

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

    invoke-virtual {v0, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, " ms="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    .line 113
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v1

    sub-long/2addr v1, v9

    invoke-virtual {v0, v1, v2}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    .line 112
    invoke-static {v14, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 114
    return v12

    .line 95
    :catchall_0
    move-exception v0

    move-object v1, v15

    goto :goto_1

    :catchall_1
    move-exception v0

    :goto_1
    if-eqz v1, :cond_9

    .line 96
    invoke-virtual {v1}, Landroid/graphics/Bitmap;->recycle()V

    .line 98
    :cond_9
    throw v0

    .line 79
    :cond_a
    :goto_2
    return v11

    .line 70
    :cond_b
    :goto_3
    return v11

    .line 66
    :cond_c
    :goto_4
    return v11
.end method

.method private static resizeBicubicRs(Landroid/content/Context;Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;
    .locals 7

    .line 301
    const-string v0, "StarlightSR"

    .line 302
    nop

    .line 303
    nop

    .line 304
    nop

    .line 306
    const/4 v1, 0x0

    :try_start_0
    invoke-static {p0}, Landroid/renderscript/RenderScript;->create(Landroid/content/Context;)Landroid/renderscript/RenderScript;

    move-result-object p0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_8

    .line 307
    :try_start_1
    sget-object v2, Landroid/renderscript/RenderScript$Priority;->NORMAL:Landroid/renderscript/RenderScript$Priority;

    invoke-virtual {p0, v2}, Landroid/renderscript/RenderScript;->setPriority(Landroid/renderscript/RenderScript$Priority;)V

    .line 308
    sget-object v2, Landroid/graphics/Bitmap$Config;->ARGB_8888:Landroid/graphics/Bitmap$Config;

    invoke-static {p2, p3, v2}, Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;

    move-result-object v2

    .line 309
    sget-object v3, Landroid/renderscript/Allocation$MipmapControl;->MIPMAP_NONE:Landroid/renderscript/Allocation$MipmapControl;

    const/4 v4, 0x1

    invoke-static {p0, p1, v3, v4}, Landroid/renderscript/Allocation;->createFromBitmap(Landroid/renderscript/RenderScript;Landroid/graphics/Bitmap;Landroid/renderscript/Allocation$MipmapControl;I)Landroid/renderscript/Allocation;

    move-result-object p1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_7

    .line 310
    :try_start_2
    sget-object v3, Landroid/renderscript/Allocation$MipmapControl;->MIPMAP_NONE:Landroid/renderscript/Allocation$MipmapControl;

    invoke-static {p0, v2, v3, v4}, Landroid/renderscript/Allocation;->createFromBitmap(Landroid/renderscript/RenderScript;Landroid/graphics/Bitmap;Landroid/renderscript/Allocation$MipmapControl;I)Landroid/renderscript/Allocation;

    move-result-object v3
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_6

    .line 311
    :try_start_3
    invoke-static {p0}, Landroid/renderscript/ScriptIntrinsicResize;->create(Landroid/renderscript/RenderScript;)Landroid/renderscript/ScriptIntrinsicResize;

    move-result-object v4
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_5

    .line 312
    :try_start_4
    invoke-virtual {v4, p1}, Landroid/renderscript/ScriptIntrinsicResize;->setInput(Landroid/renderscript/Allocation;)V

    .line 313
    invoke-virtual {v4, v3}, Landroid/renderscript/ScriptIntrinsicResize;->forEach_bicubic(Landroid/renderscript/Allocation;)V

    .line 314
    invoke-virtual {v3, v2}, Landroid/renderscript/Allocation;->copyTo(Landroid/graphics/Bitmap;)V

    .line 315
    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    const-string v6, "RenderScript bicubic "

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object p2

    const-string v5, "x"

    invoke-virtual {p2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p2

    invoke-virtual {p2, p3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object p2

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-static {v0, p2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_4

    .line 316
    nop

    .line 321
    if-eqz v4, :cond_0

    .line 322
    :try_start_5
    invoke-virtual {v4}, Landroid/renderscript/ScriptIntrinsicResize;->destroy()V
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    goto :goto_0

    :catchall_0
    move-exception p2

    .line 324
    :cond_0
    :goto_0
    if-eqz p1, :cond_1

    .line 325
    :try_start_6
    invoke-virtual {p1}, Landroid/renderscript/Allocation;->destroy()V
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_1

    goto :goto_1

    :catchall_1
    move-exception p1

    .line 327
    :cond_1
    :goto_1
    if-eqz v3, :cond_2

    .line 328
    :try_start_7
    invoke-virtual {v3}, Landroid/renderscript/Allocation;->destroy()V
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_2

    goto :goto_2

    :catchall_2
    move-exception p1

    .line 330
    :cond_2
    :goto_2
    if-eqz p0, :cond_3

    .line 331
    :try_start_8
    invoke-virtual {p0}, Landroid/renderscript/RenderScript;->destroy()V
    :try_end_8
    .catchall {:try_start_8 .. :try_end_8} :catchall_3

    goto :goto_3

    :catchall_3
    move-exception p0

    .line 316
    :cond_3
    :goto_3
    return-object v2

    .line 317
    :catchall_4
    move-exception p2

    goto :goto_6

    :catchall_5
    move-exception p2

    move-object v4, v1

    goto :goto_6

    :catchall_6
    move-exception p2

    move-object v3, v1

    goto :goto_5

    :catchall_7
    move-exception p2

    move-object p1, v1

    goto :goto_4

    :catchall_8
    move-exception p2

    move-object p0, v1

    move-object p1, p0

    :goto_4
    move-object v3, p1

    :goto_5
    move-object v4, v3

    .line 318
    :goto_6
    :try_start_9
    new-instance p3, Ljava/lang/StringBuilder;

    invoke-direct {p3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "RenderScript bicubic failed: "

    invoke-virtual {p3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p3

    invoke-virtual {p3, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object p2

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-static {v0, p2}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_9
    .catchall {:try_start_9 .. :try_end_9} :catchall_d

    .line 319
    nop

    .line 321
    if-eqz v4, :cond_4

    .line 322
    :try_start_a
    invoke-virtual {v4}, Landroid/renderscript/ScriptIntrinsicResize;->destroy()V
    :try_end_a
    .catchall {:try_start_a .. :try_end_a} :catchall_9

    goto :goto_7

    :catchall_9
    move-exception p2

    .line 324
    :cond_4
    :goto_7
    if-eqz p1, :cond_5

    .line 325
    :try_start_b
    invoke-virtual {p1}, Landroid/renderscript/Allocation;->destroy()V
    :try_end_b
    .catchall {:try_start_b .. :try_end_b} :catchall_a

    goto :goto_8

    :catchall_a
    move-exception p1

    .line 327
    :cond_5
    :goto_8
    if-eqz v3, :cond_6

    .line 328
    :try_start_c
    invoke-virtual {v3}, Landroid/renderscript/Allocation;->destroy()V
    :try_end_c
    .catchall {:try_start_c .. :try_end_c} :catchall_b

    goto :goto_9

    :catchall_b
    move-exception p1

    .line 330
    :cond_6
    :goto_9
    if-eqz p0, :cond_7

    .line 331
    :try_start_d
    invoke-virtual {p0}, Landroid/renderscript/RenderScript;->destroy()V
    :try_end_d
    .catchall {:try_start_d .. :try_end_d} :catchall_c

    goto :goto_a

    :catchall_c
    move-exception p0

    .line 319
    :cond_7
    :goto_a
    return-object v1

    .line 321
    :catchall_d
    move-exception p2

    if-eqz v4, :cond_8

    .line 322
    :try_start_e
    invoke-virtual {v4}, Landroid/renderscript/ScriptIntrinsicResize;->destroy()V
    :try_end_e
    .catchall {:try_start_e .. :try_end_e} :catchall_e

    goto :goto_b

    :catchall_e
    move-exception p3

    .line 324
    :cond_8
    :goto_b
    if-eqz p1, :cond_9

    .line 325
    :try_start_f
    invoke-virtual {p1}, Landroid/renderscript/Allocation;->destroy()V
    :try_end_f
    .catchall {:try_start_f .. :try_end_f} :catchall_f

    goto :goto_c

    :catchall_f
    move-exception p1

    .line 327
    :cond_9
    :goto_c
    if-eqz v3, :cond_a

    .line 328
    :try_start_10
    invoke-virtual {v3}, Landroid/renderscript/Allocation;->destroy()V
    :try_end_10
    .catchall {:try_start_10 .. :try_end_10} :catchall_10

    goto :goto_d

    :catchall_10
    move-exception p1

    .line 330
    :cond_a
    :goto_d
    if-eqz p0, :cond_b

    .line 331
    :try_start_11
    invoke-virtual {p0}, Landroid/renderscript/RenderScript;->destroy()V
    :try_end_11
    .catchall {:try_start_11 .. :try_end_11} :catchall_11

    goto :goto_e

    :catchall_11
    move-exception p0

    .line 333
    :cond_b
    :goto_e
    throw p2
.end method

.method private static resizeLanczos(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;
    .locals 17

    .line 337
    move/from16 v7, p1

    move/from16 v8, p2

    invoke-virtual/range {p0 .. p0}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v1

    .line 338
    invoke-virtual/range {p0 .. p0}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v6

    .line 339
    mul-int v0, v1, v6

    new-array v3, v0, [I

    .line 340
    const/4 v11, 0x0

    const/4 v13, 0x0

    const/4 v14, 0x0

    move-object/from16 v9, p0

    move-object v10, v3

    move v12, v1

    move v15, v1

    move/from16 v16, v6

    invoke-virtual/range {v9 .. v16}, Landroid/graphics/Bitmap;->getPixels([IIIIIII)V

    .line 342
    mul-int/lit8 v0, v7, 0x6

    new-array v4, v0, [I

    .line 343
    new-array v5, v0, [F

    .line 344
    invoke-static {v1, v7, v4, v5}, Lcom/starlight/camera/StarlightSR;->buildWeights(II[I[F)V

    .line 345
    mul-int v0, v7, v6

    new-array v9, v0, [I

    .line 347
    invoke-static {}, Ljava/lang/Runtime;->getRuntime()Ljava/lang/Runtime;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Runtime;->availableProcessors()I

    move-result v0

    const/16 v2, 0x8

    invoke-static {v2, v0}, Ljava/lang/Math;->min(II)I

    move-result v0

    const/4 v2, 0x1

    invoke-static {v2, v0}, Ljava/lang/Math;->max(II)I

    move-result v0

    .line 348
    invoke-static {}, Ljava/lang/Runtime;->getRuntime()Ljava/lang/Runtime;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/Runtime;->maxMemory()J

    move-result-wide v10

    .line 349
    const-wide/32 v12, 0x10000000

    cmp-long v2, v10, v12

    if-gez v2, :cond_0

    .line 350
    const/4 v2, 0x2

    invoke-static {v0, v2}, Ljava/lang/Math;->min(II)I

    move-result v0

    move v10, v0

    goto :goto_0

    .line 351
    :cond_0
    const-wide/32 v12, 0x20000000

    cmp-long v2, v10, v12

    if-gez v2, :cond_1

    .line 352
    const/4 v2, 0x4

    invoke-static {v0, v2}, Ljava/lang/Math;->min(II)I

    move-result v0

    move v10, v0

    goto :goto_0

    .line 351
    :cond_1
    move v10, v0

    .line 354
    :goto_0
    new-instance v11, Lcom/starlight/camera/StarlightSR$1;

    move-object v0, v11

    move/from16 v2, p1

    move v12, v6

    move-object v6, v9

    invoke-direct/range {v0 .. v6}, Lcom/starlight/camera/StarlightSR$1;-><init>(II[I[I[F[I)V

    invoke-static {v12, v10, v11}, Lcom/starlight/camera/StarlightSR;->runRows(IILcom/starlight/camera/StarlightSR$RowTask;)V

    .line 380
    sget-object v0, Landroid/graphics/Bitmap$Config;->ARGB_8888:Landroid/graphics/Bitmap$Config;

    invoke-static {v7, v8, v0}, Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;

    move-result-object v11

    .line 381
    mul-int/lit8 v0, v8, 0x6

    new-array v3, v0, [I

    .line 382
    new-array v4, v0, [F

    .line 383
    invoke-static {v12, v8, v3, v4}, Lcom/starlight/camera/StarlightSR;->buildWeights(II[I[F)V

    .line 384
    new-instance v5, Ljava/lang/Object;

    invoke-direct {v5}, Ljava/lang/Object;-><init>()V

    .line 385
    new-instance v12, Lcom/starlight/camera/StarlightSR$2;

    move-object v0, v12

    move/from16 v1, p1

    move-object v2, v9

    move-object v6, v11

    invoke-direct/range {v0 .. v6}, Lcom/starlight/camera/StarlightSR$2;-><init>(I[I[I[FLjava/lang/Object;Landroid/graphics/Bitmap;)V

    invoke-static {v8, v10, v12}, Lcom/starlight/camera/StarlightSR;->runRows(IILcom/starlight/camera/StarlightSR$RowTask;)V

    .line 412
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "Lanczos3 "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, "x"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v8}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, " threads="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v10}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "StarlightSR"

    invoke-static {v1, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 413
    return-object v11
.end method

.method private static runRows(IILcom/starlight/camera/StarlightSR$RowTask;)V
    .locals 8

    .line 421
    const/4 v0, 0x0

    const/4 v1, 0x1

    if-gt p1, v1, :cond_0

    .line 422
    invoke-interface {p2, v0, p0}, Lcom/starlight/camera/StarlightSR$RowTask;->run(II)V

    .line 423
    return-void

    .line 425
    :cond_0
    new-array v2, p1, [Ljava/lang/Thread;

    .line 426
    add-int v3, p0, p1

    sub-int/2addr v3, v1

    div-int/2addr v3, p1

    .line 427
    move v1, v0

    :goto_0
    if-ge v1, p1, :cond_2

    .line 428
    mul-int v4, v1, v3

    .line 429
    add-int v5, v4, v3

    invoke-static {p0, v5}, Ljava/lang/Math;->min(II)I

    move-result v5

    .line 430
    if-lt v4, v5, :cond_1

    .line 431
    goto :goto_1

    .line 433
    :cond_1
    new-instance v6, Ljava/lang/Thread;

    new-instance v7, Lcom/starlight/camera/StarlightSR$3;

    invoke-direct {v7, p2, v4, v5}, Lcom/starlight/camera/StarlightSR$3;-><init>(Lcom/starlight/camera/StarlightSR$RowTask;II)V

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "starlight-sr-"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-direct {v6, v7, v4}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;Ljava/lang/String;)V

    aput-object v6, v2, v1

    .line 439
    invoke-virtual {v6}, Ljava/lang/Thread;->start()V

    .line 427
    add-int/lit8 v1, v1, 0x1

    goto :goto_0

    .line 441
    :cond_2
    :goto_1
    nop

    :goto_2
    if-ge v0, p1, :cond_4

    aget-object p0, v2, v0

    .line 442
    if-eqz p0, :cond_3

    .line 444
    :try_start_0
    invoke-virtual {p0}, Ljava/lang/Thread;->join()V
    :try_end_0
    .catch Ljava/lang/InterruptedException; {:try_start_0 .. :try_end_0} :catch_0

    .line 447
    goto :goto_3

    .line 445
    :catch_0
    move-exception p0

    .line 446
    invoke-static {}, Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/Thread;->interrupt()V

    .line 441
    :cond_3
    :goto_3
    add-int/lit8 v0, v0, 0x1

    goto :goto_2

    .line 450
    :cond_4
    return-void
.end method

.method private static stem(Ljava/lang/String;)Ljava/lang/String;
    .locals 2

    .line 670
    const/16 v0, 0x2e

    invoke-virtual {p0, v0}, Ljava/lang/String;->lastIndexOf(I)I

    move-result v0

    .line 671
    if-lez v0, :cond_0

    .line 672
    const/4 v1, 0x0

    invoke-virtual {p0, v1, v0}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object p0

    .line 674
    :cond_0
    const-string v0, "_Starlight"

    const-string v1, ""

    invoke-virtual {p0, v0, v1}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object p0

    const-string v0, "_Upscaled"

    invoke-virtual {p0, v0, v1}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object p0

    .line 675
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

    .line 678
    :cond_1
    return-object p0

    .line 676
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

.method public static upscaleBitmap(Landroid/content/Context;Landroid/graphics/Bitmap;IJZ)Landroid/graphics/Bitmap;
    .locals 7

    .line 122
    const/high16 v6, 0x40000000    # 2.0f

    move-object v0, p0

    move-object v1, p1

    move v2, p2

    move-wide v3, p3

    move v5, p5

    invoke-static/range {v0 .. v6}, Lcom/starlight/camera/StarlightSR;->upscaleBitmap(Landroid/content/Context;Landroid/graphics/Bitmap;IJZF)Landroid/graphics/Bitmap;

    move-result-object p0

    return-object p0
.end method

.method public static upscaleBitmap(Landroid/content/Context;Landroid/graphics/Bitmap;IJZF)Landroid/graphics/Bitmap;
    .locals 8

    .line 127
    const/4 v0, 0x0

    if-eqz p1, :cond_a

    invoke-virtual {p1}, Landroid/graphics/Bitmap;->isRecycled()Z

    move-result v1

    if-eqz v1, :cond_0

    goto/16 :goto_3

    .line 130
    :cond_0
    const/4 v1, 0x4

    invoke-static {v1, p2}, Ljava/lang/Math;->min(II)I

    move-result p2

    const/4 v1, 0x2

    invoke-static {v1, p2}, Ljava/lang/Math;->max(II)I

    move-result p2

    .line 131
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v2

    int-to-long v2, v2

    int-to-long v4, p2

    mul-long/2addr v2, v4

    .line 132
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result p2

    int-to-long v6, p2

    mul-long/2addr v6, v4

    .line 133
    long-to-double p2, p3

    long-to-double v2, v2

    long-to-double v4, v6

    mul-double v6, v2, v4

    div-double/2addr p2, v6

    invoke-static {p2, p3}, Ljava/lang/Math;->sqrt(D)D

    move-result-wide p2

    const-wide/high16 v6, 0x3ff0000000000000L    # 1.0

    invoke-static {v6, v7, p2, p3}, Ljava/lang/Math;->min(DD)D

    move-result-wide p2

    .line 134
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result p4

    mul-double/2addr v2, p2

    invoke-static {v2, v3}, Ljava/lang/Math;->round(D)J

    move-result-wide v2

    long-to-int v2, v2

    invoke-static {p4, v2}, Ljava/lang/Math;->max(II)I

    move-result p4

    .line 135
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v2

    mul-double/2addr v4, p2

    invoke-static {v4, v5}, Ljava/lang/Math;->round(D)J

    move-result-wide p2

    long-to-int p2, p2

    invoke-static {v2, p2}, Ljava/lang/Math;->max(II)I

    move-result p2

    .line 136
    and-int/lit8 p3, p4, -0x2

    .line 137
    and-int/lit8 p2, p2, -0x2

    .line 138
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result p4

    if-le p3, p4, :cond_9

    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result p4

    if-gt p2, p4, :cond_1

    goto :goto_2

    .line 143
    :cond_1
    nop

    .line 144
    nop

    .line 145
    const/4 p4, 0x0

    if-eqz p5, :cond_4

    .line 146
    invoke-static {}, Lcom/starlight/camera/StarlightGL;->create()Lcom/starlight/camera/StarlightGL;

    move-result-object p5

    .line 147
    if-eqz p5, :cond_2

    .line 148
    const/4 v6, 0x1

    move-object v2, p5

    move-object v3, p1

    move v4, p3

    move v5, p2

    move v7, p6

    invoke-virtual/range {v2 .. v7}, Lcom/starlight/camera/StarlightGL;->resize(Landroid/graphics/Bitmap;IIZF)Landroid/graphics/Bitmap;

    move-result-object v2

    .line 149
    invoke-virtual {p5}, Lcom/starlight/camera/StarlightGL;->close()V

    .line 150
    if-eqz v2, :cond_3

    const/4 p4, 0x1

    goto :goto_0

    .line 147
    :cond_2
    move-object v2, v0

    .line 152
    :cond_3
    :goto_0
    if-nez v2, :cond_5

    .line 153
    invoke-static {p0, p1, p3, p2}, Lcom/starlight/camera/StarlightSR;->resizeBicubicRs(Landroid/content/Context;Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;

    move-result-object v2

    goto :goto_1

    .line 156
    :cond_4
    move-object v2, v0

    :cond_5
    :goto_1
    if-nez v2, :cond_6

    .line 157
    invoke-static {p1, p3, p2}, Lcom/starlight/camera/StarlightSR;->resizeLanczos(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;

    move-result-object v2

    .line 159
    :cond_6
    if-nez v2, :cond_7

    .line 160
    return-object v0

    .line 162
    :cond_7
    if-nez p4, :cond_8

    .line 163
    invoke-static {v2}, Lcom/starlight/camera/StarlightSR;->applyColorMatrix(Landroid/graphics/Bitmap;)V

    .line 164
    invoke-static {v2, p6, v1}, Lcom/starlight/camera/StarlightSR;->detailEnhance(Landroid/graphics/Bitmap;FI)V

    .line 166
    :cond_8
    return-object v2

    .line 139
    :cond_9
    :goto_2
    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0}, Ljava/lang/StringBuilder;-><init>()V

    const-string p2, "skip, source already at cap "

    invoke-virtual {p0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result p2

    invoke-virtual {p0, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object p0

    const-string p2, "x"

    invoke-virtual {p0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result p1

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string p1, "StarlightSR"

    invoke-static {p1, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 140
    return-object v0

    .line 128
    :cond_a
    :goto_3
    return-object v0
.end method

.method public static upscaleBitmapFast(Landroid/content/Context;Landroid/graphics/Bitmap;IJ)Landroid/graphics/Bitmap;
    .locals 8

    .line 175
    const/4 v0, 0x0

    if-eqz p1, :cond_5

    invoke-virtual {p1}, Landroid/graphics/Bitmap;->isRecycled()Z

    move-result v1

    if-eqz v1, :cond_0

    goto :goto_3

    .line 178
    :cond_0
    const/4 v1, 0x4

    invoke-static {v1, p2}, Ljava/lang/Math;->min(II)I

    move-result p2

    const/4 v1, 0x1

    invoke-static {v1, p2}, Ljava/lang/Math;->max(II)I

    move-result p2

    .line 179
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v2

    int-to-long v2, v2

    int-to-long v4, p2

    mul-long/2addr v2, v4

    .line 180
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result p2

    int-to-long v6, p2

    mul-long/2addr v6, v4

    .line 181
    long-to-double p2, p3

    long-to-double v2, v2

    long-to-double v4, v6

    mul-double v6, v2, v4

    div-double/2addr p2, v6

    invoke-static {p2, p3}, Ljava/lang/Math;->sqrt(D)D

    move-result-wide p2

    const-wide/high16 v6, 0x3ff0000000000000L    # 1.0

    invoke-static {v6, v7, p2, p3}, Ljava/lang/Math;->min(DD)D

    move-result-wide p2

    .line 182
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result p4

    mul-double/2addr v2, p2

    invoke-static {v2, v3}, Ljava/lang/Math;->round(D)J

    move-result-wide v2

    long-to-int v2, v2

    invoke-static {p4, v2}, Ljava/lang/Math;->max(II)I

    move-result p4

    .line 183
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v2

    mul-double/2addr v4, p2

    invoke-static {v4, v5}, Ljava/lang/Math;->round(D)J

    move-result-wide p2

    long-to-int p2, p2

    invoke-static {v2, p2}, Ljava/lang/Math;->max(II)I

    move-result p2

    .line 184
    and-int/lit8 p3, p4, -0x2

    .line 185
    and-int/lit8 p2, p2, -0x2

    .line 186
    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result p4

    if-le p3, p4, :cond_4

    invoke-virtual {p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result p4

    if-gt p2, p4, :cond_1

    goto :goto_2

    .line 189
    :cond_1
    nop

    .line 190
    invoke-static {p0}, Lcom/starlight/camera/StarlightSR;->hwAccel(Landroid/content/Context;)Z

    move-result p4

    if-eqz p4, :cond_2

    .line 191
    invoke-static {p0, p1, p3, p2}, Lcom/starlight/camera/StarlightSR;->resizeBicubicRs(Landroid/content/Context;Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;

    move-result-object p0

    goto :goto_0

    .line 190
    :cond_2
    move-object p0, v0

    .line 193
    :goto_0
    if-nez p0, :cond_3

    .line 195
    :try_start_0
    invoke-static {p1, p3, p2, v1}, Landroid/graphics/Bitmap;->createScaledBitmap(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;

    move-result-object p0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 198
    goto :goto_1

    .line 196
    :catchall_0
    move-exception p0

    .line 197
    return-object v0

    .line 200
    :cond_3
    :goto_1
    invoke-static {p0}, Lcom/starlight/camera/StarlightSR;->postProcessVideo(Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;

    move-result-object p0

    return-object p0

    .line 187
    :cond_4
    :goto_2
    return-object v0

    .line 176
    :cond_5
    :goto_3
    return-object v0
.end method

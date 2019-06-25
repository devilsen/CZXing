#include "yuv2bmp.h"

#ifdef __cplusplus
extern "C" 
{
#endif

static void decodeI420(U8 yuv[], int width, int height, int pitch, U8 rgb[])
{
    int frameSize = width * height;    
    int w, h;
    int yp, y, y1m, up, u, vp, v, rgbp, r, g, b;

    for (h = height-1; h >= 0; h--) 
    {
        yp = h * width;
        up = frameSize + (h >> 1) * (width >> 1);
        vp = frameSize + (frameSize >> 2) + (h >> 1) * (width >> 1);
        rgbp = (height - h - 1) * pitch;
        for (w = 0; w < width; w++, yp++)
        {
            y = (int) yuv[yp];
            if ((w & 1) == 0) 
            {
                u = ((int) yuv[up++]) - 128;
                v = ((int) yuv[vp++]) - 128;
            }

            y1m = y << 20;
            r = (y1m + 1475871 * v);
            g = (y1m - 751724 * v - 362283 * u);
            b = (y1m + 1865417 * u);

            r = (r + 524288);
            g = (g + 524288);
            b = (b + 524288);
            if (r < 0) r = 0; else if (r > 268435455) r = 268435455;
            if (g < 0) g = 0; else if (g > 268435455) g = 268435455;
            if (b < 0) b = 0; else if (b > 268435455) b = 268435455;

            rgb[rgbp++] = b >> 20;
            rgb[rgbp++] = g >> 20;
            rgb[rgbp++] = r >> 20;
        }
    }
}

static void decodeYV12(U8 yuv[], int width, int height, int pitch, U8 rgb[])
{
    int frameSize = width * height;    
    int w, h;
    int yp, y, y1m, up, u, vp, v, rgbp, r, g, b;

    for (h = height-1; h >= 0; h--) 
    {
        yp = h * width;
        vp = frameSize + (h >> 1) * (width >> 1);
        up = frameSize + (frameSize >> 2) + (h >> 1) * (width >> 1);
        rgbp = (height - h - 1) * pitch;
        for (w = 0; w < width; w++, yp++)
        {
            y = (int) yuv[yp];
            if ((w & 1) == 0) 
            {
                v = ((int) yuv[vp++]) - 128;
                u = ((int) yuv[up++]) - 128;
            }

            y1m = y << 20;
            r = (y1m + 1475871 * v);
            g = (y1m - 751724 * v - 362283 * u);
            b = (y1m + 1865417 * u);

            r = (r + 524288);
            g = (g + 524288);
            b = (b + 524288);
            if (r < 0) r = 0; else if (r > 268435455) r = 268435455;
            if (g < 0) g = 0; else if (g > 268435455) g = 268435455;
            if (b < 0) b = 0; else if (b > 268435455) b = 268435455;

            rgb[rgbp++] = b >> 20;
            rgb[rgbp++] = g >> 20;
            rgb[rgbp++] = r >> 20;
        }
    }
}

static void decodeNV12(U8 yuv[], int width, int height, int pitch, U8 rgb[])
{
    int frameSize = width * height;    
    int w, h;
    int yp, y, y1m, uvp, u, v, rgbp, r, g, b;
    
    for (h = height-1; h >= 0; h--) 
    {
        yp = h * width;
        uvp = frameSize + (h >> 1) * width;
        rgbp = (height - h - 1) * pitch;
        for (w = 0; w < width; w++, yp++)
        {
            y = (int) yuv[yp];
            if ((w & 1) == 0) 
            {
                u = ((int) yuv[uvp++]) - 128;
                v = ((int) yuv[uvp++]) - 128;
            }

            y1m = y << 20;
            r = (y1m + 1475871 * v);
            g = (y1m - 751724 * v - 362283 * u);
            b = (y1m + 1865417 * u);

            r = (r + 524288);
            g = (g + 524288);
            b = (b + 524288);
            if (r < 0) r = 0; else if (r > 268435455) r = 268435455;
            if (g < 0) g = 0; else if (g > 268435455) g = 268435455;
            if (b < 0) b = 0; else if (b > 268435455) b = 268435455;

            rgb[rgbp++] = b >> 20;
            rgb[rgbp++] = g >> 20;
            rgb[rgbp++] = r >> 20;
        }
    }
}

static void decodeNV21(U8 yuv[], int width, int height, int pitch, U8 rgb[])
{
    int frameSize = width * height;    
    int w, h;
    int yp, y, y1m, uvp, u, v, rgbp, r, g, b;
    
    for (h = height-1; h >= 0; h--) 
    {
        yp = h * width;
        uvp = frameSize + (h >> 1) * width;
        rgbp = (height - h - 1) * pitch;
        for (w = 0; w < width; w++, yp++)
        {
            y = (int) yuv[yp];
            if ((w & 1) == 0) 
            {
                v = ((int) yuv[uvp++]) - 128;
                u = ((int) yuv[uvp++]) - 128;
            }

            y1m = y << 20;
            r = (y1m + 1475871 * v);
            g = (y1m - 751724 * v - 362283 * u);
            b = (y1m + 1865417 * u);

            r = (r + 524288);
            g = (g + 524288);
            b = (b + 524288);
            if (r < 0) r = 0; else if (r > 268435455) r = 268435455;
            if (g < 0) g = 0; else if (g > 268435455) g = 268435455;
            if (b < 0) b = 0; else if (b > 268435455) b = 268435455;

            rgb[rgbp++] = b >> 20;
            rgb[rgbp++] = g >> 20;
            rgb[rgbp++] = r >> 20;
        }
    }
}

Bitmap *yuv2bmp(YUV_FORMAT format, U8 yuv[], int width, int height)
{
    Bitmap *bmp = NULL;
    BitmapInfo *bmpInfo = NULL;
    U32 pitch = ((width * 24 + 31) >> 5) << 2; // size of a row, include pad.
    U32 dataSize = pitch * height;

    bmp = (Bitmap *)malloc(sizeof(BitmapInfo) + dataSize);
    memset(bmp, 0, sizeof(BitmapInfo) + dataSize);
    bmpInfo = &bmp->bInfo;

    //Let user write it by himself.
    //bmpInfo->bfHeader.bfType = 0x4D42;  // "BM"
    bmpInfo->bfHeader.bfSize = 2 + sizeof(BitmapInfo) + dataSize;
    bmpInfo->bfHeader.bfReserved1 = 0;
    bmpInfo->bfHeader.bfReserved2 = 0;
    bmpInfo->bfHeader.bfOffBits = 2 + sizeof(BitmapInfo);
  
    bmpInfo->biHeader.biSize = sizeof(BitmapInfoHeader);
    bmpInfo->biHeader.biWidth = width;
    bmpInfo->biHeader.biHeight = height;
    bmpInfo->biHeader.biPlanes = 1;
    bmpInfo->biHeader.biBitCount = 24;
    bmpInfo->biHeader.biCompression = BI_RGB;
    bmpInfo->biHeader.biSizeImage = dataSize;
    bmpInfo->biHeader.biXPelsPerMeter = 0;
    bmpInfo->biHeader.biYPelsPerMeter = 0;
    bmpInfo->biHeader.biClrUsed = 0;
    bmpInfo->biHeader.biClrImportant = 0;
    
    switch(format)
    {
        case YUV_I420:
            decodeI420(yuv, width, height, pitch, bmp->bData.rgb);
            break;
        case YUV_YV12:
            decodeYV12(yuv, width, height, pitch, bmp->bData.rgb);
            break;
        case YUV_NV12:
            decodeNV12(yuv, width, height, pitch, bmp->bData.rgb);
            break;
        case YUV_NV21:
            decodeNV21(yuv, width, height, pitch, bmp->bData.rgb);
            break;
        default:
            break;
    }
    
    return bmp;
}

int yuv2bmpfile(YUV_FORMAT format, U8 yuv[], int width, int height, const char *file)
{
    FILE *fp = NULL;
    int size = 0;
    Bitmap *bmp = yuv2bmp(format, yuv, width, height);
    
    if(bmp == NULL)
    {
        return 0;
    }
    
    fp = fopen(file, "w+b");
    if(fp == NULL)
    {
        free(bmp);
        return 0;
    }
    
    fwrite(BITMAP_HEADER_TYPE, 1, strlen(BITMAP_HEADER_TYPE), fp);
    size = bmp->bInfo.bfHeader.bfSize;
    fwrite(bmp, 1, size-2, fp);
    fclose(fp);
    free(bmp);
    
    return size;
}

#ifdef __cplusplus
}
#endif


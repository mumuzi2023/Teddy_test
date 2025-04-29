package com.sismics.util;

import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Test of the image utilities.
 *
 * 作者: bgamard
 */
public class TestImageUtil {

    @Test
    public void computeGravatarTest() {
        Assert.assertEquals("0bc83cb571cd1c50ba6f3e8a78ef1346", ImageUtil.computeGravatar("MyEmailAddress@example.com "));
    }

    @Test
    public void testIsBlackForBinaryImage() {
        int width = 10, height = 10;
        BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster raster = binaryImage.getRaster();
        // 设置 (0,0) 像素为黑色（值 0）
        raster.setSample(0, 0, 0, 0);
        // 设置 (1,0) 像素为白色（值非 0，这里设置为 1）
        raster.setSample(1, 0, 0, 1);

        Assert.assertTrue("二值图像中 (0,0) 应为黑色", ImageUtil.isBlack(binaryImage, 0, 0));
        Assert.assertFalse("二值图像中 (1,0) 不应为黑色", ImageUtil.isBlack(binaryImage, 1, 0));
    }

    @Test
    public void testIsBlackForRGBImage() {
        int width = 10, height = 10;
        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // 设置 (0,0) 为纯黑色
        rgbImage.setRGB(0, 0, Color.BLACK.getRGB());
        // 设置 (1,0) 为纯白色
        rgbImage.setRGB(1, 0, Color.WHITE.getRGB());
        // 设置 (2,0) 为较暗的灰色 (80,80,80) -> luminance约为80，应返回 true
        rgbImage.setRGB(2, 0, new Color(80, 80, 80).getRGB());
        // 设置 (3,0) 为较亮的灰色 (150,150,150) -> luminance约为150，应返回 false
        rgbImage.setRGB(3, 0, new Color(150, 150, 150).getRGB());

        Assert.assertTrue("RGB图像中 (0,0) 应为黑色", ImageUtil.isBlack(rgbImage, 0, 0));
        Assert.assertFalse("RGB图像中 (1,0) 不应为黑色", ImageUtil.isBlack(rgbImage, 1, 0));
        Assert.assertTrue("RGB图像中 (2,0) 应为黑色", ImageUtil.isBlack(rgbImage, 2, 0));
        Assert.assertFalse("RGB图像中 (3,0) 不应为黑色", ImageUtil.isBlack(rgbImage, 3, 0));

        // 边界测试：超出图像边界的像素应返回 false
        Assert.assertFalse("超出边界 (-1,0) 应返回 false", ImageUtil.isBlack(rgbImage, -1, 0));
        Assert.assertFalse("超出边界 (width,0) 应返回 false", ImageUtil.isBlack(rgbImage, width, 0));
    }
}
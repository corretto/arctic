/*
 *   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.amazon.corretto.arctic.common.backend.impl;

import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import com.amazon.corretto.arctic.common.backend.ArcticScreenRecorder;
import com.amazon.corretto.arctic.common.gui.ShadeManager;
import com.amazon.corretto.arctic.common.gui.WorkbenchManager;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures a {@link ScreenshotCheck} using {@link Robot}. The ScreenshotCheck contains not only the image with the
 * captured screen, but also de position of the workbench and all the shades, so they can be positioned in the same
 * location during playback.
 */
public class AwtRobotScreenRecorder implements ArcticScreenRecorder {
    private final Robot robot;
    private final WorkbenchManager wbManager;
    private final ShadeManager shadeManager;
    private final int xMargin;
    private final int yMargin;
    private static final Logger log = LoggerFactory.getLogger(AwtRobotScreenRecorder.class);

    private final String nativeCaptureToolLinux;

    /**
     * Creates a new instance of an AwtRobotScreenRecorder.
     * @param robot The AWS Robot used to get the screen data.
     * @param wbManager The workbench manager, used to record the position of the workbench during the capture.
     * @param shadeManager The shade manager, used to record the position of the shades during the capture
     * @param xMargin A margin for the x coordinate. If the workbench is positioned inside the margin, we assume the
     *                position we want to capture starts at 0.
     * @param yMargin A margin for the y coordinate. If the workbench is positioned inside the margin, we assume the
     *                position we want to capture starts at 0.
     */
    @Inject
    public AwtRobotScreenRecorder(final Robot robot, final WorkbenchManager wbManager, final ShadeManager shadeManager,
                                  final @Named(CommonInjectionKeys.SCREEN_CAPTURE_MARGIN_X) int xMargin,
                                  final @Named(CommonInjectionKeys.SCREEN_CAPTURE_MARGIN_Y) int yMargin,
                                  final @Named(CommonInjectionKeys.NATIVE_CAPTURE_TOOL_LINUX) String nativeCaptureToolLinux) {
        this.robot = robot;
        this.wbManager = wbManager;
        this.shadeManager = shadeManager;
        this.xMargin = xMargin;
        this.yMargin = yMargin;
        this.nativeCaptureToolLinux = nativeCaptureToolLinux;
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public ScreenshotCheck capture() {
        return capture(false);
    }

    /**
     * {@inheritDoc}
     */
    public ScreenshotCheck capture(boolean captureMouseCursor) {
        ScreenArea wbsa = wbManager.getScreenArea();
        ScreenArea targetsa = new ScreenArea(
                wbsa.getX() < xMargin ? 0 : wbsa.getX(),
                wbsa.getY() < yMargin ? 0 : wbsa.getY(),
                wbsa.getX() < xMargin ? wbsa.getW() + wbsa.getX() : wbsa.getW(),
                wbsa.getY() < yMargin ? wbsa.getH() + wbsa.getY() : wbsa.getH());
        return capture(targetsa, captureMouseCursor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScreenshotCheck capture(final ScreenArea area, boolean captureMouseCursor) {
        final BufferedImage image;
        if (captureMouseCursor) {
            image = nativeCapture(area);
        } else {
            image = robot.createScreenCapture(area.asRectangle());
        }
        final ScreenshotCheck sc = new ScreenshotCheck();
        sc.setImage(image);
        sc.setWorkbench(wbManager.getWorkbench());
        sc.setShades(shadeManager.getShades());
        sc.setSa(area);
        sc.setTimestamp(System.nanoTime());
        sc.setMouseCursor(captureMouseCursor);
        return sc;
    }
    
    /*
     * This method is used to capture the screen with the mouse cursor using OS native tool.
     * It saves image to file, then crops it and returns BufferedImage object.
     */
    private BufferedImage nativeCapture(final ScreenArea area) {
        String os = System.getProperty("os.name").toLowerCase();
        String filename = "screen_" + System.currentTimeMillis() + ".png";
        int captureCmdResult;
        if (os.contains("linux")) {
            captureCmdResult = saveScreenToFile(buildCaptureCommand(this.nativeCaptureToolLinux, filename));
        } else if (os.contains("mac")) {
            throw new RuntimeException("Mac native capture is not supported yet");
        } else if (os.contains("win")) {
            throw new RuntimeException("Windows native capture is not supported yet");
        } else {
            throw new RuntimeException(os + "native capture is not supported");
        }
        File imgFile = new File(filename);
        BufferedImage img;
        try {
            if (captureCmdResult != 0 || !imgFile.exists()) {
                throw new RuntimeException("Failed to capture screen using native tool \'" + this.nativeCaptureToolLinux
                    + "\'. Please make sure it is installed and available in PATH."
                );
            }
            img = getCroppedImageFromFile(area.asRectangle(), imgFile);
        }
        finally {
            imgFile.delete();
        }
        return img;
    }

    /*
     * Builds the argument vector for the configured native capture tool. Only a fixed set of known tools is supported;
     * each expands to a hardcoded invocation that includes the mouse cursor (the reason native capture exists, as
     * java.awt.Robot cannot capture it). The configured value selects a tool but is never itself parsed or executed as
     * a command, so nothing from the properties file reaches ProcessBuilder as free-form input. The only variable
     * argument is the output file path generated by this class.
     */
    private String[] buildCaptureCommand(final String tool, final String filename) {
        switch (tool) {
            case "gnome-screenshot":
                return new String[] {"gnome-screenshot", "-p", "-f", filename};
            case "scrot":
                return new String[] {"scrot", "-p", filename};
            case "maim":
                return new String[] {"maim", filename};
            default:
                throw new RuntimeException("Unsupported native capture tool \'" + tool
                    + "\'. Supported tools: gnome-screenshot, scrot, maim. Set "
                    + "arctic.common.backend.nativeCaptureTool.linux to one of these.");
        }
    }

    /*
     * Invoke native OS tool to save the screenshot to a file.
     */
    private int saveScreenToFile (String... nativeToolCmd) {
        ProcessBuilder processBuilder = new ProcessBuilder(nativeToolCmd);
        processBuilder.redirectErrorStream(true);
        int screenShotRun = 0;
        String output = "";
        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                screenShotRun = process.waitFor();
            }
        } catch (IOException | InterruptedException ex) {
            if (screenShotRun == 0) screenShotRun = 255;
            output += "Exception: " + ex;
        }
        if (screenShotRun != 0) {
            log.error("Command run: {}%n Return code: {}%n Output/Error Message: {}%n",
                    String.join(" ", nativeToolCmd), screenShotRun, output);
        }
        
        return screenShotRun;
    }

    /*
     * Load data from image file, crop it and return BufferedImage object
     */
    private static BufferedImage getCroppedImageFromFile(Rectangle cropRectangle, File imgFile) {
        BufferedImage image;
        try {
            image = ImageIO.read(imgFile);
        } catch (IOException e) {
            throw new RuntimeException("Error reading image file " + imgFile.getName(), e);
        }

        if (cropRectangle != null) {
            // Ensure the cropRectangle is within the bounds of the image
            Rectangle bounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            Rectangle croppedRect = cropRectangle.intersection(bounds);

            return image.getSubimage(croppedRect.x, croppedRect.y, croppedRect.width, croppedRect.height);
        } else {
            return image;
        }
    }
}

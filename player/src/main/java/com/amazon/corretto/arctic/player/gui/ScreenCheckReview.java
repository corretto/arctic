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

package com.amazon.corretto.arctic.player.gui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.OverlayLayout;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import com.amazon.corretto.arctic.player.model.ArcticDiffProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the review UI that can be used to approve or reject new alternatives.
 */
public final class ScreenCheckReview {
    private static final Logger log = LoggerFactory.getLogger(ScreenCheckReview.class);
    private final Object lock = new Object();
    private final ImgControl[] imgControls;
    private final PixelCheck.Type reviewOrder;
    private TimerControl timerControl;
    private AlternativeControl alternativeControl;
    private final List<UpdatableComponent> components = new ArrayList<>();
    private JLayeredPane imgPanel;
    private JFrame controlFrame;
    private int position = 0;
    private int selected = 2;
    private ScreenCheckReview.Result result = ScreenCheckReview.Result.IGNORE;

    /**
     * Creates a new instance of the UI. This will not make the display visible, nor it requires a specific result to be
     * reviewed.
     * @param reviewOrder Display first the alternative with the smallest value in the failed property for this check.
     */
    @Inject
    public ScreenCheckReview(final @Named(InjectionKeys.GUI_REVIEW_ORDER) PixelCheck.Type reviewOrder) {
        this.reviewOrder = reviewOrder;
        final int numImages = (int) Arrays.stream(PixelCheck.Type.values()).filter(PixelCheck.Type::isImage).count();
        imgControls = new ImgControl[numImages];
        buildUI();
    }

    /**
     * Starts the review process for a specific failures.
     * @param diffImages The actual images that will be displayed on the screen for the user to review.
     * @return based on user choice.
     */
    public ScreenCheckReview.Result run(final ArcticDiffImages diffImages) {
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        Path closestFailure = getClosestFailure(diffImages);
        alternativeControl.update(closestFailure, diffImages);
        if (Arrays.stream(imgControls).noneMatch(it -> it.radioButton.isSelected())) {
            imgControls[0].display(true);
        }
        controlFrame.pack();

        controlFrame.setLocationRelativeTo(null);
        controlFrame.setLocation(controlFrame.getX(), screenHeight - (controlFrame.getHeight() + 100));
        controlFrame.setVisible(true);
        controlFrame.toFront();

        try {
            Thread.sleep(20);
            controlFrame.pack();
            controlFrame.setLocation(controlFrame.getX(), screenHeight - (controlFrame.getHeight() + 100));
            synchronized (lock) {
                lock.wait();
            }
        } catch (final InterruptedException e) {
            result = ScreenCheckReview.Result.IGNORE;
        }
        controlFrame.setVisible(false);
        return result;
    }

    private Path getClosestFailure(final ArcticDiffImages diffImages) {
        if (reviewOrder == PixelCheck.Type.UNKNOWN) {
            log.debug("{} order is set to UNKNOWN", InjectionKeys.GUI_REVIEW_ORDER);
            return diffImages.getFailureId().getSavedImagePath();
        }
        try {
            Path current = diffImages.getFailureId().getSavedImagePath();
            long failures = ((Number) diffImages.getProperties(current)
                    .get(ArcticDiffProperty.keyOf(reviewOrder, "failed")).getValue()).longValue();
            for (Path alternative : diffImages.getSavedImagePaths()) {
                try {
                    long altFailures = ((Number) diffImages.getProperties(alternative)
                            .get(ArcticDiffProperty.keyOf(reviewOrder, "failed")).getValue()).longValue();
                    if (altFailures < failures) {
                        current = alternative;
                        failures = altFailures;
                    }
                } catch (Exception e) {
                    log.debug("Unable to get failures for alternative {} type {}", alternative, reviewOrder, e);
                }
            }
            return current;
        } catch (Exception e) {
            log.debug("Unable to get closest failure based on type {}", reviewOrder, e);
            return diffImages.getFailureId().getSavedImagePath();
        }
    }

    private void doReturn(final ScreenCheckReview.Result resultToReturn) {
        synchronized (lock) {
            this.result = resultToReturn;
            lock.notifyAll();
        }
        controlFrame.setVisible(false);
    }



    private void buildUI() {
        imgPanel = buildImagePanel();
        controlFrame = buildControlFrame(imgPanel);
    }

    private JLayeredPane buildImagePanel() {
        imgPanel = new JLayeredPane();
        imgPanel.setLayout(new OverlayLayout(imgPanel));
        imgPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return imgPanel;
    }

    private JFrame buildControlFrame(final JLayeredPane destinationImgPanel) {
        final JFrame frame = new JFrame();
        frame.setResizable(true);
        final JPanel mainPanel = new JPanel();
        frame.add(mainPanel);
        mainPanel.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        mainPanel.add(destinationImgPanel, c);

        final JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;

        mainPanel.add(radioPanel, c);

        final JPanel chkPanel = new JPanel();
        chkPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        c.gridx = 0;
        c.gridy = 2;
        mainPanel.add(chkPanel, c);
        final JLabel imgLabel = new JLabel();
        imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Arrays.stream(PixelCheck.Type.values())
                .filter(PixelCheck.Type::isImage)
                .forEach(it -> imgControls[it.getOrder()] = new ImgControl(it, imgLabel, chkPanel, radioPanel,
                        components, this::validateUI));

        final JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        c.gridx = 1;
        c.gridy = 1;
        mainPanel.add(hintPanel, c);
        Arrays.stream(PixelCheck.Type.values())
                .filter(PixelCheck.Type::isHint)
                .forEach(it -> new HintControl(it, destinationImgPanel, hintPanel, components));
        destinationImgPanel.add(imgLabel, JLayeredPane.DEFAULT_LAYER);


        c.gridx = 2;
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        buildSpinnerPanel(mainPanel, c);

        c.gridx = 3;
        c.gridy = 1;
        buildLogAlternativesPanel(mainPanel, c, frame);

        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        buildPlaybackPanel(mainPanel, c);

        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        buildDecisionPanel(mainPanel, c);

        frame.pack();

        return frame;
    }

    private void buildSpinnerPanel(final JPanel mainPanel, final GridBagConstraints c) {
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(3000, 0, 10000, 50));
        timerControl = new TimerControl(components);
        spinner.addChangeListener(e -> timerControl.setDelay((Integer) spinner.getValue()));

        final JPanel spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        spinnerPanel.add(spinner);
        mainPanel.add(spinnerPanel, c);

    }

    private void buildLogAlternativesPanel(final JPanel mainPanel, final GridBagConstraints c,
                                           final JFrame parentFrame) {
        final JPanel logAlternativePanel = new JPanel();
        logAlternativePanel.setLayout(new BoxLayout(logAlternativePanel, BoxLayout.X_AXIS));

        new PropertiesControl(logAlternativePanel, parentFrame, components);
        logAlternativePanel.add(Box.createHorizontalGlue());
        alternativeControl = new AlternativeControl(logAlternativePanel, components);


        mainPanel.add(logAlternativePanel, c);
    }

    private void buildPlaybackPanel(final JPanel mainPanel, final GridBagConstraints c) {
        final JButton prevButton = new JButton("◀◀");
        final JButton playButton = new JButton("▶");
        final JButton nextButton = new JButton("▶▶");

        timerControl.wireButton(it -> next(), playButton);
        prevButton.addActionListener(e -> {
            timerControl.restart();
            previous();
        });
        playButton.addActionListener(e -> timerControl.changeState());
        nextButton.addActionListener(e -> {
            timerControl.restart();
            next();
        });

        final JPanel buttonPlaybackPanel = new JPanel();
        buttonPlaybackPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        buttonPlaybackPanel.add(prevButton);
        buttonPlaybackPanel.add(playButton);
        buttonPlaybackPanel.add(nextButton);

        mainPanel.add(buttonPlaybackPanel, c);
    }

    private void buildDecisionPanel(final JPanel mainPanel, final GridBagConstraints c) {
        final JButton abortButton = new JButton("Abort");
        final JButton rejectButton = new JButton("Reject");
        final JButton ignoreButton = new JButton("Ignore");
        final JButton acceptButton = new JButton("Accept");

        abortButton.addActionListener(a -> doReturn(Result.ABORT));
        rejectButton.addActionListener(a -> doReturn(Result.REJECT));
        ignoreButton.addActionListener(a -> doReturn(Result.IGNORE));
        acceptButton.addActionListener(a -> doReturn(Result.ACCEPT));

        final JPanel decisionPanel = new JPanel();
        decisionPanel.setLayout(new BoxLayout(decisionPanel, BoxLayout.X_AXIS));
        decisionPanel.add(abortButton);
        decisionPanel.add(Box.createHorizontalGlue());
        decisionPanel.add(rejectButton);
        decisionPanel.add(ignoreButton);
        decisionPanel.add(acceptButton);

        mainPanel.add(decisionPanel, c);
    }

    private synchronized void validateUI() {
        selected = (int) Arrays.stream(imgControls).filter(ImgControl::isSelected).count();
        if (position == -1 || !imgControls[position].isSelected()) {
            next();
        }
    }

    private synchronized void next() {
        selected = (int) Arrays.stream(imgControls).filter(ImgControl::isSelected).count();
        if (selected != 0 && (selected != 1 || !imgControls[position].isSelected())) {
            boolean found = false;
            final int oldPos = position;
            for (int i = 0; i < imgControls.length; i++) {
                final int x = (oldPos + (i + 1)) % imgControls.length;
                if (imgControls[x].isSelected() && !found) {
                    position = x;
                    imgControls[x].display(true);
                    found = true;
                } else {
                    imgControls[x].display(false);
                }
            }
        }
        controlFrame.setTitle(imgControls[position].type.getName());
    }

    private synchronized void previous() {
        selected = (int) Arrays.stream(imgControls).filter(ImgControl::isSelected).count();
        if (selected != 0 && (selected != 1 || !imgControls[position].isSelected())) {
            boolean found = false;
            final int oldPos = position;
            for (int i = 0; i < imgControls.length; i++) {
                final int x = (oldPos + imgControls.length - (i + 1)) % imgControls.length;
                if (imgControls[x].isSelected() && !found) {
                    position = x;
                    imgControls[x].display(true);
                    found = true;
                } else {
                    imgControls[x].display(false);
                }
            }
        }
        controlFrame.setTitle(imgControls[position].type.getName());
    }

    private interface UpdatableComponent {
        void update(Path alternative, ArcticDiffImages diffImages);
        void repaint();
    }

    private static final class ImgControl implements UpdatableComponent {
        private final PixelCheck.Type type;
        private final JLabel imgLabel;
        private final JCheckBox checkBox;
        private final JRadioButton radioButton;
        private boolean enabled;
        private ImageIcon imageIcon;

        ImgControl(final PixelCheck.Type type, final JLabel imgLabel, final JPanel checkPanel,
                          final JPanel radioPanel, final List<UpdatableComponent> components,
                          final Runnable validateAction) {
            this.type = type;
            this.imgLabel = imgLabel;
            checkBox = new JCheckBox();
            checkBox.setToolTipText(type.getName());
            checkBox.setSelected(PixelCheck.Type.CURRENT == type || PixelCheck.Type.RECORDED == type);
            checkBox.addActionListener(it -> validateAction.run());
            radioButton = new JRadioButton();
            radioButton.setEnabled(false);
            radioButton.setToolTipText(type.getName());
            checkPanel.add(checkBox);
            radioPanel.add(radioButton);
            components.add(this);
        }

        boolean isSelected() {
            return checkBox.isSelected();
        }

        void display(final boolean shouldDisplay) {
            radioButton.setSelected(shouldDisplay && enabled);
            if (shouldDisplay) {
                imgLabel.setIcon(imageIcon);
            }
        }

        @Override
        public void update(final Path alternative, final ArcticDiffImages diffImages) {
            this.enabled = diffImages.getImages(alternative).containsKey(type);
            checkBox.setEnabled(enabled);
            if (!enabled) {
                checkBox.setSelected(false);
            } else {
                imageIcon = new ImageIcon(diffImages.getImages(alternative).get(type));
                if (radioButton.isSelected()) {
                    imgLabel.setIcon(imageIcon);
                }
            }

        }

        @Override
        public void repaint() {
            imgLabel.repaint();
        }
    }

    private static final class HintControl implements UpdatableComponent {
        private final PixelCheck.Type type;
        private final JCheckBox checkBox;
        private final JLabel hintLabel;
        private final List<UpdatableComponent> components;

        HintControl(final PixelCheck.Type type, final JLayeredPane imgPanel, final JPanel checkboxPanel,
                           final List<UpdatableComponent> components) {
            this.type = type;
            this.components = components;
            checkBox = new JCheckBox();
            checkBox.setToolTipText(type.getName());
            checkBox.setSelected(type.equals(PixelCheck.Type.FUZZY_HINT));
            checkBox.addActionListener(it -> display());
            checkboxPanel.add(checkBox);

            hintLabel = new JLabel();
            hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            imgPanel.add(hintLabel, JLayeredPane.PALETTE_LAYER);

            components.add(this);
        }

        @Override
        public void update(final Path alternative, final ArcticDiffImages diffImages) {
            final boolean enabled = diffImages.getHints(alternative).containsKey(type);
            checkBox.setEnabled(enabled);
            if (!enabled) {
                checkBox.setSelected(false);
            } else {
                hintLabel.setIcon(new ImageIcon(diffImages.getHints(alternative).get(type)));
            }
            hintLabel.setVisible(checkBox.isSelected());
        }

        @Override
        public void repaint() {
            hintLabel.repaint();
        }

        public void display() {
            hintLabel.setVisible(checkBox.isSelected());
        }


    }

    private static final class TimerControl implements UpdatableComponent {
        private static final int DEFAULT_REFRESH_TIMER = 3000;
        private Timer timer;
        private JButton playButton;

        private TimerControl(final List<UpdatableComponent> components) {
            components.add(this);
        }

        private void wireButton(final ActionListener action, final JButton buttonToWire) {
            timer = new Timer(DEFAULT_REFRESH_TIMER, action);
            this.playButton = buttonToWire;
        }

        private void changeState() {
            changeState(!timer.isRunning());
        }

        void changeState(final boolean state) {
            if (state) {
                if (!timer.isRunning()) {
                    timer.start();
                }
                playButton.setText("||");
                playButton.setToolTipText("Pause reel");
            } else {
                if (timer.isRunning()) {
                    timer.stop();
                }
                playButton.setText("▶");
                playButton.setToolTipText("Play reel");
            }
        }

        public void restart() {
            if (timer.isRunning()) {
                timer.restart();
            }
        }

        public void setDelay(final int delay) {
            timer.setDelay(delay);
        }

        @Override
        public void update(final Path alternative, final ArcticDiffImages diffImages) {

        }

        @Override
        public void repaint() {

        }
    }

    private static final class PropertiesControl implements UpdatableComponent {
        private final JDialog dialog;
        private final JTextArea textArea;
        private final JFrame controlFrame;

        PropertiesControl(final JPanel buttonPanel, final JFrame controlFrame,
                          final List<UpdatableComponent> components) {
            this.controlFrame = controlFrame;
            dialog = new JDialog(controlFrame, "Properties", false);
            final JPanel dialogPanel = new JPanel();
            final JButton logButton = new JButton("Properties");

            textArea = new JTextArea();
            textArea.setMargin(new Insets(5, 5, 5, 5));
            textArea.setOpaque(false);
            textArea.setEditable(false);

            dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            dialog.setVisible(false);
            dialog.setContentPane(dialogPanel);
            dialog.setResizable(false);
            dialogPanel.add(textArea);

            logButton.addActionListener(it -> this.display());

            buttonPanel.add(logButton);
            components.add(this);
        }

        @Override
        public void update(final Path alternative, final ArcticDiffImages diffImages) {
            StringBuilder sb = new StringBuilder();
            diffImages.getGlobalProperties().values().forEach(it -> sb.append(it).append(System.lineSeparator()));
            diffImages.getProperties(alternative).values().forEach(it -> sb.append(it).append(System.lineSeparator()));
            diffImages.getLog(alternative).forEach(it -> sb.append(it).append(System.lineSeparator()));
            textArea.setText(sb.toString());
            dialog.pack();
        }

        @Override
        public void repaint() {

        }

        private void display() {
            if (!dialog.isVisible()) {
                dialog.setLocation(controlFrame.getX() + controlFrame.getWidth(), controlFrame.getY());
            }
            dialog.setVisible(!dialog.isVisible());
        }
    }

    /**
     * This component controls a combobox that allows the selection of which alternative should the image that was
     * captured during playback has to be compared with. Changing the selection in the combobox updates the other
     * components.
     */
    private static final class AlternativeControl implements UpdatableComponent {
        private final List<UpdatableComponent> components;
        private final JComboBox<ComboBoxElement> cbx;
        private ArcticDiffImages diffImages;
        private List<ComboBoxElement> elements;
        private Path currentAlternative;
        private final DefaultComboBoxModel<ComboBoxElement> model = new DefaultComboBoxModel<>();

        AlternativeControl(final JPanel panel, final List<UpdatableComponent> components) {
            cbx = new JComboBox<>();
            ((JLabel) cbx.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
            cbx.setPrototypeDisplayValue(new ComboBoxElement(Path.of("sample/sample.png")));
            panel.add(cbx);
            cbx.setModel(model);
            this.components = components;
            cbx.addActionListener(this::changed);
        }

        /**
         * ActionListener for the combobox. When the selection change, update all other components to use a different
         * alternative.
         * @param ae the ActionEven that triggered the call.
         */
        private void changed(final ActionEvent ae) {
            ComboBoxElement selectedItem = (ComboBoxElement) cbx.getSelectedItem();
            if (selectedItem != null && selectedItem.alternative != null) {
                Path alternative = selectedItem.alternative;
                components.forEach(it -> it.update(alternative, diffImages));
            }
        }

        @Override
        public void update(final Path alternative, final ArcticDiffImages updatedDiffImages) {
            boolean forceUpdate = false;
            if (updatedDiffImages != null && !updatedDiffImages.equals(diffImages)) {
                this.diffImages = updatedDiffImages;
                model.removeAllElements();
                elements = updatedDiffImages.getSavedImagePaths().stream()
                        .map(ComboBoxElement::new)
                        .collect(Collectors.toList());
                model.addAll(elements);
                forceUpdate = true;
            }
            if (forceUpdate || !(alternative.equals(this.currentAlternative))) {
                this.currentAlternative = alternative;
                elements.stream()
                        .filter(it -> it.alternative.equals(alternative))
                        .findAny()
                        .ifPresent(model::setSelectedItem);
            }
         }

        @Override
        public void repaint() {

        }

        private static final class ComboBoxElement {
            private final Path alternative;
            private final String text;

            ComboBoxElement(final Path alternative) {
                this.alternative = alternative;
                text = alternative.getFileName().toString();
            }

            @Override
            public String toString() {
                return text;
            }
        }
    }

    /**
     * Represents the different choices the user has in the UI.
     */
    public enum Result {
        /**
         * Add the current image as a valid alternative. Next time the test is run, it will be compared against this
         * image too.
         */
        ACCEPT,

        /**
         * Do nothing. Move the image to the back of the review queue
         */
        IGNORE,

        /**
         * Image is not a valid alternative. Remove from the list of pending reviews.
         */
        REJECT,

        /**
         * Ignore this review and all the reviews pending in the queue.
         */
        ABORT
    }
}


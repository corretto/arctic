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
package com.amazon.corretto.arctic.demo;
import com.amazon.corretto.arctic.common.BaseMain;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main extends BaseMain {

    public static void main(final String[] args) {
        //Creating the Frame
        JFrame frame = new JFrame("This is an example application");
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int height = screenSize.height;
        int width = screenSize.width;
        frame.setSize(width/4, height/4);
        frame.setLocationRelativeTo(null);


        //Creating the panel at bottom and adding components
        JTextField textBox = new JTextField();
        JPanel panel = new JPanel();
        JButton button1 = new JButton("Button 1");
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.add(textBox);
                textBox.setText("Button 1 was clicked");
                frame.getContentPane().add(BorderLayout.CENTER,textBox);
                frame.setVisible(true);
            }
        });
        JButton button2 = new JButton("Button 2");
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.add(textBox);
                textBox.setText("Button 2 was clicked");
                frame.getContentPane().add(BorderLayout.CENTER,textBox);
                frame.setVisible(true);
            }
        });
        JButton button3 = new JButton("Button 3");
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.add(textBox);
                textBox.setText("Button 3 was clicked");
                frame.getContentPane().add(BorderLayout.CENTER,textBox);
                frame.setVisible(true);
            }
        });
        panel.add(button1);
        panel.add(button2);
        panel.add(button3);

        JButton exit = new JButton("Exit");
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            frame.dispose();
            }
        });


        //Adding Components to the frame.
        frame.getContentPane().add(BorderLayout.NORTH, panel);
        frame.getContentPane().add(BorderLayout.SOUTH, exit);
        frame.setVisible(true);
    }
}

//
// Copyright 2012 Jeff Bush
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

class TTYEmulator extends JPanel
{
	public TTYEmulator()
	{
		super(new BorderLayout());

		fConversationView = new JTextPane();
		fConversationView.setEditable(false);
		StyledDocument doc = fConversationView.getStyledDocument();
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		Style regular = doc.addStyle("regular", def);
		StyleConstants.setFontFamily(regular, "Serif");
		Style me = doc.addStyle("me", regular);
		StyleConstants.setBold(me, true);
		StyleConstants.setForeground(me, Color.DARK_GRAY);
		Style them = doc.addStyle("them", regular);

		JScrollPane conversationScroll = new JScrollPane(fConversationView);
		add(conversationScroll, BorderLayout.CENTER);

		fInputField = new JTextField(15);
		fInputField.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					handleTextInput();
				}
			});

		fWriteToFileButton = new JButton("Export to WAV");
		fWriteToFileButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String newFileStr = "";
				File oldFile = new File("TTYWavOutput.wav");
				File newFile;

				newFileStr = JOptionPane.showInputDialog(null, "Enter name for file", "Export to WAV", JOptionPane.INFORMATION_MESSAGE);

				if (newFileStr != null)
					newFile = new File(newFileStr + ".wav");
				else
				{
					JOptionPane.showMessageDialog(null, "Export cancelled!", "Failure!", JOptionPane.ERROR_MESSAGE);
					return;
				}

				if (oldFile.renameTo(newFile))
					JOptionPane.showMessageDialog(null, "Export successful", "Success!", JOptionPane.INFORMATION_MESSAGE);
				else
					JOptionPane.showMessageDialog(null, "There was a problem!", "Failure!", JOptionPane.ERROR_MESSAGE);

				fOutput.resetWriter();
			}
		});

		fInputPanel = new JPanel(new FlowLayout());
		fInputPanel.add(fInputField);
		fInputPanel.add(fWriteToFileButton);
		add(fInputPanel, BorderLayout.SOUTH);
		setPreferredSize(new Dimension(400,300));

		fOutput = new TTYOutput();
		fInput = new TTYInput();

		fInput.setListener(new TTYInput.TTYInputListener() {
			public void handleCode(char ch) {
				addReceivedCharacter(ch);
			}
		});

		fOutput.setListener(new TTYOutput.TTYOutputListener() {
			public void ttyIsSending(boolean isSending) {
				// If we are actively sending, disable our receiver so
				// we don't echo characters.
				fInput.setIgnoreInput(isSending);
			}
		});
	}

	void handleTextInput()
	{
		String input = fInputField.getText() + "\n";
		fInputField.setText("");

		try
		{
			StyledDocument doc = fConversationView.getStyledDocument();
			if (fUnterminatedInputLine)
			{
				// If the remote user was in the middle of a line, add a
				// line break here so the conversations don't get mixed.
				doc.insertString(doc.getLength(), "\n", doc.getStyle("them"));
			}

			doc.insertString(doc.getLength(), input, doc.getStyle("me"));
		}
		catch (Exception exc)
		{
			System.out.println(exc);
		}

		fOutput.enqueueString(input);
	}

	void addReceivedCharacter(char ch)
	{
		fUnterminatedInputLine = (ch != '\n' && ch != '\r');

		try
		{
			StyledDocument doc = fConversationView.getStyledDocument();
			doc.insertString(doc.getLength(), "" + ch, doc.getStyle("them"));
		}
		catch (Exception exc)
		{
			System.out.println(exc);
		}
	}

	private static void createAndShowGUI()
	{
		JFrame frame = new JFrame("TTY Emulator");
		frame.addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowClosing(WindowEvent e)
      {
				new File("TTYWavOutput.wav").delete();
        e.getWindow().dispose();
      }
    });

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		TTYEmulator emulator = new TTYEmulator();
		frame.getContentPane().add(emulator);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args)
	{
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	private boolean fUnterminatedInputLine = false;
	private TTYOutput fOutput;
	private TTYInput fInput;
	private JPanel fInputPanel;
	private JButton fWriteToFileButton;
	private JTextPane fConversationView;
	private JTextField fInputField;
}

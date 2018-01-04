package ravn.networkbroadcasttestawt;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import ravn.networkbroadcastshared.BroadcastTestImpl;
import ravn.networkbroadcastshared.SocketUtils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

public class BroadcastTest {
	static BroadcastTestImpl broadcastTest = new BroadcastTestImpl();
	static boolean changeCheckboxColor = false;
	static {
        BroadcastTestImpl.messageCall = new BroadcastTestImpl.MessageCallback() {
            @Override
            public void call(String msg) {
                //Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            		System.out.println("BroadcastTest: " + msg);
            }
            @Override
            public void addTextLineToLog(String txt){
            		textPane.setText(textPane.getText() + System.lineSeparator() + txt);
            		SwingUtilities.invokeLater(new Runnable() {
            			public void run() {
                    		JScrollBar vertical = scrollPane.getVerticalScrollBar();
                    		vertical.setValue( vertical.getMaximum() );
            			}
            		});
            }
            @Override
            public void udpConnected(boolean con) {
            		connectedUDPCheckbox.setSelected(con);
            		if (changeCheckboxColor)
            			connectedUDPCheckbox.setForeground(con ? Color.BLACK : Color.RED);
            		udpButton.setEnabled(con);
            		if (!con)
            			udpCheckbox.setSelected(false);
            		udpCheckbox.setEnabled(con);
            }
            @Override
            public void multicastConnected(boolean con) {
            		connectedMulticastCheckbox.setSelected(con);
            		if (changeCheckboxColor)
            			connectedMulticastCheckbox.setForeground(con ? Color.BLACK : Color.RED);
            		multicastButton.setEnabled(con);
            		if (!con)
            			multicastCheckbox.setSelected(false);
            		multicastCheckbox.setEnabled(con);

            }
        };
        broadcastTest.init();
	}
	public BroadcastTest() {
	}
	static JTextField computerNumberField = null;
	static JButton multicastButton = null, udpButton = null;
	static JCheckBox multicastCheckbox = null, udpCheckbox = null;
	static JCheckBox connectedMulticastCheckbox = null, connectedUDPCheckbox = null;
	static JButton clearButton = null, resetButton = null;
	static JScrollPane scrollPane = null;
	static JTextPane textPane = null;
	private static void createAndShowGUI() {
		System.out.println("BroadcastTest.main() called");
		JFrame frame = new JFrame("Broadcast Window Testing");
		JComponent newContentPane = new JComponent() {};
		BoxLayout boxLayout = new BoxLayout(newContentPane, BoxLayout.Y_AXIS);
		newContentPane.setLayout(boxLayout);

		JComponent row1 = new JComponent() {};
		row1.setLayout(new FlowLayout());
		row1.add(new JLabel("Computer #:"));
		row1.add(computerNumberField = new JTextField(20));
		computerNumberField.setEditable(false);
		computerNumberField.setText(String.valueOf(broadcastTest.computerID));
		JComponent borderComponent = new JComponent() {};
		borderComponent.setLayout(new BorderLayout());
		
		JComponent row2 = new JComponent() {};
		row2.setLayout(new FlowLayout());
		row2.add(new JLabel("Connected:"));
		row2.add(connectedMulticastCheckbox = new JCheckBox("Multicast"));
		row2.add(new JComponent() { } );
		row2.add(connectedUDPCheckbox = new JCheckBox("UDP"));
		connectedMulticastCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
				boolean con = false;
				if (broadcastTest!=null) {
					con = broadcastTest.multicastIsConnected;
				}
				if (con != isSelected) {
					connectedMulticastCheckbox.setSelected(con);
				}
			}
		});
		connectedUDPCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
				boolean con = false;
				if (broadcastTest!=null) {
					con = broadcastTest.udpIsConnected;
				}
				if (con != isSelected) {
					connectedUDPCheckbox.setSelected(con);
				}
			}
		});
		
		JComponent row3 = new JComponent() {};
		row3.setLayout(new FlowLayout());
		row3.add(new JLabel("Send:"));
		row3.add(multicastButton = new JButton("MULTICAST"));
		row3.add(multicastCheckbox = new JCheckBox());
		row3.add(new JComponent() { } );
		row3.add(udpButton = new JButton("UDP"));
		row3.add(udpCheckbox = new JCheckBox());
		
		scrollPane = new JScrollPane();
		textPane = new JTextPane();
		textPane.setFont(new Font("Courier New", 0, 20));
		textPane.setEditable(false);

		scrollPane.setViewportView(textPane);
		
		JComponent bottomRow = new JComponent() {};
		bottomRow.setLayout(new FlowLayout());
		bottomRow.add(clearButton = new JButton("CLEAR"));
		bottomRow.add(new JComponent() { } );
		bottomRow.add(resetButton = new JButton("RESET"));
		
		newContentPane.add(row1);

		JComponent topRow = new JComponent() {};
		topRow.setLayout(new BorderLayout());
		topRow.add(row2, BorderLayout.NORTH);
		topRow.add(row3, BorderLayout.SOUTH);
		borderComponent.add(topRow, BorderLayout.NORTH);
		borderComponent.add(scrollPane, BorderLayout.CENTER);
		borderComponent.add(bottomRow, BorderLayout.SOUTH);
		newContentPane.add(borderComponent);
		frame.setContentPane(newContentPane);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		multicastButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
                Thread newT = new Thread(){
                    public void run(){
                        broadcastTest.sendMulticastMessage();
                    }
                };
                newT.start();
			}
		});
		udpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
                Thread newT = new Thread(){
                    public void run(){
                        broadcastTest.sendUDPMessage(SocketUtils.getBroadcastAddress());
                    }
                };
                newT.start();
			}
		});
		multicastCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {

				broadcastTest.repeatMulticastSendThreadSet(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		udpCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				System.out.println("e: stateChange=" + e.getStateChange() + " ItemEvent.SELECTED=" + ItemEvent.SELECTED);
				broadcastTest.repeatUDPSendThreadSet(SocketUtils.getBroadcastAddress(), e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textPane.setText("");
			}
		});
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				broadcastTest.setupListeningThreads();
			}
		});
		BroadcastTestImpl.messageCall.udpConnected(false);
		BroadcastTestImpl.messageCall.multicastConnected(false);
		changeCheckboxColor = true;
		Thread thread = new Thread(){
			public void run(){
				try {
					Thread.currentThread().sleep(2000l);
				} catch (Exception ex) {}
				System.out.println("Calling broadcastTest.setupListeningThreads();");
				broadcastTest.setupListeningThreads();
			}
		};
		thread.start();

		frame.setSize(500,500);
		frame.setVisible(true);
	}
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
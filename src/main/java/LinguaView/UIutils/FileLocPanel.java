package LinguaView.UIutils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class FileLocPanel extends JPanel
{
	private JTextField LocTextComponent = new JTextField(20);
	private JButton browseButton = new JButton("Browse...");
	
	public FileLocPanel() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				BrowseFile();
			}
		});
		add(LocTextComponent);
		add(browseButton);
	}
	
	public void BrowseFile() {
		String filename = Utils.fileSelection(true);
		if(filename != null) {
			LocTextComponent.setText(filename);
		}
	}
	
	public String getLoc() {
		return LocTextComponent.getText();
	}
}
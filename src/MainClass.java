import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class MainClass {
	public static void main(String[] args) {
		MainCanvas meuCanvas = new MainCanvas();
		
		JFrame f = new JFrame();
		f.setSize(640, 480);
		f.getContentPane().add(meuCanvas); // adiciona antes de mostrar a janela
		f.setVisible(true);
		meuCanvas.requestFocusInWindow(); // teclas vao direto para o painel

	
		f.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent e) {
		        System.exit(0);
		    }
		});
		
		meuCanvas.start();
	}
}

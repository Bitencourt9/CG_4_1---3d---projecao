package core3d;

import java.awt.Graphics2D;

public class Triangulo3D {
	Ponto3D pa;
	Ponto3D pb;
	Ponto3D pc;
	public Triangulo3D(Ponto3D a, Ponto3D b, Ponto3D c) {
		super();
		this.pa = new Ponto3D(a);
		this.pb = new Ponto3D(b);
		this.pc = new Ponto3D(c);
	}
	
	public void desenhase(Graphics2D dbg) {
		
		dbg.drawLine((int)pa.x,(int)pa.y,(int)pb.x,(int)pb.y);
		dbg.drawLine((int)pb.x,(int)pb.y,(int)pc.x,(int)pc.y);
		dbg.drawLine((int)pc.x,(int)pc.y,(int)pa.x,(int)pa.y);
	}
	
	public void desenhase(Graphics2D dbg, Mat4x4 modelview,Mat4x4 projection) {
		Ponto3D pa1 = modelview.multiplicaPonto(pa);
		Ponto3D pb1 = modelview.multiplicaPonto(pb);
		Ponto3D pc1 = modelview.multiplicaPonto(pc);
		
		desenhaAresta(dbg, projection, pa1, pb1);
		desenhaAresta(dbg, projection, pb1, pc1);
		desenhaAresta(dbg, projection, pc1, pa1);
	}

	// Menor w aceito antes da divisao. Na perspectiva, w = (z+d)/d, entao
	// w <= 0 significa ponto atras do centro de projecao (sairia invertido na tela)
	static final float W_MINIMO = 0.1f;

	private void desenhaAresta(Graphics2D dbg, Mat4x4 projection, Ponto3D a, Ponto3D b) {
		float wa = projection.calculaW(a);
		float wb = projection.calculaW(b);

		if (wa < W_MINIMO && wb < W_MINIMO) {
			return; // aresta inteira atras do centro de projecao
		}
		// recorta a parte da aresta que fica atras do centro de projecao
		if (wa < W_MINIMO) {
			a = interpola(a, b, (W_MINIMO - wa) / (wb - wa));
		} else if (wb < W_MINIMO) {
			b = interpola(b, a, (W_MINIMO - wb) / (wa - wb));
		}

		Ponto3D a2 = projection.multiplicaPonto(a);
		Ponto3D b2 = projection.multiplicaPonto(b);

		dbg.drawLine((int)a2.x,(int)a2.y,(int)b2.x,(int)b2.y);
	}

	private Ponto3D interpola(Ponto3D a, Ponto3D b, float t) {
		return new Ponto3D(a.x + (b.x - a.x)*t,
				a.y + (b.y - a.y)*t,
				a.z + (b.z - a.z)*t,
				a.w + (b.w - a.w)*t);
	}
	
	public void translacao(float a,float b, float c) {
		Mat4x4 m = new Mat4x4();
		m.setTranslate(a, b,c);
		pa.multiplicaMat(m);
		pb.multiplicaMat(m);
		pc.multiplicaMat(m);
	}
	public void escala(float a,float b,float c) {
		Mat4x4 m = new Mat4x4();
		m.setSacale(a, b, c);
		pa.multiplicaMat(m);
		pb.multiplicaMat(m);
		pc.multiplicaMat(m);
	}	
	
	public void rotacao(float ang) {
		Mat4x4 m = new Mat4x4();
		m.setRotateY(ang);
		//System.out.println("rot Y");
		pa.multiplicaMat(m);
		pb.multiplicaMat(m);
		pc.multiplicaMat(m);
	}
	public void rotacao(Mat4x4 m) {
		//System.out.println("rot Y");
		pa.multiplicaMat(m);
		pb.multiplicaMat(m);
		pc.multiplicaMat(m);
	}
}

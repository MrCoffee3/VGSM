package newRepartition;

import java.util.HashSet;
import java.util.Set;

public class Vertex {
	public int id;
	public int pre;
	public int belong;
	public int belong0;
	//public int[] dv;
	public Set<Vertex> neibor;
	public int gain = 0;
	public int target;
	//public boolean inQ = false;
	public boolean inG=false;
	public int checked=0;
	public Vertex(int id, int belong) {
		this.id = id;
		this.belong = belong;
		this.belong0 = belong;
		this.neibor = new HashSet<Vertex>();
	}

}

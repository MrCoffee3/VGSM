package newRepartition;

import java.util.HashSet;
import java.util.Set;

public class Partition {
	public int id;
	public int w = 0;
	public Set<Vertex> vertexs;
	public Partition(int id) {
		this.id = id;
		vertexs = new HashSet<Vertex>();
	}

	public void addVertex(Vertex v) {
		this.vertexs.add(v);
		this.w += 1;
	}
	

	public void removeVertex(Vertex v) {
		this.vertexs.remove(v);
		this.w--;
	}

	
}

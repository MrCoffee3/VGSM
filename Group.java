package newRepartition;

import java.util.HashSet;
import java.util.Set;

public class Group {
	Set<Vertex> vertexs;
	int belong;
	int target;
	int[] dv;
	public int gain=0;
	
	public Group(int belong) {
		// TODO Auto-generated constructor stub
		 this.belong=belong;
		 this.vertexs=new HashSet<Vertex>();
	}
}

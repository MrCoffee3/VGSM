package newRepartition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CalMetis {

	int[] partitions;
	public List<Vertex> vertexList;
	public int pNum;
	public int vertexNum;
	public int edgeNum;
	public int capacity;
	public float balance;

	public CalMetis(int pNum, float balance, String vertexPath, String partitionPath) throws IOException {
		this.vertexNum = 0;
		this.edgeNum = 0;
		this.pNum = pNum;
		this.vertexList = new ArrayList<Vertex>();
		this.balance = balance;
		this.capacity = 0;
		this.partitions=new int[pNum];
		load(vertexPath, partitionPath);
	}
	
	public void load(String vertexPath, String partitionPath) throws IOException {
		BufferedReader in1 = new BufferedReader(new FileReader(new File(vertexPath)));// 点
		BufferedReader in2 = new BufferedReader(new FileReader(new File(partitionPath)));// 分区
		String str1;
		int id = 0;
		while ((str1 =in2.readLine()) != null) {
			id++;
			int belong=Integer.parseInt(str1);
			Vertex v = new Vertex(id, belong);
			partitions[belong]++;
			vertexList.add(v);
		}
		vertexNum = id;
		int id2 = 0;
		while ((str1 = in1.readLine()) != null) {
			Vertex vertex = vertexList.get(id2);
			// vertex.dv = new int[pnum];
			// vertex.virtualdv = new int[pnum];
			if (str1.length() != 0) {
				String[] neibors = str1.split(" ");
				int len = neibors.length;
				edgeNum += len;
				for (int i = 0; i < len; i++) {
					int neiborid = Integer.parseInt(neibors[i]);
					Vertex nei = vertexList.get(neiborid - 1);
					vertex.neibor.add(nei) ;
					// vertex.dv[nei.belong]++;
					// vertex.virtualdv[nei.belong]++;
				}
			} else {

			}
			id2++;
		}
		in2.close();
		in1.close();
		edgeNum /= 2;
		capacity = (int) (balance * (vertexNum / pNum));
	}
	
	public void calec() {
		int ec = 0;
		for (Vertex vertex : vertexList) {
			for (Vertex neibor : vertex.neibor) {
				if(neibor.belong!=vertex.belong) {
					ec++;
				}
			}
		}
		System.out.println("ec:" + (ec / 2) + "  ratio:" + ((float) (ec / 2) / (float) edgeNum) * 100 + "%");
	}

	public void output() {
		float[] bal = new float[pNum];
		System.out.println("vertexnum:" + vertexNum);
		System.out.println("edgenum:" + edgeNum);
		System.out.println("avg:" + (float) vertexNum / (float) pNum);
		System.out.println("min:" + ((float) vertexNum / (float) pNum) * (2 - balance));
		System.out.println("max:" + ((float) vertexNum / (float) pNum) * balance);
		float avg = (float) vertexNum / (float) pNum;
		for (int i = 0; i < pNum; i++) {
			float b = (float) (partitions[i] * 1.0 / avg);
			bal[i] = b;
			System.out.println(i + "   w:" + partitions[i] + "  bal:" + b);
		}
		float max = 0;
		for (int i = 0; i < pNum; i++) {
			if (bal[i] > max) {
				max = bal[i];
			}
		}
		calec();
		int lb=0;
		for (int i = 0; i < pNum; i++) {
			//System.out.println(partitions[i]);
			lb += Math.abs(partitions[i] - vertexNum * 1.0 / pNum);
		}
		System.out.println("lb:"+lb);
		System.out.println("maxBal:" + max);
	}
	
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String[] datas = { "dblp", "roadNet-PA", "youtube", "lj", "sp", "orkut","power" };
		int[] ps = { 2, 4, 8, 16 };
		for (String string : datas) {
			String data = string;
			for (int i : ps) {
				int pNum = i;
				float balance = 1.1f;// 平衡系数
				String inputVertexPath = "D:\\DGR-VE-dataset\\" + data + "2.txt";
				String inputPartitionPath = "D:\\DGR-VE-dataset\\Metis1.1\\" + data + "2.txt.part." + pNum;
				CalMetis repartition = new CalMetis(pNum, balance, inputVertexPath, inputPartitionPath);
				System.out.println("+++++++++++++++++"+data + ":" + pNum+"+++++++++++++++++");
				repartition.output();
			}
		}
	}


}

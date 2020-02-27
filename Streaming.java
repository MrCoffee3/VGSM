package newRepartition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Streaming {
	int[] partitions;
	public List<Vertex> vertexList;
	public List<Integer> vids;
	public int pNum;
	public int vertexNum;
	public int edgeNum;
	public int capacity;
	public float balance;
	public Random random;
	public String data;

	public Streaming(String data, int pNum, float balance, String vertexPath) throws IOException {
		this.data=data;
		this.vertexNum = 0;
		this.edgeNum = 0;
		this.pNum = pNum;
		this.vertexList = new ArrayList<Vertex>();
		this.vids=new ArrayList<Integer>();
		this.balance = balance;
		this.capacity = 0;
		this.random = new Random();
		this.partitions=new int[pNum];
		load(vertexPath);
	}

	public void load(String vertexPath) throws IOException {
		BufferedReader in1 = new BufferedReader(new FileReader(new File(vertexPath)));// 点
		//BufferedReader in2 = new BufferedReader(new FileReader(new File(partitionPath)));// 分区
		String str1;
		int id = 0;
		while (id<2150708) {
			id++;
			Vertex v = new Vertex(id, -1);
			vertexList.add(v);
			vids.add(id);
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
		//in2.close();
		in1.close();
		edgeNum /= 2;
		capacity = (int) (balance * (vertexNum / pNum));
		Collections.shuffle(vids);
	}

	public void BFS() {
		for(int i=0;i<vertexNum;i++) {
			Vertex vertex=vertexList.get(vids.get(i)-1);
			float[] ind = new float[pNum];
			for (Vertex v : vertex.neibor) {
				if (v.belong != -1) {
					ind[v.belong]++;
				}
			}
			float max = 0;
			int maxPartitonId = 0;
			for (int i1 = 0; i1 < pNum; i1++) {
				ind[i1] *= (1 - partitions[i1]*1.0 / capacity);
				//System.out.print(ind[i1]);
				if (ind[i1] > max||(ind[i1]==max&&partitions[i1]<partitions[maxPartitonId])) {
					max = ind[i1];
					maxPartitonId = i1;
				}
			}
			//System.out.println();
			vertex.belong=maxPartitonId;
			partitions[maxPartitonId]++;
		}
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

	public void output() throws IOException {
		BufferedWriter out=new BufferedWriter(new FileWriter("D:\\DGR-VE-dataset\\" + data + "22-70%-streaming-" + pNum+".txt"));
		for(int i=0;i<vertexNum;i++) {
			out.write(vertexList.get(i).belong+"\n");
		}
		out.close();
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
		String[] datas = { /*"dblp", "roadNet-PA", "youtube", "lj", "sp", */"orkut" /*,"power","snap","facebook"*/};
		int[] ps = { 2/*, 4, 8, 16*/ };
		for (String string : datas) {
			String data = string;
			for (int i : ps) {
				int pNum = i;
				float balance = 1.1f;// 平衡系数
				String inputVertexPath = "D:\\DGR-VE-dataset\\" + data + "2-70%.txt";
				//String inputPartitionPath = "D:\\DGR-VE-dataset\\" + data + "2-70%.txt.part." + pNum;
				Streaming repartition = new Streaming(data, pNum, balance, inputVertexPath);
				System.out.println("+++++++++++++++++"+data + ":" + pNum+"+++++++++++++++++");
				long time1 = System.currentTimeMillis();
				repartition.BFS();
				long time2 = System.currentTimeMillis();
				repartition.output();
				System.out.println((time2 - time1) * 1.0 / 1000);
			}
		}
	}

}

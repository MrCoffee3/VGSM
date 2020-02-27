package newRepartition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class newVGSM {
	public List<Partition> partitionList;
	public List<Integer> pidList;
	public int pNum;
	public int vertexNum;
	public int edgeNum;
	public float balance;
	public String data;
	public int ec = 0;
	public int lb = 0;
	public int groupSize;
	public int realmigration = 0;
	public int totalmigration = 0;
	public float minpw = 0;
	public float maxpw = 0;
	public float avg = 0;
	public int initialec = 0;
	public int im=0;//无效转移
	public int lm=0;//低效转移

	public newVGSM(String data, int pNum, int vNum, int eNum, String inputVertexPath, String inputPartitionPath,
			float balance, int groupSize) {
		// TODO Auto-generated constructor stub
		this.data = data;
		this.vertexNum = vNum;
		this.edgeNum = eNum;
		this.pNum = pNum;
		this.balance = balance;
		this.avg = (float) (vertexNum * 1.0 / pNum);
		this.partitionList = new ArrayList<Partition>();
		this.pidList = new ArrayList<Integer>();
		this.groupSize = groupSize;
		this.maxpw = balance * avg;
		this.minpw = (2 - balance) * avg;
		for (int i = 0; i < pNum; i++) {
			partitionList.add(new Partition(i));
			pidList.add(i);
		}
		try {
			load(inputVertexPath, inputPartitionPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void load(String inputVertexPath, String inputPartitionPath) throws IOException {
		Vertex[] vertexs = new Vertex[vertexNum];
		BufferedReader in1 = new BufferedReader(new FileReader(new File(inputVertexPath)));// 点
		BufferedReader in2 = new BufferedReader(new FileReader(new File(inputPartitionPath)));// 分区
		String string;
		int id = 0;
		while ((string = in2.readLine()) != null) {
			int belong = Integer.parseInt(string);
			vertexs[id] = new Vertex(id + 1, belong);
			id++;
		}
		if (id != vertexNum) {
			System.out.println("wrong vertexNum!!");
		}
		int vertexId = 0;
		while ((string = in1.readLine()) != null) {
			vertexId++;
			Vertex vertex = vertexs[vertexId - 1];
			if (string.length() != 0) {
				String[] strings = string.split(" ");
				///vertex.dv = new int[pNum];
				for (int i = 0; i < strings.length; i++) {
					int neibor = Integer.parseInt(strings[i]);
					vertex.neibor.add(vertexs[neibor - 1]);
					//vertex.dv[vertexs[neibor - 1].belong]++;
				}
			}
			partitionList.get(vertex.belong).addVertex(vertex);
		}
		in2.close();
		in1.close();
		ec = calEdgeCut();
		initialec=ec;
		System.out.println("vertexnum:" + vertexNum + "  edgenum:" + edgeNum);
		System.out.println("avg:" + (float) vertexNum / (float) pNum + "  min:" + minpw + "  max:" + maxpw);
		output();
		for (Partition partition : partitionList) {
			lb += Math.abs(partition.w - vertexNum * 1.0 / pNum);
		}
		//System.out.println("0" + "\t" + ec*1.0/edgeNum + "\t" + lb*1.0/vertexNum );
	}

	public void repartitioning() {
		int communication = 0;
		float initiallb = lb;
		int lastec = Integer.MAX_VALUE;
		int loop = 0;
		System.out.println("iterator " + loop + ":" + "\t" + "ec:" + ec + "\t" + "lb:" + lb+ "\t"
				+ String.format("%.2f", ec * 100.0 / edgeNum) + "%");
		while (lastec > ec) {
			loop++;
			lastec = ec;
			Collections.sort(pidList, new compareLoad());
			
			for (int i = 0; i < pNum; i++) {
				Set<Partition> targets = new HashSet<Partition>();
				Partition partition = partitionList.get(pidList.get(i));
				Queue<Vertex> set = new PriorityQueue<Vertex>(new compareGain());
				for (Vertex vertex : partition.vertexs) {
					if (vertex.neibor.size()!=0) {
						int[] dv=new int[pNum];
						for (Vertex n : vertex.neibor) {
							dv[n.belong]++;
						}
						int maxdv = 0;
						int target = partition.id;
						for (int j = 0; j < pNum; j++) {
							if (dv[j] > maxdv && j != partition.id) {
								maxdv = dv[j];
								target = j;
							}
						}
						if (maxdv > dv[partition.id]) {
							vertex.gain = maxdv - dv[partition.id];
							vertex.target = target;
							set.add(vertex);
						}
					}
				}
				List<Vertex> tops=new ArrayList<Vertex>();
				while (!set.isEmpty() && partition.w > minpw) {
					Vertex v = set.poll();
					int[] dv=new int[pNum];
					for (Vertex n : v.neibor) {
						dv[n.belong]++;
					}
					int maxdv = 0;
					int target = partition.id;
					for (int j = 0; j < pNum; j++) {
						if (dv[j] > maxdv && j != partition.id && partitionList.get(j).w < maxpw) {
							maxdv = dv[j];
							target = j;
						}
					}
					v.target = target;
					if (target != partition.id && dv[target] - dv[v.belong] > 0) {
						v.gain = dv[v.target] - dv[v.belong];
						/*
						 * if(v.gain<=0) { System.out.println("fuck!"); }
						 */
						Partition tp = partitionList.get(target);
						partition.removeVertex(v);
						tp.addVertex(v);
						targets.add(tp);
						/*for (Vertex neibor : v.neibor) {
							neibor.dv[v.belong]--;
							neibor.dv[v.target]++;
						}*/
						tops.add(v);
						ec -= v.gain;
						v.pre=v.belong;
						v.belong = v.target;
						realmigration++;
					}
				}
				set.clear();
				for (Vertex vertex2 : tops) {
					int[] dv=new int[pNum];
					for (Vertex n : vertex2.neibor) {
						dv[n.belong]++;
					}
					int maxdv=0;
					for(int i1=0;i1<pNum;i1++) {
						if(dv[i1]>maxdv) {
							maxdv=dv[i1];
						}
					}
					if(dv[vertex2.belong]!=maxdv) {
						if(dv[vertex2.belong]<=dv[vertex2.pre]) {
							im++;
						}else {
							lm++;
						}
					}
				}
				// System.out.println(pQueue.size());
				communication += targets.size();
			}
			
			lb = 0;
			for (Partition partition : partitionList) {
				lb += Math.abs(partition.w - vertexNum * 1.0 / pNum);
			}
			//System.out.println(loop+"\t"+(ec*1.0/edgeNum)+"\t"+(lb*1.0/vertexNum)+"\t"+lb);
		}
		lb = 0;
		for (Partition partition : partitionList) {
			lb += Math.abs(partition.w - vertexNum * 1.0 / pNum);
		}
		System.out.println("iterator " + loop + ":" + "\t" + "ec:" + ec + "\t"+ "lb:" + lb+ "\t"
				+ String.format("%.2f", ec * 100.0 / edgeNum) + "%");
		System.out.println("InitialEC:" + initialec + ";  FinalEC:" + ec + "\t"
				+ String.format("%.2f", (initialec - ec) * 100.0 / initialec) + "%");
		System.out.println("InitialLB:" + initiallb + ";  FinalLB:" + lb + "\t"
				+ String.format("%.2f", (initiallb - lb) * 100.0 / initiallb) + "%");
		System.out.println("communication times:" + communication);
	}

	public void grouprepartitioning() {
		int communication = 0;
		float initiallb = lb;
		int lastec = Integer.MAX_VALUE;
		int loop = 0;
		System.out.println("iterator " + loop + ":" + "\t" + "ec:" + ec + "\t" + "lb:" + lb+ "\t"
				+ String.format("%.2f", ec * 100.0 / edgeNum) + "%");
		while (lastec > ec) {
			loop++;
			lastec = ec;
			Collections.sort(pidList, new compareLoad());
			for (int i = 0; i < pNum; i++) {
				Set<Partition> targets = new HashSet<Partition>();
				Partition partition = partitionList.get(pidList.get(i));
				// System.out.println(partition.id+";"+partition.w);
				Queue<Group> set = new PriorityQueue<Group>(new compareGroup());
				for (Vertex vertex : partition.vertexs) {
					if (vertex.neibor.size() != 0 && vertex.belong == partition.id && vertex.inG == false) {
						int[] dv=new int[pNum];
						for (Vertex n : vertex.neibor) {
							dv[n.belong]++;
						}
						int maxdv = 0;
						int target = partition.id;
						for (int j = 0; j < pNum; j++) {
							if (dv[j] > maxdv && j != partition.id) {
								maxdv = dv[j];
								target = j;
							}
						}
						if (maxdv > dv[partition.id]) {
							Group group = new Group(partition.id);
							group.vertexs.add(vertex);
							vertex.inG = true;
							group.gain = maxdv - dv[partition.id];
							group.target = target;
							set.add(group);
						
						} else {
							Group group = findGv(vertex);
							if (group != null) {
								set.add(group);
							}
						}
					}
				}
				Set<Vertex> tops=new HashSet<Vertex>();
				while (!set.isEmpty() && partition.w > minpw) {
					Group group = set.poll();
					for (Vertex v : group.vertexs) {
						v.inG = false;
						v.checked = 0;
					}
					int[] dv = new int[pNum];
					int maxdv = 0;
					int target = partition.id;
					for (int j = 0; j < pNum; j++) {
						if (j != partition.id) {
							for (Vertex vertex : group.vertexs) {
								int neis=0;
								for (Vertex n : vertex.neibor) {
									if(n.belong==j) {
										neis++;
									}
								}
								//dv[j] += vertex.dv[j];
								dv[j] += neis;
							}
							if (dv[j] > maxdv && partitionList.get(j).w < maxpw) {
								maxdv = dv[j];
								target = j;
							}
						} else {
							for (Vertex vertex : group.vertexs) {
								int k = 0;
								for (Vertex vertex2 : vertex.neibor) {
									if (vertex2.belong == partition.id && !group.vertexs.contains(vertex2)) {
										k++;
									}
								}
								dv[j] += k;
							}
						}

					}
					if (target != partition.id && dv[target] > dv[partition.id]) {
						group.target = target;
						// System.out.println(group.vertexs.size()+";"+dv[target]
						// +";"+dv[partition.id]);
						Partition tp = partitionList.get(target);
						targets.add(tp);
						for (Vertex v : group.vertexs) {
							partition.removeVertex(v);
							tp.addVertex(v);
							/*for (Vertex neibor : v.neibor) {
								neibor.dv[group.belong]--;
								neibor.dv[group.target]++;
							}*/
							v.pre=v.belong;
							v.belong = group.target;
							tops.add(v);
						}
						ec -= dv[target] - dv[partition.id];
						realmigration += group.vertexs.size();
					}
				}
				for (Vertex vertex2: tops) {
						//System.out.println(vertex2.pre);
						int[] dv=new int[pNum];
						for (Vertex n : vertex2.neibor) {
							dv[n.belong]++;
						}
						int maxdv=0;
						for(int i1=0;i1<pNum;i1++) {
							if(dv[i1]>maxdv) {
								maxdv=dv[i1];
							}
						}
						if(dv[vertex2.belong]!=maxdv) {
							if(dv[vertex2.belong]<=dv[vertex2.pre]) {
								im++;
							}else {
								lm++;
							}
						}
				}
				// while (!set.isEmpty()) {
				// Group group = set.poll();
				// for (Vertex vertex : group.vertexs) {
				// vertex.checked = 0;
				// }
				// }
				for (Vertex vertex : partition.vertexs) {
					vertex.inG = false;
					vertex.checked = 0;
				}
				communication += targets.size();
			}
			// System.out.println(calEdgeCut());
			lb=0;
			for (Partition partition : partitionList) {
				lb += Math.abs(partition.w - vertexNum * 1.0 / pNum);
			}
			//System.out.println(loop+"\t"+(ec)*1.0/edgeNum+"\t"+(lb*1.0/vertexNum));
		}
		lb = 0;
		for (Partition partition : partitionList) {
			lb += Math.abs(partition.w - vertexNum * 1.0 / pNum);
		}
		System.out.println("iterator " + loop + ":" + "\t" + "ec:" + ec + "\t" + "lb:" + lb+ "\t"
				+ String.format("%.2f", ec * 100.0 / edgeNum) + "%");
		System.out.println("InitialEC:" + initialec + ";  FinalEC:" + ec + "\t"
				+ String.format("%.2f", (initialec - ec) * 100.0 / initialec) + "%");
		System.out.println("InitialLB:" + initiallb + ";  FinalLB:" + lb + "\t"
				+ String.format("%.2f", (initiallb - lb) * 100.0 / initiallb) + "%");
		System.out.println("communication times:" + communication);
	}

	public void finalrepartitioning(float gama) {
		int communication = 0;
		float initiallb = lb;
		initialec = ec;
		int lastec = Integer.MAX_VALUE;
		float lastlb = Integer.MAX_VALUE;
		int loop = 0;
		System.out.println("iterator " + loop + ":" + "\t" + "ec:" + ec + "\t" + "lb:" + lb+ "\t"
				+ String.format("%.2f", ec * 100.0 / edgeNum) + "%");
		while ((lastec - ec) * gama / lastec + (lastlb - lb) * (1 - gama) / lastlb > 0) {
			loop++;
			lastec = ec;
			lastlb = lb;
			// System.out.println(lastlb+";"+lb);
			Collections.sort(pidList, new compareLoad());
			for (int i = 0; i < pNum; i++) {
				Partition partition = partitionList.get(pidList.get(i));
				Set<Partition> targets = new HashSet<Partition>();
				Queue<Group> set = new PriorityQueue<Group>(new compareGroup());
				for (Vertex vertex : partition.vertexs) {
					
					if (vertex.neibor.size() != 0 && vertex.belong == partition.id && vertex.inG == false) {
						int[] dv=new int[pNum];
						for (Vertex n : vertex.neibor) {
							dv[n.belong]++;
						}
						int maxdv = 0;
						int target = partition.id;
						for (int j = 0; j < pNum; j++) {
							if (dv[j] > maxdv && j != partition.id) {
								maxdv = dv[j];
								target = j;
							}
						}

						if (maxdv > dv[partition.id]) {
							Group group = new Group(partition.id);
							group.vertexs.add(vertex);
							vertex.inG = true;
							group.gain = maxdv - dv[partition.id];
							group.target = target;
							set.add(group);
						} else {
							Group group = findGv(vertex);
							if (group != null) {
								set.add(group);
							} else {
								group = new Group(partition.id);
								group.vertexs.add(vertex);
								vertex.inG = true;
								group.gain = maxdv - dv[partition.id];
								group.target = target;
								set.add(group);
							}
						}
					}
				}
				List<Group> tops=new ArrayList<Group>();
				while (!set.isEmpty() /* && partition.w > 0.95 * avg */) {
					Group group = set.poll();
					for (Vertex v : group.vertexs) {
						v.inG = false;
						v.checked = 0;
					}
					int[] dv = new int[pNum];
					int maxdv = 0;
					int target = partition.id;
					for (Vertex vertex : group.vertexs) {
						int k = 0;
						for (Vertex vertex2 : vertex.neibor) {
							if (vertex2.belong == partition.id && !group.vertexs.contains(vertex2)) {
								k++;
							}
						}
						dv[partition.id] += k;
					}
					for (int j = 0; j < pNum; j++) {
						if (j != partition.id) {
							for (Vertex vertex : group.vertexs) {
								for (Vertex n : vertex.neibor) {
									if(n.belong==j)
									dv[j]++;
								}
								//dv[j] += dv[j];
							}
							int llb = 0;
							llb += partition.w > avg ? 1 : -1;
							llb += partitionList.get(j).w < avg ? 1 : -1;
							if (gama * (dv[j] - dv[partition.id]) + (1 - gama) * llb > maxdv) {
								maxdv = dv[j];
								target = j;
							}
						}
					}
					if (target != partition.id) {
						int llb = 0;
						llb += partition.w > avg ? 1 : -1;
						llb += partitionList.get(target).w < avg ? 1 : -1;
						// System.out.println(partition.id+";"+target+";"+llb);
						if (gama * (dv[target] - dv[partition.id])/initialec*edgeNum/vertexNum/2 + (1 - gama) * llb/initiallb > 0) {
							group.target = target;
							// System.out.println(group.vertexs.size()+";"+dv[target]
							// +";"+dv[partition.id]);
							Partition pt = partitionList.get(target);
							targets.add(pt);
							int pbw = partition.w;
							int ptw = pt.w;
							for (Vertex v : group.vertexs) {
								partition.removeVertex(v);
								pt.addVertex(v);
								/*for (Vertex neibor : v.neibor) {
									neibor.dv[group.belong]--;
									neibor.dv[group.target]++;
								}*/
								v.pre=v.belong;
								v.belong = group.target;
							}
							ec -= dv[target] - dv[partition.id];
							realmigration += group.vertexs.size();
							lb -= Math.abs(pbw - avg) - Math.abs(partition.w - avg);
							lb -= Math.abs(ptw - avg) - Math.abs(pt.w - avg);
							tops.add(group);
						}
					}
				}
				
				for (Group group2 : tops) {
					
					for (Vertex vertex2 : group2.vertexs) {
						int[] dv=new int[pNum];
						for (Vertex n : vertex2.neibor) {
							dv[n.belong]++;
						}
						int maxdv=0;
						for(int i1=0;i1<pNum;i1++) {
							if(dv[i1]>maxdv) {
								maxdv=dv[i1];
							}
						}
						if(dv[vertex2.belong]!=maxdv) {
							if(dv[vertex2.belong]<=dv[vertex2.pre]) {
								im++;
							}else {
								lm++;
							}
						}
					}
				}
				
				communication += targets.size();
				// System.out.println(partition.id+";"+nums);
				for (Vertex vertex : partition.vertexs) {
					vertex.inG = false;
					vertex.checked = 0;
				}
			}
			/*
			 * for (Partition partition2 : partitionList) {
			 * System.out.println(partition2.w); }
			 */
			// System.out.println(calEdgeCut());
			//System.out.println(loop+"\t"+(ec*1.0/edgeNum)+"\t"+(lb*1.0/vertexNum));
		}
		System.out.println("iterator " + loop + ":" + "\t" + "ec:" + ec + "\t" + "lb:" + lb + "\t"
				+ String.format("%.2f", ec * 100.0 / edgeNum) + "%");
		System.out.println("scoremin:"+(ec + lb));
		System.out.println("scoremin(gama):"+(ec*1.0/edgeNum+lb*1.0/vertexNum));
		System.out.println("f(P):"+((initialec - ec) * gama / initialec + (initiallb - lb) * (1 - gama) / initiallb) );
		System.out.println("InitialEC:" + initialec + ";  FinalEC:" + ec + "\t"
				+ String.format("%.2f", (initialec - ec) * 100.0 / initialec) + "%");
		System.out.println("InitialLB:" + initiallb + ";  FinalLB:" + lb + "\t"
				+ String.format("%.2f", (initiallb - lb) * 100.0 / initiallb) + "%");
		System.out.println("communication times:" + communication);
	}

	public Group findGv(Vertex vertex) {
		Group group = new Group(vertex.belong);
		group.dv = new int[pNum];
		PriorityQueue<Vertex> pQueue = new PriorityQueue<Vertex>(new compareGain());
		pQueue.add(vertex);
		// vertex.inQ = true;
		vertex.checked++;
		int max = 0;
		while (!pQueue.isEmpty() && group.vertexs.size() < groupSize) {
			Vertex tVertex = pQueue.poll();
			int[] dv=new int[pNum];
			for (Vertex n : tVertex.neibor) {
				dv[n.belong]++;
			}
			int connect0 = 0;
			for (Vertex vertex2 : group.vertexs) {
				if (tVertex.neibor.contains(vertex2)) {
					connect0++;
				}
			}
			group.vertexs.add(tVertex);
			for (int i = 0; i < pNum; i++) {
				group.dv[i] += dv[i];
				if (i != tVertex.belong && group.dv[i] > max) {
					max = group.dv[i];
					group.target = i;
				}
			}
			group.dv[tVertex.belong] -= 2 * connect0;
			if (max - group.dv[tVertex.belong] > 0) {
				group.gain = max - group.dv[tVertex.belong];
				for (Vertex vertex2 : group.vertexs) {
					// vertex2.inQ = false;
					vertex2.inG = true;
				}
				/*
				 * while (!pQueue.isEmpty()) { pQueue.poll().inQ = false; }
				 */
				return group;
			}
			for (Vertex neibor : tVertex.neibor) {
				if (neibor.belong == tVertex.belong && neibor.inG == false && neibor.checked < 1) {
					int[] dv1=new int[pNum];
					for (Vertex n : neibor.neibor) {
						dv1[n.belong]++;
					}
					int maxdv = 0;
					int target = neibor.belong;
					for (int j = 0; j < pNum; j++) {
						if (dv1[j] > maxdv && j != neibor.belong) {
							maxdv = dv1[j];
							target = j;
						}
					}
					// if (maxdv > vertex.dv[neibor.belong]) {
					//
					// vertex.gain = maxdv - vertex.dv[neibor.belong];
					// if (partitionList.get(target).w < maxpw) {
					// vertex.target = target;
					// partitionList.get(vertex.belong).removeVertex(vertex);
					// partitionList.get(vertex.target).addVertex(vertex);
					// for (Vertex neibor1 : vertex.neibor) {
					// neibor1.dv[vertex.belong]--;
					// neibor1.dv[vertex.target]++;
					// }
					// ec -= vertex.dv[vertex.target] - vertex.dv[vertex.belong];
					// vertex.belong = vertex.target;
					// realmigration++;
					// }
					//
					// } else {
					int connect = 0;
					for (Vertex vertex2 : group.vertexs) {
						if (neibor.neibor.contains(vertex2)) {
							connect++;
						}
					}
					neibor.gain = maxdv - (dv1[tVertex.belong] - 2 * connect);
					neibor.target = target;
					pQueue.add(neibor);
					// neibor.inQ = true;
					neibor.checked++;
					// }
				}
			}
		}
		/*
		 * for (Vertex vertex2 : group.vertexs) { vertex2.inQ = false; } while
		 * (!pQueue.isEmpty()) { pQueue.poll().inQ = false; }
		 */
		return null;
	}

	public int calEdgeCut() {
		int ec = 0;
		for (Partition partition : partitionList) {
			for (Vertex vertex : partition.vertexs) {
				if (vertex.neibor.size() !=0) {
					int[] dv=new int[pNum];
					for (Vertex n : vertex.neibor) {
						if (n.belong != partition.id) {
							ec ++;
						}
						dv[n.belong]++;
					}
					/*for (int i = 0; i < pNum; i++) {
						if (i != partition.id) {
							ec += vertex.dv[i];
						}
					}*/
				}
			}
		}
		ec /= 2;
		return ec;
	}

	public void output() {
		float avg = (float) vertexNum / (float) pNum;
		float max = Integer.MIN_VALUE;
		float min = Integer.MAX_VALUE;
		for (int i = 0; i < pNum; i++) {
			Partition partition = partitionList.get(i);
			float bal = (float) (partition.w * 1.0 / avg);
			if (bal > max) {
				max = bal;
			}
			if (bal < min) {
				min = bal;
			}
		}
		System.out.println("maxBal: " + max + ";  minBal: " + min);
		System.out.println("Invalid migration:" + im+"\t"+im*1.0/vertexNum);
		System.out.println("Inefficient migration:"+lm+"\t"+lm*1.0/vertexNum);
	}

	public void calMigrationRatio() {
		for (Partition partition : partitionList) {
			for (Vertex vertex : partition.vertexs) {
				if (vertex.belong != vertex.belong0) {
					totalmigration++;
				}
			}
		}
		System.out.println(
				"totalMigration: " + totalmigration + "\t" + String.format("%.2f", totalmigration * 100.0 / vertexNum)
						+ "%" + "\t" + String.format("%.2f", (initialec - ec) * 1.0 / totalmigration));
		System.out.println(
				"realMigration: " + realmigration + "\t" + String.format("%.2f", realmigration * 100.0 / vertexNum)
						+ "%" + "\t" + String.format("%.2f", (initialec - ec) * 1.0 / realmigration));
	}

	public class compareLoad implements Comparator<Integer> {

		@Override
		public int compare(Integer i1, Integer i2) {
			// TODO Auto-generated method stub
			if (partitionList.get(i1).w > partitionList.get(i2).w) {
				return -1;
			} else if (partitionList.get(i1).w < partitionList.get(i2).w) {
				return 1;
			}
			return 0;
		}

	}

	public class compareGain implements Comparator<Vertex> {

		@Override
		public int compare(Vertex v1, Vertex v2) {
			// TODO Auto-generated method stub
			if (v1.gain > v2.gain) {
				return -1;
			} else if (v1.gain < v2.gain) {
				return 1;
			}
			return 0;
		}

	}

	public class compareGroup implements Comparator<Group> {

		@Override
		public int compare(Group v1, Group v2) {
			// TODO Auto-generated method stub
			if (v1.gain * 1.0 / v1.vertexs.size() > v2.gain * 1.0 / v2.vertexs.size()) {
				return -1;
			} else if (v1.gain * 1.0 / v1.vertexs.size() < v2.gain * 1.0 / v2.vertexs.size()) {
				return 1;
			}
			return 0;
		}

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String[] datas = {  /*"dblp", "roadNet-PA", "youtube",*/ "lj",  "orkut", "power" ,"snap"};
		int[] ps = {  2, 4, 8, 16 };
		
		
		/*for (String string : datas) {
			String data = string;
			for (int i : ps) {
				int pNum = i;
				float balance = 1.1f;// 平衡系数
				int groupSize = 40;
				int vNum = 0;// 点数量317080,1088092,1134890,3997962,7600000,3072441
				int eNum = 0;
				switch (data) {
				case "dblp":
					vNum = 317080;
					eNum = 1049866;
					break;
				case "roadNet-PA":
					vNum = 1088092;
					eNum = 1541898;
					break;
				case "youtube":
					vNum = 1134890;
					eNum = 2987624;
					break;
				case "lj":
					vNum = 3997962;
					eNum = 34681189;
					break;
				case "sp":
					vNum = 7600000;
					eNum = 40264007;
					break;
				case "orkut":
					vNum = 3072441;
					eNum = 117185083;
					break;
				case "power":
					vNum = 10000000;
					eNum=9672167;
					break;
				case "snap":
					vNum = 30000000;
					eNum=174470043;
					break;
				default:
					break;
				}
				String inputVertexPath = "D:\\DGR-VE-dataset\\" + data + "2.txt";
				String inputPartitionPath = "D:\\DGR-VE-dataset\\notbalance2\\" + data + "2-streaming-" + pNum + ".txt";
				System.out.println("------------" + data + "-" + pNum + "-repartitioning------------");
				newVGSM nVgsm = new newVGSM(data, pNum, vNum, eNum, inputVertexPath, inputPartitionPath, balance,
						groupSize);
				long t1 = System.currentTimeMillis();
				nVgsm.repartitioning();
				long t2 = System.currentTimeMillis();
				System.out.println("time for repartitioning: " + (t2 - t1) * 1.0 / 1000);
				nVgsm.output();
				nVgsm.calMigrationRatio();
			}
		}*/

		for (String string : datas) {
			String data = string;
			for (int i : ps) {
				int pNum = i;
				float balance = 1.1f;// 平衡系数
				int groupSize = 40;
				int vNum = 0;// 点数量317080,1088092,1134890,3997962,7600000,3072441
				int eNum = 0;
				switch (data) {
				case "dblp":
					vNum = 317080;
					eNum = 1049866;
					break;
				case "roadNet-PA":
					vNum = 1088092;
					eNum = 1541898;
					break;
				case "youtube":
					vNum = 1134890;
					eNum = 2987624;
					break;
				case "lj":
					vNum = 3997962;
					eNum = 34681189;
					break;
				case "sp":
					vNum = 7600000;
					eNum = 40264007;
					break;
				case "orkut":
					vNum = 3072441;
					eNum = 117185083;
					break;
				case "power":
					vNum = 10000000;
					eNum=9672167;
					break;
				case "snap":
					vNum = 30000000;
					eNum=174470043;
					break;
				default:
					break;
				}
				String inputVertexPath = "D:\\DGR-VE-dataset\\" + data + "2.txt";
				String inputPartitionPath = "D:\\DGR-VE-dataset\\notbalance2\\" + data + "2-streaming-" + pNum + ".txt";
				System.out.println("------------" + data + "-" + pNum + "-grouprepartitioning------------");
				newVGSM nVgsm = new newVGSM(data, pNum, vNum, eNum, inputVertexPath, inputPartitionPath, balance,
						groupSize);
				long t1 = System.currentTimeMillis();
				nVgsm.grouprepartitioning();
				long t2 = System.currentTimeMillis();
				System.out.println("time for repartitioning: " + (t2 - t1) * 1.0 / 1000);
				nVgsm.output();
				nVgsm.calMigrationRatio();
			}
		}
		
		
		/*for (String string : datas) {
			String data = string;
			for (int i : ps) {
				int pNum = i;
				float balance = 1.1f;// 平衡系数
				int groupSize = 40;
				int vNum = 0;// 点数量317080,1088092,1134890,3997962,7600000,3072441
				int eNum = 0;
				switch (data) {
				case "dblp":
					vNum = 317080;
					eNum = 1049866;
					break;
				case "roadNet-PA":
					vNum = 1088092;
					eNum = 1541898;
					break;
				case "youtube":
					vNum = 1134890;
					eNum = 2987624;
					break;
				case "lj":
					vNum = 3997962;
					eNum = 34681189;
					break;
				case "sp":
					vNum = 7600000;
					eNum = 40260741;
					break;
				case "orkut":
					vNum = 3072441;
					eNum = 117185083;
					break;
				case "power":
					vNum = 10000000;
					eNum=9672167;
					break;
				case "snap":
					vNum = 30000000;
					eNum=174470043;
					break;
				default:
					break;
				}
				String inputVertexPath = "D:\\DGR-VE-dataset\\" + data + "2.txt";
				String inputPartitionPath = "D:\\DGR-VE-dataset\\notbalance2\\" + data + "2-streaming-" + pNum + ".txt";
				newVGSM nVgsm = null;
				float[] gamas = { 0, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1 };
				for (int j = 0; j < gamas.length; j++) {
					System.out.println(
							"------------" + data + "-" + pNum + "-finalrepartitioning-" + gamas[j] + "-----------");
					nVgsm = new newVGSM(data, pNum, vNum, eNum, inputVertexPath, inputPartitionPath, balance,
							groupSize);
					long t1 = System.currentTimeMillis();
					nVgsm.finalrepartitioning(gamas[j]);
					long t2 = System.currentTimeMillis();
					System.out.println("time for repartitioning: " + (t2 - t1) * 1.0 / 1000);
					nVgsm.output();
					nVgsm.calMigrationRatio();
					nVgsm=null;
				}
			}
		}*/
	}

}

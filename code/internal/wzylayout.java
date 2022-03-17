package wzylayout;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class wzylayout {
    private double[][] dMat;    // n*n PPI score matrix
    private double[][] dPos;    // n*2 matrix to show the positions of nodes
    private Boolean[][] bMat;   // n*n boolean matrix to show which two nodes are connected, 1 connected, 0 non-connected
    private double[][] dSetPos; // n*2 matrix to show the positions of the centers for each set
    private ArrayList<ArrayList<Integer>> nSet;   // the lists of sets 
    private double[] dFC;       // n-d vector to record the nodes' FCs 
    private double[][] dICMat;  // n*n IC (PPI score) matrix
    private double[][] dECMat;  // n*n EC matrix

    private int nNodeNum;       // node number n
    private int nSetNum;        // set number
    private int nKA;            
    private int nKB;
    private int nKC;
    private int nRound;         // the rounds that the algorithm will run
    private double dEps;        // a parameter annotated in the paper
    
    private int nSearNum;
    private double[] dSear;
    private double[] dGeneRP;
    private double dLeastDis;

    
    //multi-thread setting
    private double[][] dPosBac;    // n*2 matrix to show the positions of nodes, used for multi-threads
    private int nThreads;      //thread number
    
    public wzylayout(Boolean[][] bMat, double[] dFC, int nSetNum, ArrayList<ArrayList<Integer>> nSet, double[][] dICMat, double[][] dECMat, int nKA, int nKB, int nKC) {
		
    	
    	
    	// basic configuration
    	this.nNodeNum = bMat.length;    // the number of nodes
    	this.dPos = new double[this.nNodeNum][2];   // the positions of nodes
    	this.dPosBac = new double[this.nNodeNum][2];   // the positions of nodes
    	this.nKA = nKA;                             // parameter 
    	this.nKB = nKB;                             // parameter
    	this.nKC = nKC;								// parameter
    	this.nRound = 200;                          // the maximum rounds that DEMA can run
    	this.dEps = 0.00000001;                     // the parameter                     
    	this.bMat = bMat;                           // network matrix, true means connected
    	this.dLeastDis = 0.4;                       // the parameter
    	
    	this.nThreads = 4;                          // number of threads
    	
    	//fold change
    	if (dFC != null)
    		this.dFC = dFC;
    	else {
    		this.dFC = new double[this.nNodeNum];
    		Arrays.fill(this.dFC, 1);               // default value of FC is 1
    	}
    	
    	
    	//weight
    	this.dECMat = dECMat;
    	this.dICMat = dICMat;
    	cal_weight();
    	
    	//RP
    	cal_rp();
    	
    	// set
    	this.nSetNum = nSetNum;              // the number of sets
    	if (this.nSetNum == 0) {
    		this.nSet = null;
    		this.dSetPos = null;
    	}
    	else {
    		this.nSet = nSet;
    		this.dSetPos = new double[nSetNum][2];   // positions of set centers
    	}
    	
    	// step
    	this.nSearNum = 100;
    	dSear = new double[nSearNum+1];
    	double d = 1.0 / nSearNum;
    	for (int i = 1; i <= nSearNum; i++)
    		dSear[i] = d * (i+1);
    	dSear[0] = 0;
    }

    public double dis(double a, double b)    // calculate the distance between two single values
    {
    	return Math.sqrt(a * a + b * b);
    }
    
    
    public void cal_weight() {  // calculate the weight for mds_layout
    	this.dMat = new double[this.nNodeNum][this.nNodeNum];
    	for (int i = 0; i < this.nNodeNum; i++)
    		for (int j = 0; j < this.nNodeNum; j++)
    		if (this.bMat[i][j]) {
    			
    			if (this.dICMat == null && this.dECMat == null) {
    				this.dMat[i][j] = 1;
    				continue;
    			}
    			
    			if (this.dICMat == null) {
        			this.dMat[i][j] = 1/(- Math.log((1-this.dECMat[i][j])/(1+this.dECMat[i][j]))) + 1;
    				continue;
    			}
    			
    			if (this.dECMat == null) {
        			this.dMat[i][j] = 1/(-Math.log((1-this.dICMat[i][j])/(1+this.dICMat[i][j]))) + 1;
    				continue;
    			}
    			
    			
    			this.dMat[i][j] = 1/(-Math.log((1-this.dICMat[i][j])/(1+this.dICMat[i][j])) - Math.log((1-this.dECMat[i][j])/(1+this.dECMat[i][j]))) + 1;
    		}
    		else this.dMat[i][j] = 0;
    	
    }
    
    public double dis_cal(double[] a, double[] b)  // calculate the Euclidean distance between two nodes
    {
    	return Math.sqrt((a[0]-b[0]) * (a[0]-b[0]) + (a[1]-b[1]) * (a[1]-b[1]));
    }
    
    public void ran_layout(){    // randomized initial layout

    	Random randomGenerator = new Random();    	
    	
    	for (int i = 0; i < this.nNodeNum; i++)
    	{
    		
    		Boolean u = true;
    		while (u)
    		{
    			u = false;
    			
    			
    			dPos[i][0] = randomGenerator.nextDouble();
    			dPos[i][1] = randomGenerator.nextDouble();
    			double d = dis(dPos[i][0], dPos[i][1]);
    			
    			dPos[i][0] = dPos[i][0] / (d * 4);
    			dPos[i][1] = dPos[i][1] / (d * 4);
    			
    			for (int j = 0; j < i; j++)
    			{
    				if (dis_cal(dPos[i], dPos[j]) <= dEps)
    				{
    					u = true;
    					break;
    				}
    				if (bMat[i][j]) {
    					if (dis_cal(dPos[i], dPos[j]) >= dMat[i][j]) {
    						u = true;
    						break;
    					}
    				}
    			}
    			
    		

    		}
    		

    	}			
	}
	
    public void cal_rp() {   //cal rp score
    	this.dGeneRP = new double[this.nNodeNum];
    	for (int i = 0; i < this.nNodeNum; i++) {
    		double RP = 0;
    		int n = 0;
    		for (int j = 0; j < this.nNodeNum; j++)
    		if (bMat[i][j]) {
    			if (this.dICMat == null) {
    				RP = RP + 1;
    				n = n + 1;
    				continue;
    			}
    			RP = RP + this.dICMat[i][j];
    			n = n + 1;
    		}
    		this.dGeneRP[i] = Math.exp(2*Math.log10(RP)-Math.log10(n));
    	}
    }

    public boolean check_dis(int x) {          // check if the distance meets the requirement that distance smaller than w, if yes return false  
    	for (int j = 0; j < this.nNodeNum; j++)
    		if (bMat[x][j]) {
    			if (dis_cal(dPos[x], dPos[j]) > dMat[x][j]) return true;
    		}
    	return false;
    }
    
    
    public double cal_energy(int x) {  // calculate the energy of the single node
    	double e = 0;
    	for (int i = 0; i < this.nNodeNum; i++) {
    		if (x != i) {
    			double dis = dis_cal(dPos[x], dPos[i]);
    			//System.out.println(x+":"+i+":"+dis+":"+dFC.length);
    			e = e + nKA * dFC[x] * dFC[i] / dis;
    			if (bMat[x][i]) {
    				e = e + nKB * dFC[x] * dFC[i] / (dMat[x][i] - dis);    				
    			}
    		}
    	}

		for (int i = 0; i < nSetNum; i++) {
			Boolean u = false;
			for (int j = 0; j < nSet.get(i).size(); j++) {
				if (nSet.get(i).get(j) == x) {
					u = true;
					break;
				}
			}
			if (u) {
				double dis = dis_cal(dPos[x], dSetPos[i]);
				e = e + dis * dis * nKC;
			}
		}
    
    			
    	return e;    
    
    }
    
    public double[][] return_pos() {  // return the positions of nodes
    	return dPos;
    }
    
    public boolean check_in_set(int x, int s) {     // chech if the node is inside the set
    	for (int i = 0; i < nSet.get(s).size(); i++) {
    		if (x == nSet.get(s).get(i)) return true;
    	}
    	return false;
    }
    
	public void itr_layout(){   // iteration to calculate the layout
		int nItr = 0;
		while (true) {
			//System.out.println(nItr);
			for (int i = 0; i < this.nNodeNum; i++) {
				if (check_dis(i)) {
					System.out.println(i+" is larger than weight");
					System.exit(0);
				}
			}
			
			//Expectation
			//System.out.println("Expectation stage");
			for (int i = 0; i < nSetNum; i++) {
				
				double x,y;
				x = 0;
				y = 0;
				for (int j = 0; j < nSet.get(i).size(); j++) {
					x = x + dPos[nSet.get(i).get(j)][0];
					y = y + dPos[nSet.get(i).get(j)][1];
				}
				x = x / nSet.get(i).size();
				y = y / nSet.get(i).size();
				dSetPos[i][0] = x;
				dSetPos[i][1] = y;
			}
			
			double dOri_energy = cal_total_energy();
			
			//Maximization
			//System.out.println("Maximization stage");
			for (int i = 0; i < this.nNodeNum; i++) {
				
				double dEnergy = cal_energy(i);
				double x = 0;
				double y = 0;
				for (int j = 0; j < this.nNodeNum; j++) {
					if (i != j) {
						double dis = dis_cal(dPos[i], dPos[j]);
						x = x - (dPos[j][0] - dPos[i][0]) / dis * nKA * dFC[i] * dFC[j] / dis / dis;
						y = y - (dPos[j][1] - dPos[i][1]) / dis * nKA * dFC[i] * dFC[j] / dis / dis;
					
						if (bMat[i][j]) {
							x = x + (dPos[j][0] - dPos[i][0]) / dis * nKB * dFC[i] * dFC[j] / (dMat[i][j] - dis) / (dMat[i][j] - dis);
							y = y + (dPos[j][1] - dPos[i][1]) / dis * nKB * dFC[i] * dFC[j] / (dMat[i][j] - dis) / (dMat[i][j] - dis);
							
						}
					}
				}
				
				for (int j = 0; j < nSetNum; j++) {
					if (check_in_set(i,j)) {
						double dis = dis_cal(dPos[i], dSetPos[j]);
						x = x + (dSetPos[j][0] - dPos[i][0]) * 2 * dis * nKC;
						y = y + (dSetPos[j][1] - dPos[i][1]) * 2 * dis * nKC;
					}
				}
				double tt = dis(x, y);
				x = x / tt;
				y = y / tt;
				
	
				int t = binary_search(1, this.nSearNum, x, y, i, dEnergy);
				dPos[i][0] = dPos[i][0] + x * dSear[t];
				dPos[i][1] = dPos[i][1] + y * dSear[t];
				
			}
			
			nItr = nItr + 1;
			if (nItr > nRound || Math.abs(dOri_energy-cal_total_energy()) <= dEps) {
				System.out.println("round : "+nItr);
				break;
			}
			
		}
	}
	

	
	public void itr_layout_multi() throws InterruptedException{   // iteration to calculate the layout, multi-threads
		int nItr = 0;
		
		for (int i = 0; i < this.nNodeNum; i++) {
			for (int j = 0; j < 2; j++)
			dPosBac[i][j] = dPos[i][j];
		}
		
		
		while (true) {
			//System.out.println(nItr);
			for (int i = 0; i < this.nNodeNum; i++) {
				if (check_dis(i)) {
					System.out.println(i+" is larger than weight");
					System.exit(0);
				}
			}
			
			//Expectation
			//System.out.println("Expectation stage");
			for (int i = 0; i < nSetNum; i++) {
				
				double x,y;
				x = 0;
				y = 0;
				for (int j = 0; j < nSet.get(i).size(); j++) {
					x = x + dPos[nSet.get(i).get(j)][0];
					y = y + dPos[nSet.get(i).get(j)][1];
				}
				x = x / nSet.get(i).size();
				y = y / nSet.get(i).size();
				dSetPos[i][0] = x;
				dSetPos[i][1] = y;
			}
			
			double dOri_energy = cal_total_energy();
			
			//multi-thread
			
			Thread tr[] = new Thread[this.nThreads];
			
			for (int i = 0; i < this.nThreads; i++) {
			     MyRunnable r = new MyRunnable(dMat, dPos, bMat, dFC, nKA, nKB, nKC, nNodeNum, nSetNum, nSet, dSetPos, nSearNum, dSear, dPosBac, i, nNodeNum, nThreads);
			     tr[i] = new Thread(r);		
			     tr[i].start();
			}
			
			for (int i = 0; i < this.nThreads; i++) {
				tr[i].join();
			}
			
		
			for (int i = 0; i < this.nNodeNum; i++) {
				for (int j = 0; j < 2; j++)
				dPos[i][j] = dPosBac[i][j];
			}		
			
			
			nItr = nItr + 1;
			if (nItr > nRound || Math.abs(dOri_energy-cal_total_energy()) <= dEps) {
				System.out.println("round : "+nItr);
				break;
			}
			
		}
		

		
	}	
	
	
	
	public int binary_search(int l, int r, double x, double y, int i, double dEnergy) { //binary search for the best step length
		if (l > r) return 0;
		int mid = (l+r) / 2;
		dPos[i][0] = dPos[i][0] + x * dSear[mid];
		dPos[i][1] = dPos[i][1] + y * dSear[mid];
		
		if (cal_energy(i) < dEnergy && !check_dis(i)) {
			dPos[i][0] = dPos[i][0] - x * dSear[mid];
			dPos[i][1] = dPos[i][1] - y * dSear[mid];
			int t = binary_search(mid + 1, r, x, y, i, dEnergy);
			if (t == 0) return mid;
			else return t;
		} 
		
		dPos[i][0] = dPos[i][0] - x * dSear[mid];
		dPos[i][1] = dPos[i][1] - y * dSear[mid];
		return binary_search(l, mid - 1, x, y, i, dEnergy);
	}
	
	public double cal_total_energy() {  // calculate the whole energy
		double e = 0;
		for (int i = 0; i < this.nNodeNum; i++)
			e = e + cal_energy(i);
		return e / 2;
	}
	

	public static void for_cheha() throws IOException, InterruptedException { // this is designed to test the time by running in Cheha

		
	    int nNodeNum = 14505;
	    //int nEdgeFac = 1;
  
	    
	    Boolean[][] bMat = new Boolean[nNodeNum][nNodeNum];
	    double [] dFC = new double[nNodeNum];
	    double [][] dIC = new double[nNodeNum][nNodeNum];
	    int nKA = 1;
	    int nKB = nNodeNum;
	    int nKC = 0;
	    
		Map<String, Integer> nameMap = new HashMap();

	    
	    String sFileName = "sub5_s";
	    BufferedReader br = new BufferedReader(new FileReader(sFileName));
	    

	    int nIDCount = 0;
	    int nLineCount = 0;
	    try {
	        String line;
	        
	        for (int i = 0; i < nNodeNum; i++)
	        	Arrays.fill(bMat[i], false);
	        
	        for (int i = 0; i < nNodeNum; i++)
	        	Arrays.fill(dIC[i], 0);
	        
	        Arrays.fill(dFC, 1);
	        
	        
	        line = br.readLine();
            while (line != null) {
            	nLineCount++;
	            String [] arr = line.split("\\s+");
	            
	            
	            int x,y;
	            
	            if (nameMap.get(arr[0].trim()) == null) {
	            	x = nIDCount++;
	            	nameMap.put(arr[0].trim(), x);
	            } else {
	            	x = nameMap.get(arr[0].trim());
	            }
	            	
	
	            if (nameMap.get(arr[1].trim()) == null) {
	            	y = nIDCount++;
	            	nameMap.put(arr[1].trim(), y);
	            } else {
	            	y = nameMap.get(arr[1].trim());
	            }
	            
	            
	            
	            bMat[x][y] = true;
	            bMat[y][x] = true;
		        line = br.readLine();
	        
            }
	    } finally {
	        br.close();
	    } 	    	
	    
	    System.out.println("line num:"+ String.valueOf(nLineCount));

	    
	    wzylayout Wlayout = new wzylayout(bMat, dFC, 0, null, null, null, nKA, nKB, nKC);					
	    
		Wlayout.ran_layout();
		System.out.println("finish random layout!");
		Wlayout.itr_layout();		

		
		System.out.println("node num:"+ nNodeNum);
		
        double[][] pos = Wlayout.return_pos();
        for (int i = 0; i < nNodeNum; i++)
        	System.out.println(pos[i][0]+","+pos[i][1]);
		
		

	}
	
	
	public static void for_cytoscape(String sEdgeFileName, String sNodeFileName) throws IOException, InterruptedException { // the input format is like that of Cytoscape
			
	    //variables
		int nMaxNodeNum = 2000; // we set the maximum number of nodes is 20000
		Boolean[][] bMat = new Boolean[nMaxNodeNum][nMaxNodeNum];
		
		
		Map<String, Integer> nameMap = new HashMap();
		
	    int nIDCount = 0;     // count the number of the nodes
	    int nLineCount = 0;   // count the number of the edges
		
		
	    // deal with the edge file  
		//String sEdgeFileName = "D:\\zongliang\\geneterrain-7-10\\b";    //edge file
	    BufferedReader br = new BufferedReader(new FileReader(sEdgeFileName));
	    
	    int nEcActivate = 0; // denote of the exist of EC, 0 means no-existence 
	    int nIcActivate = 0; // denote of the exist of IC, 0 means no-existence

	    double [][] dIC = new double[nMaxNodeNum][nMaxNodeNum];
	    double [][] dEC = new double[nMaxNodeNum][nMaxNodeNum];

        for (int i = 0; i < nMaxNodeNum; i++)
        	Arrays.fill(bMat[i], false);
	    
	    
	    
	    try {
	        String line;
	        
	        line = br.readLine();
	        while (line != null) {
	        	nLineCount++;
	            String [] arr = line.split("\\s+");
	            
	            if (nLineCount == 1) {   // first line should include source, target, EC, IC
	            	if (arr.length > 2) {
	            		if (arr[2].trim().equals("EC")) {
	            			nEcActivate = 2;
	            		}
	            		if (arr[2].trim().equals("IC")) {
	            			nIcActivate = 2;
	            		}
	            	}
	            	if (arr.length > 3) {
	            		if (arr[3].trim().equals("EC")) {
	            			nEcActivate = 3;
	            		}
	            		if (arr[3].trim().equals("IC")) {
	            			nIcActivate = 3;
	            		}
	            	}
	            	line = br.readLine();
	            	continue;
	            }
	            
	            
	            int x,y;
	            
	            if (nameMap.get(arr[0].trim()) == null) {
	            	x = nIDCount++;
	            	nameMap.put(arr[0].trim(), x);
	            } else {
	            	x = nameMap.get(arr[0].trim());
	            }
	            	
	
	            if (nameMap.get(arr[1].trim()) == null) {
	            	y = nIDCount++;
	            	nameMap.put(arr[1].trim(), y);
	            } else {
	            	y = nameMap.get(arr[1].trim());
	            }
	            
	            bMat[x][y] = true;
	            bMat[y][x] = true;
	            
	            if (nEcActivate != 0) {
	            	dEC[y][x] = dEC[x][y] = Double.parseDouble(arr[nEcActivate].trim());
	            }
	            
	            if (nIcActivate != 0) {
	            	dIC[y][x] = dIC[x][y] = Double.parseDouble(arr[nIcActivate].trim());
	            }
	            
		        line = br.readLine();
	        
	        }
	    } finally {
	        br.close();
	        System.out.println("num of node: " + nIDCount);
	        System.out.println("num of edge: " + (nLineCount - 1));
	    } 	    			
		

	    int nFcActivate = 0; // denote of the exist of FC, 0 means no-existence 
	    double [] dFC = new double[nMaxNodeNum];
	    Arrays.fill(dFC, 0);
	    
	    int nSetActivate = 0;
	    int nSet = 0;
	    ArrayList<ArrayList<Integer>> sSet = new ArrayList<ArrayList<Integer>>();
	    Map<String, Integer> setMap = new HashMap();
	    
		if (sNodeFileName != null) {
			
			File f = new File(sNodeFileName);
	    
		
	    
			if (f.exists() && !f.isDirectory()) { 
				nLineCount = 0;
			    br = new BufferedReader(new FileReader(sNodeFileName));
			    
			    try {
			        String line;
			        
			        line = br.readLine();
			        while (line != null) {
			        	nLineCount++;
			            String [] arr = line.split("\\s+");
			            
			            if (nLineCount == 1) { // first line should include node, FC, set
			            	//System.out.println(arr[0].trim() + " " + arr[1].trim() + arr.length);
			            	if (arr.length > 1) {
			            		if (arr[1].trim().equals("FC")) {
			            			nFcActivate = 1;
			            		}
			            		
			            		if (arr[1].trim().equals("set")) {
			            			nSetActivate = 1;
			            		}
			            	}
			            	
			            	if (arr.length > 2) {
			            		

			            		if (arr[2].trim().equals("FC")) {
			            			nFcActivate = 2;
			            		}
			            	

			            		if (arr[2].trim().equals("set")) {
			            			nSetActivate = 2;
			            		}
			            	
			            	
			            	}
			            	line = br.readLine();
			            	continue;
			            }
			            
			            
			            int x;
			            
			            if (nameMap.get(arr[0].trim()) == null) {
			            	x = -1;
			            } else {
			            	x = nameMap.get(arr[0].trim());
			            }
			            	
			            //System.out.println(x + " " + arr[0].trim());
			            
			            if (x != -1 && nFcActivate != 0) {
			            	dFC[x] = Double.parseDouble(arr[nFcActivate].trim()); 
			            	//System.out.println(x + " " + arr[0].trim() + " " + dFC[x]);
			            }
			            
			            
			            if (x != -1 && nSetActivate != 0 && arr[nSetActivate] != null) {
			            	String [] sSetList = arr[nSetActivate].trim().split(",");
							for (String iS : sSetList) {
							//System.out.println(sName+":"+iS);
							
								String ss = iS.trim();
								if (setMap.get(ss) == null) {
									setMap.put(ss, nSet);
									nSet++;
								}

								int a = setMap.get(ss);
								sSet.get(a).add(x);
								
								
								
							}			            
						}
			            
			            
				        line = br.readLine();
			        
			        }
			    } finally {
			        br.close();
			    } 	    				    
				

			    
			    
			    
			    for (int i = 0; i < nIDCount; i++) {
			    	if (nFcActivate != 0 && dFC[i] == 0) {
			    		System.out.println((i+1) + " FC should not be 0");
			    		dFC[i] = 0.0001;
			    		System.exit(0);
			    	}
			    }
		
				
			}
		}
			
		int nKA = 1;
		int nKB = nIDCount;
		int nKC = 0;
		
		
		Boolean[][] bMMat = new Boolean[nIDCount][nIDCount];
		
		// creat a new matrix according to the number of nodes
		for (int i = 0; i < nIDCount; i++)
			for (int j = 0; j < nIDCount; j++)
				bMMat[i][j] = bMat[i][j];
		

		if (nFcActivate == 0) dFC = null;
		if (nEcActivate == 0) dEC = null;
		if (nIcActivate == 0) dIC = null;
		if (nSet == 0) sSet = null;
		

        wzylayout Wlayout = new wzylayout(bMMat, dFC, 0, sSet, dIC, dEC, nKA, nKB, nKC);	// creat a object    
		
		
		Wlayout.ran_layout();
		System.out.println("finish random layout!");
		//Wlayout.itr_layout();		
		Wlayout.itr_layout_multi();		

		
		Iterator it = nameMap.entrySet().iterator();
		
        double[][] pos = Wlayout.return_pos();
        for (int i = 0; i < nIDCount; i++) {
        	Map.Entry pair = (Map.Entry)it.next();
        	System.out.println(pair.getKey()+"," +pos[i][0]+","+pos[i][1]);
        	it.remove();
        }
		
	    
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		for_cheha();
		
	
	}
}

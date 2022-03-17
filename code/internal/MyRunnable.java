package wzylayout;

import java.util.ArrayList;

public class MyRunnable extends Thread {

    //private int var;
    private double[][] dMat;    // n*n PPI score matrix
    private double[][] dPos;    // n*2 matrix to show the positions of nodes
    private Boolean[][] bMat;   // n*n boolean matrix to show which two nodes are connected, 1 connected, 0 non-connected

    private double[] dFC;       // n-d vector to record the nodes' FCs 

    
    private int nKA;            
    private int nKB;
    private int nKC;

    private int nNodeNum;       // node number n
    private int nSetNum;        // set number
    private ArrayList<ArrayList<Integer>> nSet;   // the lists of sets 
    private double[][] dSetPos; // n*2 matrix to show the positions of the centers for each set

    private int nSearNum;
    private double[] dSear;
    
    private int iBegin, iEnd, iStep;
    
    private double[][] dPosBac;    // n*2 matrix to show the positions of nodes, used for multi-threads
    
    private double[] dDisConstraint;
    
    public MyRunnable(double[][] dMat, double[][] dPos,  Boolean[][] bMat, double[] dFC, int nKA, int nKB, int nKC, int nNodeNum, int nSetNum, ArrayList<ArrayList<Integer>> nSet, double[][] dSetPos, int nSearNum, double[] dSear, double[][] dPosBac, int iBegin, int iEnd, int iStep) {
    	this.dMat = dMat;
    	this.dPos = dPos;
    	this.bMat = bMat;
    	this.dFC = dFC;

    	this.nKA = nKA;
    	this.nKB = nKB;
    	this.nKC = nKC;
    	this.nNodeNum = nNodeNum;
    	this.nSetNum = nSetNum;
    	
    	this.nSet = nSet;
    	this.dSetPos = dSetPos;
    	this.nSearNum = nSearNum;
    	this.dSear = dSear;
    	this.dPosBac = dPosBac; 
    	this.iBegin = iBegin; 
    	this.iEnd = iEnd;
    	this.iStep = iStep;    	
        
        this.dDisConstraint = new double[this.nNodeNum];
    }

    
    public double[] return_pos(int i) {
    	return dPosBac[i];
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
    
    
    public double dis(double a, double b)    // calculate the distance between two single values
    {
    	return Math.sqrt(a * a + b * b);
    }
    
    public double dis_cal(double[] a, double[] b)  // calculate the Euclidean distance between two nodes
    {
    	return Math.sqrt((a[0]-b[0]) * (a[0]-b[0]) + (a[1]-b[1]) * (a[1]-b[1]));
    }

    
	public int binary_search(int l, int r, double x, double y, int i, double dEnergy) { //binary search for the best step length
		if (l > r) return 0;
		int mid = (l+r) / 2;
		dPos[i][0] = dPos[i][0] + x * dSear[mid];
		dPos[i][1] = dPos[i][1] + y * dSear[mid];
		
		if (cal_energy(i) < dEnergy && dis(x * dSear[mid], y * dSear[mid]) < dDisConstraint[i]) {
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
    

    public boolean check_in_set(int x, int s) {     // chech if the node is inside the set
    	for (int i = 0; i < nSet.get(s).size(); i++) {
    		if (x == nSet.get(s).get(i)) return true;
    	}
    	return false;
    }	
	
    
    public void cal_dis_constraint(int x) {
    	double dMinDis = 0;
    	boolean u = false;
    	for (int i = 0; i < this.nNodeNum; i++) 
    	if (bMat[x][i] && i != x){
    		double dDis = dMat[x][i] - dis_cal(dPos[x], dPos[i]);
    		if (!u || dMinDis > (dDis / 2 - 0.000000001)) {
    			dMinDis = dDis / 2 - 0.000000001;
    			u = true;
    		}
    	}
    		
    	dDisConstraint[x] = dMinDis;
    }
    
    public void run() {
        // code in the other thread, can reference "var" variable
    	for (int i = this.iBegin; i < this.iEnd; i+= this.iStep) {
    		
    		cal_dis_constraint(i);
    		
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
			dPosBac[i][0] = dPosBac[i][0] + x * dSear[t];
			dPosBac[i][1] = dPosBac[i][1] + y * dSear[t];    	
    	}
    	
    }
}
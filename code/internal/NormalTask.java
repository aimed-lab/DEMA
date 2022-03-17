package org.cytoscape.sample.internal;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.AbstractLayoutTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.undo.UndoSupport;

public class NormalTask extends AbstractLayoutTask{
	public NormalTask(String s1, CyNetworkView networkView, Set<View<CyNode>> nodesToLayOut, String attrName, UndoSupport undo, NormalContext myContext) {
		super(s1, networkView, nodesToLayOut, attrName, undo);
		this.myContext = myContext;
	}

	//@Tunable(description="Parameter KA:")
	public int nKA = 1;

	@Tunable(description="Parameter Kb/Ka:")
	public int nKB = networkView.getModel().getNodeCount();
	
	@Tunable(description="Parameter KC:")
	public int nKC = networkView.getModel().getDefaultNodeTable().getColumn("set") == null ? 0 : 1 * networkView.getModel().getNodeCount() * 10;
	
	private NormalContext myContext;	
	@Override
	protected void doLayout(TaskMonitor taskMonitor) {
		double currX = 0.0d;
		double currY = 0.0d;
		
		System.out.println("KB paramter:"+nKB);
		//System.out.println("KA paramter:"+lastName);
		
		final VisualProperty<Double> xLoc = BasicVisualLexicon.NODE_X_LOCATION;
		final VisualProperty<Double> yLoc = BasicVisualLexicon.NODE_Y_LOCATION;
		
		//Random randomGenerator = new Random();
		
		CyNetwork netWork = networkView.getModel();
		System.out.println("node number:"+netWork.getNodeCount());
		
		
		
		
		
		//List<CyNode> list = netWork.getNodeList();
		
		CyTable nodeTable = netWork.getDefaultNodeTable();
		
		List<CyRow> list = nodeTable.getAllRows();
		
		//construct mapping
		Map<String, Integer> nameMap = new HashMap();
		Map<String, Integer> suidMap = new HashMap();
		
		
		int suidCount = 0;
		for (CyRow r : list) {
			String s = r.get("name", String.class);
			Integer suid = r.get("SUID", Long.class).intValue();
			nameMap.put(s,suid);
			suidMap.put(suid.toString(), suidCount);
			
			suidCount = suidCount + 1;
			
			
			
			
		}
		
		
		//List<CyNode> list = netWork.getNodeList();
		
		CyTable edgeTable = netWork.getDefaultEdgeTable();
		
		List<CyRow> edgeList = edgeTable.getAllRows();
	
		Boolean[][] bMat = new Boolean[netWork.getNodeCount()][netWork.getNodeCount()];
		
		double[][] dICMat = new double[netWork.getNodeCount()][netWork.getNodeCount()];
		double[][] dECMat = new double[netWork.getNodeCount()][netWork.getNodeCount()];
		
		String[] sNodeColor = new String[netWork.getNodeCount()];
		
		
		for (int i = 0; i < bMat.length; i++)
			Arrays.fill(bMat[i], false);
		
		CyColumn CICCol = edgeTable.getColumn("IC");
		Boolean bICCol = false;
		if (CICCol == null) {
			System.out.println("no IC column");
		} else {
			bICCol = true;
			System.out.println("IC column exists");
		}
			
		CyColumn CECCol = edgeTable.getColumn("EC");
		Boolean bECCol = false;
		if (CECCol == null) {
			System.out.println("no EC column");
		} else {
			bECCol = true;
			System.out.println("EC column exists");
		}
		
		
		
		for (CyRow r : edgeList) {
			String sInteraction = r.get("interaction", String.class);
			String sName = r.get("name", String.class);
			String[] sList = sName.split("\\("+sInteraction+"\\)");
			
			//找到基因的情况还没处理
			int a = suidMap.get(nameMap.get(sList[0].trim()).toString());
			int b = suidMap.get(nameMap.get(sList[1].trim()).toString());
			bMat[a][b] = true;
			bMat[b][a] = true;
			
			if (bICCol) {
				double d1 = r.get("IC", Double.class);
				dICMat[a][b] = d1;
				dICMat[b][a] = d1;
			}
			
			if (bECCol) {
				double d2 = r.get("EC", Double.class);
				dECMat[a][b] = d2;
				dECMat[b][a] = d2;
			}
		
		}
		
		
		// color
		Boolean bColorCol = false;
		CyColumn CColorCol = nodeTable.getColumn("color");
		if (CColorCol == null) {
			System.out.println("no color column");
		} else {
			bColorCol = true;
			System.out.println("color column exists");
			
		}		
		if (bColorCol == false) {
			for (int i = 0; i < netWork.getNodeCount(); i++)
				sNodeColor[i] = "white";
			
		} else {

			
			List<CyRow> nodeList = nodeTable.getAllRows();
			for (CyRow r : nodeList) {

				String sName = r.get("name", String.class);
				String sColor = r.get("color", String.class);
				if (sColor == null) sNodeColor[suidMap.get(nameMap.get(sName.trim()).toString())]  = "white";
				else sNodeColor[suidMap.get(nameMap.get(sName.trim()).toString())] = sColor;
				//dFC[suidMap.get(nameMap.get(sName.trim()).toString())] = dfc;
			}
		}		
		
		
		
		// fold change
		double[] dFC = new double[netWork.getNodeCount()];
		CyColumn CFCCol = nodeTable.getColumn("FC");

		Boolean bFCCol = false;
		if (CFCCol == null) {
			System.out.println("no FC column");
		} else {
			bFCCol = true;
			System.out.println("FC column exists");
		}
		
		Arrays.fill(dFC, 1);
		if (bFCCol == false) {
		} else {
			List<CyRow> nodeList = nodeTable.getAllRows();
			for (CyRow r : nodeList) {

				String sName = r.get("name", String.class);
				Double dfc = r.get("FC", Double.class);
				dFC[suidMap.get(nameMap.get(sName.trim()).toString())] = dfc;
			}
		}
		
		
		CyColumn CSetCol = nodeTable.getColumn("set");
		Boolean bSetCol = false;
		if (CSetCol == null) {
			System.out.println("no set column");
		} else {
			bSetCol = true;
			System.out.println("set column exists");
		}		
		
		ArrayList<ArrayList<Integer>> sSet = new ArrayList<ArrayList<Integer>>();
		int nSet;
		if (bSetCol == false) {
			nSet = 0;
			sSet = null;
		} else {
			Map<String, Integer> setMap = new HashMap();
			List<CyRow> nodeList = nodeTable.getAllRows();
			nSet = 0;
			for (CyRow r : nodeList) {

				String sName = r.get("name", String.class);
				System.out.println(sName);
				String sSetList1 = r.get("set", String.class);
				if (sSetList1 == null) continue;
				//System.out.println(sName+":"+sSetList1);
				//System.out.println(sSetList1.toString());
				String[] sSetList = sSetList1.split(",");
				for (String iS : sSetList) {
				System.out.println(sName+":"+iS);
				
					String ss = iS.trim();
					if (setMap.get(ss) == null) {
						setMap.put(ss, nSet);
						nSet++;
					} 
				}
				
				
			}
			System.out.println("set number:"+nSet);
			for (int i = 0; i < nSet; i++)
				sSet.add(new ArrayList<Integer>());
			for (CyRow r : nodeList) {

				String sName = r.get("name", String.class);
				String sSetList1 = r.get("set", String.class);
				if (sSetList1 == null) continue;
				String[] sSetList = sSetList1.split(",");
				for (String iS : sSetList) {
					String ss = iS.trim();
					int a = setMap.get(ss);
					sSet.get(a).add(suidMap.get(nameMap.get(sName.trim()).toString()));
						
				}
				
				
			}
			
		}
		
		
		wzylayout Wlayout;
		if (bICCol && bECCol)
			Wlayout = new wzylayout(bMat, dFC, nSet, sSet, dICMat, dECMat, nKA, nKB, nKC);
		else if (bICCol) {
			Wlayout = new wzylayout(bMat, dFC, nSet, sSet, dICMat, null, nKA, nKB, nKC);
		} else if (bECCol) {
			Wlayout = new wzylayout(bMat, dFC, nSet, sSet, null, dECMat, nKA, nKB, nKC);					
		} else {
			Wlayout = new wzylayout(bMat, dFC, nSet, sSet, null, null, nKA, nKB, nKC);					
		}
		
		if (netWork.getNodeCount() >= 1000)
			Wlayout.mds_layout();
		else
			Wlayout.ran_layout();
		
			Wlayout.itr_layout();
		
		
		int n = netWork.getNodeCount();
        double[][] dPos = Wlayout.return_pos();
        for (int i = 0; i < n; i++)
        	System.out.println("original position:"+dPos[i][0]+","+dPos[i][1]);
		
		
        double dXMin = dPos[0][0];
        double dXMax = dPos[0][0];
        double dYMin = dPos[0][1];
        double dYMax = dPos[0][1];
        
        for (int i = 1; i < n; i++) {
        	dXMin = Math.min(dXMin, dPos[i][0]);
        	dXMax = Math.max(dXMax, dPos[i][0]);
        	dYMin = Math.min(dYMin, dPos[i][1]);
        	dYMax = Math.max(dYMax, dPos[i][1]);
        }
        double dXMid = (dXMax + dXMin) / 2;
        double dYMid = (dYMax + dYMin) / 2;
        double dMaxLen = Math.max(dXMax-dXMin, dYMax-dYMin);
        
		/*for (CyNode n : list) {
			System.out.println(n.getSUID().toString());
			CyRow row = nodeTable.getRow(n);
			//Map<String,Object> map = row.getAllValues(); 

			
			//for (Map.Entry<String, Object> entry : map.entrySet()) {
			//	System.out.println(entry.getKey());
			//}
		}*/
		
		
		
		// Set visual property.
		for (final View<CyNode> nView : nodesToLayOut ) { //numerate each node
			
			//get the node and network
			CyNode node = nView.getModel(); 
			//System.out.println("node:"+node.getSUID());
			
			//CyRow row = nodeTable.getRow(node.getSUID());
			//System.out.println("row:"+row.get("name", String.class));
			//CyNetwork netWork = node.getNetworkPointer();
			
			//get the node table
			//CyTable nodeTable = network.getDefaultNodeTable();
			
			//CyRow row = nodeTable.getRow(node);
			
			//Map<String,Object> map = row.getAllValues(); 
			
			// Shift current nodeView to a new position based on a random value
			//currX = nView.getVisualProperty(xLoc) + myContext.XRange * randomGenerator.nextDouble();
			//currY = nView.getVisualProperty(yLoc) + myContext.YRange * randomGenerator.nextDouble();
			//String s = nView.getSUID();
			//currX = nView.getVisualProperty(xLoc) + myContext.XRange * nView.getSUID().doubleValue();
			//currY = nView.getVisualProperty(yLoc) + myContext.YRange * nView.getSUID().doubleValue();
			int nSuid = suidMap.get(node.getSUID().toString());
			double dExpendFactor = 0.1;
			currX = myContext.XRange * (dPos[nSuid][0] - dXMid) / dMaxLen / dExpendFactor;
			currY = myContext.YRange * (dPos[nSuid][1] - dYMid) / dMaxLen / dExpendFactor;
			System.out.println("Adjusted Position:"+(dPos[nSuid][0] - dXMid) / dMaxLen+","+(dPos[nSuid][1] - dYMid) / dMaxLen);
			//for (Map.Entry<String, Object> entry : map.entrySet()) {
				
				//JOptionPane.showMessageDialog(null, entry.getKey()+":"+entry.getValue().toString(), "debug", JOptionPane.INFORMATION_MESSAGE);
				//JOptionPane.showMessageDialog(null, entry.getValue().toString(), "debug", JOptionPane.INFORMATION_MESSAGE);
			//	System.out.println(entry.getValue().toString());
			//}
			
			//System.out.println(netWork.getNodeCount());
			//JOptionPane.showMessageDialog(null, String.valueOf(nView.getSUID()), "debug", JOptionPane.INFORMATION_MESSAGE);
			nView.setVisualProperty(xLoc,currX); // the position of node
			nView.setVisualProperty(yLoc,currY);
			
			switch (sNodeColor[nSuid]) {
				case "blue":
					nView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR,Color.BLUE);
					break;
				case "red":
					nView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR,Color.RED);
					break;
				case "green":
					nView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR,Color.GREEN);
					break;
				case "pink":
					nView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR,Color.PINK);
					break;
				default:
					nView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR,Color.BLUE);
					
			}
			nView.setVisualProperty(BasicVisualLexicon.NODE_BORDER_PAINT,Color.BLACK);
			
			nView.setVisualProperty(BasicVisualLexicon.NODE_BORDER_WIDTH,5.0);
		}
	}



}

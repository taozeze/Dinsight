import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;


public class KNN_basic {

	private static String workingDirectory;
	private static boolean force_reprepare_data;
	private static boolean use_fixed_seed;
	private static HashMap<String, ArrayList<String[]>> datasets;
	private static String[] features;

	private static void load_data() {		
		if(force_reprepare_data||!loadData("KNN_train")||!loadData("KNN_valid")||!loadData("KNN_test")){
			if(loadData("train")){
				ArrayList<String[]> allData = datasets.get("train");
				ArrayList<String[]> trainData = new ArrayList<String[]>();
				ArrayList<String[]> validData = new ArrayList<String[]>();
				ArrayList<String[]> testData = new ArrayList<String[]>();
				ArrayList<ArrayList<String[]>> datapool = new ArrayList<ArrayList<String[]>>();
				datapool.add(trainData);
				datapool.add(validData);
				datapool.add(testData);
				
				Random generator;
				if(use_fixed_seed){
					generator = new Random(0);
				}else{
					generator = new Random();
				}
				int idx;
				for(String[] tmp:allData){
					idx = generator.nextInt(datapool.size());
					datapool.get(idx).add(tmp);
					if(datapool.get(idx).size()>allData.size()/3) datapool.remove(idx);
				}
				saveData("KNN_train",trainData);
				saveData("KNN_valid",validData);
				saveData("KNN_test",testData);
				
				datasets.put("KNN_train",trainData);
				datasets.put("KNN_valid",validData);
				datasets.put("KNN_test",testData);
			}else{
				System.out.println("Data prepare failded.");
			}
		}
	}

	private static boolean loadData(String fileName) {
		File data = new File(workingDirectory + "/data/algebra_2005_2006_"+fileName+".txt");
		try {
			ArrayList<String[]> dataset = new ArrayList<String[]>();
			BufferedReader br = new BufferedReader(new FileReader(data));
			String str = br.readLine();
			features = (str.split("\t"));
			while((str = br.readLine())!=null){
				if(!str.equals("")){
					String[] tmps = str.split("\t",features.length);
					dataset.add(tmps);
				}
			}
			datasets.put(fileName,dataset);
			br.close();
		} catch (Exception e) {
			return false;
		} 
		return true;
	}

	private static void saveData(String fileName, ArrayList<String[]> savedata) {
		File data = new File(workingDirectory + "/data/algebra_2005_2006_"+fileName+".txt");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(data));
			writeArray(bw,features);
			for(String[] tmp:savedata){
				writeArray(bw,tmp);
			}
			bw.close();
		} catch (IOException e) {
		}		
	}

	private static void writeArray(BufferedWriter bw, String[] array) throws IOException {
		for(int i=0;i<array.length;i++){
			if(i!=0) bw.write("\t");
			bw.write(array[i]);
		}
		bw.write("\n");		
	}

	private static void computeCorrelations() {
		HashMap<String, HashMap<String,String>> sid_step_cfa = reconstructData("KNN_train");
		HashMap<String, HashMap<String,Double>> correlations = new HashMap<String, HashMap<String,Double>>();
		for(String s1:sid_step_cfa.keySet()) correlations.put(s1, new HashMap<String,Double>());
		for(String s1:sid_step_cfa.keySet()){
			for(String s2:sid_step_cfa.keySet()){
				if(!correlations.get(s1).containsKey(s2)) {
					Double correlation = getCorrelation(sid_step_cfa,s1,s2,12.9,6.2,-1.9);
					correlations.get(s1).put(s2, correlation);
					correlations.get(s2).put(s1, correlation);
				}
			}	
		}		
	}
	
	private static double getCorrelation(HashMap<String, HashMap<String, String>> sid_step_cfa, String s1, String s2,double alpha,double delta,double gamma) {
		HashMap<String, String> set_s1 = sid_step_cfa.get(s1);
		HashMap<String, String> set_s2 = sid_step_cfa.get(s1);
		HashSet<String> set_s1s2 = new HashSet<String>();
		for(String step:set_s1.keySet()) if(set_s2.keySet().contains(step)) set_s1s2.add(step);
		double us1 = 0, us2 = 0;
		for(String step:set_s1s2){
			us1 += Double.valueOf(set_s1.get(step));
			us2 += Double.valueOf(set_s2.get(step));
		}
		us1 /= set_s1s2.size();
		us2 /= set_s1s2.size();
		double d1 = 0, d2 = 0, d3 = 0;
		for(String step:set_s1s2){
			d1 += (Double.valueOf(set_s1.get(step)) - us1) * (Double.valueOf(set_s2.get(step)) - us2);
			d2 += (Double.valueOf(set_s1.get(step)) - us1) * (Double.valueOf(set_s1.get(step)) - us1);
			d3 += (Double.valueOf(set_s2.get(step)) - us2) * (Double.valueOf(set_s2.get(step)) - us2);
		}
		double d4 = 1/(set_s1s2.size()-1);
		double rho = (d4*d1) / (Math.sqrt(d4*d2)*Math.sqrt(d4*d3));
		double rho_bar = (set_s1s2.size()*rho)/(set_s1s2.size()+alpha);
		double rho_tilde = -1/Math.expm1(-delta*rho_bar-gamma);
		return rho_tilde;
	}

	private static HashMap<String, HashMap<String,String>> reconstructData(String datasetName) {
		HashMap<String, HashMap<String,String>> data = new HashMap<String, HashMap<String,String>>();
		ArrayList<String[]> trainData = datasets.get(datasetName);
		String sid,step,cfa;
		for(String[] tmp:trainData){
			sid = tmp[1];step = tmp[5];cfa = tmp[13];
			if(!data.containsKey(sid)) data.put(sid,new HashMap<String,String>());
			data.get(sid).put(step, cfa);
		}
		return data;		
	}

	public static void main(String[] args) {
		// configurations
		workingDirectory = System.getProperty("user.dir");
		force_reprepare_data = false;
		use_fixed_seed = true;
		
		// initialize
		datasets = new HashMap<String, ArrayList<String[]>>();
		
		// load data
		load_data();
		
		// compute correlations
		computeCorrelations();
		
	
	}

}
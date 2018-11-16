import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class DataProcessing {
	public static void main(String[] args) throws IOException {
		
		List<Float> vik1300 = new ArrayList<Float>();
		List<Float> vik810 = new ArrayList<Float>();

		vik1300 = readCSV("JavaSignalProcessingVik_1300.csv");
        vik810 = readCSV("JavaSignalProcessingVik_810.csv");
        float numRatio = calculateAverageRatioOfPeaksAndValleys(vik810);
        float denomRatio = calculateAverageRatioOfPeaksAndValleys(vik1300);
        System.out.println("Ratio of ratio (810nm/1300nm): " + numRatio/denomRatio);
        
				
		List<Float> amr810 = new ArrayList<Float>();
		List<Float> amr1300 = new ArrayList<Float>();
        amr810 = readCSV("JavaSignalProcessingAmr_810.csv");
		amr1300 = readCSV("JavaSignalProcessingAmr_1300.csv");
        numRatio = calculateAverageRatioOfPeaksAndValleys(amr810);
        denomRatio = calculateAverageRatioOfPeaksAndValleys(amr1300);
        System.out.println("Ratio of 810 over 1300 is: " + numRatio/denomRatio);
	}
	
	public static float calculateAverageRatioOfPeaksAndValleys(List<Float> inputData) {
        /** Prevents loop going out of bounds when previous, next iterations and lookahead are checked **/
		// @129HZ, each sample is taken at roughly 7.8ms, we want lookahead of around 80ms so need to examine 10 elements in front
		float count = 0;
		float sumRatio = 0;
        int lookAhead = 10;
        for(int i = 1 + lookAhead; i < inputData.size() - 1 - lookAhead; i++) { 
        	float previous = inputData.get(i - 1);
        	float current = inputData.get(i);
        	float next = inputData.get(i + 1);
        	// Point by point comparison to find all local maximum. This includes random peaks that may be caused by noise
        	if(current > previous && current > next) {
        		// To check if this peak was a random peak or a real local maximum
        		// We need to confirm that the previous samples(lookahead) were also increasing
        		// If determined to be random peak, break out of loop and test next sample
        		boolean strayPeak = false;
        		for(int j = i - lookAhead; j < i-1; j++) {
        			if(inputData.get(j+1) < inputData.get(j)) {
        				strayPeak = true;
        				break;
        			}
        		}
        		if(!strayPeak) {
        			// PPG signal consists of M and Q peaks. We want to only calculate the M peaks 
        			// Test the next few samples (lookahead) and determine if it continues to decrease and does not inflect back up (characteristic of Q peak)
        			
        			boolean isMPeak = true;
        			for(int k = i; k < i + lookAhead - 1; k++) {
        				if(inputData.get(k+1) > inputData.get(k)) {
        					isMPeak = false;
        					break;
        				}
        			}
        			
        			if(isMPeak) {
        				Float min = findCorrespondingValley(inputData, i);
        				if(min != Float.MIN_VALUE) {
        					//TODO Perform standard deviation and only add to sumRatio and count if not too divergent from current data???
//        					System.out.println("The ratio is " + current/min);
        					count++;
        					sumRatio += (current/min);
        				} else {
//        					System.out.println("No valley detected for corresponding peak");
        				}
        			}
        		}
        	}
        }
        System.out.println("Signal ratio of peak/trough: " + sumRatio/count);
        return sumRatio/count;
	}
	
	public static List<Float> readCSV(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        List<Float> output = new ArrayList<Float>();
        while ((line = br.readLine()) != null && !line.isEmpty()) {
        	String[] result = line.split(",");
        	output.add(Float.parseFloat(result[2]));
        }
        br.close();
        return output;
	}
	
	@SuppressWarnings("null")
	public static float findCorrespondingValley(List<Float> list, int index) {
		int start = index;
		int next = start + 1;
		while(list.get(next) < list.get(start) && start < list.size() - 2) {
			start = next;
			next++;
		}
		// Edge case where data is still trending down but data recording has stopped
		if(list.get(start) < list.get(start+1)) {
			return list.get(start);
		}
		return Float.MIN_VALUE;
	}
}

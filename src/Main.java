import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
public class Main {

	/**
	 * Calls parser to embed the input into the program
	 * Creates the cpu object and starts the execution.
	 * Displays the statistics. 
	 */
	public static void main(String[] args) throws IOException{
		
		String code = parser(args[0]);   //input file is given as an argument
		RiscvCpu cpu = new RiscvCpu(code);   
		cpu.run();
		cpu.showStats();
		
	}
	
	/**
	 * Opens the input file
	 * Stores the content of the file (riscv code) into a string
	 * @param inputFile the name of input file
	 * @return Code as a string
	 */
	private static String parser(String inputFile) throws FileNotFoundException, IOException {
		String fileAsString;
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();
		while (line != null) {
			// got rid of comments in Assembly code
			if (line.indexOf(';') != -1) {
				line = line.substring(0, line.indexOf(';'));
			}
			sb.append(line).append("\n");
			line = br.readLine();
		}
		fileAsString = sb.toString(); // this is the whole assembly code as a String
		br.close();
		return fileAsString;
	}
}

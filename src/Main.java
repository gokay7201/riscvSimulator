import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
public class Main {

	
	public static void main(String[] args) throws IOException{
		String code = parser("input.txt");
		
		RiscvCpu obj = new RiscvCpu(code);
		/*
		for(int i = 0; i < 32; i++) {
			System.out.print("here we are at step" + i);
			System.out.println(" and what we have at this specific part of memory is " + obj.InstructionMemory[i]);
		}
		
		System.out.println(obj.labels.get("loop"));
		*/
		
		System.out.println("Instruction type in IF/ID: "+obj.IF_ID_type);
		System.out.println("Instruction type in ID/EX: "+obj.ID_EX_type);
		System.out.println("Rd in IF/ID: "+obj.IF_ID_rd);
		System.out.println("Rd in ID/EX: "+obj.ID_EX_rd);
		System.out.println("Rs1 in IF/ID: "+obj.IF_ID_rs1);
		System.out.println("Rs1 in ID/EX: "+obj.ID_EX_rs1);
		System.out.println("Rs2 in IF/ID: "+obj.IF_ID_rs2);
		System.out.println("Rs2 in ID/EX: "+obj.ID_EX_rs2);
		
		
		
		//System.out.println(code);
		
		
	}
	
	private static String parser(String xx) throws FileNotFoundException, IOException {
		String fileAsString;
		BufferedReader br = new BufferedReader(new FileReader(xx));
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

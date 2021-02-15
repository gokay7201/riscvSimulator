import java.util.Scanner;
import java.util.HashMap;
import java.util.ArrayList;


/**
 * The class of the CPU object that keeps the instruction memory,
 * data memory, registers and intermediate registers. In short, 
 * an instance of RiscvCpu keeps all the modules in the pipeline.
 */
public class RiscvCpu {

	public HashMap<String, Integer> labels = new HashMap<>();// map to find the memory locations of the labels
	
	int numberOfInstructions = 0;  // Number of instructions in the given code
	
	// Instructions are kept in this string array with groups of 4 (1. Instrction type / 2.Rd / 3.Rs1 / 4. Rs2 or immidiate)
	public String[] InstructionMemory = new String[4*1024]; 

	public long[] DataMemory = new long[65536];   //Data memory that keeps 65536 doublewords, implicit data alignment

	public int pc = -4;   //program counter(instructionFetch uses it, but instruction decode increments it by 4 at first so it starts with 0)
	public int old_pc = -4;   //keeps the pc of last execution (this is how we catch stalls and count)

	public long[] registers = new long[32];  //registers[i] = value of xi
	

	/**
	 * Intermediate Registers
	 */
	public String IF_ID_type="nop";     //type of instruction in IF_ID initiated as nop (when pipeline is empty at first, all stages should apply nop)
	public String ID_EX_type="nop";	//type of instruction in IF_EX initiated as nop
	public String EX_MEM_type="nop";	//type of instruction in EX_MEM initiated as nop
	public String MEM_WB_type="nop";	//type of instruction in MEM_WB initiated as nop
	public String WB_END_type="nop";    //we added this regsiter to store type in last stage

	public String IF_ID_rd;  //second token of the instruction    (instr_type rd, rs1, rs2  or instr_type rd, imm(rs1))
	public String IF_ID_rs1;  //third token of the instruction
	public String IF_ID_rs2;  //last token of the instruction   (immidiate in ld and sd)
	public boolean IF_ID_flush=false;  //flush signal   
	public boolean IF_ID_putStall=false;   //stall signal
	//Hazard detection is conducted in ID and EX generally, therefore stall and flush decisions should be signaled to IF/ID. 

	public int ID_EX_rd;  //enumeration of the second token of the instruction  (if x3->3, if x6->6)
	public long ID_EX_rs1;  //decoded value of the third token of the instruction   (if x3->registers[3] if x6->registers[6])
	public long ID_EX_rs2;  //decoded value of the last token of the instruction   (immidiate in ld and sd)
	public long ID_EX_storeData; // keeps the data to store (used only for store operation)
	public int ID_EX_rs1_id; // not the value but the id of rs1 register (enumeration)
	public int ID_EX_rs2_id;// not the value but the id of rs2 register
	

	public int EX_MEM_rd;  //enumeration of the second token of the instruction  (if x3->3, if x6->6)
	public long EX_MEM_aluResult; // the result of the ALU operation
	public long EX_MEM_storeData; //  keeps the data to store (used only for store operation)

	public int MEM_WB_rd;   //enumeration of destination register
	public long MEM_WB_wbData; // writeback data which we put it into the rd of this stage, later

	public int WB_END_rd;
	public long WB_END_wbData;
	
	//PC of the carried instruction in the intermediate registers, useful when indicating the reason of a stall
	public int IF_ID_PC; 
	public int ID_EX_PC;
	public int EX_MEM_PC;
	public int MEM_WB_PC;
	public int WB_END_PC;

	public int cycle = 0; // cycle counter

	public int executedIns = 0;   //number of executed instructions (stalls are not counted, jumped instructions are not counted)

	public int stall=0;  //stall counter

	Executor ex;   //executor object which brings the implementations of module functions

	
	// keep tracks of the PCs of the instructions that causes stall
	public ArrayList<Integer> stallPC = new ArrayList<Integer>(); // stall Pcs found


	
	public RiscvCpu(String code){
		
		//tokenizing the input
		tokenize(code);

		//Initialization of data structures
		for(int i=0;i<32;i++){
			registers[i]=0;
		}
		for(int i=0;i<65536;i++){
			DataMemory[i]=0;
		}
		//  DataMemory[0] = 3;
		// DataMemory[1] = 6;
		// DataMemory[2] = 3;
		 // DataMemory[10] = 105;
		 // DataMemory[6] = 7;

       
		ex=new Executor(this);
		
	}


	/**
	 * Simulates the cycles with a while loop
	 * 5 Pipeline stages executes in each cycle. 
	 * Even though we simulated the stages sequentially, we preserved their parallel dependencies
	 * For forwarding purposes, the sequential order of the stages are from right to left
	 * When all stages have nop, it means the program is termminated, it is the exit condition. 
	 */
	public void run(){
		while(true){
			ex.writebackOperations();
			ex.memoryOperations();
			ex.executionALU();
			ex.instructionDecode();
			ex.instructionFetch();
			if(IF_ID_type.equals("nop") && ID_EX_type.equals("nop") && EX_MEM_type.equals("nop") && MEM_WB_type.equals("nop") && WB_END_type.equals("nop")){
				break;
			}
			cycle++;
		}
	}

	/**
	 * CPI, number of stalls, number of cycles are displayed.
	 * Register contents are also printed.
	 */
	public void showStats(){
		System.out.print("Registers: ");
		for(int i=0;i<32;i++){
			System.out.print(registers[i]+" ");
		}
		System.out.println();
		System.out.println("Cycle: "+cycle);
		System.out.println("Number of Instructions: "+executedIns);
		System.out.println("CPI: "+((double)cycle/executedIns));
		System.out.println("Stall: "+stall);
		System.out.println("Instructions that causes stall:");
		
		int tempPC ;
		String tempType;
		for(int i = 0; i< stallPC.size();i++){
			tempPC =  stallPC.get(i);
			tempType = InstructionMemory[tempPC];
			if(tempType.equals("ld")||tempType.equals("sd")){
				System.out.println(tempType + " " +InstructionMemory[tempPC+1]+ ", " + 
				InstructionMemory[tempPC+3] + "(" + InstructionMemory[tempPC+2] + ") at PC:" + tempPC);
			}else{
				System.out.println(tempType + " " +InstructionMemory[tempPC+1]+ ", " + 
				InstructionMemory[tempPC+2] + ", " + InstructionMemory[tempPC+3] + " at PC:" + tempPC);
			}			

			
		}
		

	}


	/**
	 * Receives the code as a string, and tokenizes it line by line.
	 * Labels and empty lines are handled
	 * Each line with regular instruction is tokenized and contents are stored in InstructionMemory.
	 * Labels are detected and stored in a map with their adresses 
	 * Since instruction memory is a global variable, it will be achieved from executor object
	 */
	public void tokenize(String code){

		Scanner scanner = new Scanner(code);
		String line;
		Scanner token;
		int indexCursor = 0;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			token = new Scanner(line);
			if (!token.hasNext()) {// for empty lines
				continue;
			}
			String first = token.next().toLowerCase(); // in case if it is given as upper case
			
			
			if(first.equals("add")|| first.equals("addi")||first.equals("sub")||first.equals("and")|| first.equals("or")||first.equals("beq")) {
				numberOfInstructions++;
				InstructionMemory[indexCursor] = first;
				String second = token.next().toLowerCase();
				InstructionMemory[indexCursor+1] = second.substring(0,second.indexOf(",")); 
				String third = token.next().toLowerCase();
				InstructionMemory[indexCursor+2] = third.substring(0,third.indexOf(",")); 
				String fourth = token.next().toLowerCase();
				InstructionMemory[indexCursor+3] = fourth; 
				
				indexCursor +=4;
			}else if(first.equals("ld") || first.equals("sd")) {
				numberOfInstructions++;
				InstructionMemory[indexCursor] = first;
				String second = token.next().toLowerCase();
				InstructionMemory[indexCursor+1] = second.substring(0,second.indexOf(",")); 
				String third = token.next().toLowerCase();
				String element1 = third.substring(0, third.indexOf('('));
				String element2 = third.substring(third.indexOf('(')+1, third.indexOf(')'));
				InstructionMemory[indexCursor+2] = element2; // which is register
				InstructionMemory[indexCursor+3] = element1; // which is immediate value
				indexCursor +=4;
			}else if(line.indexOf(":") != -1) {// it means there is a label
				line = line.trim().substring(0, line.indexOf(":"));
				labels.put(line, indexCursor);
				continue;
				
			}else {
				System.out.println("error");
				System.exit(0);
			}
			
			
			token.close();
		}
		
		scanner.close();
	}
	
	
	

}

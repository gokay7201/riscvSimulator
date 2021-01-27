import java.util.Scanner;
import java.util.HashMap;

public class RiscvCpu {
	public HashMap<String, Integer> labels = new HashMap<>();// to find the memory locations of the labels
	
	int numberOfInstructions = 0;// we are going to use it when calculating CPI
	
	public String[] InstructionMemory = new String[4*32]; // we are now supporting 32 instructions in our instruction memory (daha çok mu yapsak???)

	public int[] DataMemory = new int[65536];   //we can decide on the size of data memory

	public int pc = -4;   //program counter(instructionFetch uses it, but instruction decode increments it by 4 at first so it starts with 0)

	public int[] registers = new int[32]; //registers[i]=xi
	

	public String IF_ID_type="nop";     //type of instruction in IF_ID initiated as nop (when pipeline is empty at first, all stages should apply nop)
	public String ID_EX_type="nop";	//type of instruction in IF_EX initiated as nop
	public String EX_MEM_type="nop";	//type of instruction in EX_MEM initiated as nop
	public String MEM_WB_type="nop";	//type of instruction in MEM_WB initiated as nop

	public String IF_ID_rd;  //second token of the instruction
	public String IF_ID_rs1;  //third token of the instruction
	public String IF_ID_rs2;  //last token of the instruction   (immidiate in ld and sd)
	public boolean IF_ID_flush=false;  //flushing requirement will be signaled 

	public int ID_EX_rd;  //enumeration of the second token of the instruction  (if x3->3, if x6->6)
	public int ID_EX_rs1;  //decoded value of the third token of the instruction   (if x3->registers[3] if x6->registers[6])
	public int ID_EX_rs2;  //decoded value of the last token of the instruction   (immidiate in ld and sd)
	public int ID_EX_storeData; // used only for store operation
	public int ID_EX_rs1_id; // not the value but the id of rs1 register
	public int ID_EX_rs2_id;// not the value but the id of rs2 register

	public int EX_MEM_rd;  //enumeration of the second token of the instruction  (if x3->3, if x6->6)
	public int EX_MEM_aluResult; // the result of the ALU operation
	public int EX_MEM_storeData; // used only for store operation

	public int MEM_WB_rd;
	public int MEM_WB_wbData; // writeback data which we put it into the rd of this stage, later




	public int stall=0;  //stall counter


	
	RiscvCpu(String code){
		
		//tokenizing the input
		tokenize(code);

		//Initialization of data structures
		for(int i=0;i<32;i++){
			registers[i]=0;
		}
		for(int i=0;i<65536;i++){
			DataMemory[i]=0;
		}

       //4 instructionlık deneme
		Executor ex=new Executor();
		ex.executionALU(this);
		ex.instructionDecode(this);
		ex.instructionFetch(this);
		ex.executionALU(this);
		ex.instructionDecode(this);
		ex.instructionFetch(this);
		ex.executionALU(this);
		ex.instructionDecode(this);
		ex.instructionFetch(this);
	//	ex.executionALU(this);
	//	ex.instructionDecode(this);
	//	ex.instructionFetch(this);
		//ex.executionALU(this);
		//ex.instructionDecode(this);
		//ex.instructionFetch(this);
		
	}

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
			
			// means instruction
			if(first.equals("add")|| first.equals("addi")||first.equals("sub")||first.equals("and")|| first.equals("or")||first.equals("beq")) {
				numberOfInstructions++;
				InstructionMemory[indexCursor] = first;
				String second = token.next().toLowerCase();
				InstructionMemory[indexCursor+1] = second.substring(0,second.indexOf(",")); // if no comma it can be problematic
				String third = token.next().toLowerCase();
				InstructionMemory[indexCursor+2] = third.substring(0,third.indexOf(",")); // if no comma it can be problematic
				String fourth = token.next().toLowerCase();
				InstructionMemory[indexCursor+3] = fourth; // if no comma it can be problematic
				
				indexCursor +=4;
			}else if(first.equals("ld") || first.equals("sd")) {
				numberOfInstructions++;
				InstructionMemory[indexCursor] = first;
				String second = token.next().toLowerCase();
				InstructionMemory[indexCursor+1] = second.substring(0,second.indexOf(",")); // if no comma it can be problematic
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
